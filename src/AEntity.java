import java.util.HashMap;
import java.util.HashSet;

public class AEntity {
    private final int windowSize;
    private int tempSeqNum; // the sequence number that
    private int windowStartNum;   // this value is also equal to the first unAcked sequence number
    private int packetLastSend;
    private int tempWindowSize;
    private final int limitSeqNum;
    private final double rxmInterval;
    private boolean hasResent;
    private final Checksum checksum;
    private final HashMap<Integer, Packet> buffer;  // buffer all the unAcked packets that generated from received messages
    private final HashMap<Integer, Packet> bufferForSend;  // buffer all the packets that generated from received messages but are not sent yet.
    private int numOfOriginal;
    private int numOfRetransmit;
    private int receivedCorruptPackets;
    private double totalRTT;
    private double totalComTime;
    private int count1;
    private int count2;
    private final HashMap<Integer, Double> sendTime;
    private final HashSet<Integer> retransmitPackets;

    public AEntity(int windowSize, int limitSeqNum, double rxmInterval) {
        this.windowSize = windowSize;
        this.limitSeqNum = limitSeqNum;
        this.rxmInterval = rxmInterval;
        this.tempSeqNum = 0;
        this.windowStartNum = 0;
        this.packetLastSend = 0;
        this.tempWindowSize = 0;
        this.totalRTT = 0.0;
        this.totalComTime = 0.0;
        this.hasResent = false;
        this.checksum = new Checksum();
        this.buffer = new HashMap<>();
        this.bufferForSend = new HashMap<>();
        this.sendTime = new HashMap<>();
        this.retransmitPackets = new HashSet<>();
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
        numOfOriginal++;
        Packet packet = new Packet(tempSeqNum, -1, checksum.calculateChecksum(tempSeqNum, -1, message.getData()), message.getData());
        buffer.put(tempSeqNum, packet);
        if(isNotWaiting()) {
//            sendTime.put(tempSeqNum, NetworkSimulator.getTime());
            NetworkSimulator.toLayer3(0, packet); // send the packet to layer3 and transfer
//            System.out.println("Packet: ---------------------");
//            System.out.println(packet.toString());
//            System.out.println("------------------------------");
            packetLastSend = tempSeqNum;
            tempWindowSize++;
            NetworkSimulator.startTimer(0, rxmInterval);
        } else {
            System.out.println("buffer the packet: " + packet.toString());
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
        System.out.println(" A received packet---------------------------------------------------------------------");
        System.out.println(packet.toString());
        System.out.println("---------------------------------------------------------------------------------");

        int checkSum = checksum.calculateChecksum(packet);
        if(checkSum == packet.getChecksum()) {
            NetworkSimulator.stopTimer(0);   // receive a packet, stop timer
            int ackedNum = packet.getAcknum();
            // if the packet is acknowledged, then remove it from the buffer
            if(ackedNum == 0) buffer.remove(limitSeqNum-1);
            else
                buffer.remove(ackedNum-1);
            if(ackedNum == windowStartNum && !hasResent) {
                // this means it is a duplicate ack, retransmit the first unAcked packet, which is windowStartNum
                System.out.println("A received duplicate ACK, retransmit---------------------------------------------------");
                numOfRetransmit++;
                Packet retransmitPacket = buffer.get(windowStartNum);
                retransmitPackets.add(retransmitPacket.getSeqnum());
                NetworkSimulator.toLayer3(0, retransmitPacket);
                // timer?
                NetworkSimulator.startTimer(0, rxmInterval);
                hasResent = true;
            } else {
                // received the cumulative acknowledgement
                Double receiveTime = NetworkSimulator.getTime();
//                if(!retransmitPackets.contains(ackedNum-1)) {
//                    totalRTT += receiveTime - sendTime.get(ackedNum-1);
//                    count1++;
//                }
//                totalComTime += receiveTime - sendTime.get(ackedNum-1);
//                count2++;
//                retransmitPackets.remove(ackedNum-1);


                if(ackedNum > windowStartNum) {
                    tempWindowSize -= (ackedNum - windowStartNum);
                } else {
                    tempWindowSize -= (ackedNum + limitSeqNum - windowStartNum);
                }
                windowStartNum = ackedNum;
            }

            // if not in waiting state, check if there are available packets to be sent in bufferForSend
            if (isNotWaiting() && !bufferForSend.isEmpty()) {
                System.out.println("send the buffered packets");
               for( ; !bufferForSend.isEmpty() && isNotWaiting(); packetLastSend++) {
                   packetLastSend %= limitSeqNum;
                   Packet sendPacket = bufferForSend.get((packetLastSend+1) % limitSeqNum);
//                   sendTime.put(sendPacket.getSeqnum(), NetworkSimulator.getTime());
                   System.out.println("buffered packet: " + sendPacket.toString());
                   NetworkSimulator.toLayer3(0, sendPacket);
                   // timer?
                   NetworkSimulator.startTimer(0, rxmInterval);
                   tempWindowSize++;
                   bufferForSend.remove((packetLastSend+1) % limitSeqNum);
               }
            }
        } else {
//            System.out.println("A received corrupt Ack");
            receivedCorruptPackets++;
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
        System.out.println("A timeout, retransmit---------------------------------------------------------");
        numOfRetransmit++;
        Packet timoutPacket = buffer.get(windowStartNum);
        retransmitPackets.add(timoutPacket.getSeqnum());
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
     * @return true if it is not in waiting state, false otherwise
     */
    private boolean isNotWaiting() {
        return tempWindowSize < windowSize;
    }

    public int getNumOfOriginal() {
        return numOfOriginal;
    }

    public int getNumOfRetransmit() {
        return numOfRetransmit;
    }

    public int getReceivedCorruptPackets() {
        return receivedCorruptPackets;
    }

    public double getTotalRTT() {
        return totalRTT;
    }

    public double getTotalComTime() {
        return totalComTime;
    }

    public int getCount1() {
        return count1;
    }

    public int getCount2() {
        return count2;
    }
}
