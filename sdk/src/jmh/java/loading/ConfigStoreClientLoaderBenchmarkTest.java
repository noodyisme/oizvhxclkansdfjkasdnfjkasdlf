package loading;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.client.dynamic.PollingConfiguration;
import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.platform.loading.ConfigStoreClientLoader;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ConfigStoreClientLoaderBenchmarkTest {
    private static final int FORK_COUNT = 2;
    private static final int WARMUP_COUNT = 10;
    private static final int ITERATION_COUNT = 10;
    private static final int THREAD_COUNT = 2;
    private ConfigStoreClient client;
    private VirtualTimeScheduler testScheduler;

    private ConfigStoreClientLoader loader;

    @Setup
    public void setUp() {
        PollingConfiguration properties = new PollingConfiguration(Duration.ofDays(365));
        client = ConfigStoreClient.newLocalClient(
                "src/test/resources/web-client-config-store/us_consumers",
                properties,
                ConfigStoreClient_ApplicationEventPublisher.EMPTY
        );
        testScheduler = VirtualTimeScheduler.getOrSet();
        loader = new ConfigStoreClientLoader(
                client,
                ConfigStoreClient_ApplicationEventPublisher.EMPTY,
                testScheduler,
                EntityType.POLICY
        );
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public void constructorBenchmark(Blackhole bh) {
        bh.consume(new ConfigStoreClientLoader(
                client,
                ConfigStoreClient_ApplicationEventPublisher.EMPTY,
                testScheduler,
                EntityType.POLICY
        ));
    }

    @Benchmark
    public void loadBenchmark(Blackhole bh) {
        bh.consume(loader.load());
    }


}
