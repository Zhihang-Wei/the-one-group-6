package routing;

import core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Router module mimicking the game-of-life behavior
 */
public class SpyRouter extends ActiveRouter {

    /**
     * Neighboring message count -setting id ({@value}). Two comma
     * separated values: min other spies, min counter spies.
     * min other spies is the limit to receive messages from other spies
     * while min counter spies is the limit to disrupt information transfers and to corrupt spies
     */
    public static final String NM_COUNT_S = "nmcount";
    private int countRange[];
    private static boolean emergencyAgent = true;
    private String currentlySendingMessageType;

    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * @param s The settings object
     */
    public SpyRouter(Settings s) {
        super(s);
        countRange = s.getCsvInts(NM_COUNT_S, 2);
    }

    /**
     * Copy constructor.
     * @param r The router prototype where setting values are copied from
     */
    protected SpyRouter(SpyRouter r) {
        super(r);
        this.countRange = r.countRange;
    }

    /**
    Initialize the different spies. The first initialized spy is sending an emergency message while the rest send
     Information Transfers. An appropriate message is created and added. The id consists of the type-string
     concatenated with the node's id.
     */
    @Override
    public void init(DTNHost host, List<MessageListener> mListeners){
        super.init(host, mListeners);
        String initialMessageType;
        if(emergencyAgent){
            initialMessageType = "emergencySignal";
            emergencyAgent = false;
        } else {
            initialMessageType = "informationTransfer";
        }
        this.currentlySendingMessageType = initialMessageType;
        Message message = new Message(this.getHost(), this.getHost(), currentlySendingMessageType + "_" + this.getHost().getAddress(), 100);
        this.createNewMessage(message);
    }

    /**
     * This method counts the message types connected peers try to send and based on these counts, decide what messages to accept.
     * 1) In case a node sends either a disruption signal or an information transfer signal, no incoming messages are accepted.
     * 2) In case at least one connected node sends an emergency signal while at least two connected nodes either send an emergency signal or an information
     *      transfer signal and less than 5 connected nodes try to send disruption signals, accept the emergency signal
     * 3) In case the only connected nodes are ones sending a disruption signal while there are more than 5 of them, accept the disruption signal.
     * 4) In case at least one connected node tries to send an emergency signal and if more than 5 nodes try to disrupt it, accept no messages.
     *      In this case, a new message is created that indicates that the transfer failed.
     * 5) If at least two connected nodes try to send an information transfer, accept it.
     * 6) Default deny connections.
     * @return The string-type of the message to accept
     */
    private String getDominantId() {
        DTNHost me = getHost();
        int informationTransferCount = 0;
        int emergencySignalCount = 0;
        int disruptionSignalCount = 0;
        String dominantId = "None";

        for (Connection c : getConnections()) {
            if (c.getOtherNode(me).getRouter().hasMessage("informationTransfer_" + c.getOtherNode(me).getAddress())) {
                informationTransferCount++;
            } else if(c.getOtherNode(me).getRouter().hasMessage("emergencySignal_" + c.getOtherNode(me).getAddress())) {
                emergencySignalCount++;
            } else if(c.getOtherNode(me).getRouter().hasMessage("disruptionSignal_" + c.getOtherNode(me).getAddress())) {
                disruptionSignalCount++;
            }

        }
        if(this.currentlySendingMessageType.equals("disruptionSignal") || this.currentlySendingMessageType.equals("emergencySignal")){
            dominantId = "None";
        }
        else if (emergencySignalCount > 0 && informationTransferCount + emergencySignalCount > countRange[0]-1 && disruptionSignalCount < countRange[1]){
            dominantId = "emergencySignal";
        }
        else if (emergencySignalCount + informationTransferCount == 0 && disruptionSignalCount >= countRange[1]){
            dominantId = "disruptionSignal";
        }
        else if (emergencySignalCount + informationTransferCount > 0 && disruptionSignalCount > countRange[1]) {
            Message message = new Message(this.getHost(), this.getHost(), "transferDisrupted" + this.getHost().getAddress(), 100);
            this.createNewMessage(message);
        }
        else if (informationTransferCount > countRange[0]-1){
            dominantId = "informationTransfer";
        } else{
            dominantId = "None";
        }
        return dominantId;
    }

    /**
     * Here, we only accept the messages that correspond to the message string-type returned by getDominantId()
     * @param m The message to check
     * @param from Host the message was from (previous hop)
     * @return Receiving Policy
     */
    @Override
    protected int checkReceiving(Message m, DTNHost from) {
        String dominantId = getDominantId();

        if (!from.getRouter().hasMessage(dominantId + "_" + from.getAddress())) {
            return DENIED_POLICY;
        }

        /* peer message count check OK; receive based on other checks */
        return super.checkReceiving(m, from);
    }

    /**
     * In case an emergency signal or a disruption signal is received, create a new message of the respective type with the
     * message id containing the id of the recipient.
     * @param id id of the transferred message
     * @param from Host the message was from (previous hop)
     */
    @Override
    public Message messageTransferred(String id, DTNHost from) {
        // In case a disruption signal was transferred, create a new, own disruption signal and set currently send message to disruption signal
        if(id.startsWith("disruptionSignal")){
            Message message = new Message(this.getHost(), this.getHost(), "disruptionSignal_" + this.getHost().getAddress(), 100);
            this.createNewMessage(message);
            this.currentlySendingMessageType = "disruptionSignal";
        }
        // In case a new emergency signal was transferred, create a new, own emergency signal and set currently send message to emergency signal
        else if(id.startsWith("emergencySignal")) {
            Message message = new Message(this.getHost(), this.getHost(), "emergencySignal_" + this.getHost().getAddress(), 100);
            this.createNewMessage(message);
            this.currentlySendingMessageType = "emergencySignal";
        }
        return super.messageTransferred(id, from);

    }

    @Override
    public void update() {
        super.update();

        if (isTransferring() || !canStartTransfer()) {
            return; /* transferring, don't try other connections yet */
        }

        this.sendSelectiveMessages();
    }

    /**
     * In case a disruption signal or emergency signal is currently sent, only send the one belonging to the sender.
     * In case an information transfer signal is sent, send all the router has got.
     */
    protected void sendSelectiveMessages(){
        List<Connection> connections = getConnections();
        if (connections.size() == 0 || this.getNrofMessages() == 0) {
            return;
        }
        List<Message> messages =
                new ArrayList<>();
        if(currentlySendingMessageType.equals("disruptionSignal")){
            // send own disruption signal
            messages.add(this.getMessage("disruptionSignal_" + this.getHost().getAddress()));
        } else if(currentlySendingMessageType.equals("emergencySignal")) {
            messages.add(this.getMessage("emergencySignal_" + this.getHost().getAddress()));
        } else {
            // Transfer all currently holding information transfer messages to propagate collected information
            for (Message message : this.getMessageCollection()) {
                if (message.getId().startsWith(currentlySendingMessageType)) {
                    messages.add(message);
                }
            }
            this.sortByQueueMode(messages);
        }
        tryMessagesToConnections(messages, connections);
    }

    static {
        DTNSim.registerForReset(SpyRouter.class.getCanonicalName());
        reset();
    }


    @Override
    public SpyRouter replicate() {
        return new SpyRouter(this);
    }

    public static void reset() {
        emergencyAgent = true;
    }

}
