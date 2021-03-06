package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import java.io.PrintStream;
import java.io.IOException;
import us.monoid.json.*;


/**
 * The READ_OPTIONS_FILE command
 */
public class ReadOptionsStringCommand extends RouterCommand {
    /**
     * Construct a GetAddressCommand.
     */
    public ReadOptionsStringCommand() {
        super(MCRP.READ_OPTIONS_STRING.CMD, MCRP.READ_OPTIONS_STRING.CODE,
              MCRP.READ_OPTIONS_STRING.ERROR);
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
            String rest = value.substring(MCRP.READ_OPTIONS_STRING.CMD.length()).trim();
            // Logger.getLogger("log").logln(USR.ERROR, "RECEIVED STRING");
            // Logger.getLogger("log").logln(USR.ERROR, rest);

            String options = java.net.URLDecoder.decode(rest, "UTF-8");

            if (controller.readOptionsString(options)) {
                JSONObject jsobj = new JSONObject();

                jsobj.put("response", "Read Options String");
                //jsobj.put("options", options);
                out.println(jsobj.toString());
                response.close();

                return true;

            } else {
                response.setCode(302);

                JSONObject jsobj = new JSONObject();
                jsobj.put("error", "Cannot read XML Options String");

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
