package usr.test;

import us.monoid.web.*;
import us.monoid.json.*;
import static us.monoid.web.Resty.*;
import java.io.IOException;
import demo_usr.rest.VimClient;

/**
 * Test some calls to GlobalController using Resty
 */
class Know1  {
    public static void main(String[] args) {
        try {
            VimClient test = new VimClient();

            JSONObject r1 = test.createRouter();
            int router1 = (Integer)r1.get("routerID");
            System.out.println("r1 = " + r1);

            JSONObject r2 = test.createRouter();
            int router2 = (Integer)r2.get("routerID");
            System.out.println("r2 = " + r2);

            JSONObject l1 = test.createLink(router1, router2, 10);
            int link1 = (Integer)l1.get("linkID");
            System.out.println("l1 = " + l1);


            // let the routing tables propogate
            Thread.sleep(12000);


            // on router2, 
            JSONObject a1 = test.createApp(router2, "demo_usr.know.InformationSharingNEM", ".");
            System.out.println("a1 = " + a1);

            Thread.sleep(10000);

            // on router1, 
            JSONObject a2 = test.createApp(router1, "demo_usr.know.InformationRetrievalNEM", "."); 
            System.out.println("a2 = " + a2);

            /* sleep 60 seconds = 1 minute = 60000 ms */
            Thread.sleep(60000);

            JSONObject r1D = test.deleteRouter(router1);

            JSONObject r2D = test.deleteRouter(router2);



        } catch (Exception e) {
        } catch (Error err) {
        }
    }

}
