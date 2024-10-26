package gc.garcol.exchangecore.exchangelog;

import gc.garcol.exchange.proto.MetadataProto;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.common.InternalException;

import java.io.RandomAccessFile;

/**
 * @author thaivc
 * @since 2024
 */
public class PSnapshotRepository
{

    public ESnapshotMetadata read()
    {
        try (RandomAccessFile metadataFile = new RandomAccessFile(Env.SNAPSHOT_METADATA_FILE, "r"))
        {
            var buffer = new byte[(int)metadataFile.length()];
            metadataFile.readFully(buffer);
            var data = MetadataProto.SnapshotMetadata.parseFrom(buffer);
            return new ESnapshotMetadata()
                .lastSnapshotIndex(data.getLastSnapshotIndex())
                .lastSnapshotSegment(data.getLastSnapshotSegment());
        }
        catch (Exception e)
        {
            throw new InternalException(e);
        }
    }

    public void write(ESnapshotMetadata snapshotMetadata)
    {
        var data = MetadataProto.SnapshotMetadata.newBuilder()
            .setLastSnapshotIndex(snapshotMetadata.lastSnapshotIndex())
            .setLastSnapshotSegment(snapshotMetadata.lastSnapshotSegment())
            .build();
        try (RandomAccessFile metadataFile = new RandomAccessFile(Env.SNAPSHOT_METADATA_FILE, "rw"))
        {
            metadataFile.seek(0);
            metadataFile.write(data.toByteArray());
            metadataFile.setLength(data.getSerializedSize());
        }
        catch (Exception e)
        {
            throw new InternalException(e);
        }
    }
}
