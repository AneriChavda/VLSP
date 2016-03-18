package usr.umf.kb;

import us.monoid.json.*;
import java.io.IOException;

public class KnowledgeExchangeInterface {
    // A client of the KnowledgeBlock itself
    KnowledgeBlockClient knowledgeBlock;

    /**
     * Access the KnowledgeExchangeInterface on host:port.
     * All interaction is via REST calls.
     */
    public KnowledgeExchangeInterface(String host, String port) {
        knowledgeBlock = new KnowledgeBlockClient(host, port);
    }


    /**
     * Request Information value using the Pull Method
     * passes the id of the requesting NEM
     */
    public JSONObject RequestInformation (int nemid, String key) throws IOException, JSONException {
        return knowledgeBlock.requestInformation(nemid, key);
    }


    /**
     * Publish an Information value to KNOW
     * passes the id of the NEM sharing the information
     */
    public JSONObject PublishInformation (int nemid, String key, JSONObject value) throws IOException, JSONException {
        return knowledgeBlock.publishInformation(nemid, key, value);
    }

    /**
     * A NEM subscribes for information to be sent out at a later date
     */
    public JSONObject SubscribeForInformation (int nemid, String callbackURI, String key) throws IOException, JSONException {
        return knowledgeBlock.subscribeForInformation(nemid, callbackURI, key);
    }

}
