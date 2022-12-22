package routing;

import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;

import java.util.List;

public class SpyCounterRouter extends ActiveRouter{


    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * @param s The settings object
     */
    public SpyCounterRouter(Settings s) {
        super(s);
    }

    /**
     * Copy constructor.
     * @param r The router prototype where setting values are copied from
     */
    protected SpyCounterRouter(SpyCounterRouter r) {
        super(r);
    }

    /**
     *Spy counter routers try to send a single disruption signal to all connected nodes.
     * The disruption signal id contains the node's id.
     */
    @Override
    public void init(DTNHost host, List<MessageListener> mListeners){
        super.init(host, mListeners);
        this.createNewMessage(new Message(this.getHost(), this.getHost(), "disruptionSignal_" + this.getHost().getAddress(), 100));
    }


    @Override
    protected int checkReceiving(Message m, DTNHost from) {return DENIED_POLICY;}

    @Override
    public void update() {
        super.update();
        if (isTransferring() || !canStartTransfer()) {
            return; /* transferring, don't try other connections yet */
        }
        this.tryAllMessagesToAllConnections();
    }


    @Override
    public SpyCounterRouter replicate() {
        return new SpyCounterRouter(this);
    }
}
