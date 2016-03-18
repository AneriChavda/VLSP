package usr.umf;

import usr.globalcontroller.GlobalController;
import usr.logging.*;
import usr.console.Command;
import usr.globalcontroller.command.*;
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
import uself.common.know.kinterface.KnowledgeMap;
import uself.common.know.kinterface.ObservableInfo;
import uself.common.know.info.NegotiationParams;


/**
 * A ManagementConsole for the GlobalController.
 * It listens for commands.
 * It folds USRRestConsole and AbstractRestConsole into BasicNEM
 */
public class VimNEMSkin extends NEMSkin implements ManagementConsole, ConsoleSupport {
    BasicContainer container;
    int port;
    VimNem globalController;

    // HashMap of command name -> Command
    HashMap<String, Command> commandMap;


    /**
     * @param manifestUri
     * @throws ClassCastException
     * @throws FileNotFoundException
     * @throws MalformedURLException
     */
    public VimNEMSkin(URI manifestUri) throws Exception {
        super(manifestUri);
    }

    public static VimNEMSkin create(VimNem globalController) throws Exception {
        URI manifestUri = generateExampleManifest();

        VimNEMSkin nem = NEMSkin.createNew(VimNEMSkin.class, manifestUri);

        nem.globalController = globalController;
        nem.port = globalController.getPort();

        // setup the Commands
        nem.commandMap = new HashMap<String, Command>();


        return nem;
    }

    public GlobalController getGlobalController() {
        return globalController;
    }

    public Object getAssociated() {
        return globalController;
    }

    public void setAssociated(Object o) {
        globalController = (VimNem)o;
    }

    /**
     * Start the ManagementConsole.
     */
    public boolean start() {
        try {
            setBaseURI(new URI("http://0.0.0.0:" + port + "/"));
            //setBaseURI(URI.create("http://localhost:" + port));
            System.out.println("NEM should be accessible @ " + this.getBaseURI());

            container = NEMSkin.bindToREST(this, port);



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


            /*
             * Now we need to notify GOV as well.
             */

            String govURL = System.getProperty("govURL");
            System.err.println("govURL = " + govURL);

            IGovernance gov = RestUtil.getHTTPProxy(govURL != null ? govURL : "http://localhost:7777/", IGovernance.class);
            gov.onNEMLoaded(this.getBaseURI());

            return true;

        } catch (Throwable t) {
            t.printStackTrace();
            throw new Error(t.getMessage());
            //return false;
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
        mf.setNEMSpecID(VimNEMSkin.class.getSimpleName(), "UCL", "10.5.0");

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 15);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        cal.set(Calendar.YEAR, 2013);
        mf.setReleaseDate(cal.getTime());
        mf.setFeatures("Minimal manifest for an VIM NEM NEM.");
        mf.setUserGuideURL(new URL("http://www.examplenem.com/support/VimNEM"));
        mf.addPossibleHost(new OS("MacOS", "*", "*"));

        String fname = mf.save();
        return new File(fname).toURI();
    }

    private String leadin() {
        return "VimNEMSkin: ";
    }

    /*
     * Stubs
     */


    /**
     * Stop the ManagementConsole.
     */
    public boolean stop() {
        container.close();
        return true;
    }

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

    @Override
    protected boolean doDeployOverEquipt(URI equipt) throws NEMDeploymentException {
        super.logln("Deploying over " + equipt);
        return true;
    }


    @Override
    protected boolean doUnDeployFromEquipt(URI equipt) throws NEMDeploymentException {
        super.logln("Undeploying from " + equipt);
        System.out.println("Undeploying from " + equipt);
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
        return null; 
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
    }

    @Override
    protected void runMAPE_MAPE() throws MAPEException {
        this.monitor();
        this.compute();
        this.execute();
    }

    private boolean monitor() {
        super.logln("monitoring...");		
        return true;
    }

    private boolean compute() {
        super.logln("computing...");
        return true;
    }

    private boolean execute() {
        super.logln("executing...");
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

    public static void main(String[] args) throws IOException {
        VimNem.main(args);
    }




}
