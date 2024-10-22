package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchangecore.common.ByteUtil;
import gc.garcol.exchangecore.common.ClusterConstant;
import gc.garcol.exchangecore.ringbuffer.ManyToManyRingBuffer;
import gc.garcol.exchangecore.ringbuffer.OneToManyRingBuffer;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
public class ExchangeClusterStateLeader implements ExchangeClusterState
{
    private final ExchangeCluster exchangeCluster;

    private final AgentRunner commandInboundTransformerRunner;
    private final AgentRunner heartBeatRunner;
    private final AgentRunner journalerRunner;
    private final AgentRunner replayLogRunner;

    public ExchangeClusterStateLeader(final ExchangeCluster cluster)
    {
        this.exchangeCluster = cluster;

        var heartBeatAgent = new AgentHeartBeat("Try to keep leader role " + ClusterGlobal.NODE_ID);
        var journalerAgent = new AgentJournaler();
        var replayLogAgent = new AgentRequestConsumer(ExchangeIOC.SINGLETON.getInstance(StateMachineDelegate.class));

        ByteUtil.eraseByteBuffer(exchangeCluster.requestAcceptorBuffer.byteBuffer());
        ByteUtil.eraseByteBuffer(exchangeCluster.requestBuffer.byteBuffer());

        var manyToOneRingBuffer = new ManyToOneRingBuffer(exchangeCluster.requestAcceptorBuffer);
        var oneToManyRingBuffer = new OneToManyRingBuffer(
            exchangeCluster.requestBuffer,
            List.of(journalerAgent, replayLogAgent)
        );

        exchangeCluster.requestRingBuffer = new ManyToManyRingBuffer(
            manyToOneRingBuffer,
            oneToManyRingBuffer
        );

        var agentCommandInboundTransformer = new AgentRequestTransformConsumer(exchangeCluster.requestRingBuffer);

        this.commandInboundTransformerRunner = new AgentRunner(
            new SleepingIdleStrategy(),
            error -> log.error("Leader commandInboundTransformer error", error),
            null,
            agentCommandInboundTransformer
        );

        this.journalerRunner = new AgentRunner(
            new SleepingIdleStrategy(10_000),
            error -> log.error("Leader journaler error", error),
            null,
            journalerAgent
        );

        this.replayLogRunner = new AgentRunner(
            new SleepingIdleStrategy(),
            error -> log.error("Leader replayLog error", error),
            null,
            replayLogAgent
        );

        this.heartBeatRunner = new AgentRunner(
            new SleepingMillisIdleStrategy(50),
            error -> log.error("Leader heartBeat error", error),
            null,
            heartBeatAgent
        );
    }

    @Override
    public void start()
    {
        AgentRunner.startOnThread(commandInboundTransformerRunner);
        AgentRunner.startOnThread(heartBeatRunner);
        AgentRunner.startOnThread(journalerRunner);
        AgentRunner.startOnThread(replayLogRunner);

        ClusterGlobal.ENABLE_COMMAND_INBOUND.set(true);
    }

    @Override
    public void stop()
    {
        ClusterGlobal.ENABLE_COMMAND_INBOUND.set(false);
        commandInboundTransformerRunner.close();
        heartBeatRunner.close();
        journalerRunner.close();
        replayLogRunner.close();
    }

    @Override
    public boolean enqueueRequest(UUID sender, ClusterPayloadProto.Request request)
    {
        int messageType = request.getPayloadCase() == ClusterPayloadProto.Request.PayloadCase.COMMAND
            ? ClusterConstant.COMMAND_MSG_TYPE
            : ClusterConstant.QUERY_MSG_TYPE;
        return exchangeCluster.requestRingBuffer.publishMessage(messageType, sender, request.toByteArray());
    }

    @Override
    public void handleHeartBeatEvent()
    {
        var ringBuffer = exchangeCluster.heartBeatInboundRingBuffer;

        AtomicBoolean keepLeaderState = new AtomicBoolean(true);
        ringBuffer.controlledRead((messageType, buffer, offset, length) -> {
            if (!keepLeaderState.get())
            {
                return ControlledMessageHandler.Action.CONTINUE;
            }
            boolean keepLeaderStateSuccess = buffer.getByte(offset) == 1;
            if (!keepLeaderStateSuccess)
            {
                keepLeaderState.set(false);
            }
            return ControlledMessageHandler.Action.COMMIT;
        });
        if (!keepLeaderState.get())
        {
            exchangeCluster.transitionToFollower();
        }
    }
}
