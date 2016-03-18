package usr.umf;

import usr.globalcontroller.GlobalController;
import usr.umf.kb.*;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.io.IOException;
import usr.logging.*;
import usr.common.ANSI;
import usr.common.LocalHostInfo;
import us.monoid.json.*;
import umf.common.nem.NEMSkin;
import uself.common.know.kinterface.KnowledgeInterfaces;
import java.util.Calendar;
import java.net.URI;
import umf.common.info.InfoCfgSetting;
import umf.common.info.ManagementInfo.GenerationMethod;
import umf.common.info.UMFInfo;

/**
 * The VimNem is an externtion of GlobalController which
 * operates within the domain of UMF.
 * It has all the GlobalController functionality, yet interacts
 * with the KnowledgeBlock and the Governance via REST interfaces.
 */
public class VimNem extends GlobalController {
    // NEM info
    int nemid = 0;

    NEMSkin nemSkin;

    int skinPort = 0;

    // KnowledgeManagementInterface which talks to the KnowledgeBlock
    //KnowledgeManagementInterface knowledgeManagementInterface;

    // KnowledgeExchangeInterface which talks to the KnowledgeBlock
    //KnowledgeExchangeInterface knowledgeExchangeInterface;

    // Gobv KnowledgeInterfaces
    KnowledgeInterfaces know;

    // Predefine functions for the KnowledgeBlock
    // It maps the Key in the KnowledgeBlock
    // with a Callable to get that value
    HashMap<String, Callable> kbFunctions;

    // A sequence number for JSON messages
    int jsonSeq = 1;

    /**
     * Construct a VimNEM
     */
    public VimNem() {
    }

    /**
     * Construct a VimNEM with a skin
     */
    public VimNem(NEMSkin skin, int port) {
        nemSkin = skin;
        skinPort = port;
    }

    protected int setupListenPort() {
        if (skinPort == 0) {
            // it has not been set
            // so called from VimNem
            return super.setupListenPort();
        } else {
            // it is set
            // so called from VimNemSkin
            Logger.getLogger("log").logln(USR.STDOUT, "VimNem: setupListenPort - return " + skinPort);

            return skinPort;
        }
    }



    /*
     * Intialisation for the VimNEM.
     * Call global controller init() first
     */
    @Override
    protected boolean init() {
        // Call global controller init() first
        boolean gcInit = super.init();

        if (gcInit) {
            try {
                // NEMID
                nemid = 5688;

                kbFunctions = new HashMap<String, Callable>();

                final GlobalController self = this;

                kbFunctions.put("/VIM/Routers/Detail/All", new Callable<JSONObject>() {
                        public JSONObject call() throws JSONException { return self.getAllRouterInfoAsJSON("all"); }
                    } );

                kbFunctions.put("/VIM/Links/Detail/All", new Callable<JSONObject>() {
                        public JSONObject call() throws JSONException { return self.getAllLinkInfoAsJSON("all"); }
                    } );

                kbFunctions.put("/VIM/Removed/", new Callable<JSONObject>() {
                        public JSONObject call() throws JSONException { return self.listShutdownRoutersAsJSON(); }
                    } );


                /*
                // Allocate knowledgeManagementInterface
                String knowAddr = System.getProperty("knowAddr");
                System.err.println("knowAddr = " + knowAddr);

                String knowHost = "localhost";
                String knowPort = "9900";

                if (knowAddr != null) {
                    String[] parts = knowAddr.split(":");

                    knowHost = parts[0];
                    knowPort = parts[1];
                }

                knowledgeManagementInterface = new KnowledgeManagementInterface(knowHost, knowPort);
                knowledgeExchangeInterface = new KnowledgeExchangeInterface(knowHost, knowPort);

                while (true) {
                    // check if can talk to KnowledgeBlock
                    if (registerWithKnowledgeBlock(nemid)) {
                        Logger.getLogger("log").logln(USR.STDOUT, "Make connection with KnowledgeBlock");
                        break;
                    } else {
                        Logger.getLogger("log").logln(USR.STDOUT, ANSI.YELLOW + "Cannot interact with KnowledgeBlock on " + knowHost +":" + knowPort + " retrying after 5000 ms" + ANSI.RESET_COLOUR);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                        }
                    }
                }

                */

                return true;

            } catch (Throwable t) {
                return false;
            }

        } else {
            return false;
        }
    }

