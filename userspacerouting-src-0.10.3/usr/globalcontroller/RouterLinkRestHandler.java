package usr.globalcontroller;

import usr.logging.*;
import usr.common.LinkInfo;
import usr.common.Pair;
import cc.clayman.console.BasicRequestHandler;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import us.monoid.json.*;
import java.util.Scanner;
import java.util.Map;
import java.util.Collection;
import java.io.PrintStream;
import java.io.IOException;

/**
 * A class to handle /router/[0-9]+/link/ requests
 */
public class RouterLinkRestHandler extends BasicRequestHandler {
    // get GlobalController
    GlobalController gc;

    public RouterLinkRestHandler() {
    }

    /**
     * Handle a request and send a response.
     */
    public boolean handle(Request request, Response response) {
        // get GlobalController
        gc = (GlobalController)getManagementConsole().getAssociated();

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
            response.set("Server", "GlobalController/1.0 (SimpleFramework 4.0)");
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
                if (name == null && segments.length == 3) {
                    // looks like a create
                    notFound(response, "POST bad request");
                } else {
                    notFound(response, "POST bad request");
                }
            } else if (method.equals("DELETE")) {
                if (segments.length == 4) {
                    // looks like a delete
                    notFound(response, "DELETE bad request");
                } else {
                    notFound(response, "DELETE bad request");
                }
            } else if (method.equals("GET")) {
                if (name == null) {      // no arg, so list links
                    listLinks(request, response);
                } else if (segments.length == 4) {   // get link info
                    getLinkInfo(request, response);
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
     * List links given a request and send a response.
     */
    public void listLinks(Request request, Response response) throws IOException, JSONException {
        int routerID;

        Scanner scanner;

        // get the path - path len == 3
        Path path = request.getPath();
        String[] segments = path.getSegments();

        // process router ID
        // it is 2nd element of segments
        String routerValue = segments[1];

        scanner = new Scanner(routerValue);

        if (scanner.hasNextInt()) {
            routerID = scanner.nextInt();
        } else {
            badRequest(response, "arg router id is not an Integer");
            response.close();
            return;
        }

        // if it exists, get data, otherwise complain
        if (!gc.isValidRouterID(routerID)) {
            badRequest(response, " arg is not valid router id: " + routerValue);
            response.close();
            return;
        }

        // 3rd element == "link"

        // process query

        Query query = request.getQuery();

        // the attribute we want about the link
        String attr;

        if (query.containsKey("attr")) {
            attr = query.get("attr");

            // check attr
            if (attr.equals("id") ||
                attr.equals("name") ||
                attr.equals("weight") ||
                attr.equals("connected")) {
                // fine
            } else {
                badRequest(response, "Bad attr: " + attr);
            }

        } else {
            attr = "id";
        }

        // this is list of connected routers
        //int [] links = gc.getOutLinks(routerID);
        // same as /router/9/link/?attr=connected

        Collection<LinkInfo> links = gc.findLinkInfoByRouter(routerID);

        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj = new JSONObject();
        JSONArray array = new JSONArray();

        for (LinkInfo link : links) {

            if (attr.equals("id")) {
                array.put(link.getLinkID());
            } else if (attr.equals("name")) {
                array.put(link.getLinkName());
            } else if (attr.equals("weight")) {
                array.put(link.getLinkWeight());
            } else if (attr.equals("connected")) {
                Pair<Integer, Integer> routers = link.getEndPoints();

                // put out router ID of other end
                if (routers.getSecond() == routerID) {
                    array.put(routers.getFirst());
                } else {
                    array.put(routers.getSecond());
                }
            } else {
                // should not get here
                throw new Error("RouterLinkRestHandler: should not get here");
            }
        }

        jsobj.put("routerID", routerID);
        jsobj.put("type", "link");
        jsobj.put("list", array);

        /*
        if (attr.equals("connected")) {
            jsobj.put("check", gc.getOutLinks(routerID));
        } else if (attr.equals("weight")) {
            jsobj.put("check", gc.getLinkCosts(routerID));
        }
        */

        out.println(jsobj.toString());

    }

    /**
     * Get info on a link given a request and send a response.
     */
    public void getLinkInfo(Request request, Response response) throws IOException, JSONException {
        int routerID;
        int linkID;

        Scanner scanner;

        // get the path - path len == 4
        Path path = request.getPath();
        String[] segments = path.getSegments();

        // process router ID
        // it is 2nd element of segments
        String routerValue = segments[1];

        scanner = new Scanner(routerValue);

        if (scanner.hasNextInt()) {
            routerID = scanner.nextInt();
        } else {
            badRequest(response, "arg router id is not an Integer");
            response.close();
            return;
        }

        // if it exists, get data, otherwise complain
        if (!gc.isValidRouterID(routerID)) {
            badRequest(response, " arg is not valid router id: " + routerValue);
            response.close();
            return;
        }

        // 3rd element == "link"

        // process link ID
        // it is 4th element of segments
        String linkValue = segments[3];

        scanner = new Scanner(linkValue);

        if (scanner.hasNextInt()) {
            linkID = scanner.nextInt();
        } else {
            badRequest(response, "arg link id is not an Integer");
            response.close();
            return;
        }


        // if it exists, get data, otherwise complain
        if (!gc.isValidLinkID(linkID)) {
            badRequest(response, " arg is not valid link id: " + linkValue);
            response.close();
            return;
        }


        // send back link info as the return value
        PrintStream out = response.getPrintStream();

        JSONObject jsobj = gc.findLinkInfoAsJSON(linkID);

        out.println(jsobj.toString());



    }

}
