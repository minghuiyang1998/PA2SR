import java.util.HashMap;

public class BEntity {
    private final int windowSize;
    private final Checksum checksum;
    private final HashMap<Integer, Packet> buffer;  // buffer all the unAcked packets that generated from received messages

    BEntity(int windowSize) {
        this.windowSize = windowSize;
        this.checksum = new Checksum();
        this.buffer = new HashMap<>();
    }

    // called by simulator
    public void input(Packet packet) {
        // 1. Check if the packet is corrupted and drop it
        int seqNumb = packet.getSeqnum();
        int ackNumb = packet.getAcknum();
        String payload = packet.getPayload();
        int checkSum = checksum.calculateChecksum(seqNumb, ackNumb, payload);
        if (checkSum != packet.getChecksum()) {
            return;
        }
        // 2. If the data packet is new, and in-order, deliver the data to layer5 and send ACK to A. Note
        //that you might have subsequent data packets waiting in the buffer at B that also need to be
        //delivered to layer5

        //3. If the data packet is new, and out of order, buffer the data packet and send an ACK
        //4. If the data packet is duplicate, drop it and send an ACK

    }
}
