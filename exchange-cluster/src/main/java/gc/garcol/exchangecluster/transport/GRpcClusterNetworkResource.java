package gc.garcol.exchangecluster.transport;

import com.google.common.util.concurrent.MoreExecutors;
import gc.garcol.exchangecluster.anotations.GRpcResource;
import gc.garcol.exchangecore.NetworkClusterService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
@GRpcResource
@RequiredArgsConstructor
public class GRpcClusterNetworkResource
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

    public void start() throws IOException
    {
        this.init();
        this.server.start();
    }

    public void stop()
    {
        this.server.shutdown();
    }
}
