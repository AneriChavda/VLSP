package usr.router;

import usr.net.*;
import usr.logging.*;
import usr.common.ANSI;
import usr.protocol.Protocol;
import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * A Network Interface for a Router using TCP
 * <p>
 * On the reading side, it queues up Datagrams that come
 * from the remote end.
 * <p>
 * It also implements control datagrams, so one end can inform the
 * other end of stuff.
 */
public class TCPNetIF implements NetIF, Runnable {

    // The connection
    ConnectionOverTCP connection;
    // The name of this
    String name;
    // The weight
    int outq = 0;
    int weight;
    // int ID
    int id;
    boolean local_ = false;
    // Address at this end
    Address address;
    // Remote router name
    String remoteRouterName;
    // Remote Router Address
    Address remoteRouterAddress;
    // The Listener
    NetIFListener listener = null;
    // The RouterPort
    RouterPort port = null;

    long startTime = 0;
    long stopTime = 0;

    // closed ?
    Boolean closed = true;
    // got a remote close
    boolean remoteClose = false;
    // Is the current reading thread running
    boolean running_ = false;
    // Has the stream closed
    boolean eof = false;
    // Fabric device does the data transfer
    FabricDevice fabricDevice_ = null;
    // Run thread for main loop
    Thread runThread_ = null;

    Object runWait_ = null;

