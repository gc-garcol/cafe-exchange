package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.walcore.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
@RequiredArgsConstructor
public class StateMachineLoader implements StateMachinePersistable
{
    private final LogRepository logRepository;
    private final StateMachineDelegate stateMachineDelegate;

    @SneakyThrows
    public void loadSnapshot()
    {
        loadSnappedData();
        replayRemainLog();
    }

    private void loadSnappedData()
    {
    }

    @SneakyThrows
    private void replayRemainLog()
    {
        long latestSegment = logRepository.getLatestSegment();
        // todo switch to latest segment
        logRepository.switchToSegment(0);

        long lastSyncSegment = -1;
        ByteBuffer buffer = ByteBuffer.allocate(1 << 18);
        for (long segment = lastSyncSegment + 1; segment <= latestSegment; segment++)
        {
            long maxOffset = logRepository.totalRecords(segment);
            var offset = 0;
            while (offset < maxOffset)
            {
                buffer.clear();
                logRepository.read(segment, offset, buffer);
                var totalMessage = buffer.getInt();
                for (int commandIndex = 0; commandIndex < totalMessage; commandIndex++)
                {
                    int messageLength = buffer.getInt();
                    byte[] messageBytes = new byte[messageLength];
                    buffer.get(messageBytes);
                    stateMachineDelegate.apply(ClusterPayloadProto.Request.parseFrom(messageBytes).getCommand());
                }
                offset++;
            }
        }
    }

    public void snapshot()
    {
    }
}
