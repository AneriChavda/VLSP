package usr.localcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;

/**
 * The ROUTER_CONFIG command takes a string for default router config
 */
public class RouterConfigCommand extends LocalCommand {
    /**
     * Construct a RouterConfigCommand
     */
    public RouterConfigCommand() {
        super(MCRP.ROUTER_CONFIG.CMD);
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
            String rest = value.substring(MCRP.ROUTER_CONFIG.CMD.length()).trim();

            String options = java.net.URLDecoder.decode(rest, "UTF-8");
            controller.setRouterOptions(options);

            JSONObject jsobj = new JSONObject();

            jsobj.put("msg", "Router Config String received");
            jsobj.put("success", Boolean.TRUE);
            out.println(jsobj.toString());
            response.close();

            return true;

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