package ru.mbc.manager.verification;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.mbc.manager.config.ChildLedger;
import ru.mbc.manager.config.Config;
import ru.mbc.manager.config.NodeConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

class Response{
    public ArrayList<NodeConfig> nodes;
    public Boolean end;
    public String nextLedger;

    public Response(){
        nodes = new ArrayList<>();
        end = false;

    }
}

public class MbcLogic extends Thread {
    private String myLedger;
    private ArrayList<NodeConfig> parentNodes;
    private String parentLedger;
    private ArrayList<ChildLedger> childLedgers;
    ArrayBlockingQueue<TxDescriptor> queue;
    private Integer bcPort;

    public MbcLogic(Config config, ArrayBlockingQueue<TxDescriptor> q, Integer _bcPort){
        myLedger = config.general.ledgerAddress;
        parentNodes = config.parent.nodes;
        childLedgers = config.children.ledgers;
        queue = q;
        parentLedger = config.parent.ledgerAddress;
        bcPort = _bcPort;
    }

    public String getNextLedger(String currentLedger, String targetLedger) throws InvalidPathException {
        if(currentLedger.equals(targetLedger))
            return null;
        if(targetLedger.contains(currentLedger)){
            ArrayList<String> tmp = new ArrayList(Arrays.asList(targetLedger.split("/")));
            ArrayList<String> myTmp = new ArrayList(Arrays.asList(currentLedger.split("/")));
            return currentLedger + "/" + tmp.get(myTmp.size());
        }
        return parentLedger;
    }

    public String getCurrentLedgerAddress(){
        return myLedger;
    }

    public ArrayList<NodeConfig> getNodesForLedger(String ledgerAddress){
        if(ledgerAddress.equals(parentLedger)){
            return parentNodes;
        }
        for(ChildLedger l: childLedgers){
            if(ledgerAddress.equals(l.ledgerAddress)){
                return l.nodes;
            }
        }
        return null;
    }

    public Response requestSearch(String ledger, NodeConfig node){
        Response response = new Response();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = null;
        HttpResponse<String> httpResponse = null;
        try {
            request = HttpRequest.newBuilder().uri(new URI(
                    "http",
                    null,
                    node.ip,
                    node.port,
                    "/search/" + new String(Hex.encodeHex(ledger.getBytes(StandardCharsets.UTF_8))) + "/",
                    null,
                    null
            )).build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(httpResponse.body());
            String code = json.getString("response");
            if(code.equals("me")){
                response.end = true;
            }
            else if (code.equals("next")){
                response.nextLedger = json.getString("address");
                JSONArray ja = json.getJSONArray("nodes");
                for(int i = 0; i < ja.length(); i++){
                    JSONObject jo = ja.getJSONObject(i);
                    NodeConfig nextNode = new NodeConfig();
                    nextNode.ip = jo.getString("ip");
                    nextNode.port = jo.getInt("port");
                    response.nodes.add(nextNode);
                }
            }

        } catch (InterruptedException | JSONException | NullPointerException | IOException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }

        return response;
    }

    public boolean requestVerify(TxDescriptor tx, NodeConfig node){
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = null;
        HttpResponse<String> httpResponse = null;

        JSONObject jo = new JSONObject();
        jo.put("tx", tx.getTxHash());
        jo.put("value", tx.getValue());
        jo.put("to", tx.getToAddress());
        jo.put("ledgerAddress", tx.getLedgerAddress());
        String requestBody = jo.toString();

        try {
            request = HttpRequest.newBuilder().uri(new URI(
                    "http",
                    null,
                    node.ip,
                    node.port,
                    "/verify/",
                    null,
                    null
            ))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return httpResponse.statusCode() != 404;
    }

    public void notifyBc(TxDescriptor tx){
        String requestBody = "{\"tx\":\"" + tx.getTxInHash() +"\" }";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = null;
        HttpResponse<String> httpResponse = null;
        try {
            request = HttpRequest.newBuilder().uri(new URI(
                    "http",
                    null,
                    "127.0.0.1",
                    bcPort,
                    "/post_verify/",
                    null,
                    null
            ))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){
        while(true){
            try {
                TxDescriptor tx = queue.take();
                String nextLedger = getNextLedger(myLedger, tx.getLedgerAddress());
                ArrayList<NodeConfig> nodes = getNodesForLedger(nextLedger);
                boolean noNodes = false;
                while(true){
                    if(nodes.size() == 0){
                        noNodes = true;
                        break;
                    }
                    ArrayList<Response> responses = new ArrayList<>();
                    for(NodeConfig node: nodes){
                        Response r = requestSearch(tx.getLedgerAddress(), node);
                        if(r != null)
                            responses.add(r);
                    }

                    // End
                    int endCount = 0;
                    for(Response response: responses){
                        if(response.end)
                            endCount += 1;
                    }
                    if(endCount >= (responses.size() / 2) + 1){
                        break;
                    }

                    // Intersection and next nodes
                    Hashtable<NodeConfig, Integer> intersection = new Hashtable<>();
                    Hashtable<String, Integer> nextLedgerIntersection = new Hashtable<>();
                    for(Response response: responses){
                        for(NodeConfig N: response.nodes){
                            if(intersection.containsKey(N)){
                                intersection.put(N, intersection.get(N) + 1);
                            }
                            else{
                                intersection.put(N, 1);
                            }
                        }
                        if(response.nextLedger != null){
                            if(nextLedgerIntersection.containsKey(response.nextLedger)){
                                nextLedgerIntersection.put(response.nextLedger, nextLedgerIntersection.get(response.nextLedger) + 1);
                            }
                            else{
                                nextLedgerIntersection.put(response.nextLedger, 1);
                            }
                        }
                    }

                    // find intersection of responses
                    nodes = new ArrayList<>();
                    for(NodeConfig N: intersection.keySet()){
                        if(intersection.get(N) > responses.size() / 2)
                            nodes.add(N);
                    }
                    int count = 0;
                    String candidate = "";
                    for(String s: nextLedgerIntersection.keySet()){
                        if(nextLedgerIntersection.get(s) > count){
                            candidate = s;
                            count = nextLedgerIntersection.get(s);
                        }
                    }
                    nextLedger = candidate;
                }
                if(!noNodes){
                    ArrayList<Boolean> responses = new ArrayList<>();
                    for(NodeConfig node: nodes){
                        responses.add(requestVerify(tx, node));
                    }
                    Integer responseCount = 0;
                    for(Boolean r: responses){
                        if(r)
                            responseCount += 1;
                    }
                    if(responseCount > responses.size() / 2){
                        notifyBc(tx);
                    }
                    else{
                        System.out.println("Invalid verification");
                    }
                }
                else{
                    System.out.println("No ledger found");
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
