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
 * A class to handle /router/ requests
 */
public class RouterRestHandler extends USRRequestHandler {
    // get VimNem
    VimNem gc;

    public RouterRestHandler() {
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
                    createRouter(request, response);
                } else {
                    notFound(response, "POST bad request");
                }
            } else if (method.equals("DELETE")) {
                if (segments.length == 2) {
                    // looks like a delete
                    deleteRouter(request, response);
                } else {
                    notFound(response, "DELETE bad request");
                }
            } else if (method.equals("GET")) {
                if (name == null) {      // no arg, so list routers
                    listRouters(request, response);
                } else if (segments.length == 2) {   // get router info
                    getRouterInfo(request, response);
                } else if (segments.length == 3 || segments.length == 4) {   // get router other data e.g. link stats
                    getRouterOtherData(request, response);
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
     * Create router given a request and send a response.
     */
    public void createRouter(Request request, Response response) throws IOException, JSONException {
        // Args:
        // [name]
        // [address]

        String name = "";
        String address = "";

        Query query = request.getQuery();

        /* process optional args */

        if (query.containsKey("name")) {
            name = query.get("name");
        }

        if (query.containsKey("address")) {
            address = query.get("address");
        }


        /* do work */

        // start a router, and get it's ID
        int rID = gc.startRouter(System.currentTimeMillis(), address, name);

        if (rID < 0) {
            // error
            complain(response, "Error creating router");

        } else {
            // now lookup all the saved details
            BasicRouterInfo bri = gc.findRouterInfo(rID);

            // and send them back as the return value
            PrintStream out = response.getPrintStream();

            JSONObject jsobj = new JSONObject();

            jsobj.put("routerID", bri.getId());
            jsobj.put("name", bri.getName());
            jsobj.put("address", bri.getAddress());
            jsobj.put("mgmtPort", bri.getManagementPort());
            jsobj.put("r2rPort", bri.getRoutingPort());

            out.println(jsobj.toString());

        }
    }

    /**
     * Delete a router given a request and send a response.
     */
    public void deleteRouter(Request request, Response response) throws IOException, JSONException {
        // if we got here we have 2 parts
        // /router/ and another bit
        String name = request.getPath().getName();
        Scanner sc = new Scanner(name);

        if (sc.hasNextInt()) {
            int id = sc.nextInt();

            // if it exists, stop it, otherwise complain
            if (gc.isValidRouterID(id)) {
                // delete a router
                gc.endRouter(System.currentTimeMillis(), id);

                // and send them back as the return value
                PrintStream out = response.getPrintStream();

                JSONObject jsobj = new JSONObject();

                jsobj.put("status", "done");

                out.println(jsobj.toString());
            } else {
                complain(response, "deleteRouter arg is not valid router id: " + name);
            }

        } else {
            complain(response, "deleteRouter arg is not Integer: " + name);
        }
    }

    /**
     * List routers given a request and send a response.
     */
    public void listRouters(Request request, Response response) throws IOException, JSONException {
        // process query

        Query query = request.getQuery();

        // the attribute we want about the router
        String detail;

        if (query.containsKey("detail")) {
            detail = query.get("detail");

            // check detail
            if (detail.equals("id") ||
                detail.equals("all")) {
                // fine
            } else {
                complain(response, "Bad detail: " + detail);
            }

        } else {
            detail = "id";
        }


        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj = gc.getAllRouterInfoAsJSON(detail);


        out.println(jsobj.toString());

    }

    /**
     * Get info on a router given a request and send a response.
     */
    public void getRouterInfo(Request request, Response response) throws IOException, JSONException {
        // if we got here we have 2 parts
        // /router/ and another bit
        String name = request.getPath().getName();
        Scanner sc = new Scanner(name);

        if (sc.hasNextInt()) {
            int routerID = sc.nextInt();

            // if it exists, get data, otherwise complain
            if (!gc.isValidRouterID(routerID)) {
                complain(response, " arg is not valid router id: " + name);
                response.close();
                return;
            }


            // and send them back as the return value
            PrintStream out = response.getPrintStream();


            JSONObject jsobj = gc.findRouterInfoAsJSON(routerID);

            out.println(jsobj.toString());


        } else {
            // not an Integer
            if (name.equals("maxid")) {
                int maxid = gc.getMaxRouterId();

                // and send them back as the return value
                PrintStream out = response.getPrintStream();

                JSONObject jsobj = new JSONObject();

                jsobj.put("value", maxid);

                out.println(jsobj.toString());

            } else if (name.equals("count")) {
                int count = gc.getNoRouters();

                // and send them back as the return value
                PrintStream out = response.getPrintStream();

                JSONObject jsobj = new JSONObject();

                jsobj.put("value", count);

                out.println(jsobj.toString());

            } else {
                complain(response, "getRouterInfo arg is not appropriate: " + name);
            }

        }

    }

    /**
     * Get other data on a router given a request and send a response.
     * e.g. link stats
     */
    public void getRouterOtherData(Request request, Response response) throws IOException, JSONException {
        // if we got here we have 3 parts
        // /router/ an id and another bit
        int routerID = 0;
        int dstID = 0;

        Scanner scanner;

        // get the path
        Path path = request.getPath();
        String[] segments = path.getSegments();


        // process router ID
        // it is 2nd element of segments
        String routerValue = segments[1];

        scanner = new Scanner(routerValue);

        if (scanner.hasNextInt()) {
            routerID = scanner.nextInt();

        } else {
            badRequest(response, "arg routerID is not an Integer");
            response.close();
            return;
        }

        // if it exists, get data, otherwise complain
        if (!gc.isValidRouterID(routerID)) {
            complain(response, " arg is not valid router id: " + routerValue);
            response.close();
            return;
        }


        // process name
        // it is 3rd element of segments
        String name = segments[2];


        // check if we need the dstID
        if (segments.length == 4) {
            // process dst router ID
            // it is 4th element of segments
            String dstValue = segments[3];

            scanner = new Scanner(dstValue);

            if (scanner.hasNextInt()) {
                dstID = scanner.nextInt();

            } else {
                badRequest(response, "arg dstID is not an Integer");
                response.close();
                return;
            }

            // if it exists, get data, otherwise complain
            if (!gc.isValidRouterID(dstID)) {
                complain(response, " arg is not valid router id: " + dstValue);
                response.close();
                return;
            }

        }
        
        // not an Integer
        if (name.equals("link_stats")) {
            // and send them back as the return value
            PrintStream out = response.getPrintStream();

            JSONObject jsobj = null;

            if (segments.length == 3) {
                // get all link stats
                jsobj = gc.getRouterLinkStatsAsJSON(routerID);
            } else if (segments.length == 4) {
                // get specified link stats
                jsobj = gc.getRouterLinkStatsAsJSON(routerID, dstID);
            }


            out.println(jsobj.toString());

        } else {
            complain(response, "getRouterOtherData arg is not appropriate: " + name);
        }

    }

}
