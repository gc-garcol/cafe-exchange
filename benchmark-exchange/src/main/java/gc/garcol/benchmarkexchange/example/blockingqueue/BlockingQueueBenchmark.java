package gc.garcol.benchmarkexchange.example.blockingqueue;

/**
 * @author thaivc
 * @since 2024
 */

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAccumulator;

@BenchmarkMode(Mode.All)  // Measure average time taken for a complete unit of work
@State(Scope.Group)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 3)
public class BlockingQueueBenchmark
{

    private BlockingQueue<Message> queue;
    private LongAccumulator totalElapsedTime;  // Accumulator for total elapsed time

    @Setup(Level.Iteration)
    public void setUp()
    {
        queue = new LinkedBlockingQueue<>();
        totalElapsedTime = new LongAccumulator(Long::sum, 0L);
    }

    @Benchmark
    @Group("pushAndConsumeGroup")
    @GroupThreads(1)
    public void push() throws InterruptedException
    {
        long timestamp = System.nanoTime();
        queue.put(new Message(timestamp));
    }

    // Consumer
    @Benchmark
    @Group("pushAndConsumeGroup")
    @GroupThreads(1)
    public long consume() throws InterruptedException
    {
        Message message = queue.take();

        long endTime = System.nanoTime();
        long elapsedTime = endTime - message.timestamp;
        totalElapsedTime.accumulate(elapsedTime);

        // Return elapsed time for reporting
        return elapsedTime;
    }

    @TearDown(Level.Iteration)
    public void tearDown()
    {
        queue.clear();
    }

    static class Message
    {
        final long timestamp;

        public Message(long timestamp)
        {
            this.timestamp = timestamp;
        }
    }
}
