package ru.mbc.manager.verification;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
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
    public static ArrayList<NodeConfig> nodes;
    public static Boolean end;
    public static String nextLedger;

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

    public MbcLogic(Config config, ArrayBlockingQueue<TxDescriptor> q){
        myLedger = config.general.ledgerAddress;
        parentNodes = config.parent.nodes;
        childLedgers = config.children.ledgers;
        queue = q;
        parentLedger = config.parent.ledgerAddress;
    }

    public String getNextLedger(String currentLedger, String targetLedger) throws InvalidPathException {
        if(currentLedger.equals(targetLedger))
            return null;
        if(currentLedger.contains(targetLedger)){
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
                    "/search/" + Hex.encodeHex(ledger.getBytes(StandardCharsets.UTF_8)) + "/",
                    null,
                    null
            )).build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }
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
        return response;
    }

    public boolean requestVerify(String ledger, String txHash, NodeConfig node){
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = null;
        HttpResponse<String> httpResponse = null;
        try {
            request = HttpRequest.newBuilder().uri(new URI(
                    "http",
                    null,
                    node.ip,
                    node.port,
                    "/verify/" + Hex.encodeHex(ledger.getBytes(StandardCharsets.UTF_8)) + "/" + txHash + "/",
                    null,
                    null
            )).build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return httpResponse.statusCode() != 404;
    }

    @Override
    public void run(){
        while(true){
            try {
                TxDescriptor tx = queue.take();
                String nextLedger = getNextLedger(myLedger, tx.getLedgerAddress());
                ArrayList<NodeConfig> nodes = getNodesForLedger(nextLedger);
                while(true){
                    ArrayList<Response> responses = new ArrayList<>();
                    for(NodeConfig node: nodes){
                        responses.add(requestSearch(nextLedger, node));
                    }

                    // End
                    int endCount = 0;
                    for(Response response: responses){
                        if(response.end)
                            endCount += 1;
                    }
                    if(endCount >= responses.size() / 2){
                        break;
                    }

                    // Intersection and next nodes
                    Hashtable<NodeConfig, Integer> intersection = new Hashtable<>();
                    for(Response response: responses){
                        for(NodeConfig N: response.nodes){
                            if(intersection.containsKey(N)){
                                intersection.put(N, intersection.get(N) + 1);
                            }
                            else{
                                intersection.put(N, 1);
                            }
                        }
                    }

                    // find intersection of responses
                    nodes.clear();
                    for(NodeConfig N: intersection.keySet()){
                        if(intersection.get(N) > responses.size() / 2)
                            nodes.add(N);
                    }
                }

                ArrayList<Boolean> responses = new ArrayList<>();
                for(NodeConfig node: nodes){
                    responses.add(requestVerify(tx.getLedgerAddress(), tx.getTxHash(), node));
                }
                Integer responseCount = 0;
                for(Boolean r: responses){
                    if(r)
                        responseCount += 1;
                }
                if(responseCount > responses.size() / 2){
                    System.out.println("NOTIFY BC HERE");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
