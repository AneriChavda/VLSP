package usr.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.interactor.RouterInteractor;
import usr.logging.*;
import usr.net.*;
import java.util.Scanner;
import java.io.IOException;

/**
 * Test Router startup and simple TFTP app.
 */
public class MultiRouterTFTP1 {
    // the Routers
    Router router1 = null;
    Router router2 = null;
    RouterEnv router1Env = null;
    RouterEnv router2Env = null;
    RouterInteractor router1Interactor = null;
    RouterInteractor router2Interactor = null;

    public MultiRouterTFTP1() {
        try {
            AddressFactory.setClassForAddress("usr.net.GIDAddress");

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouterTFTP1 exception: " + e);
            e.printStackTrace();
        }
    }

    void setup() {
        try {
            // Router 1
            router1Env = new RouterEnv(18181, 19181, "Router-1", new GIDAddress(1));
            router1 = router1Env.getRouter();

            if (router1Env.isActive()) {
                // talk to router1 ManagementConsole
                router1Interactor = router1Env.getRouterInteractor();
            } else {
                //router1Env.stop();
                throw new Exception("router1 will not start");
            }

            // Router 2
            router2Env = new RouterEnv(18182, 19182, "Router-2", new GIDAddress(2));
            router2 = router2Env.getRouter();

            if (router2Env.isActive()) {
                // talk to router2 ManagementConsole
                router2Interactor = router2Env.getRouterInteractor();
            } else {
                //router2Env.stop();
                throw new Exception("router2 will not start");
            }

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouterTFTP1 exception: " + e);
            e.printStackTrace();
        }
    }

    void connect() {
        try {
            // connect router1 to router2

            // then set up Router-to-Router data connection, weight 1
            router1Interactor.createConnection("localhost:" + router2.getManagementConsolePort(), 1);

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouterTFTP1 exception: " + e);
            e.printStackTrace();
        }
    }

    void go() {
        try {
            Thread.sleep(1000);

            // list on router2
            String[] args2 = { "1069" }; 
            router2Interactor.appStart("plugins_usr.tftp.com.globalros.tftp.server.TFTPServer", args2);


            Thread.sleep(4000);

            String[] args1 = { "2" }; // "192.168.7.2", "3000", "100" };
            router1Interactor.appStart("plugins_usr.tftp.com.globalros.tftp.client.TFTPClient", args1);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouterTFTP1 exception: " + e);
            e.printStackTrace();
        }
    }

    void end() {
        try {
            Thread.sleep(5000);

            router1Interactor.quit();
            router2Interactor.quit();

            router1.shutDown();
            router2.shutDown();
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouterTFTP1 exception: " + e);
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        MultiRouterTFTP1 mr = new MultiRouterTFTP1();

        mr.setup();
        mr.connect();
        mr.go();
        mr.end();
    }

}
