package usr.umf;

import usr.globalcontroller.GlobalController;
import usr.logging.*;
import usr.console.Command;
import usr.globalcontroller.command.*;
import java.util.concurrent.*;
import cc.clayman.console.ManagementConsole;
import cc.clayman.console.RequestHandler;

import gr.tns.RestUtil;
import gr.tns.http.BasicContainer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Observer;
import java.util.Set;

import umf.common.action.NEMActionSpecification;
import umf.common.commands.CommandResult;
import umf.common.commands.CommandResultCode;
import umf.common.gov.IGovernance;
import umf.common.info.UMFInfo;
import umf.common.info.UMFInfoSpecification;
import umf.common.model.OS;
import umf.common.nem.Manifest;
import umf.common.nem.NEMSkin;
import umf.common.nem.exception.NEMDeploymentException;
import umf.common.nem.regime.MAPEException;
import umf.common.option.policy.SpecificNEMPolicy;
import umf.common.nem.evt.OperationEvent;
import umf.common.nem.evt.OperationEventHandler;
import uself.common.know.info.InformationFlowRequirementsAndConstraints;
import uself.common.know.kinterface.KnowledgeMap;
import uself.common.know.kinterface.ObservableInfo;
import uself.common.know.info.NegotiationParams;
import uself.common.know.kinterface.KnowledgeInterfaces;
import us.monoid.json.JSONObject;


/**
 * A ManagementConsole for the GlobalController.
 * It listens for commands.
 * It folds USRRestConsole and AbstractRestConsole into BasicNEM
 */
public class VimNemGov extends NEMSkin implements ManagementConsole, ConsoleSupport {
    BasicContainer container;
    int port;
    VimNem vimNem;

    // HashMap of command name -> Command
    HashMap<String, Command> commandMap;

    // console up ?
    boolean consoleUp = false;

    /*
 OperationEventListener extends EventListener {

	public void onInstantiated(OperationEvent oe);
	
	public void onPreDeployment(OperationEvent oe);
	public void onPostDeployment(OperationEvent oe);
	
	public void onPreUndeployment(OperationEvent oe);
	public void onPostUndeployment(OperationEvent oe);
	
	public void onPreRegistration(OperationEvent oe);
	public void onPostRegistration(OperationEvent oe);
	
	public void onPreUnregistration(OperationEvent oe);
	public void onPostUnregistration(OperationEvent oe);
	
	public void onPreSetUp(OperationEvent oe);
	public void onPostSetUp(OperationEvent oe);
	
	public void onPreSetDown(OperationEvent oe);
	public void onPostSetDown(OperationEvent oe);
	
	public void onPreRevoke(OperationEvent oe);
	public void onPostRevoke(OperationEvent oe);	
    */


    /**
     * @param manifestUri
     * @throws ClassCastException
     * @throws FileNotFoundException
     * @throws MalformedURLException
     */
    public VimNemGov(URI manifestUri) throws Exception {
        super(manifestUri);

        System.out.println("Constructor VimNEMGov(URI) = " + manifestUri);

        addOperationListener(new OperationEventHandler() {
                public void onInstantiated(OperationEvent oe) {
                    // do stuff
                    System.out.println("VimNEMGov: EVENT onInstantiated");
                    doVimNemGovSetup(oe);
                }

                public void onPreDeployment(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPreDeployment");
                }

                public void onPostDeployment(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPostDeployment");
                    doVimNemPostDeployment(oe);
                }

	
                public void onPreUndeployment(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPreUndeployment");
                }

                public void onPostUndeployment(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPostUndeployment");
                }
	

                public void onPreRegistration(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPreRegistration");
                }

                public void onPostRegistration(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPostRegistration");
                    doVimNemPostRegistration(oe);
                }

                public void onPreUnregistration(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPreUnregistration");
                }

                public void onPostUnregistration(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPostUnregistration");
                }

                public void onPreSetUp(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPreSetUp");
                }

                public void onPostSetUp(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPostSetUp");
                    doVimNemPostSetup(oe);
                }

                public void onPreSetDown(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPreSetDown");
                }

                public void onPostSetDown(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPostSetDown");
                }

                public void onPreRevoke(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPreRevoke");
                }

                public void onPostRevoke(OperationEvent oe) {
                    System.out.println("VimNEMGov: EVENT onPostRevoke");
                }

            });

        System.out.println("Constructor VimNEMGov: addOperationListener");


    }


