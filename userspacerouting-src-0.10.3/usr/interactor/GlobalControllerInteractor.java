package usr.interactor;

import usr.protocol.MCRP;
import usr.logging.*;
import java.net.Socket;
import usr.common.LocalHostInfo;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import us.monoid.web.*;
import us.monoid.json.*;

/**
 * This class implements the MCRP protocol and acts as a client
 * for interacting with the ManagementConsole of a GlobalController.
 */
public class GlobalControllerInteractor {
    // A URI for a GlobalController to interact with
    String globalControllerURI;
    Resty rest;
    int port;

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a GlobalController.
     * @param addr the name of the host
     * @param port the port the server is listening on
     */
    public GlobalControllerInteractor(String addr, int port) throws UnknownHostException, IOException  {
        initialize(InetAddress.getByName(addr), port);
    }

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a GlobalController.
     * @param addr the InetAddress of the host
     * @param port the port the server is listening on
     */
    public GlobalControllerInteractor(InetAddress addr, int port) throws UnknownHostException, IOException  {
        initialize(addr, port);
    }

    /**
     * Constructor for a MCRP connection
     * to the ManagementConsole of a GlobalController.
     * @param lh the LocalHostInfo description
     */
    public GlobalControllerInteractor(LocalHostInfo lh) throws UnknownHostException, IOException  {
        initialize(lh.getIp(), lh.getPort());
    }

    /**
     * Initialize
     */
    private synchronized void initialize(InetAddress addr, int port) {
        this.port = port;
        //URI uri = new URI("http", null, addr.toString(), port, null, null, null);
        globalControllerURI = "http://" + addr.getHostName() + ":" + Integer.toString(port);

        Logger.getLogger("log").logln(USR.STDOUT, "globalControllerURI: " + globalControllerURI);

        rest = new Resty();
    }

    /**
     * Get the port this LocalControllerInteractor is connecting to
     */
    public int getPort() {
        return port;
    }

    /**
     * Interact
     */
    private JSONObject interact(String str) throws IOException, JSONException {
        String uri = globalControllerURI +  "/command/" + java.net.URLEncoder.encode(str, "UTF-8");

        Logger.getLogger("log").logln(USR.STDOUT, "GC call: " + uri.substring(0, Math.min(64, uri.length())));

        JSONObject jsobj = rest.json(uri).toObject();

        Logger.getLogger("log").logln(USR.STDOUT, "GC response: " + jsobj.toString());

        return jsobj;
    }

    /* Calls for ManagementConsole */

    /**
     * Responds to the GlobalController.
     */
    public Boolean respondToGlobalController(LocalHostInfo lc) throws IOException, JSONException {
        String command = MCRP.OK_LOCAL_CONTROLLER.CMD+" "+lc.getName()+" "+ lc.getPort();
        interact(command);
        return true;
    }

    /**
     * Sends collected router stats to the global controller
     */
    public Boolean sendRouterStats(String stats) throws IOException, JSONException {

        String command = MCRP.SEND_ROUTER_STATS.CMD+" "+stats;
        interact(command);
        return true;
    }

    /**
     * Quit talking to the router
     * Close a connection to the ManagementConsole of the router.
     */
    public Boolean quit() throws IOException, JSONException {
        return true;
    }

    /** Send a message to a local controller informing it about a routers
       status as an aggregation point */
    public Boolean reportAP(int GID, int AP) throws IOException, JSONException {
        String toSend = MCRP.REPORT_AP.CMD + " " + GID + " " +AP;

        interact(toSend);
        return true;
    }

    /**
     * Get the networkGraph as a String representation.
     */
    public String networkGraph(String arg) throws IOException, JSONException {
        String toSend = MCRP.NETWORK_GRAPH.CMD + " " + arg;

        JSONObject response = interact(toSend);

        // return the graph
        return (String)response.get("graph");
    }

}