    /*
     * Shutdown
     */
    protected void shutDown() {
        // reset data to KnowledgeBlock
        resetDataToKnowledgeBlock("/VIM/Routers/Detail/All", "router");
        resetDataToKnowledgeBlock("/VIM/Links/Detail/All", "link");
        resetDataToKnowledgeBlock("/VIM/Removed/", "shutdown");

        wakeWait();
        super.shutDown();

    }

    /**
     * Start the console.
     */
    protected void startConsole() {
        LocalHostInfo myHostInfo_ = getLocalHostInfo();

        if (nemSkin == null) {
            try {
                nemSkin = VimNEMSkin.create(this);

                Logger.getLogger("log").logln(USR.STDOUT, "Awaiting Governance console ");
                ((VimNEMSkin)nemSkin).start();
            } catch (Throwable t) {
                t.printStackTrace();
                Logger.getLogger("log").logln(USR.STDOUT, ANSI.RED + "Cannot interact with Governance" + ANSI.RESET_COLOUR);
                throw new Error(t.getMessage());
            }
        } else {
            // already have a NEM skin
            Logger.getLogger("log").logln(USR.STDOUT, "Already have skin");
        }
    }

    /**
     * Stop the console.
     */
    protected void stopConsole() {
        ((ConsoleSupport)nemSkin).stop();
    }


    /**
     * Set the KnowledgeInterfaces
     */
    protected void setKnowledgeInterfaces(KnowledgeInterfaces k) {
        know = k;
    }

    /**
     * Register with the KB
     */
    /*
    private boolean registerWithKnowledgeBlock(int nemid) {
        // send equiv of: {"nemid":200, "urisforavailableinformation":[],"iccallbackURL":"",
        // "urisforrequiredinformation":["/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation"],"ircallbackURL":"TBA","urisforknowledge":[],"knowledgebuildingrequesturls":[],"knowledgeproductiononregistration":[],"ipkpcallbackURL":""}

        LocalHostInfo myHostInfo_ = getLocalHostInfo();

        try {
            // {"nemid": 5688, "urisforavailableinformation":["/VIM/Routers/Detail/All", "/VIM/Links/Detail/All", "/VIM/Removed/"]}
            JSONObject registrationInfo = new JSONObject();
            registrationInfo.put("nemid", nemid);

            JSONArray availableArray = new JSONArray();

            for (String key : kbFunctions.keySet()) {
                availableArray.put(key);
            }

            registrationInfo.put("urisforavailableinformation", availableArray);

            registrationInfo.put("iccallbackURL", "http://" + myHostInfo_.getName() + ":" + myHostInfo_.getPort() + "/kbdata/");

            JSONObject jsobj = knowledgeManagementInterface.RegisterNEM(registrationInfo);

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "registrationInfo " + registrationInfo);
            Logger.getLogger("log").logln(USR.STDOUT, leadin() + "registration result  " + jsobj);



            return true;


        } catch (IOException ioe) {
            //System.err.println("IOException " + ioe);
            //ioe.printStackTrace();
        } catch (JSONException je) {
            //System.err.println("JSONException " + je);
            //je.printStackTrace();
        }

        return false;


    }
    */

