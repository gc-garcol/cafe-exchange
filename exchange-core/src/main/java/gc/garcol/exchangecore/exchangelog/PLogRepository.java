package gc.garcol.exchangecore.exchangelog;

import gc.garcol.exchange.proto.MetadataProto;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.common.InternalException;
import lombok.SneakyThrows;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author thaivc
 * @since 2024
 */
public class PLogRepository
{
    private final ELogMetadata logMetadata;

    static ByteBuffer indexBuffer = ByteBuffer.allocate(ELogIndex.SIZE);


    @SneakyThrows
    public PLogRepository()
    {
        LogUtil.createDirectoryNX(Env.DATA_DIR);
        LogUtil.createDirectoryNX(Env.LOG_DIR);
        logMetadata = readMetadata();
    }

    public byte[] read(long segment, long index)
    {
        try (
            RandomAccessFile indexFile = new RandomAccessFile(LogUtil.indexPath(segment), "r");
            RandomAccessFile logFile = new RandomAccessFile(LogUtil.logPath(segment), "r")
        )
        {
            indexFile.seek(index * ELogIndex.SIZE);
            var logIndex = new ELogIndex()
                .index(indexFile.readLong())
                .entryLength(indexFile.readInt());
            logFile.seek(logIndex.index());
            var buffer = new byte[logIndex.entryLength()];
            logFile.readFully(buffer);
            return buffer;
        }
        catch (Exception e)
        {
            throw new InternalException(e);
        }
    }

    @SneakyThrows
    public void write(ByteBuffer commands)
    {
        var segment = logMetadata.currentSegment();
        try (
            RandomAccessFile indexFile = new RandomAccessFile(LogUtil.indexPath(segment), "rw");
            RandomAccessFile logFile = new RandomAccessFile(LogUtil.logPath(segment), "rw")
        )
        {
            FileChannel indexChannel = indexFile.getChannel();
            FileChannel logChannel = logFile.getChannel();

            indexBuffer.clear();
            indexBuffer.putLong(logFile.length());
            indexBuffer.putInt(commands.limit());
            indexBuffer.flip();

            var logOffset = logChannel.size();
            var indexOffset = indexChannel.size();
            logChannel.write(commands, logOffset);
            indexChannel.write(indexBuffer, indexOffset);

            logChannel.force(true);
            indexChannel.force(true);
        }
    }

    public long totalIndexOffset(long segment)
    {
        try (
            RandomAccessFile indexFile = new RandomAccessFile(LogUtil.indexPath(segment), "rw");
        )
        {
            return indexFile.length() / ELogIndex.SIZE;
        }
        catch (FileNotFoundException e)
        {
            return -1;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public ELogMetadata readMetadata()
    {
        try (RandomAccessFile metadataFile = new RandomAccessFile(Env.METADATA_FILE, "rw"))
        {
            if (metadataFile.length() == 0)
            {
                var metadata = new ELogMetadata().currentSegment(1);
                writeMetadata(metadata);
                return metadata;
            }
            // todo using ByteBuffer
            var buffer = new byte[(int)metadataFile.length()];
            metadataFile.readFully(buffer);
            var data = MetadataProto.LogMetadata.parseFrom(buffer);
            return new ELogMetadata()
                .currentSegment(data.getCurrentSegment());
        }
    }

    @SneakyThrows
    private void writeMetadata(ELogMetadata logMetadata)
    {
        var data = MetadataProto.LogMetadata.newBuilder()
            .setCurrentSegment(logMetadata.currentSegment())
            .build();
        try (RandomAccessFile metadataFile = new RandomAccessFile(Env.METADATA_FILE, "rw"))
        {
            metadataFile.seek(0);
            metadataFile.write(data.toByteArray());
            metadataFile.setLength(data.getSerializedSize());
        }
    }
}
