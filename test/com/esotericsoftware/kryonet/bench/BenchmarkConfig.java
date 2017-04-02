package com.esotericsoftware.kryonet.bench;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Created by Evan on 3/27/17.
 */
public class BenchmarkConfig {
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CachedMessageBench.class.getSimpleName())
         //       .include(CompressionBench.class.getSimpleName())
          //      .include(VarIntBench.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
