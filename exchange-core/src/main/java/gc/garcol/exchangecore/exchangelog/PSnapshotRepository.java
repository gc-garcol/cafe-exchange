package gc.garcol.exchangecore.exchangelog;

import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.common.InternalException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * @author thaivc
 * @since 2024
 */
public class PSnapshotRepository
{

    private final ByteBuffer cachedBuffer = ByteBuffer.allocate(Long.BYTES * 2);

    public ESnapshotMetadata readMetadata()
    {
        try (RandomAccessFile metadataFile = new RandomAccessFile(Env.SNAPSHOT_METADATA_FILE, "r"))
        {
            var buffer = new byte[Long.BYTES * 2];
            metadataFile.readFully(buffer);
            ByteBuffer data = ByteBuffer.wrap(buffer);
            return new ESnapshotMetadata()
                .lastSnapshotIndex(data.getLong())
                .lastSnapshotSegment(data.getLong());
        }
        catch (FileNotFoundException e)
        {
            return new ESnapshotMetadata();
        }
        catch (IOException e)
        {
            throw new InternalException(e);
        }
    }

    public void writeMetadata(ESnapshotMetadata snapshotMetadata)
    {
        cachedBuffer.clear();
        cachedBuffer.putLong(0, snapshotMetadata.lastSnapshotSegment());
        cachedBuffer.putLong(Long.BYTES, snapshotMetadata.lastSnapshotIndex());
        cachedBuffer.flip();
        try (RandomAccessFile metadataFile = new RandomAccessFile(Env.SNAPSHOT_METADATA_FILE, "rw"))
        {
            metadataFile.seek(0);
            metadataFile.write(cachedBuffer.array());
        }
        catch (Exception e)
        {
            throw new InternalException(e);
        }
    }
}
