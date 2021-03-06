package usr.globalcontroller.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.globalcontroller.*;
import usr.common.LocalHostInfo;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;

/**
 * A LocalOKCommand.
 */
public class LocalOKCommand extends GlobalCommand {
    /**
     * Construct a LocalOKCommand.
     */
    public LocalOKCommand() {
        super(MCRP.OK_LOCAL_CONTROLLER.CMD);
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

            String [] args = value.split(" ");

            if (args.length != 3) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Expected three arguments for LocalOKCommand");

                out.println(jsobj.toString());
                response.close();
                return false;
            }

            // process args
            String hostName = args[1];
            int port = Integer.parseInt(args[2]);
            LocalHostInfo lh = null;
            try {
                lh = new LocalHostInfo(hostName, port);
            } catch (Exception e) {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Cannot parse host from LocalOKCommand");

                out.println(jsobj.toString());
                response.close();
                return false;

            }

            // ok
            controller.aliveMessage(lh);
            JSONObject jsobj = new JSONObject();

            jsobj.put("msg", "Local OK received from "+args[1]+":"+args[2]);
            out.println(jsobj.toString());
            response.close();

            return true;

        } catch (IOException ioe) {
            ioe.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR, leadin() + ioe.getMessage());
            return false;
        } catch (JSONException jex) {
            jex.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR, leadin() + jex.getMessage());
            return false;
        }

    }

}
