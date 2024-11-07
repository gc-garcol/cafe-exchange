package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.ClusterPayloadProto;
import gc.garcol.exchangecore.exchangelog.PLogRepository;
import gc.garcol.exchangecore.exchangelog.PSnapshotRepository;
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
    private final PLogRepository logRepository;
    private final PSnapshotRepository snapshotRepository;
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
        var snapshotMetadata = snapshotRepository.readMetadata();
        var logMetadata = logRepository.readMetadata();

        for (long segment = snapshotMetadata.lastSnapshotSegment() + 1; segment <= logMetadata.currentSegment(); segment++)
        {
            long maxOffset = logRepository.totalIndexOffset(segment);
            var offset = 0;
            while (offset < maxOffset)
            {
                var recordBytes = logRepository.read(segment, offset);

                var buffer = ByteBuffer.wrap(recordBytes);
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
