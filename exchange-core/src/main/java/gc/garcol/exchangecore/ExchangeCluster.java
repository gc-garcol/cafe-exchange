package gc.garcol.exchangecore;

import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;

/**
 * @author thaivc
 * @since 2024
 */
public class ExchangeCluster
{
    ExchangeClusterState state;
    OneToOneRingBuffer commandsInboundBuffer;
    OneToOneRingBuffer commandsOutboundBuffer;

    OneToOneRingBuffer heartBeatInboundBuffer;
    OneToOneRingBuffer relayLogInboundBuffer;

    public void onStart() {
        transitionToFollower();
    }

    void transitionToFollower() {
        if (state != null) {
            state.stop();
        }
        state = new ExchangeClusterStateFollower(this);
    }

    void transitionToLeader() {
        if (state != null) {
            state.stop();
        }
        state = new ExchangeClusterStateLeader(this);
    }

}
