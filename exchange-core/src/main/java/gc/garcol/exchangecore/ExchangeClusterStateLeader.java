package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchangecore.common.ByteUtil;
import gc.garcol.exchangecore.common.ClusterConstant;
import gc.garcol.exchangecore.common.ClusterGlobal;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.exchangelog.PLogRepository;
import gc.garcol.exchangecore.ringbuffer.ManyToManyRingBuffer;
import gc.garcol.libcore.OneToManyRingBuffer;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
public class ExchangeClusterStateLeader implements ExchangeClusterState
{
    private final ExchangeCluster exchangeCluster;

    private final AgentRunner requestTransformerRunner;
    private final AgentRunner heartBeatRunner;
    private final AgentRunner journalerRunner;

    public ExchangeClusterStateLeader(final ExchangeCluster cluster)
    {
        this.exchangeCluster = cluster;

        var heartBeatAgent = new AgentHeartBeat("Try to keep leader role " + ClusterGlobal.NODE_ID);

        ByteUtil.eraseByteBuffer(exchangeCluster.requestAcceptorBuffer.byteBuffer());

        var manyToOneRingBuffer = new ManyToOneRingBuffer(exchangeCluster.requestAcceptorBuffer);
        var oneToManyRingBuffer = new OneToManyRingBuffer(
            Env.BUFFER_SIZE_REQUEST_POW,
            2 // journaler then AgentDomainMessageHandler
        );
        AgentDomainMessageHandler.CONSUMER_INDEX.set(1);
        var journalerAgent = new AgentJournal(ExchangeIOC.SINGLETON.getInstance(PLogRepository.class), oneToManyRingBuffer);

        exchangeCluster.requestRingBuffer = new ManyToManyRingBuffer(
            manyToOneRingBuffer,
            oneToManyRingBuffer
        );

        var agentCommandInboundTransformer = new AgentRequestTransformConsumer(exchangeCluster.requestRingBuffer);

        this.requestTransformerRunner = new AgentRunner(
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

        AgentRunner.startOnThread(requestTransformerRunner);
        AgentRunner.startOnThread(heartBeatRunner);
        AgentRunner.startOnThread(journalerRunner);

        var networkGRpc = ExchangeIOC.SINGLETON.getInstance(NetworkGrpc.class);
        networkGRpc.start();
        ClusterGlobal.ENABLE_COMMAND_INBOUND.set(true);
    }

    @Override
    public void stop()
    {
        var networkGRpc = ExchangeIOC.SINGLETON.getInstance(NetworkGrpc.class);
        networkGRpc.stop();
        ClusterGlobal.ENABLE_COMMAND_INBOUND.set(false);
        requestTransformerRunner.close();
        heartBeatRunner.close();
        journalerRunner.close();
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

        AtomicReference<UUID> newLeaderNode = new AtomicReference<>();
        ringBuffer.controlledRead((messageType, buffer, offset, length) -> {
            if (newLeaderNode.get() != null)
            {
                return ControlledMessageHandler.Action.CONTINUE;
            }
            UUID leaderNodeId = new UUID(buffer.getLong(offset), buffer.getLong(offset + Long.BYTES));
            boolean keepLeaderStateSuccess = Objects.equals(leaderNodeId, ClusterGlobal.NODE_ID);
            if (!keepLeaderStateSuccess)
            {
                newLeaderNode.set(leaderNodeId);
            }
            return ControlledMessageHandler.Action.COMMIT;
        });
        if (newLeaderNode.get() != null)
        {
            exchangeCluster.transitionToFollower(newLeaderNode.get());
        }
    }
}
