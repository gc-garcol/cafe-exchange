package gc.garcol.benchmarkexchange.example.blockingqueue;

/**
 * @author thaivc
 * @since 2024
 */

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class BlockingQueueBenchmarkRunner
{
    public static void main(String[] args) throws RunnerException
    {
        Options opt = new OptionsBuilder()
            .include(BlockingQueueBenchmark.class.getSimpleName())
            .forks(1)
            .resultFormat(ResultFormatType.JSON)
            .result("benchmark-result/blocking-queue-result.json")
            .build();

        new Runner(opt).run();
    }
}

