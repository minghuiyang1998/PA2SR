import com.sun.org.apache.xpath.internal.operations.Bool;
import jdk.nashorn.internal.objects.NativeError;

import java.util.ArrayList;
import java.util.HashMap;

public class AEntity {
    private int windowSize;
    private int tempSeqNum; // the sequence number that
    private int windowStartNum;   // this value is also equal to the first unAcked sequence number
    private int packetLastSend;
    private int limitSeqNum;
    private double rxmInterval;
    private boolean hasResent;
    private Checksum checksum;
    private HashMap<Integer, Packet> buffer;  // buffer all the unAcked packets that generated from received messages
    private HashMap<Integer, Packet> bufferForSend;  // buffer all the packets that generated from received messages but are not sent yet.


    public AEntity(int windowSize, int limitSeqNum, double rxmInterval) {
        this.windowSize = windowSize;
        this.limitSeqNum = limitSeqNum;
        this.rxmInterval = rxmInterval;
        this.tempSeqNum = 0;
        this.windowStartNum = 0;
        this.packetLastSend = 0;
        this.hasResent = false;
        this.checksum = new Checksum();
        this.buffer = new HashMap<>();
        this.bufferForSend = new HashMap<>();
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
        if(isNotWaiting(packetLastSend)) {
            NetworkSimulator.toLayer3(0, packet); // send the packet to layer3 and transfer
            packetLastSend = tempSeqNum;
            NetworkSimulator.startTimer(0, rxmInterval);
        } else {
            bufferForSend.put(tempSeqNum, packet);
        }
        tempSeqNum = (tempSeqNum+1) % limitSeqNum;
    }

    /**
     * This routine will be called when A's timer expires (thus generating a
     * timer interrupt). You'll probably want to use this routine to control
     * the retransmission of packets. See startTimer() and stopTimer(), above,
     * for how the timer is started and stopped.
     * @param packet: The packet that got from the layer3 medium
     */
    public void input(Packet packet) {
        int checkSum = checksum.calculateChecksum(packet);
        if(checkSum == packet.getChecksum()) {
            NetworkSimulator.stopTimer(0);   // receive a packet, stop timer
            int ackedNum = packet.getAcknum();
            // if the packet is acknowledged, then remove it from the buffer
            buffer.remove(ackedNum-1);
            if(ackedNum == windowStartNum && !hasResent) {
                // this means it is a duplicate ack, retransmit the first unAcked packet, which is windowStartNum
                Packet retransmitPacket = buffer.get(windowStartNum);
                NetworkSimulator.toLayer3(0, retransmitPacket);
                // timer?
                NetworkSimulator.startTimer(0, rxmInterval);
                hasResent = true;
            } else {
                // received the cumulative acknowledgement
                windowStartNum = ackedNum;
            }

            // if not in waiting state, check if there are available packets to be sent in bufferForSend
            if (isNotWaiting(packetLastSend) && !bufferForSend.isEmpty()) {
               for( ; bufferForSend.isEmpty() || !isNotWaiting(packetLastSend); packetLastSend++) {
                   packetLastSend %= limitSeqNum;
                   Packet sendPacket = bufferForSend.get((packetLastSend+1) % limitSeqNum);
                   NetworkSimulator.toLayer3(0, sendPacket);
                   // timer?
                   NetworkSimulator.startTimer(0, rxmInterval);
                   bufferForSend.remove((packetLastSend+1) % limitSeqNum);
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
        // not sure whether it's correct
        Packet timoutPacket = buffer.get(windowStartNum);
        NetworkSimulator.toLayer3(0, timoutPacket);
        NetworkSimulator.stopTimer(0);
        NetworkSimulator.startTimer(0, rxmInterval);
    }

//    /**
//     * This routine will be called once, before any of your other A-side
//     * routines are called. It can be used to do any required
//     * initialization (e.g. of member variables you add to control the state
//     * of entity A).
//     */
//    public void init() {
//
//    }

    /**
     * Check if the window is now waiting for an ACK to slide
     * @param packetLastSend The last sequence number of the temp window
     * @return true if it is not in waiting state, false otherwise
     */
    private boolean isNotWaiting(int packetLastSend) {
        if(packetLastSend > windowStartNum)
            return packetLastSend - windowStartNum + 1 < windowSize;
        else
            return packetLastSend + 2*windowSize - windowStartNum + 1 < windowSize;
    }
}
