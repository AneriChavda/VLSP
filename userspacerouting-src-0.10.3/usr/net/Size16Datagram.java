package usr.net;

import java.nio.ByteBuffer;
import usr.logging.*;
import java.net.UnknownHostException;

/**
 * An abstract Foundation class for Datagrams that use Size16 addresses.
 */
class Size16Datagram implements Datagram, DatagramPatch {
    // The full datagram is 48 bytes plus the payload
    // as an  Address is 16 bytes long
    final static int HEADER_SIZE = 48;
    final static int CHECKSUM_SIZE = 4;

    // USRD       - 0 / 4
    // hdr size   - 4 / 1
    // total len  - 5 / 2
    // flags      - 7 / 1
    // ttl        - 8 / 1
    // protocol   - 9 / 1
    // src addr   - 10 / 16
    // dst addr   - 26 / 16
    // spare      - 42 / 1
    // spare      - 43 / 1
    // src port   - 44 / 2
    // dst port   - 46 / 2
    // payload    - 48 / N
    // checksum   - 48 + N / 4

    // The full datagram contents
    ByteBuffer fullDatagram;

    // Dst address
    Address dstAddr = null;

    // Dst port
    int dstPort = 0;

    static int initialTTL_ = 64; // TTL used for new

    /**
     * Construct a Size16Datagram given a payload.
     */
    Size16Datagram(ByteBuffer payload) {
        payload.rewind();
        int payloadSize = payload.limit();
        //Logger.getLogger("log").logln(USR.ERROR, "PAYLOAD SIZE "+payloadSize);
        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    /**
     * Construct a Size16Datagram given a payload.
     */
    Size16Datagram(byte[] payload) {
        this(ByteBuffer.wrap(payload));
    }

    /**
     * Construct a Size16Datagram given a payload and a destination address
     */
    Size16Datagram(ByteBuffer payload, Address address) {
        payload.rewind();
        dstAddr = address;

        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    /**
     * Construct a Size16Datagram given a payload and a destination address
     */
    Size16Datagram(byte[] payload, Address address) {
        this(ByteBuffer.wrap(payload), address);
    }

    /**
     * Construct a Size16Datagram given a payload, a destination address,
     * and a destination port.
     */
    Size16Datagram(ByteBuffer payload, Address address, int port) {
        payload.rewind();
        dstAddr = address;
        dstPort = port;

        int payloadSize = payload.limit();

        fullDatagram = ByteBuffer.allocate(payloadSize + HEADER_SIZE + CHECKSUM_SIZE);

        fillDatagram(payload);
    }

    /**
     * Construct a Size16Datagram given a payload, a destination address,
     * and a destination port.
     */
    Size16Datagram(byte[] payload, Address address, int port) {
        this(ByteBuffer.wrap(payload), address, port);
    }

    Size16Datagram() {
    }

    /**
     * Get the length of the data, i.e. the payload length.
     */
    public int getLength() {
        return getTotalLength() - getHeaderLength() - getChecksumLength();
    }

    /**
     * Get the header len
     */
    public byte getHeaderLength() {
        // return HEADER_SIZE;
        return fullDatagram.get(4);
    }

    /**
     * Get the total len
     */
    public short getTotalLength() {
        return fullDatagram.getShort(5);
    }

    /**
     * Get the checksum size
     */
    public byte getChecksumLength() {
        return (byte)(CHECKSUM_SIZE & 0xFF);
    }

    /**
     * Get the flags
     */
    public byte getFlags() {
        return fullDatagram.get(7);
    }

    /**
     * Get the TTL
     */
    public int getTTL() {
        byte b = fullDatagram.get(8);
        return 0 | (0xFF & b);
    }

    /**
     * Set the TTL
     */
    public Datagram setTTL(int ttl) {
        byte b = (byte)(ttl & 0xFF);
        fullDatagram.put(8, b);
        return this;
    }

    /**
     * Get the protocol
     */
    public byte getProtocol() {
        byte b = fullDatagram.get(9);
        return b;
    }

    /**
     * Set the protocol
     */
    public Datagram setProtocol(int p) {
        byte b = (byte)(p & 0xFF);
        fullDatagram.put(9, b);

        return this;
    }

    /**
     * Get src address.
     */
    public Address getSrcAddress() {
        // get 4 bytes for address
        byte[] address = new byte[16];
        fullDatagram.position(10);
        fullDatagram.get(address, 0, 16);

        if (emptyAddress(address)) {
            return null;
        } else {
            return AddressFactory.newAddress(address);
        }
    }

    /**
     * Set the src address
     */
    public Datagram setSrcAddress(Address addr) {
        if (addr != null && !(addr instanceof Size16)) {
            throw new UnsupportedOperationException("Cannot use " + addr.getClass().getName() + " addresses in Size16Datagram");
        }

        // put src addr
        fullDatagram.position(10);

        if (addr == null) {
            fullDatagram.put(Size16.EMPTY, 0, 16);
        } else {
            fullDatagram.put(addr.asByteArray(), 0, 16);
        }

        return this;
    }

    /**
     * Get dst address.
     */
    public Address getDstAddress() {
        // get 4 bytes for address
        byte[] address = new byte[16];
        fullDatagram.position(26);
        fullDatagram.get(address, 0, 16);

        if (emptyAddress(address)) {
            return null;
        } else {
            return AddressFactory.newAddress(address);
        }
    }

    /**
     * Set the dst address
     */
    public Datagram setDstAddress(Address addr) {
        if (addr != null && !(addr instanceof Size16)) {
            throw new UnsupportedOperationException("Cannot use " + addr.getClass().getName() + " addresses in Size16Datagram");
        }

        dstAddr = addr;
        fullDatagram.position(26);

        // put dst addr
        // to be filled in later
        if (addr == null) {
            fullDatagram.put(Size16.EMPTY, 0, 16);
        } else {
            fullDatagram.put(dstAddr.asByteArray(), 0, 16);
        }

        return this;
    }

    boolean emptyAddress(byte [] address) {
        for (int i = 0; i < 16; i++) {
            if (address[i] != Size16.EMPTY[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get src port.
     */
    public int getSrcPort() {
        int p = (int)fullDatagram.getShort(44);

        // convert signed to unsigned
        if (p < 0) {
            return p + 65536;
        } else {
            return p;
        }
    }

    /**
     * Set the src port
     */
    public Datagram setSrcPort(int p) {
        fullDatagram.putShort(44, (short)p);

        return this;
    }

    /**
     * Get dst port.
     */
    public int getDstPort() {
        int p = (int)fullDatagram.getShort(46);

        // convert signed to unsigned
        if (p < 0) {
            return p + 65536;
        } else {
            return p;
        }
    }

    /**
     * Set the dst port
     */
    public Datagram setDstPort(int p) {
        dstPort = p;
        fullDatagram.putShort(46, (short)dstPort);

        return this;
    }

    /** Reduce TTL and return true if packet still valid */
    public boolean TTLReduce() {
        int ttl = getTTL();
        ttl--;
        setTTL(ttl);

        if (ttl <= 0) {
            return false;
        }
        return true;
    }

    /**
     * Get header
     */
    public byte[] getHeader() {
        int headerLen = getHeaderLength();

        fullDatagram.position(0);

        byte[] headerBytes = new byte[headerLen];

        fullDatagram.get(headerBytes);

        return headerBytes;
    }

    /**
     * Get payload
     */
    public byte[] getPayload() {
        int headerLen = getHeaderLength();
        int totalLen = getTotalLength();

        // Logger.getLogger("log").logln(USR.ERROR, "Size16Datagram getPayload: headerLen = " + headerLen + " totalLen = " +
        // totalLen);

        fullDatagram.position(headerLen);

        byte[] payloadBytes = new byte[totalLen - CHECKSUM_SIZE - headerLen];

        fullDatagram.get(payloadBytes);

        //ByteBuffer payload =  ByteBuffer.wrap(payloadBytes);

        // Logger.getLogger("log").logln(USR.ERROR, "Size16Datagram getPayload: payload = " + payload.position() + " < " +
        // payload.limit() + " < " + payload.capacity());

        return payloadBytes;

    }

    /**
     * Get payload
     */
    public byte[] getData() {
        return getPayload();
    }

    /**
     * Get the checksum
     */
    public byte[] getChecksum() {
        int checksumLen = getChecksumLength();
        int totalLen = getTotalLength();

        fullDatagram.position(totalLen - totalLen);

        byte [] checksumBytes = new byte[checksumLen];

        fullDatagram.get(checksumBytes);

        return checksumBytes;
    }

    /**
     * To ByteBuffer.
     */
    public ByteBuffer toByteBuffer() {
        fullDatagram.rewind();
        return fullDatagram;
    }

    /**
     * From ByteBuffer.
     */
    public boolean fromByteBuffer(ByteBuffer b) {
        fullDatagram = b;

        // Logger.getLogger("log").logln(USR.ERROR, "Size16Datagram fromByteBuffer: fullDatagram = " + fullDatagram.position() + " <
        // " + fullDatagram.limit() + " < " + fullDatagram.capacity());

        return true;
    }

    /**
     * Fill in the Datagram with the relevant values.
     */
    void fillDatagram(ByteBuffer payload) {
        // put USRD literal - 4 bytes
        fullDatagram.put("USRD".getBytes(), 0, 4);
        //Logger.getLogger("log").logln(USR.ERROR, "USRD set");
        // put header len
        fullDatagram.put(4, (byte)(HEADER_SIZE & 0xFF));

        // put total len
        fullDatagram.putShort(5, (short)fullDatagram.capacity());

        // put flags
        byte flags = 0;
        fullDatagram.put(7, (byte)flags);

        // put ttl
        // start with default
        fullDatagram.put(8, (byte)initialTTL_);

        // protocol
        byte protocol = 0;
        fullDatagram.put(9, (byte)protocol);

        // put src addr
        fullDatagram.position(10);
        fullDatagram.put(Size16.EMPTY, 0, 16);

        // put dst addr
        // to be filled in later
        fullDatagram.position(26);

        if (dstAddr == null) {
            fullDatagram.put(Size16.EMPTY, 0, 16);
        } else {
            fullDatagram.put(dstAddr.asByteArray(), 0, 16);
        }

        // 2 spare bytes
        fullDatagram.put(42, (byte)0);
        fullDatagram.put(43, (byte)0);

        // put src port
        fullDatagram.putShort(44, (short)0);

        // put dst port
        // to be filled in later
        fullDatagram.putShort(46, (short)dstPort);

        /*
         * copy in payload
         */
        fullDatagram.position(48);
        payload.rewind();
        // Logger.getLogger("log").logln(USR.ERROR, "payload size = " + payload.limit());

        fullDatagram.put(payload);

        // evaluate checksum
        int checksum = -1;
        fullDatagram.putInt(fullDatagram.capacity() - 4, checksum);

        fullDatagram.rewind();

        // Logger.getLogger("log").logln(USR.ERROR, "Size16Address fillDatagram: fullDatagram = " + fullDatagram.position() + " < "
        // + fullDatagram.limit() + " < " + fullDatagram.capacity());

    }

    /**
     * To String
     */
    public String toString() {
        return "( src: " + getSrcAddress() + "/" + getSrcPort() +
               " dst: " + getDstAddress() + "/" + getDstPort() +
               " len: " + getTotalLength() + " proto: " + getProtocol() +
               " ttl: " + getTTL() + " payload: " + (getTotalLength() - HEADER_SIZE - CHECKSUM_SIZE) +
               " )";
    }

}
