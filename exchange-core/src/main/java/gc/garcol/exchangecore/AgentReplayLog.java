package gc.garcol.exchangecore;

import org.agrona.concurrent.Agent;

/**
 * Fetch data from log
 *
 * @author thaivc
 * @since 2024
 */
public class AgentReplayLog implements Agent
{
    public int doWork() throws Exception
    {
        return 0;
    }

    public String roleName()
    {
        return "ReplayLog";
    }
}