package usr.umf;

import usr.globalcontroller.GlobalController;
import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import us.monoid.json.*;
import java.util.Scanner;
import java.util.Collection;
import java.io.PrintStream;
import java.io.IOException;

/**
 * A class to handle /kbdata/ requests
 */
public class KBDataHandler extends USRRequestHandler {
    // get VimNem
    VimNem gc;


    public KBDataHandler() {
    }

    /**
     * Handle a request and send a response.
     */
    public boolean  handle(Request request, Response response) {
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

            System.out.println("/kbdata/ REQUEST: " + request.getMethod() + " " +  request.getTarget());

            long time = System.currentTimeMillis();

            response.set("Content-Type", "application/json");
            response.set("Server", "Knowledge Block/1.0 (SimpleFramework 4.0)");
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
                notFound(response, "POST bad request");
            } else if (method.equals("DELETE")) {
                // can't delete data via REST
                notFound(response, "DELETE bad request");
            } else if (method.equals("GET")) {
                if (segments.length >= 2) {   // get data info
                    getData(request, response);
                } else {
                    notFound(response, "GET bad request");
                }
            } else if (method.equals("PUT")) {
                notFound(response, "PUT bad request");
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
     * Get data given a request and send a response.
     */
    public void getData(Request request, Response response) throws IOException, JSONException {
        // see if we should send xml
        boolean xml = false;

        boolean hasHTTPAccept = request.contains("Accept");

        if (hasHTTPAccept) {
            String accept = request.getValue("Accept");

            if (accept.equals("application/xml") || accept.equals("text/xml")) {
                xml = true;
            }
        }


        // get the path
        Path path = request.getPath();
        String name = path.getName();

        Query query = request.getQuery();
        Scanner scanner;

        String uriPath;


        // Convert path to uri
        uriPath = path.getPath(1);

        //uriPath = uriPath.replaceFirst("/","");
        if (name == null) {
            uriPath = uriPath + "/";
        }




        // interact with VimNem
        System.out.println("/kbdata/ calling gc.evaluateKnowledgeBlockKey " + uriPath);

        JSONObject result = gc.evaluateKnowledgeBlockKey(uriPath);

        System.out.println("gc.evaluateKnowledgeBlockKey " + uriPath + " = " + result);

        // and send them back as the return value
        PrintStream out = response.getPrintStream();

        if (xml) {
            out.println(XML.toString(result, "response"));
        } else {
            out.println(result.toString());
        }

    }

}