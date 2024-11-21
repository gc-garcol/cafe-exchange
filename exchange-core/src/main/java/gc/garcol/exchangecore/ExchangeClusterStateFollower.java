package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchangecore.common.*;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
public class ExchangeClusterStateFollower implements ExchangeClusterState
{
    private final ExchangeCluster exchangeCluster;

    private final AgentRunner replayLogRunner;
    private final AgentRunner heartBeatRunner;
    private final AgentRunner requestTransformerRunner;

    public ExchangeClusterStateFollower(final ExchangeCluster cluster)
    {
        this.exchangeCluster = cluster;
        var replayLogAgent = new AgentReplayLog();
        var heartBeatAgent = new AgentHeartBeat("Try to acquire leader role " + ClusterGlobal.NODE_ID);

        ByteUtil.eraseByteBuffer(exchangeCluster.requestAcceptorBuffer.byteBuffer());

        var manyToOneRingBuffer = new ManyToOneRingBuffer(exchangeCluster.requestAcceptorBuffer);
        var oneToManyRingBuffer = new OneToManyRingBuffer(
            Env.BUFFER_SIZE_REQUEST_POW,
            1 // AgentDomainMessageHandler
        );
        AgentDomainMessageHandler.CONSUMER_INDEX.set(0);

        exchangeCluster.requestRingBuffer = new ManyToManyRingBuffer(
            manyToOneRingBuffer,
            oneToManyRingBuffer
        );

        this.replayLogRunner = new AgentRunner(
            new SleepingMillisIdleStrategy(),
            error -> log.error("Follower replayLog error", error),
            null,
            replayLogAgent
        );
        this.heartBeatRunner = new AgentRunner(
            new SleepingMillisIdleStrategy(10),
            error -> log.error("Follower heartBeat error", error),
            null,
            heartBeatAgent
        );

        var agentCommandInboundTransformer = new AgentRequestTransformConsumer(exchangeCluster.requestRingBuffer);

        this.requestTransformerRunner = new AgentRunner(
            new SleepingIdleStrategy(),
            error -> log.error("Leader commandInboundTransformer error", error),
            null,
            agentCommandInboundTransformer
        );
    }

    @Override
    public void start()
    {
        AgentRunner.startOnThread(heartBeatRunner);
        AgentRunner.startOnThread(replayLogRunner);
        AgentRunner.startOnThread(requestTransformerRunner);
    }

    @Override
    public void stop()
    {
        requestTransformerRunner.close();
        heartBeatRunner.close();
        replayLogRunner.close();
    }

    @Override
    public boolean enqueueRequest(UUID sender, ClusterPayloadProto.Request request)
    {
        int messageType = request.getPayloadCase() == ClusterPayloadProto.Request.PayloadCase.COMMAND
            ? ClusterConstant.COMMAND_MSG_TYPE
            : ClusterConstant.QUERY_MSG_TYPE;
        return exchangeCluster.requestRingBuffer
            .publishMessage(messageType, sender, ClusterPayloadProto.CommonResponse
                .newBuilder()
                .setCode(StatusCode.BAD_REQUEST.code)
                .setStatus(MessageCode.FOLLOWER_CANNOT_HANDLE_REQUEST.code)
                .build().toByteArray());
    }

    @Override
    public void handleHeartBeatEvent()
    {
        var ringBuffer = exchangeCluster.heartBeatInboundRingBuffer;

        AtomicBoolean becomeLeaderDetect = new AtomicBoolean(false);
        ringBuffer.controlledRead((messageType, buffer, offset, length) -> {
            if (becomeLeaderDetect.get())
            {
                return ControlledMessageHandler.Action.CONTINUE;
            }
            UUID leaderNodeId = new UUID(buffer.getLong(offset), buffer.getLong(offset + Long.BYTES));
            boolean tryBecomeLeaderSuccess = Objects.equals(leaderNodeId, ClusterGlobal.NODE_ID);
            if (tryBecomeLeaderSuccess)
            {
                becomeLeaderDetect.set(true);
            }
            return ControlledMessageHandler.Action.COMMIT;
        });
        if (becomeLeaderDetect.get())
        {
            exchangeCluster.transitionToLeader();
        }
    }
}
