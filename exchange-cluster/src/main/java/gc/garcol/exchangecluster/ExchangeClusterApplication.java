package gc.garcol.exchangecluster;

import gc.garcol.exchangecore.BootstrapCluster;
import gc.garcol.exchangecore.ExchangeCluster;
import gc.garcol.exchangecore.ExchangeClusterState;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class ExchangeClusterApplication
{

    private final BootstrapCluster bootstrapCluster;
    private final ExchangeCluster exchangeCluster;

    public static void main(String[] args)
    {
        SpringApplication.run(ExchangeClusterApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        log.info("Application is ready to start.");
        bootstrapCluster.start();
    }

    @PreDestroy
    public void destroy() {
        log.info("Application is going to stop.");
        bootstrapCluster.stop();
    }
}