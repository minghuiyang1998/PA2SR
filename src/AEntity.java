import java.util.ArrayList;
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
//    private boolean hasResent;
    private final Checksum checksum;
    private final HashMap<Integer, Packet> buffer;  // buffer all the unAcked packets that generated from received messages
    private final ArrayList<Message> bufferForSend;  // buffer all the packets that generated from received messages but are not sent yet.
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
//        this.hasResent = false;
        this.checksum = new Checksum();
        this.buffer = new HashMap<>();
        this.bufferForSend = new ArrayList<>();
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
        if(isNotWaiting()) {
            Packet packet = new Packet(tempSeqNum, -1, checksum.calculateChecksum(tempSeqNum, -1, message.getData()), message.getData());
            buffer.put(tempSeqNum, packet);
//            System.out.println("Sequence " + tempSeqNum + " first send time: " + NetworkSimulator.getTime());
            sendTime.put(tempSeqNum, NetworkSimulator.getTime());
            NetworkSimulator.toLayer3(0, packet); // send the packet to layer3 and transfer
            packetLastSend = tempSeqNum;
            tempWindowSize++;
            NetworkSimulator.startTimer(0, rxmInterval);
            tempSeqNum = (tempSeqNum+1) % limitSeqNum;
        } else {
//            System.out.println("buffer the Message: " + message.getData());
            bufferForSend.add(message);
        }
    }

    /**
     * This routine will be called when A's timer expires (thus generating a
     * timer interrupt). You'll probably want to use this routine to control
     * the retransmission of packets. See startTimer() and stopTimer(), above,
     * for how the timer is started and stopped.
     * @param packet: The packet that got from the layer3 medium
     */
    public void input(Packet packet) {
//        System.out.println(" A received packet---------------------------------------------------------------------");
//        System.out.println(packet.toString());
//        System.out.println("---------------------------------------------------------------------------------");

        int checkSum = checksum.calculateChecksum(packet);
        if(checkSum == packet.getChecksum()) {
            NetworkSimulator.stopTimer(0);   // receive a packet, stop timer
            int ackedNum = packet.getAcknum();
            // if the packet is acknowledged, then remove it from the buffer
            if(ackedNum == 0) buffer.remove(limitSeqNum-1);
            else
                buffer.remove(ackedNum-1);
            if(ackedNum == windowStartNum) {
                // this means it is a duplicate ack, retransmit the first unAcked packet, which is windowStartNum
//                System.out.println("A received duplicate ACK, retransmit---------------------------------------------------");
                numOfRetransmit++;
                Packet retransmitPacket = buffer.get(windowStartNum);
                if(retransmitPacket != null) {
//                    System.out.println("retransmitPacket: " + retransmitPacket.toString());
                    retransmitPackets.add(retransmitPacket.getSeqnum());
                    sendTime.put(retransmitPacket.getSeqnum(), NetworkSimulator.getTime());
                    NetworkSimulator.toLayer3(0, retransmitPacket);
                    // timer?
                    NetworkSimulator.startTimer(0, rxmInterval);
//                    hasResent = true;
                }
            } else {
//                hasResent = false;
                // received the cumulative acknowledgement
                Double receiveTime = NetworkSimulator.getTime();
//                System.out.println("Sequence " + (ackedNum-1) + " received time: " + receiveTime);
                int lastReceiveNum = ackedNum == 0? ackedNum+limitSeqNum-1:ackedNum-1;
                double tempTime = receiveTime - sendTime.get(lastReceiveNum);
//                System.out.println("tempTime = " + tempTime);

                if(!retransmitPackets.contains(lastReceiveNum)) {
//                    System.out.println("It is not retransmit");
                    totalRTT += tempTime;
//                    System.out.println("totalRTT = " + totalRTT);
                    count1++;
                }
                totalComTime += tempTime;
//                System.out.println("totalComTime = " + totalComTime);
                count2++;
                retransmitPackets.remove(lastReceiveNum);

                if(ackedNum > windowStartNum) {
                    tempWindowSize -= (ackedNum - windowStartNum);
                } else {
                    tempWindowSize -= (ackedNum + limitSeqNum - windowStartNum);
                }
                windowStartNum = ackedNum;
            }

            // if not in waiting state, check if there are available packets to be sent in bufferForSend
            if (isNotWaiting() && !bufferForSend.isEmpty()) {
//                System.out.println("send the buffered packets");
               for( ; !bufferForSend.isEmpty() && isNotWaiting(); packetLastSend++) {
                   packetLastSend %= limitSeqNum;
                   Message message = bufferForSend.remove(0);
                   Packet sendPacket = new Packet(tempSeqNum, -1,
                           checksum.calculateChecksum(tempSeqNum, -1, message.getData()), message.getData());
                   buffer.put(tempSeqNum, sendPacket);
                   tempSeqNum = (tempSeqNum+1) % limitSeqNum;

//                   System.out.println("Send buffered : " + sendPacket.toString());
//                   System.out.println("Sequence " + sendPacket.getSeqnum() + " first send time: " + NetworkSimulator.getTime());
                   sendTime.put(sendPacket.getSeqnum(), NetworkSimulator.getTime());

                   NetworkSimulator.toLayer3(0, sendPacket);
                   // timer?
                   NetworkSimulator.startTimer(0, rxmInterval);
                   tempWindowSize++;

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
//        System.out.println("A timeout, retransmit---------------------------------------------------------");
        numOfRetransmit++;
        Packet timoutPacket = buffer.get(windowStartNum);
        retransmitPackets.add(timoutPacket.getSeqnum());
        NetworkSimulator.toLayer3(0, timoutPacket);
        NetworkSimulator.stopTimer(0);
        NetworkSimulator.startTimer(0, rxmInterval);
    }

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
