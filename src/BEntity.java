import java.util.HashMap;

public class BEntity {
    private final int windowSize;
    private final Checksum checksum;
    private final HashMap<Integer, Packet> outOfOrderBuffer;
    private int next = 0;
    private int rightBound = 0;

    BEntity(int windowSize) {
        this.windowSize = windowSize;
        this.checksum = new Checksum();
        this.outOfOrderBuffer = new HashMap<>();
    }

    private void sendCumulativeACK() {
        final int ID = 0;
        int seqNumb = 0;
        int ackNumb = next;
        String payload = "";
        int check = checksum.calculateChecksum(seqNumb, ackNumb, payload);
        NetworkSimulator.toLayer3(ID, new Packet(seqNumb, ackNumb, check, payload));
    }

    private void dealWithInOrder(Packet packet) {
        next += 1;
        String payload = packet.getPayload();
        // send all this consecutive to layer5
        NetworkSimulator.toLayer5(payload);
    }

    private boolean isDuplicate(int seqNumb) {
        return seqNumb < next || outOfOrderBuffer.containsKey(seqNumb);
    }

    private boolean isFull() {
        return rightBound - next + 1 >= windowSize;
    }

    // called by simulator
    public void input(Packet packet) {
        // 1. Check if the packet is corrupted and drop it
        int seqNumb = packet.getSeqnum();
        int checkSum = checksum.calculateChecksum(packet);
        if (checkSum != packet.getChecksum()) {
            return;
        }

        // If the data packet is duplicate, drop it and send an ACK
        if (isDuplicate(seqNumb)) {
            sendCumulativeACK();
            return;
        }

        // 2. If the buffer is full, drop it, else continue
        if (seqNumb > rightBound) {
            rightBound = seqNumb;
        }
        if (isFull()) return;

        // 3. If the data packet in-order, deliver the data to layer5 and send ACK to A. Note
        //that you might have subsequent data packets waiting in the buffer at B that also need to be
        //delivered to layer5
        if (seqNumb == next) {
            dealWithInOrder(packet);
            // check out of buffer, if there are consecutive, addToInOrder()
            for (Integer seq : outOfOrderBuffer.keySet()) {
                if (seq.equals(next)) {
                    Packet p = outOfOrderBuffer.remove(next);
                    dealWithInOrder(p);
                }
            }
            sendCumulativeACK();
            return;
        }

        //3. If the data packet is out of order, buffer the data packet and send an ACK
        outOfOrderBuffer.put(seqNumb, packet);
        sendCumulativeACK();
    }
}