    /**
     * Post data to knowledge block
     */
    private boolean postDataToKnowledgeBlock(String key) {
        try {

            JSONObject jsobj = evaluateKnowledgeBlockKey(key);

            Logger.getLogger("log").logln(USR.STDOUT, System.currentTimeMillis() + " _KB_ SEND " + leadin() + " postDataToKnowledgeBlock: + " + key + " -> " + jsobj);

            InfoCfgSetting<String> infoCfgSetting = null;


            if (jsobj != null) {
                //knowledgeExchangeInterface.PublishInformation(nemid, key, jsobj); 
                //knowledgeExchangeInterface.ShareInformation(nemid, key, jsobj);


                // create UMFInfo object
                if (key.equals("/VIM/Removed/")) {
                    infoCfgSetting = new InfoCfgSetting<String>(jsobj.toString(),
                                                                GenerationMethod.GENERATED, DemoVocabulary.VIMRemovedMIS,
                                                                Calendar.getInstance().getTime());
                } else if (key.equals("/VIM/Routers/Detail/All")) {
                    infoCfgSetting = new InfoCfgSetting<String>(jsobj.toString(),
                                                                GenerationMethod.GENERATED, DemoVocabulary.VIMRoutersDetailMIS,
                                                                Calendar.getInstance().getTime());
                } else if (key.equals("/VIM/Links/Detail/All")) {
                    infoCfgSetting = new InfoCfgSetting<String>(jsobj.toString(),
                                                                GenerationMethod.GENERATED, DemoVocabulary.VIMLinksDetailMIS,
                                                                Calendar.getInstance().getTime());
                }			

                // publish value per equipment
                UMFInfo<String> info = null;
                for (URI equiptUri : nemSkin.getMandate().getManagedEquipement()) {
                    info = new UMFInfo<String>(nemSkin.getInstanceID(), infoCfgSetting);
                    //know.PublishInformation(info, equiptUri);
                    know.GenericShareInformation(info, equiptUri);
                }



                return true;

            } else {
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin() + "Cannot set data in KnowledgeBlock: " + key + ". Missing Callable ");

                return false;
            }
            /*
        } catch (JSONException jse) {
            jse.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot set data in KnowledgeBlock: " + key);
        } catch (IOException ioe) {
            //ioe.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot set data in KnowledgeBlock: " + key);
            */
        } catch (Exception e) {
            e.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot set data in KnowledgeBlock: " + key);
        }

