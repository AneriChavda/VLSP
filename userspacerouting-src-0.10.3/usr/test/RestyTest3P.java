package usr.test;

import us.monoid.web.*;
import us.monoid.json.*;
import static us.monoid.web.Resty.*;
import java.io.IOException;
import demo_usr.rest.VimClient;
import demo_usr.paths.Path;

/**
 * Test some calls to GlobalController using Resty
 * and using VimClient and Path.
 */
class RestyTest3P  {

    public static void main(String[] args) {
        try {
            VimClient vimNEM = new VimClient();

            JSONObject r0 = vimNEM.createRouter();
            int router0 = (Integer)r0.get("routerID");
            System.out.println("r0 = " + r0);


            JSONObject r1 = vimNEM.createRouter();
            int router1 = (Integer)r1.get("routerID");
            System.out.println("r1 = " + r1);

            JSONObject r2 = vimNEM.createRouter();
            int router2 = (Integer)r2.get("routerID");
            System.out.println("r2 = " + r2);

            JSONObject r3 = vimNEM.createRouter();
            int router3 = (Integer)r3.get("routerID");
            System.out.println("r3 = " + r3);

            JSONObject r4 = vimNEM.createRouter();
            int router4 = (Integer)r4.get("routerID");
            System.out.println("r4 = " + r4);

            JSONObject r5 = vimNEM.createRouter();
            int router5 = (Integer)r5.get("routerID");
            System.out.println("r5 = " + r5);

            JSONObject r6 = vimNEM.createRouter();
            int router6 = (Integer)r6.get("routerID");
            System.out.println("r6 = " + r6);

            JSONObject r7 = vimNEM.createRouter();
            int router7 = (Integer)r7.get("routerID");
            System.out.println("r7 = " + r7);







            JSONObject l1 = vimNEM.createLink(router1, router2, 10);
            int link1 = (Integer)l1.get("linkID");
            System.out.println("l1 = " + l1);

            JSONObject l2 = vimNEM.createLink(router1, router3, 10);
            int link2 = (Integer)l2.get("linkID");
            System.out.println("l2 = " + l2);

            JSONObject l3 = vimNEM.createLink(router2, router4, 10);
            int link3 = (Integer)l3.get("linkID");
            System.out.println("l3 = " + l3);

            JSONObject l4 = vimNEM.createLink(router3, router5, 10);
            int link4 = (Integer)l4.get("linkID");
            System.out.println("l4 = " + l4);

            JSONObject l5 = vimNEM.createLink(router4, router6, 10);
            int link5 = (Integer)l5.get("linkID");
            System.out.println("l5 = " + l5);

            JSONObject l6 = vimNEM.createLink(router5, router6, 10);
            int link6 = (Integer)l6.get("linkID");
            System.out.println("l6 = " + l6);

            JSONObject l7 = vimNEM.createLink(router4, router7, 10);
            int link7 = (Integer)l7.get("linkID");
            System.out.println("l7 = " + l7);

            JSONObject l8 = vimNEM.createLink(router5, router7, 10);
            int link8 = (Integer)l8.get("linkID");
            System.out.println("l8 = " + l8);

            JSONObject lSto1 = vimNEM.createLink(router0, router1, 10);
            int linkSto1 = (Integer)lSto1.get("linkID");
            System.out.println("lSto1 = " + lSto1);


            // let the routing tables propogate
            Thread.sleep(60000);


            // Start a path on port 4000 
            Path path = new Path(4000, vimNEM);


            // on router6, Egress on port 4000 send to localhost:8856
            
            JSONObject a1_1 = path.addEgress(router6, "localhost:8856");
            System.out.println("a1_1 = " + a1_1);
            Thread.sleep(500);

            // on router7, Egress on port 4000 send to localhost:8856
            JSONObject a1_2 = path.addEgress(router7, "localhost:8856");
            System.out.println("a1_2 = " + a1_2);
            Thread.sleep(500);

            // Ingress point - listen on localhost:8855 - send from routerS to router4 on port 4000
            JSONObject a2 = path.addIngress(router0, 8855, router4);
            System.out.println("a2 = " + a2);


            // Forward - send from router4 to router6 on port 4000
            JSONObject a3_1 = path.addForward(router4, router6);
            System.out.println("a3_1 = " + a3_1);

            // Forward - send from router5 to router7 on port 4000
            JSONObject a3_2 = path.addForward(router5, router7);
            System.out.println("a3_2 = " + a3_2);



            /* sleep 60 seconds = 1 minute = 60000 ms */

            Thread.sleep(30000);

            // Adapt forwarder
            // Forward - send from router5 to router8 on port 4000
            JSONObject a4_1 = path.adaptForward(router4, router7);
            System.out.println("a4_1 = " + a4_1);


            Thread.sleep(300000);

            JSONObject r0D = vimNEM.deleteRouter(router0);

            JSONObject r1D = vimNEM.deleteRouter(router1);

            JSONObject r2D = vimNEM.deleteRouter(router2);

            JSONObject r3D = vimNEM.deleteRouter(router3);

            JSONObject r4D = vimNEM.deleteRouter(router4);

            JSONObject r5D = vimNEM.deleteRouter(router5);

            JSONObject r6D = vimNEM.deleteRouter(router6);

            JSONObject r7D = vimNEM.deleteRouter(router7);


        } catch (Exception e) {
        }
    }

}
