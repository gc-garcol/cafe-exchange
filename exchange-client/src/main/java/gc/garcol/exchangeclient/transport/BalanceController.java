package gc.garcol.exchangeclient.transport;

import gc.garcol.exchangeclient.domain.ClusterExchangeService;
import gc.garcol.exchangeclient.domain.Response;
import gc.garcol.exchangeclient.transport.payload.BalanceCreateRequest;
import gc.garcol.exchangeclient.transport.payload.BalanceDepositRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * @author thaivc
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/v1/balance")
@RequiredArgsConstructor
public class BalanceController
{

    private final ClusterExchangeService exchangeService;

    @PostMapping
    CompletableFuture<Response> createBalance(@RequestBody BalanceCreateRequest request)
    {
        log.info("Create balance request: {}", request);
        return exchangeService.request(request);
    }

    @PostMapping("/deposit")
    CompletableFuture<Response> depositAsset(@RequestBody BalanceDepositRequest request)
    {
        log.info("Deposit request: {}", request);
        return exchangeService.request(request);
    }
}
