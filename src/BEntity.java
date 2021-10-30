public class BEntity {
    BEntity(int windowSize) {

    }

    // called by simulator
    public void input(Packet packet) {
        // 1. Check if the packet is corrupted and take appropriate actions
        // 2. If the data packet is new, and in-order, deliver the data to layer5 and send ACK to A. Note
        //that you might have subsequent data packets waiting in the buffer at B that also need to be
        //delivered to layer5
        //3. If the data packet is new, and out of order, buffer the data packet and send an ACK
        //4. If the data packet is duplicate, drop it and send an ACK

    }
}
