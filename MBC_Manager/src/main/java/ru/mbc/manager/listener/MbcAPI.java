package ru.mbc.manager.listener;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.mbc.manager.config.NodeConfig;
import ru.mbc.manager.verification.MbcLogic;
import ru.mbc.manager.verification.TxDescriptor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ArrayBlockingQueue;

public class MbcAPI extends Thread {
    Vertx vertx;
    HttpServer server;
    Router router;
    String host;
    Integer port;
    MbcLogic logic;
    Integer bcPort;

    public MbcAPI(String host, Integer port, MbcLogic _logic, Integer _bcPort){
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        this.host = host;
        this.port = port;
        logic = _logic;
        bcPort = _bcPort;
    }

    @Override
    public void run(){
        router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.route(HttpMethod.GET, "/search/:ledgerId/").handler(ctx -> {

            HttpServerResponse response = ctx.response();
            String id = ctx.request().getParam("ledgerId");
            String targetLedgerAddress = null;
            try {
                targetLedgerAddress = new String(Hex.decodeHex(id));
            } catch (DecoderException e) {
                e.printStackTrace();
            }

            String nextLedger = logic.getNextLedger(logic.getCurrentLedgerAddress(), targetLedgerAddress);
            if(nextLedger != null){
                ArrayList<NodeConfig> nextNodes = logic.getNodesForLedger(nextLedger);
                JSONObject json = new JSONObject();
                json.put("response", "next");
                json.put("address", nextLedger);
                JSONArray ja = new JSONArray();
                for(NodeConfig node: nextNodes){
                    JSONObject jo = new JSONObject();
                    jo.put("ip", node.ip);
                    jo.put("port", node.port);
                    ja.put(jo);
                }
                json.put("nodes", ja);
                response.setStatusCode(200);
                response.putHeader("content-type", "application/json");
                response.end(json.toString());
            }
            else{
                response.setStatusCode(200);
                response.putHeader("content-type", "application/json");
                response.end("{\"response\": \"me\"}");
            }
        });

        router.route(HttpMethod.POST, "/verify/").handler(ctx -> {

            HttpServerResponse response = ctx.response();
            String json = ctx.getBodyAsString().strip();
            JSONObject jo = new JSONObject(json);

            String targetLedgerAddress = jo.getString("ledgerAddress");
            if(!targetLedgerAddress.equals(logic.getCurrentLedgerAddress())){
                response.setStatusCode(404);
                response.end("Wrong node");
            }

            // TODO: request to bc api
            Boolean result = false;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = null;
            HttpResponse<String> httpResponse = null;
            try {

                request = HttpRequest.newBuilder().uri(new URI(
                        "http",
                        null,
                        "127.0.0.1",
                        bcPort,
                        "/verify/",
                        null,
                        null
                ))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jo.toString())).build();
                httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
                if(httpResponse.statusCode() == 200){
                    result = true;
                }
            } catch (InterruptedException | IOException | URISyntaxException e) {
                e.printStackTrace();
            }

            if(!result){
                response.setStatusCode(404);
                response.end("Invalid verification");
            }
            else{
                response.putHeader("content-type", "application/json");
                response.end("{\"status\": \"ok\"}");
            }
        });

        server.requestHandler(router).listen(port);
    }
}
