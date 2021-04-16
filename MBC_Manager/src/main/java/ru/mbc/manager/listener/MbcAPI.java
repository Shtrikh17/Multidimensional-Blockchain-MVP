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

    public MbcAPI(String host, Integer port, MbcLogic _logic){
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        this.host = host;
        this.port = port;
        logic = _logic;
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

        router.route(HttpMethod.GET, "/verify/:ledgerId/:txHash/").handler(ctx -> {

            HttpServerResponse response = ctx.response();
            String ledgerId = ctx.request().getParam("ledgerId");
            String hash = ctx.request().getParam("txHash");
            String targetLedgerAddress = null;
            try {
                targetLedgerAddress = new String(Hex.decodeHex(ledgerId));
            } catch (DecoderException e) {
                e.printStackTrace();
            }
            if(!targetLedgerAddress.equals(logic.getCurrentLedgerAddress())){
                response.setStatusCode(404);
                response.end("Wrong node");
            }

            // TODO: request to bc api
            Boolean result = false;
            if(hash.equals("cafebabe"))
                result = true;
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