    CountDownLatch latch = null;


    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointSrc src, NetIFListener l) throws IOException {
        connection = new ConnectionOverTCP(src);
        listener = l;

    }

    /**
     * Construct a TCPNetIF around a Socket.
     */
    public TCPNetIF(TCPEndPointDst dst, NetIFListener l) throws IOException {
        connection = new ConnectionOverTCP(dst);
        listener = l;
    }

    /**
     * Start the netIF  -- TODO make the queue lengths settable in router
     * control
     */
    public synchronized void start() {
        running_ = true;
        //System.err.println("New fabric device listener "+listener);
        fabricDevice_ = new FabricDevice(this, listener);
        fabricDevice_.setInQueueDiscipline(FabricDevice.QUEUE_BLOCKING);
        fabricDevice_.setInQueueLength(100);
        fabricDevice_.setOutQueueDiscipline(FabricDevice.QUEUE_BLOCKING);
        fabricDevice_.setOutQueueLength(100);
        fabricDevice_.setName(name);
        fabricDevice_.start();
        latch = new CountDownLatch(1);
        runThread_ = new Thread(this, "/" + listener.getName() + "/" + name + "/TCPNetIF");
        runThread_.start();
    }

    /** Run method loops and grabs input from connection to queue in
       fabricDevice */
    public void run() {
        Datagram datagram = null;
        runWait_ = new Object();

        while (running_) {
            if (eof) {
                synchronized (runWait_) {
                    try {
                        runWait_.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            try {
                datagram = connection.readDatagram();
            } catch (Exception ioe) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "TCPNetIF readDatagram error " + connection +
                                              " IOException " + ioe);
                ioe.printStackTrace();
            }

            if (datagram == null) {
                eof = true;
                continue;
            }
            try {
                if (fabricDevice_.inIsBlocking()) {
                    fabricDevice_.blockingAddToInQueue(datagram, this);
                } else {
                    fabricDevice_.addToInQueue(datagram, this);
                }
            } catch (NoRouteToHostException e) {
            }
        }

        // reduce latch count by 1
        latch.countDown();

    }

    /**
     * Activate
     */
    public boolean connect() throws IOException {
        boolean conn = connection.connect();

        closed = false;
        start();
        return conn;
    }

    /**
     * Get the name of this NetIF.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this NetIF.
     */
    public void setName(String n) {
        name = n;

        if (fabricDevice_ != null) {
            fabricDevice_.setName(n);
        }

        if (runThread_ != null) {
            runThread_.setName("/" + listener.getName() + "/" + n + "/TCPNetIF");
        }
    }

    /**
     * Get the weight of this NetIF.
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Set the weight of this NetIF.
     */
    public void setWeight(int w) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + ANSI.YELLOW + " TCPNetIF " + name + " set weight " + w + ANSI.RESET_COLOUR);
        weight = w;
    }

    /**
     * Get the ID of this NetIF.
     */
    public int getID() {
        return id;
    }

    /**
     * Set the ID of this NetIF.
     */
    public void setID(int id) {
        this.id = id;
    }

    /**
     * Get the Address for this connection.
     */
    public Address getAddress() {
        return address; // WAS connection.getAddress();
    }

    /**
     * Set the Address for this connection.
     */
    public void setAddress(Address addr) {
        address = addr;
        connection.setAddress(addr);
    }

    /**
     * Get the name of the remote router this NetIF is connected to.
     */
    public String getRemoteRouterName() {
        return remoteRouterName;
    }

    /**
     * Set the name of the remote router this NetIF is connected to.
     */
    public void setRemoteRouterName(String name) {
        remoteRouterName = name;

    }

    /**
     * Get the Address  of the remote router this NetIF is connected to
     */
    public Address getRemoteRouterAddress() {
        return remoteRouterAddress;
    }

    /**
     * Set the Address  of the remote router this NetIF is connected to.
     */
    public void setRemoteRouterAddress(Address addr) {
        remoteRouterAddress = addr;
    }

    /**
     * Get the socket.
     */
    Socket getSocket() {
        return connection.getSocket();
    }

    /**
     * Get the Listener of this NetIF.
     */
    public NetIFListener getNetIFListener() {
        return listener;
    }

    /**
     * Set the Listener of this NetIF.
     */
    public void setNetIFListener(NetIFListener l) {
        if (listener != null) {
            Logger.getLogger("log").logln(USR.ERROR, "TCPNetIF: already has a NetIFListener");
        } else {
            listener = l;

            if (fabricDevice_ != null) {
                fabricDevice_.setListener(l);
            }
        }
    }

    /**
     * Get the RouterPort a NetIF is plugIged into.
     */
    public RouterPort getRouterPort() {
        return port;
    }

    /**
     * Set the RouterPort a NetIF is plugIged into.
     */
    public void setRouterPort(RouterPort rp) {
        port = rp;
    }

    /**
     * Send a Datagram -- sets source to this interface and puts the datagram
       on the incoming queue for this interface
     */
    public boolean sendDatagram(Datagram dg) throws NoRouteToHostException {
        if (running_ == true) {
            return enqueueDatagram(dg);
        } else {
            return false;
        }
    }

    /**
     * Puts a datagram on the incoming queue for this network interface
     */
    public boolean enqueueDatagram(Datagram dg) throws NoRouteToHostException {
        return fabricDevice_.blockingAddToInQueue(dg, this);
    }

    /** Finally send the datagram onwards */
    public boolean outQueueHandler(Datagram dg, DatagramDevice dd) {
        boolean sent = false;
        try {
            if (dg.getSrcAddress() == null) {
                dg.setSrcAddress(getAddress());
            }

            sent = connection.sendDatagram(dg);

            //Logger.getLogger("log").logln(USR.STDOUT, leadin() + " TCPNetIF " + name + " sent " + dg);
        } catch (ClosedByInterruptException cbie) {
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + " ClosedByInterruptException connection.send "+address+"->"+remoteRouterAddress);

            //cbie.printStackTrace();

            listener.closedDevice(this);
            return false;

        } catch (IOException e) {
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + " failure in connection.send "+address+"->"+remoteRouterAddress);
            //Logger.getLogger("log").logln(USR.ERROR, leadin() + e.getMessage());

            //e.printStackTrace();

            listener.closedDevice(this);
            return false;

        }

        if (sent == false) {
            listener.closedDevice(this);
            return false;
        }
        return true;
    }

    /** Close a netIF given remote end has called close -- this is done as a
       spawned process since it would otherwise block out queues which might need
       to be written to during close*/
    public void remoteClose() {
        if (closed) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Already closed when remoteClose() called");
            return;
        }
        Logger.getLogger("log").logln(USR.STDOUT, leadin() +"RemoteClose");
        remoteClose = true;


        CloseThread ct = new CloseThread(this, this.closed);
        Thread t = new Thread(ct, "RemoteClose-"+name);
        t.start();
    }

    /**
     * Close a NetIF -- must be synchronized to prevent close() exiting prematurely when a
     * remoteClose has already been encountered -- close will never exit until the netif actually
     * is closed -- synchronized on "closed" object to prevent lock ups with other sync objects
     */
    public void close() {
        synchronized (closed) { // prevent this running twice by blocking
            Logger.getLogger("log").logln(USR.STDOUT, leadin() +"  Close");

            if (closed) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Already closed when close() called");
                return;
            }
            closed = true;

            // send a ControlMessage if this is a local close
            // and not a remote close
            if (!remoteClose) {
                controlClose();
                Logger.getLogger("log").logln(USR.STDOUT, leadin()+" Did control close");
            }


            Logger.getLogger("log").logln(USR.STDOUT, leadin()+" About to stop fabricDevice_");
            // tell the fabricDevice to stop
            if (fabricDevice_ != null) {
                fabricDevice_.stop();
            }

            Logger.getLogger("log").logln(USR.STDOUT, leadin()+" Did stop fabricDevice_");

            running_ = false;

            // close the connection
            if (!remoteClose) {
                connection.close();
            }

            runThread_.interrupt();

            // join runThread when it is  finished
            try {
                latch.await();
            } catch (InterruptedException ie) {
            }
  
            /*
             * try {
             *   runThread_.join();
             * } catch (InterruptedException e) {
             *
             * }
             */

            if (runWait_ != null) {
                synchronized (runWait_) {
                    runWait_.notify();
                }
            }
        }
    }

    /**
     * Is closed.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Consturct and send a control message.
     */
    protected boolean controlClose() {
        synchronized (closed) {
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+" Sending controlClose to "+remoteRouterAddress);
            ByteBuffer buffer = ByteBuffer.allocate(1);
            String c = "C";
            buffer.put(c.getBytes());
            Datagram datagram = DatagramFactory.newDatagram(Protocol.CONTROL, buffer);
            ByteBuffer b = ((DatagramPatch)datagram).toByteBuffer();
            datagram.setDstAddress(remoteRouterAddress);
            try {
                sendDatagram(datagram);
                return true;

            } catch (Exception ioe) {
                Logger.getLogger("log").logln(USR.STDOUT, leadin()+" controlClose error " + connection + " IOException " + ioe);
                //ioe.printStackTrace();
                return true;
            }
        }
    }

    /**
     * Get the interface stats.
     * Returns a NetStats object.
     */
    public NetStats getStats() {
        return fabricDevice_.getNetStats();
    }

    /** Accessor function for the fabric device associated with this */
    public FabricDevice getFabricDevice() {
        return fabricDevice_;
    }

    /**
     * To String
     */
    public String toString() {
        Address address = getAddress();
        Address remoteAddress = getRemoteRouterAddress();

        String ifName = getRouterPort() == null ? ("No port") : ("if" + Integer.toString(getRouterPort().getPortNo()));

        return ifName + " W(" + getWeight() + ") = " +
               (address == null ? "No_Address" : "" + address) + " => " +
               //getRemoteRouterName() + " " +
               (remoteAddress == null ? "No_Remote_Address" : "" + remoteAddress);
    }

    public boolean equals(Object obj) {
        if (obj instanceof NetIF) {
            NetIF b = (NetIF)obj;
            return getName().equals(b.getName());
        } else {
            return false;
        }

    }

    public boolean isLocal() {
        return local_;
    }

    public void setLocal(boolean l) {
        local_ = l;
    }

    String leadin() {
        return "TCPNetIF "+name+":";
    }

    /** Thread to perform remote close on netif */
    class CloseThread implements Runnable {
        TCPNetIF netif_;
        CloseThread(TCPNetIF n, Boolean closed) {
            netif_ = n;
        }

        public void run() {
            // sclayman synchronized (closed) {
                netif_.close();
            //}
        }

    }

}
