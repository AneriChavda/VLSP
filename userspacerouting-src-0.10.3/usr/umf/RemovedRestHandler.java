package usr.umf;

import usr.globalcontroller.GlobalController;
import usr.logging.*;
import usr.common.BasicRouterInfo;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import us.monoid.json.*;
import java.util.Scanner;
import java.io.PrintStream;
import java.io.IOException;

/**
 * A class to handle /ap/ requests
 */
public class RemovedRestHandler extends USRRequestHandler {
    // get VimNem
    VimNem gc;

    public RemovedRestHandler() {
    }

    /**
     * Handle a request and send a response.
     */
    public boolean handle(Request request, Response response) {
        // get VimNem
        gc = (VimNem)getManagementConsole().getAssociated();

        try {
            /*
               System.out.println("method: " + request.getMethod());
               System.out.println("target: " + request.getTarget());
               System.out.println("path: " + request.getPath());
               System.out.println("directory: " + request.getPath().getDirectory());
               System.out.println("name: " + request.getPath().getName());
               System.out.println("segments: " + java.util.Arrays.asList(request.getPath().getSegments()));
               System.out.println("query: " + request.getQuery());
               System.out.println("keys: " + request.getQuery().keySet());
             */

            System.out.println("REQUEST: " + request.getMethod() + " " +  request.getTarget());

            long time = System.currentTimeMillis();

            response.set("Content-Type", "application/json");
            response.set("Server", "VimNem/1.0 (SimpleFramework 4.0)");
            response.setDate("Date", time);
            response.setDate("Last-Modified", time);

            // get the path
            Path path = request.getPath();
            String directory = path.getDirectory();
            String name = path.getName();
            String[] segments = path.getSegments();

            // Get the method
            String method = request.getMethod();

            // Get the Query
            Query query = request.getQuery();

            // and evaluate the input
            if (method.equals("POST")) {
                if (name == null) {
                    // looks like a create
                    setAP(request, response);
                } else {
                    notFound(response, "POST bad request");
                }
            } else if (method.equals("DELETE")) {
                if (segments.length == 2) {
                    // looks like a delete
                    notFound(response, "DELETE bad request");
                } else {
                    notFound(response, "DELETE bad request");
                }
            } else if (method.equals("GET")) {
                if (name == null) {      // no arg, so removed elements
                    listRemoved(request, response);
                } else {
                    notFound(response, "GET bad request");
                }
            } else if (method.equals("PUT")) {
                {
                    badRequest(response, "PUT bad request");
                }
            } else {
                badRequest(response, "Unknown method" + method);
            }



            // check if the response is closed
            response.close();

            return true;

        } catch (IOException ioe) {
            System.err.println("IOException " + ioe.getMessage());
        } catch (JSONException jse) {
            System.err.println("JSONException " + jse.getMessage());
        }

        return false;

    }

    /**
     * Set an agg point given a request and send a response.
     */
    public void setAP(Request request, Response response) throws IOException, JSONException {
        // Args:
        // apID
        // routerID


        int apID;
        int routerID;
        Scanner scanner;

        Query query = request.getQuery();

        /* process compulsory args */

        // process arg routerID
        if (query.containsKey("routerID")) {
            scanner = new Scanner(query.get("routerID"));

            if (scanner.hasNextInt()) {
                routerID = scanner.nextInt();
            } else {
                badRequest(response, "arg routerID is not an Integer");
                response.close();
                return;
            }
        } else {
            badRequest(response, "missing arg routerID");
            response.close();
            return;
        }

        // process arg apID
        if (query.containsKey("apID")) {
            scanner = new Scanner(query.get("apID"));

            if (scanner.hasNextInt()) {
                apID = scanner.nextInt();
            } else {
                badRequest(response, "arg apID is not an Integer");
                response.close();
                return;
            }
        } else {
            badRequest(response, "missing arg apID");
            response.close();
            return;
        }

        /* do work */

        // if it exists, stop it, otherwise complain
        if (gc.isValidRouterID(routerID) && gc.isValidRouterID(apID)) {
            gc.apSet(routerID, apID);

            // and send back a the return value
            PrintStream out = response.getPrintStream();


            JSONObject jsobj = new JSONObject();

            jsobj.put("routerID", routerID);
            jsobj.put("ap", apID);

            out.println(jsobj.toString());
        } else {
            badRequest(response, "setAP arg is not valid router id: " + routerID + " OR not valid ap id: " + apID);
        }


    }

    /**
     * List agg points given a request and send a response.
     */
    public void listRemoved(Request request, Response response) throws IOException, JSONException {
        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj = gc.listShutdownRoutersAsJSON();

        out.println(jsobj.toString());

    }

}