    @Override
    protected boolean doDeployOverEquipt(URI equipt) throws NEMDeploymentException {
        super.logln("Deploying over " + equipt);
        System.out.println("Deploying over " + equipt);

        return true;

    }

    @Override
    protected boolean doUnDeployFromEquipt(URI equipt) throws NEMDeploymentException {
        super.logln("Undeploying from " + equipt);
        System.out.println("Undeploying from " + equipt);

        return true;
    }


    public CommandResult delete() {
        if (vimNem != null) {
            vimNem.shutDown();
        }

        if (container != null) {
            container.close();
        }

        return super.delete();
    }


    /**
     * Called after Constructor is called
     * Handler for  onInstantiated(OperationEvent oe)
     */
    public void doVimNemGovSetup(OperationEvent oe) {
        System.out.println("doVimNemGovSetup called " + oe);

        try {
            port = getBaseURI().getPort();

            // get a handle on the container
            container = getBasicContainer();

        } catch (Throwable t) {
            System.err.println("doVimNemGovSetup Exception " + t);
            t.printStackTrace();
        }

    }


    /**
     * Called after doDeployOverEquipt()
     * Handler for onPostDeployment()
     */
    public void doVimNemPostDeployment(OperationEvent oe) {
        try {
            int equipCount = getMandate().numManagedEquipement();

            System.out.println("VimNEMGov: equipCount = " + equipCount);

            // start the NEM and pass in this Skin
            vimNem = new VimNem(this, port);
            vimNem.setStartupFile("vim-startup.xml");


            //int containerPort = container.getPort();

            consoleUp = setupConsoleCommands();

            boolean init = vimNem.init();

            if (!init) {
                throw new NEMDeploymentException("Cannot start VimNEM properly");
            }

            System.out.println("NEM should be accessible @ " + getBaseURI());


        } catch (Throwable t) {
            System.err.println("doVimNemGovSetup Exception " + t);
            t.printStackTrace();
        }

    }



    /**
     * Called after doDeployOverEquipt()
     * Handler for onPostRegistration()
     */
    public void doVimNemPostRegistration(OperationEvent oe) {
        // WAS vimNem.start();
    }

    /**
     * Handler for onPostSetUp
     */
    public void doVimNemPostSetup(OperationEvent oe) {
        Executors.newSingleThreadScheduledExecutor().execute(new Runnable(){
            public void run() {
                vimNem.start();
            }
        });


        System.out.println("NEM STARTED");
    }


    public GlobalController getGlobalController() {
        return vimNem;
    }

    public Object getAssociated() {
        return vimNem;
    }

    public void setAssociated(Object o) {
        vimNem = (VimNem)o;
    }


    /**
     * Set up the commands for the console.
     */
    protected boolean setupConsoleCommands()  {
        try {
            // setup the Commands
            commandMap = new HashMap<String, Command>();

            // setup default /command handler
            defineRequestHandler("/command/", new CommandAsRestHandler());

            register(new UnknownCommand());
            register(new LocalOKCommand());
            register(new QuitCommand());
            register(new ShutDownCommand());
            register(new ReportAPCommand());
            register(new OnRouterCommand());
            register(new GetRouterStatsCommand());
            register(new SendRouterStatsCommand());



            // check the UnknownCommand exists
            Command unknown = find("__UNKNOWN__");

            if (unknown == null) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "the UnknownCommand has not been registered");
                throw new Error(leadin() + "the UnknownCommand has not been registered");
            }




            // setup default /router/id/app/ handler
            defineRequestHandler("/router/[0-9]+/app/.*", new AppRestHandler());