        return false;


    }


    /**
     * Call a function with a key
     */
    public JSONObject evaluateKnowledgeBlockKey(String key) {
        try {
            Callable<JSONObject> callable = kbFunctions.get(key);

            if (callable != null) {
                JSONObject jsobj = callable.call();

                return jsobj;

            } else {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot find value for : " + key + ". Missing Callable ");

                return null;
            }

        } catch (JSONException jse) {
            jse.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot set data in KnowledgeBlock: " + key);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot set data in KnowledgeBlock: " + key);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot set data in KnowledgeBlock: " + key);
        }

        return null;

    }

    /**
     * Reset data in knowledge block
     */
    private boolean resetDataToKnowledgeBlock(String key, String type) {
        try {
            // set to {"detail":[],"list":[],"type":"router"}

            JSONObject jsobj = new JSONObject();
            jsobj.put("detail", new JSONArray());
            jsobj.put("list", new JSONArray());
            jsobj.put("type", type);

            //knowledgeExchangeInterface.PublishInformation(nemid, key, jsobj);
            //knowledgeExchangeInterface.ShareInformation(nemid, key, jsobj);

            InfoCfgSetting<String> infoCfgSetting = null;

            // create UMFInfo object
            if (key.equals("/VIM/Removed/")) {
                infoCfgSetting = new InfoCfgSetting<String>(jsobj.toString(),
                                                            GenerationMethod.GENERATED, DemoVocabulary.VIMRemovedMIS,
                                                            Calendar.getInstance().getTime());
            } else if (key.equals("/VIM/Routers/Detail/All")) {
                infoCfgSetting = new InfoCfgSetting<String>(jsobj.toString(),
                                                            GenerationMethod.GENERATED, DemoVocabulary.VIMRoutersDetailMIS,
                                                            Calendar.getInstance().getTime());
            } else if (key.equals("/VIM/Links/Detail/All")) {
                infoCfgSetting = new InfoCfgSetting<String>(jsobj.toString(),
                                                            GenerationMethod.GENERATED, DemoVocabulary.VIMLinksDetailMIS,
                                                            Calendar.getInstance().getTime());
            }			

            // publish value per equipment
            UMFInfo<String> info = null;
            for (URI equiptUri : nemSkin.getMandate().getManagedEquipement()) {
                info = new UMFInfo<String>(nemSkin.getInstanceID(), infoCfgSetting);
                //know.PublishInformation(info, equiptUri);
                know.GenericShareInformation(info, equiptUri);			
            }



            return true;

        } catch (JSONException jse) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot reset data in KnowledgeBlock: " + key);
            /*
        } catch (IOException ioe) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot reset data in KnowledgeBlock: " + key);
            */
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot reset data in KnowledgeBlock: " + key);
        }

        return false;
    }

    /**
     * Called after a router is started.
     */
    protected void informRouterStarted(JSONObject routerAttrs) throws JSONException {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + ANSI.GREEN + "informRouterStarted " + routerAttrs + ANSI.RESET_COLOUR);
        super.informRouterStarted(routerAttrs);

        // Update KnowledgeBlock
        postDataToKnowledgeBlock("/VIM/Routers/Detail/All");
    }

    /**
     * Called after a router is ended.
     */
    protected void informRouterEnded(JSONObject routerAttrs) throws JSONException {
        super.informRouterEnded(routerAttrs);

        // Update KnowledgeBlock
        postDataToKnowledgeBlock("/VIM/Removed/");
    }

    /**
     * Called to give a snapshot of all the routers
     */
    protected void informAllRouters() {
        // update KnowledgeBlock
        postDataToKnowledgeBlock("/VIM/Routers/Detail/All");
    }

    /**
     * Called to give a snapshot of all the links
     */
    protected void informAllLinks() {
        // update KnowledgeBlock
        postDataToKnowledgeBlock("/VIM/Links/Detail/All");
    }

    /*
     * Methods that return JSON have extra attributes when called from VIM NEM
     */

    /**
     * Find some router info, given a router address or a router name
     * and return a JSONObject
     */
    public JSONObject findRouterInfoAsJSON(int routerID) throws JSONException {
        JSONObject jsobj = super.findRouterInfoAsJSON(routerID);

        jsobj.put("seq", jsonSeq++);

        return jsobj;
    }

    /**
     * List all shutdown routers
     */
    public JSONObject listShutdownRoutersAsJSON() throws JSONException {
        JSONObject jsobj = super.listShutdownRoutersAsJSON();


        jsobj.put("seq", jsonSeq++);

        return jsobj;
    }

    /**
     * List all RouterInfo as a JSON object
     */
    public JSONObject getAllRouterInfoAsJSON(String detail) throws JSONException {
        JSONObject jsobj = super.getAllRouterInfoAsJSON(detail);

        jsobj.put("seq", jsonSeq++);

        return jsobj;
    }


    /**
     * Find link info
     * and return a JSONObject
     */
    public JSONObject findLinkInfoAsJSON(int linkID) throws JSONException {
        JSONObject jsobj = super.findLinkInfoAsJSON(linkID);

        jsobj.put("seq", jsonSeq++);

        return jsobj;
    }


    /**
     * List all LinkInfo as a JSONObject
     */
    public JSONObject getAllLinkInfoAsJSON(String detail) throws JSONException {
        JSONObject jsobj = super.getAllLinkInfoAsJSON(detail);

        jsobj.put("seq", jsonSeq++);

        return jsobj;
    }

    /**
     * Find some app info, given an app ID
     * and returns a JSONObject.
     */
    public JSONObject findAppInfoAsJSON(int appID) throws JSONException {
        JSONObject jsobj = super.findAppInfoAsJSON(appID);

        jsobj.put("seq", jsonSeq++);

        return jsobj;
    }

    /**
     * Get router stats info, given a router address
     * and return a JSONObject
     */
    public JSONObject getRouterLinkStatsAsJSON(int routerID) throws JSONException {
        JSONObject jsobj = super.getRouterLinkStatsAsJSON(routerID);

        jsobj.put("seq", jsonSeq++);

        return jsobj;
    }

    /**
     * Get router stats info, given a router address and a destination router
     * and return a JSONObject
     */
    public JSONObject getRouterLinkStatsAsJSON(int routerID, int dstID) throws JSONException {
        JSONObject jsobj = super.getRouterLinkStatsAsJSON(routerID, dstID);

        jsobj.put("seq", jsonSeq++);

        return jsobj;
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String VN = "VIM NEM: ";

        return getName() + " " + VN;
    }

    /**
     * Main entry point if the NEM is started from the command line.
     */
    public static void main(String[] args) {
        try {
            VimNem gControl = new VimNem();

            if (args.length > 1) {
                String flag = args[0];

                gControl.setStartupFile(args[1]);
                gControl.init();
            } else if (args.length == 1) {
                gControl.setStartupFile(args[0]);
                gControl.init();
            } else {
                gControl.setStartupFile("startup.xml");
                gControl.init();
            }

            Logger.getLogger("log").logln(USR.STDOUT, gControl.leadin() + "About to start");
            gControl.start();

            Logger.getLogger("log").logln(USR.STDOUT, gControl.leadin() + "VimNem complete");
            System.out.flush();

        } catch (Throwable t) {
            System.exit(1);
        }

    }

}
