package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchangecore.common.ByteUtil;
import gc.garcol.exchangecore.ringbuffer.ManyToManyRingBuffer;
import gc.garcol.exchangecore.ringbuffer.OneToManyRingBuffer;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.ControlledMessageHandler;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static gc.garcol.exchangecore.common.ClusterConstant.EMPTY_BYTE_ARRAY;

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
    private final AgentRunner clientReplyRunner;

    public ExchangeClusterStateFollower(final ExchangeCluster cluster)
    {
        this.exchangeCluster = cluster;
        var replayLogAgent = new AgentReplayLog();
        var heartBeatAgent = new AgentHeartBeat("Try to acquire leader role " + ClusterGlobal.NODE_ID);
        var clientReplyAgent = new AgentClientReply(exchangeCluster);

        ByteUtil.eraseByteBuffer(exchangeCluster.commandAcceptorBuffer.byteBuffer());
        ByteUtil.eraseByteBuffer(exchangeCluster.commandBuffer.byteBuffer());

        var manyToOneRingBuffer = new ManyToOneRingBuffer(exchangeCluster.commandAcceptorBuffer);
        var oneToManyRingBuffer = new OneToManyRingBuffer(
            exchangeCluster.commandBuffer,
            List.of(clientReplyAgent)
        );

        exchangeCluster.commandInboundRingBuffer = new ManyToManyRingBuffer(
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

        this.clientReplyRunner = new AgentRunner(
            new SleepingMillisIdleStrategy(),
            error -> log.error("Follower clientReply error", error),
            null,
            new AgentClientReply(exchangeCluster)
        );
    }

    public void start()
    {
        AgentRunner.startOnThread(heartBeatRunner);
        AgentRunner.startOnThread(replayLogRunner);
        AgentRunner.startOnThread(clientReplyRunner);
    }

    public void stop()
    {
        heartBeatRunner.close();
        replayLogRunner.close();
        clientReplyRunner.close();
    }

    @Override
    public void handleHeartBeat()
    {
        var ringBuffer = exchangeCluster.heartBeatInboundRingBuffer;

        AtomicBoolean becomeLeaderDetect = new AtomicBoolean(false);
        ringBuffer.controlledRead((messageType, buffer, offset, length) -> {
            if (becomeLeaderDetect.get())
            {
                return ControlledMessageHandler.Action.CONTINUE;
            }
            boolean tryBecomeLeaderSuccess = buffer.getByte(offset) == 1;
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

    @Override
    public boolean handleCommands(UUID sender, CommandProto.Command command)
    {
        return exchangeCluster.commandInboundRingBuffer
            .publishMessage(sender, Optional.ofNullable(exchangeCluster.currentLeader)
                .map(String::getBytes).orElse(EMPTY_BYTE_ARRAY));
    }
}