            // setup default /router/id/link/ handler
            defineRequestHandler("/router/[0-9]+/link/.*", new RouterLinkRestHandler());

            // setup default /router/ handler
            defineRequestHandler("/router/", new RouterRestHandler());

            // setup default /link/ handler
            defineRequestHandler("/link/", new LinkRestHandler());

            // setup default /ap/ handler
            defineRequestHandler("/ap/", new AggPointRestHandler());

            // setup default /removed/ handler
            defineRequestHandler("/removed/", new RemovedRestHandler());

            // setup  /kbdata/ handler which handles callbacks
            // from the knowledgeblock
            defineRequestHandler("/kbdata/", new KBDataHandler());

            // setup  /graph/ handler which gathers version of
            // virtual network as a graph - e.g. a dot file
            defineRequestHandler("/graph/", new GraphRestHandler());


            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Define a handler for a request
     */
    public void defineRequestHandler(String pattern, USRRequestHandler rh) {
        rh.setBasePath(pattern);
        rh.setManagementConsole(this);
        container.addHandler(rh);
    }

    /**
     * Register a new command with the ManagementConsole.
     */
    public void register(Command command) {
        String commandName = command.getName();

        command.setManagementConsole(this);

        commandMap.put(commandName, command);

        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "registered command " + command + " -> " + command);



    }


    /**
     * Find a command in the ManagementConsole.
     * @param commandName The name of the command
     */
    public Command find(String commandName) {
        return commandMap.get(commandName);
    }

    /**
     * Find a handler in the ManagementConsole.
     * @param pattern The pattern for the handler
     */
    public Command findHandler(String pattern) {
        return commandMap.get(pattern);
    }


    public static URI generateExampleManifest() throws IOException {
        Manifest mf = new Manifest();

        // WAS 
        // mf.setName(VimNEMSkin.class.getCanonicalName());
        // mf.setProvider("UCL");
        // mf.setVersion("1.0");
        mf.setNEMSpecID(VimNemGov.class.getSimpleName(), "UCL", "10.5.0");


        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 15);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        cal.set(Calendar.YEAR, 2013);
        mf.setReleaseDate(cal.getTime());
        mf.setFeatures("Minimal manifest for a VimNEM NEM.");
        mf.setUserGuideURL(new URL("http://www.examplenem.com/support/VimNEM"));
        mf.addPossibleHost(new OS("MacOS", "*", "*"));

        mf.addManageableEntity("*");
        mf.addManageableEntity("Type-of-equipement");
        mf.setAtomicNEM(true);
        mf.setAtomicLoop(false);

        // Information available from VIM NEM
        mf.addAvailableOutput(DemoVocabulary.VIMRoutersDetailMIS);
        mf.addAvailableOutput(DemoVocabulary.VIMLinksDetailMIS);
        mf.addAvailableOutput(DemoVocabulary.VIMRemovedMIS);
		

        String fname = mf.save();
        return new File(fname).toURI();
    }

    private String leadin() {
        return "VimNemGov: ";
    }


    /*
     * Stubs
     */


    /**
     * Construct a ManagementConsole, given a specific port.
     */
    public void initialise (int port) {
    }

    public void registerCommands() {
    }

    /**
     * Define a handler for a request
     */
    public void defineRequestHandler(String pattern, RequestHandler rh) {
    }


    /*
     * Methods for V2 of the NEM Skin
     */

    /**
     * Start the ManagementConsole.
     */
    public boolean start() {
        System.out.println("ManagementConsole start");
        return true;
    }

    /**
     * Stop the ManagementConsole.
     */
    public boolean stop() {
        container.close();
        System.out.println("ManagementConsole stop");
        return true;
    }

    @Override
    public Set<UMFInfoSpecification> listAcquiredInputs() {
        return null;
    }

    @Override
    public Set<UMFInfoSpecification> listRequiredInputs() {
        return null;
    }

    @Override
    public Set<UMFInfoSpecification> listWishedInputs() {
        return null;
    }

    @Override
    public Set<UMFInfoSpecification> listNonObviousAvailableOutputs() {
        Set<UMFInfoSpecification> availableSpecifications = new HashSet<UMFInfoSpecification>();
        for (URI equiptUri : getMandate().getManagedEquipement()) {
            availableSpecifications.add(new UMFInfoSpecification(
                                                                 DemoVocabulary.VIMRoutersDetailMIS, equiptUri));
            availableSpecifications.add(new UMFInfoSpecification(
                                                                 DemoVocabulary.VIMLinksDetailMIS, equiptUri));
            availableSpecifications.add(new UMFInfoSpecification(
                                                                 DemoVocabulary.VIMRemovedMIS, equiptUri));
        }
        return availableSpecifications;
    }

    @Override
    protected Set<NEMActionSpecification> listActions() {
        return null;
    }

    @Override
    protected Iterator<NEMActionSpecification> actionSpecIterator() {
        return null;
    }

    @Override
    protected Iterator<UMFInfoSpecification> infoSpecIterator() {
        return null;
    }

    @Override
    protected void runMAPE_M() throws UnsupportedOperationException, MAPEException {
        this.monitor();
    }

    @Override
    protected void runMAPE_MA() throws UnsupportedOperationException, MAPEException {
        this.monitor(); 
        this.compute();
    }

    @Override
    protected void runMAPE_MAP() throws UnsupportedOperationException, MAPEException {
        this.monitor(); 
        this.compute();
        this.planning();
    }

    @Override
    protected void runMAPE_MAPE() throws MAPEException {
        this.monitor();
        this.compute();
        this.planning();
        this.enforcement();
    }

    private boolean monitor() {
        System.out.println("monitoring...");		
        return true;
    }

    private boolean compute() {
        System.out.println("computing...");
        return true;
    }

    private boolean planning() {
        //super.logln("planning...");
        return true;
    }

    private boolean enforcement() {
        //super.logln("enforcement...");
        return true;
    }


    @SuppressWarnings("rawtypes")
    @Override
    public UMFInfo getInfo(UMFInfoSpecification infoDesc)
        throws NoSuchElementException {
        throw new NoSuchElementException(infoDesc.getDescriptor());
    }

    @Override
    public boolean checkInfoExists(UMFInfoSpecification infoDesc) {
        return false;
    }

    @Override
    protected CommandResult applySpecificNEMPolicy(SpecificNEMPolicy policy) {
        return new CommandResult(CommandResultCode.OK);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Set<KnowledgeMap<?>> listPushableInfoCollections() {
        Set<KnowledgeMap<?>> result = new HashSet<KnowledgeMap<?>>();
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Set<ObservableInfo<?>> listPushableInfoScalars() {
        return null;
    }

    @Override
    public Observer getObservingProcess(UMFInfoSpecification externalInfoDesc) {
        return null;
    }

    @Override
    public NegotiationParams getNeeds(UMFInfoSpecification externalInfoDesc) {
        return null;
    }

    @Override
    public NegotiationParams getCapacities(UMFInfoSpecification internalInfoDesc) {
        return null;
    }

    @Override
    public InformationFlowRequirementsAndConstraints GetNegotiationParameters() {
        // Setting InformationFlowRequirementsAndConstraints
        // Setting proposed information flow requirements / constraints
        try {

            JSONObject informationflowconstraints = new JSONObject();

            informationflowconstraints.put("minimumInformationSharingRate", 2);
            informationflowconstraints.put("maximumInformationSharingRate", 5);

            informationflowconstraints.put("method", 1);

            JSONObject performanceGoal = new JSONObject();
            // Default goal
            performanceGoal.put("optGoalId", 2);
            performanceGoal.put("optGoalName", "Pubsub");
            performanceGoal.put("optGoalParameters", "");
            performanceGoal.put("optGoalLevelofEnforcement", "high");

            informationflowconstraints.put("flowOptimizationGoal",
                                           performanceGoal);

            return new InformationFlowRequirementsAndConstraints(
                                                                 informationflowconstraints.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
