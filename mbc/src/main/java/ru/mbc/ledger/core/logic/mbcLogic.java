package ru.mbc.ledger.core.logic;

import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.error.db.NoSuchEntity;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;

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
import ru.mbc.ledger.util.HashSum;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class mbcLogic extends Thread{
    Vertx vertx;
    HttpServer server;
    Router router;
    String host;
    Integer port;
    ledgerDbPostgre db;
    Integer mbcPort;

    public mbcLogic(String host, Integer verifierPort, Integer _mbcPort, ledgerDbPostgre _db){
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        this.host = host;
        this.port = verifierPort;
        db = _db;
        mbcPort = _mbcPort;
    }

    public void requestTxVerification(MvpStateTx tx){
        JSONObject jo = new JSONObject();
        jo.put("tx", tx.getFrom());
        jo.put("value", tx.getValue());
        jo.put("to", tx.getTo());
        String requestBody = jo.toString();
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = null;
        HttpResponse<String> httpResponse = null;
        try {
            request = HttpRequest.newBuilder().uri(new URI(
                    "http",
                    null,
                    "127.0.0.1",
                    mbcPort,
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
    }

    @Override
    public void run(){
        router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.route(HttpMethod.POST, "/post_verify/").handler(ctx -> {
            HttpServerResponse response = ctx.response();

            String json = ctx.getBodyAsString().strip();
            JSONObject jo = new JSONObject(json);
            HashSum txHash = new HashSum(jo.getString("tx"));
            db.txMarkAsVerified(txHash);

            response.putHeader("content-type", "application/json");
            response.end("{\"status\": \"ok\"}");
        });

        router.route(HttpMethod.POST, "/verify/").handler(ctx -> {

            HttpServerResponse response = ctx.response();
            String json = ctx.getBodyAsString().strip();
            JSONObject jo = new JSONObject(json);


            String hash = jo.getString("tx");
            try{
                MvpStateTx tx = db.getTransaction(new HashSum(hash));
                if(tx.getIncluded()){
                    if(tx.getTo().equals(jo.getString("to")) && tx.getValue() == jo.getInt("value")){
                        response.putHeader("content-type", "application/json");
                        response.end("{\"status\": \"ok\"}");
                    }
                    else{
                        System.out.println("Found incorrect tx parameters during verification: " + hash);
                        response.setStatusCode(404);
                        response.putHeader("content-type", "application/json");
                        response.end("{\"status\": \"Incorrect transaction\"}");
                    }
                }
                else{
                    System.out.println("Found not applied tx while verification: " + hash);
                    response.setStatusCode(404);
                    response.putHeader("content-type", "application/json");
                    response.end("{\"status\": \"not applied\"}");
                }
            }
            catch(NoSuchEntity e){
                System.out.println("Not found requested tx while verification: " + hash);
                response.setStatusCode(404);
                response.putHeader("content-type", "application/json");
                response.end("{\"status\": \"not found\"}");
            }


            // TODO: check that tx is included

        });

        server.requestHandler(router).listen(port);
    }
}
