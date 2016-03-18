package usr.router.command;

import usr.protocol.MCRP;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.net.*;
import java.util.Scanner;


/**
 * The SET_ROUTER_ADDRESS command.
 * SET_ROUTER_ADDRESS address
 * SET_ROUTER_ADDRESS 47
 */
public class SetRouterAddressCommand extends RouterCommand {
    /**
     * Construct a SetRouterAddressCommand
     */
    public SetRouterAddressCommand() {
        super(MCRP.SET_ROUTER_ADDRESS.CMD, MCRP.SET_ROUTER_ADDRESS.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(Request request, Response response) {
        try {
            PrintStream out = response.getPrintStream();

            // get full request string
            String path = java.net.URLDecoder.decode(request.getPath().getPath(), "UTF-8");
            // strip off /command
            String value = path.substring(9);
            // strip off COMMAND
            String idStr = value.substring(MCRP.SET_ROUTER_ADDRESS.CMD.length()).trim();

            boolean result;

            Address addr = null;
            try {
                addr = AddressFactory.newAddress(idStr);
            } catch (java.net.UnknownHostException e) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", getName() + "Cannot construct address from "+idStr);

                out.println(jsobj.toString());
                response.close();

                return false;
            }

            // set address
            boolean idSet = controller.setAddress(addr);

            if (idSet) {
                JSONObject jsobj = new JSONObject();
                jsobj.put("address", addr.asTransmitForm());

                out.println(jsobj.toString());
                response.close();

                return true;
            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", getName() + "Cannot set Global Address after communication");

                out.println(jsobj.toString());
                response.close();

                return false;
            }

        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
        } catch (JSONException jex) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
        }

        finally {
            return false;
        }
    }

}