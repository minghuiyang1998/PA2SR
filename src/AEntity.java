import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AEntity {
    private NetworkSimulator networkSimulator;
    private int windowSize;
    private int tempSeqNum;
    private int windowStartNum;
    private boolean waitingForSlide;
    private Checksum checksum;
    private HashMap<Integer, Packet> buffer;
    private HashMap<Integer, Packet> bufferForSend;
    private ArrayList<Pair<Integer, Boolean>> window;

    public AEntity() {
        this.windowSize = 0;
        this.tempSeqNum = 0;
        this.windowStartNum = 0;
        this.waitingForSlide = false;
        this.checksum = new Checksum();
        this.buffer = new HashMap<>();
        this.bufferForSend = new HashMap<>();
        this.window = new ArrayList<>();
    }

    public AEntity(int windowSize) {
        this.windowSize = windowSize;
        this.tempSeqNum = 0;
        this.windowStartNum = 0;
        this.waitingForSlide = false;
        this.checksum = new Checksum();
        this.buffer = new HashMap<>();
        this.bufferForSend = new HashMap<>();
        this.window = new ArrayList<>();
    }


    public void setNetworkSimulator(NetworkSimulator networkSimulator) {
        this.networkSimulator = networkSimulator;
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.

    /**
     *  This routine will be called whenever the upper layer at the sender [A]
     *  has a message to send.  It is the job of your protocol to insure that
     *  the data in such a message is delivered in-order, and correctly, to
     *  the receiving upper layer.
     * @param message: The message that got from the layer 5
     */
    public void output(Message message) {
        Packet packet = new Packet(tempSeqNum, -1, checksum.calculateChecksum(tempSeqNum, -1, message.getData()));
        buffer.put(tempSeqNum, packet);
        window.add(new Pair<>(tempSeqNum, false));
        if(!waitingForSlide) {
            networkSimulator.toLayer3(0, packet);
            networkSimulator.startTimer(0, 20);
            if(window.size() == windowSize) waitingForSlide = true;
        } else {
            bufferForSend.put(tempSeqNum, packet);
        }
        tempSeqNum = (tempSeqNum+1) % (2*windowSize);
    }

    /**
     * This routine will be called when A's timer expires (thus generating a
     * timer interrupt). You'll probably want to use this routine to control
     * the retransmission of packets. See startTimer() and stopTimer(), above,
     * for how the timer is started and stopped.
     * @param packet: The packet that got from the layer3 medium
     */
    public void input(Packet packet) {
        int checkSum = checksum.calculateChecksum(packet.getSeqnum(), packet.getAcknum(), packet.getPayload());
        Packet firstUnAckedPacket = null;
        boolean flag = true;
        if(checkSum == packet.getChecksum()) {
            for (Pair<Integer, Boolean> pair : window) {
                if(flag && pair.getValue().equals(false)) {
                    firstUnAckedPacket = buffer.get(pair.getKey());
                    flag = false;
                }
                if(pair.getKey().equals(packet.getAcknum())) {
                    int index = window.indexOf(pair);
                    buffer.remove(packet.getAcknum()); // remove the ACK packet from the buffer
                    if(pair.getValue()) {   // duplicate ACK
                        // retransmit the first unAcked packet
                        networkSimulator.toLayer3(0, firstUnAckedPacket);
                    } else {
                        networkSimulator.stopTimer(0);
                        // acknowledged the first packet in the window
                        if(index == 0) {
                            window.remove(pair);
                        } else {
                            window.set(index, new Pair<>(pair.getKey(), true));
                        }
                        while(window.size() < windowSize && bufferForSend.size() > 0) {
                            waitingForSlide = false;
                            int numberBehindWindow = windowStartNum + window.size();
                            if (bufferForSend.containsKey(numberBehindWindow)) {
                                Packet packet1 = bufferForSend.get(numberBehindWindow);
                                window.add(new Pair<>(numberBehindWindow, false));
                                networkSimulator.toLayer3(0, packet1);
                                bufferForSend.remove(numberBehindWindow);
                            }
                        }
                    }
                    break;
                } else {
                    // the ack packet is not in the window, which means it is duplicate ack
                    // retransmit the first unAcked packet
                    networkSimulator.toLayer3(0, firstUnAckedPacket);
                }
            }
        }
    }

    /**
     * This routine will be called when A's timer expires (thus generating a
     * timer interrupt). You'll probably want to use this routine to control
     * the retransmission of packets. See startTimer() and stopTimer(), above,
     * for how the timer is started and stopped.
     */
    public void timerInterrupt() {

    }

    /**
     * This routine will be called once, before any of your other A-side
     * routines are called. It can be used to do any required
     * initialization (e.g. of member variables you add to control the state
     * of entity A).
     */
    public void init() {

    }
}
