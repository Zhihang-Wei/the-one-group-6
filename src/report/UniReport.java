/*
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details.
 */
package report;

import java.util.List;
import core.*;
import routing.SpyRouter;

/**
 * Report metrics corresponding to the spy router scenario.
 */
public class UniReport extends Report implements ConnectionListener, UpdateListener , MessageListener{
    private int spyMeetings;
    private int spyCounterMeeting;

    private int corruptedBySpy;
    private int corruptedByCounterSpy;

    private int rescued;
    private int corrupted;

    private int informationTransfers;
    private int disruptedTransfers;
    private int interrupted;
    private static int counter = 0;

    public UniReport() {
    }

    @Override
    public void hostsConnected(DTNHost host1, DTNHost host2) {
        Boolean r1_spy = host1.getRouter() instanceof SpyRouter;
        Boolean r2_spy = host2.getRouter() instanceof SpyRouter;
        // count the connections happening between spies in spyMeetings
        if (r1_spy && r2_spy) {
            spyMeetings ++;
        }
        // count the connections happening between spies and counter spies in spyCounterMeeting
        else if (r1_spy || r2_spy) {
            spyCounterMeeting ++;
        }
    }

    @Override
    protected String getScenarioName() {
        return "Uni Report iter:" + String.valueOf(counter++);
    }


    @Override
    public void hostsDisconnected(DTNHost host1, DTNHost host2) {

    }

    @Override
    public void updated(List<DTNHost> hosts) {

    }

    @Override
    public void done() {
        write("Spy meetings: " + spyMeetings);
        write("Spy meetings with Counter Spy: " + spyCounterMeeting);

        write("Spies corrupted by other corrupted spies: " + corruptedBySpy);
        write("Spies corrupted by Counter Spies: " + corruptedByCounterSpy);

        write("Rescued Spies: " + rescued);
        write("Corrupted Spies: " + corrupted);

        write("Number of Information Exchanges: " + informationTransfers);
        write("Number of Disrupted Exchanges: " + disruptedTransfers);
        write("Number of Interrupted Exchanges: " + interrupted);
        counter++;


        super.done();
    }

    @Override
    public void newMessage(Message m) {
        if (m.getId().startsWith("transferDisrupted")) {
            disruptedTransfers++;
        }

    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {

    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {

    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
        interrupted++;
    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        boolean fromSpy = from.getRouter() instanceof SpyRouter;
        // count the number of corrupted spies (spies that start sending disruption signals)
        if(m.getId().startsWith("disruptionSignal")){
            if(fromSpy){
                // number of spies corrupted by counter spies
                corruptedBySpy++;
            } else {
                // number of spies corrupted by other corrupted spies
                corruptedByCounterSpy++;
            }
            // total number of corrupted spies
            corrupted++;
        }
        // Count the number of rescued spies. Spies are considered as rescued in case they receive an emergency signal.
        else if (m.getId().startsWith("emergencySignal")){
            rescued++;
        }
        // Count the number of messages used to transfer information between spies.
        else if (m.getId().startsWith("informationTransfer")){
            informationTransfers++;
        }

    }
}
