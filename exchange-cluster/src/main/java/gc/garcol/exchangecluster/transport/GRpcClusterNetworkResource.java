package gc.garcol.exchangecluster.transport;

import com.google.common.util.concurrent.MoreExecutors;
import gc.garcol.exchangecluster.anotations.GRpcResource;
import gc.garcol.exchangecore.NetworkClusterService;
import gc.garcol.exchangecore.NetworkGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingMillisIdleStrategy;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
@GRpcResource
@RequiredArgsConstructor
public class GRpcClusterNetworkResource implements NetworkGrpc
{
    @Value("${grpc.port}")
    private int grpcPort;

    private Server server;
    private final NetworkClusterService networkClusterService;

    private void init()
    {
        log.info("GRpc server started on port {}", grpcPort);
        this.server = ServerBuilder.forPort(grpcPort)
            .addService(networkClusterService)
            .addService(ProtoReflectionService.newInstance())
            .executor(MoreExecutors.directExecutor())
            .build();
    }

    @SneakyThrows
    @Override
    public void start()
    {
        this.init();
        IdleStrategy idleStrategy = new SleepingMillisIdleStrategy(500);
        for (; ; )
        {
            try
            {
                this.server.start();
                log.info("GRpc server started");
                return;
            }
            catch (Exception e)
            {
                log.error("GRpc server start failed", e);
                idleStrategy.idle();
            }
        }
    }

    @Override
    public void stop()
    {
        log.info("GRpc server shutdown");
        this.server.shutdown();
    }
}
