package usr.umf.kb;

import us.monoid.web.*;
import us.monoid.json.*;
import static us.monoid.web.Resty.*;
import java.io.IOException;

public class KnowledgeBlockClient {
    String kbHost;
    String kbPort;

    Resty rest;

    /**
     * Constructor
     */
    KnowledgeBlockClient(String host, String port) {
        kbHost = host;
        kbPort = port;

        // Make a Resty connection
        rest = new Resty();
    }

    /**
     * New registration given a JSONObject which contains is the registration details.
     * @return The result of the REST call as a JSONObject
     */
    public JSONObject registerNEM(JSONObject registrationInfo) throws IOException, JSONException {
        String kbURL = "http://" + kbHost + ":" + kbPort + "/register/";


        // Call the relevant URL
        JSONObject jsobj = rest.json(kbURL, content(registrationInfo)).toObject();

        return jsobj;

    }

    /**
     * Get some data
     */
    public JSONObject requestInformation(int nemid, String key) throws IOException, JSONException {
        // DO curl
        // 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?nemid=200'
        String kbURL = "http://" + kbHost + ":" + kbPort + "/data" + key + "?nemid=" + nemid;

        // Call the relevant URL
        JSONObject jsobj = rest.json(kbURL).toObject();

        String result = jsobj.getString("result");

        return new JSONObject(result);
    }

    /**
     * Set some data
     */
    public JSONObject publishInformation(int nemid, String key, JSONObject value) throws IOException, JSONException  {
        // DO curl -d '{"value": 550}'
        // 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?nemid=100'
        String kbURL = "http://" + kbHost + ":" + kbPort + "/data" + key + "?nemid=" + nemid;

        // Call the relevant URL
        JSONObject jsobj = rest.json(kbURL, content(value)).toObject();

        return jsobj;
    }

    /**
     * Subscribe for data given a request and send a response.
     * Calls KnowledgeExchangeInterface.SubscribeForInformation
     * by doing a PUT on the KnowledgeBlock.
     * <p>
     * The caller on host remotehost, must listen for REST calls
     * and handle the request http://remotehost:8500/update/
     * and the key as part of the path.
     * e.g. http://remotehost:8500/update/VIM/Removed
     * The latest data is sent as a POST to the caller.     
     */
    public JSONObject subscribeForInformation(int nemid, String callbackURI, String key) throws IOException, JSONException {
        // DO curl -X PUT
        // 'http://localhost:9900/data/NetworkResources/WirelessNetworks/network1/Routers/router1/Interfaces/if0/Metrics/loadlevelestimation?nemid=100&callback=http://remotehost:8500/update/'
        String kbURL = "http://" + kbHost + ":" + kbPort + "/data" + key + "?nemid=" + nemid + "&callback=" + callbackURI;

        // Call the relevant URL
        JSONObject jsobj = rest.json(kbURL, put(content(""))).toObject();

        return jsobj;

    }

}
