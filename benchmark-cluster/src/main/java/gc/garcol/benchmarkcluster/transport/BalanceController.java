package gc.garcol.benchmarkcluster.transport;

import gc.garcol.benchmarkcluster.domain.ClusterExchangeService;
import gc.garcol.benchmarkcluster.transport.payload.BalanceDepositRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/deposit/{duration-in-second}/{total-connection}")
    String depositAsset(
        @PathVariable("duration-in-second") int durationInSecond,
        @PathVariable("total-connection") int totalConnection,
        @RequestBody BalanceDepositRequest request)
    {
        log.info("Deposit request: {}", request);
        exchangeService.request(durationInSecond, totalConnection, request);
        return "success";
    }
}
