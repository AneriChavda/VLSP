package usr.umf.kb;

import us.monoid.json.*;
import java.io.IOException;

public class KnowledgeManagementInterface {
    // A client of the KnowledgeBlock itself
    KnowledgeBlockClient knowledgeBlock;

    /**
     * Access the KnowledgeManagementInterface on host:port.
     * All interaction is via REST calls.
     */
    public KnowledgeManagementInterface(String host, String port) {
        knowledgeBlock = new KnowledgeBlockClient(host, port);
    }

    /**
     * New registration given a JSONObject which contains is the registration details.
     * @return The result of the REST call as a JSONObject
     */
    public JSONObject RegisterNEM(JSONObject registrationInfo) throws IOException, JSONException {
        return knowledgeBlock.registerNEM(registrationInfo);
    }
}
