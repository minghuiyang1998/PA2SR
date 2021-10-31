import java.util.*;

public class BEntity {
    private final int windowSize;
    private final int limitSeqNumb;
    private final Checksum checksum;
    private final HashMap<Integer, Packet> outOfOrderBuffer;
    private int next;
    private int countACK = 0;
    private int countTo5 = 0;

    BEntity(int windowSize, int limitSeqNumb) {
        this.windowSize = windowSize;
        this.limitSeqNumb = limitSeqNumb;
        this.checksum = new Checksum();
        this.outOfOrderBuffer = new HashMap<>();
        this.next = 0;
    }

    private boolean isInWindow(int seqNumb) {
        Queue<Integer> seqNumbInWindow = new LinkedList<>();
        for (int i = 0; i < windowSize; i++) {
            int temp = next + i >= limitSeqNumb ? next + i - limitSeqNumb : next + i;
            seqNumbInWindow.offer(temp);
        }
        return seqNumbInWindow.contains(seqNumb);
    }

    public int getCountTo5() {
        return countTo5;
    }

    public int getCountACK() {
        return countACK;
    }

    private void sendCumulativeACK() {
        final int ID = 1;
        int seqNumb = 0;
        int ackNumb = next;
        String payload = "";
        int check = checksum.calculateChecksum(seqNumb, ackNumb, payload);
        NetworkSimulator.toLayer3(ID, new Packet(seqNumb, ackNumb, check, payload));
        countACK += 1;
    }

    private void dealWithInOrder(Packet packet) {
        String payload = packet.getPayload();
        // send all this consecutive to layer5
        NetworkSimulator.toLayer5(payload);
        countTo5 += 1;
        next = next >= limitSeqNumb - 1 ? 0 : next + 1;
    }

    // called by simulator
    public void input(Packet packet) {
//        System.out.println(" B received packet-------------------------------------------------------------------");
//        System.out.println(packet.toString());
//        System.out.println("--------------------------------------------------------------------------------");
        // 1. Check if the packet is corrupted and drop it
        int seqNumb = packet.getSeqnum();
        int checkSum = checksum.calculateChecksum(packet);
        if (checkSum != packet.getChecksum()) {
            System.out.println("B corrupt");
            return;
        }

        // 3. If the data packet in-order, deliver the data to layer5 and send ACK to A. Note
        //that you might have subsequent data packets waiting in the buffer at B that also need to be
        //delivered to layer5
        if (seqNumb == next) {
            dealWithInOrder(packet);
            // check out of buffer, if there are consecutive, addToInOrder()
            Set<Integer> removed = new HashSet<>();
            for (Integer seq : outOfOrderBuffer.keySet()) {
                if (seq.equals(next)) {
                    Packet p = outOfOrderBuffer.get(seq);
                    removed.add(seq);
                    dealWithInOrder(p);
                }
            }
            for (Integer r : removed) {
                outOfOrderBuffer.remove(r);
            }
            sendCumulativeACK();
        } else {
            // in window, out of order
            if (isInWindow(seqNumb)) {
                //3. If the data packet is out of order, buffer the data packet and send an ACK
                System.out.println("B out of order");
                if (!outOfOrderBuffer.containsKey(seqNumb)) {
                    outOfOrderBuffer.put(seqNumb, packet);
                }
                sendCumulativeACK();
            } else {
                // out of window, duplicate
                System.out.println("B duplicate");
                sendCumulativeACK();
            }
        }
    }
}
