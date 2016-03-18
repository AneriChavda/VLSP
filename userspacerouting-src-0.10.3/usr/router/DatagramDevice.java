package usr.router;
import java.net.*;
import usr.net.Address;
import usr.logging.*;
import usr.net.Datagram;
import java.util.Map;
import java.net.Socket;
import java.io.IOException;

/**
 * A Minimal version of netIF for devices which can ship packets but
 * do not necessarily face the network.  NetIF provides a larger interface
 */
public interface DatagramDevice {

    /**
     * Get the name of this Net Device
     */
    public String getName();

    /**
     * Set the name of this Net Device
     */
    public void setName(String name);

    /**
     * Get the Address for this connection.
     */
    public Address getAddress();

    /** Get the FabricDevice associated with Net Device */
    public FabricDevice getFabricDevice();

    /**
     * Set the Address for this connection.
     */
    public void setAddress(Address addr);

    /**
     * Send a Datagram originating at this host (sets src address) and
     */
    public boolean sendDatagram(Datagram dg) throws NoRouteToHostException;

    /**
     * forward a datagram (does not set src address)
     */
    public boolean enqueueDatagram(Datagram dg) throws NoRouteToHostException;

    /**
     *   Send the datagram onwards to the world
     */
    public boolean outQueueHandler(Datagram dg, DatagramDevice dd);


    /**
     * Get the Listener of a NetIF.
     */
    public NetIFListener getNetIFListener();

    /**
     * Set the Listener of NetIF.
     */
    public void setNetIFListener(NetIFListener l);

}