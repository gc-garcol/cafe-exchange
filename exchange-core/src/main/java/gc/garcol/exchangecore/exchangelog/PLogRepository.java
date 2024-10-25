package gc.garcol.exchangecore.exchangelog;

import gc.garcol.exchange.proto.MetadataProto;
import gc.garcol.exchangecore.common.Env;
import gc.garcol.exchangecore.common.InternalException;
import lombok.SneakyThrows;

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
            RandomAccessFile indexFile = new RandomAccessFile(LogUtil.indexName(segment), "r");
            RandomAccessFile logFile = new RandomAccessFile(LogUtil.indexPath(segment), "r")
        )
        {
            logFile.seek(index * ELogIndex.SIZE);
            var logIndex = new ELogIndex()
                .index(logFile.readLong())
                .entryLength(logFile.readInt());
            indexFile.seek(logIndex.index());
            var buffer = new byte[logIndex.entryLength()];
            indexFile.readFully(buffer);
            return buffer;
        }
        catch (Exception e)
        {
            throw new InternalException(e);
        }
    }

    @SneakyThrows
    public void write(byte[] commands)
    {
        var segment = logMetadata.currentSegment();
        try (
            RandomAccessFile indexFile = new RandomAccessFile(LogUtil.indexName(segment), "rw");
            RandomAccessFile logFile = new RandomAccessFile(LogUtil.indexPath(segment), "rw")
        )
        {
            FileChannel indexChannel = indexFile.getChannel();
            FileChannel fileChannel = logFile.getChannel();

            var indexBuffer = ByteBuffer.allocate(ELogIndex.SIZE);
            indexBuffer.putLong(logFile.length());
            indexBuffer.putInt(commands.length);
            indexBuffer.flip();

            var offset = logFile.length();
            var logBuffer = ByteBuffer.wrap(commands);
            fileChannel.write(logBuffer, offset);
            indexChannel.write(indexBuffer);
            fileChannel.force(true);
            indexChannel.force(true);
        }
    }

    @SneakyThrows
    private ELogMetadata readMetadata()
    {
        try (RandomAccessFile metadataFile = new RandomAccessFile(Env.METADATA_FILE, "rw"))
        {
            if (metadataFile.length() == 0)
            {
                var metadata = new ELogMetadata().currentSegment(1);
                writeMetadata(metadata);
                return metadata;
            }
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
