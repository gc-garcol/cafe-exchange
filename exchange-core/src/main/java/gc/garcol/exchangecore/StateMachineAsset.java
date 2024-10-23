package gc.garcol.exchangecore;

import gc.garcol.exchange.proto.CommandProto;
import gc.garcol.exchangecore.domain.Asset;
import gc.garcol.exchangecore.domain.CommonResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * @author thaivc
 * @since 2024
 */
public class StateMachineAsset implements StateMachine, StateMachinePersistable
{

    Map<String, Asset> assets = new HashMap<>();

    // todo note: hardcode, should be loaded from dataset
    {
        assets.put("USDT", new Asset().name("USDT").precision(8));
        assets.put("BTC", new Asset().name("BTC").precision(8));
        assets.put("ETH", new Asset().name("ETH").precision(8));
        assets.put("BNB", new Asset().name("BNB").precision(8));
    }

    public CommonResponse apply(final CommandProto.Command command)
    {
        return null;
    }

    public void loadSnapshot()
    {

    }

    public void snapshot()
    {

    }
}
