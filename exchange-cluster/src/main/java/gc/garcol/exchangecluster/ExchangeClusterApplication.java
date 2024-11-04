package gc.garcol.exchangecluster;

import gc.garcol.exchangecluster.transport.GRpcClusterNetworkResource;
import gc.garcol.exchangecore.BootstrapCluster;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class ExchangeClusterApplication
{

    private final BootstrapCluster bootstrapCluster;
    private final GRpcClusterNetworkResource grpcClusterResource;

    public static void main(String[] args)
    {
        SpringApplication.run(ExchangeClusterApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap()
    {
        try
        {
            log.info("Application is ready to start.");
            bootstrapCluster.start();
        }
        catch (Exception e)
        {
            log.error("bootstrap exception", e);
            System.exit(2);
        }
    }

    @PreDestroy
    public void destroy()
    {
        try
        {
            log.info("Application is going to stop.");
            bootstrapCluster.stop();
        }
        catch (Exception e)
        {
            log.error("destroy exception", e);
        }
    }
}