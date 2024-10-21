package gc.garcol.exchangecore;

import gc.garcol.exchangecore.common.ByteUtil;
import gc.garcol.exchangecore.ringbuffer.ManyToManyRingBuffer;
import gc.garcol.exchangecore.ringbuffer.OneToManyRingBuffer;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.List;
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
        var replayLogAgent = new AgentReplayLogImmediate(ExchangeIOC.SINGLETON.getInstance(StateMachineDelegate.class));

        ByteUtil.eraseByteBuffer(exchangeCluster.commandAcceptorBuffer.byteBuffer());
        ByteUtil.eraseByteBuffer(exchangeCluster.commandBuffer.byteBuffer());

        var manyToOneRingBuffer = new ManyToOneRingBuffer(exchangeCluster.commandAcceptorBuffer);
        var oneToManyRingBuffer = new OneToManyRingBuffer(
            exchangeCluster.commandBuffer,
            List.of(journalerAgent, replayLogAgent)
        );

        exchangeCluster.commandInboundRingBuffer = new ManyToManyRingBuffer(
            manyToOneRingBuffer,
            oneToManyRingBuffer
        );

        var agentCommandInboundTransformer = new AgentCommandInboundTransformer(exchangeCluster.commandInboundRingBuffer);

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

    public void start()
    {
        AgentRunner.startOnThread(commandInboundTransformerRunner);
        AgentRunner.startOnThread(heartBeatRunner);
        AgentRunner.startOnThread(journalerRunner);
        AgentRunner.startOnThread(replayLogRunner);

        ClusterGlobal.ENABLE_COMMAND_INBOUND.set(true);
    }

    public void stop()
    {
        ClusterGlobal.ENABLE_COMMAND_INBOUND.set(false);
        commandInboundTransformerRunner.close();
        heartBeatRunner.close();
        journalerRunner.close();
        replayLogRunner.close();
    }

    public void handleHeartBeat()
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
