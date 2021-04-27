package ru.mbc.ledger.network;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import ru.mbc.ledger.core.entity.block.MvpBlockContainer;
import ru.mbc.ledger.core.entity.blockchain.MvpBlockchain;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTx;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTxContainer;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.entity.state.MvpStateTxContainer;
import ru.mbc.ledger.core.error.api.InvalidParameter;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;
import ru.mbc.ledger.util.Config;
import ru.mbc.ledger.util.HashSum;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MvpConfigNetwork extends Thread {
    private Config config;
    private MvpBlockchain blockchain;
    private Vertx vertx;
    private HttpServer server;
    private WebClient client;
    private ArrayList<Endpoint> endpoints;
    private ledgerDbPostgre db;

    public MvpConfigNetwork(Config c, MvpBlockchain bc, ledgerDbPostgre _db){
        config = c;
        blockchain = bc;
        vertx = Vertx.vertx();
        client = WebClient.create(vertx);
        server = vertx.createHttpServer();
        db = _db;

        endpoints = new ArrayList<>();
        for(Endpoint e: config.network.peers){
            endpoints.add(e);
        }

    }

    public void broadcast_state_tx(MvpStateTx tx){
        for(Endpoint ep: endpoints){
            client
                    .post(ep.port, ep.ip, "/stx/")
                    .sendJsonObject(new JsonObject().put("sTx", Hex.encodeHex(tx.serialize())))
                    .onSuccess(res -> {
                        Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
                        if(res.statusCode() != 201 && res.statusCode() != 200){
                            lgr.log(Level.SEVERE, "Error sending state tx");
                        }
                        else{
                            lgr.log(Level.FINE, "Successfully sent state tx to " + ep.ip);
                        }
                    });
        }
    }

    public void broadcast_registry_tx(MvpRegistryTx tx){
        for(Endpoint ep: endpoints){
            client
                    .post(ep.port, ep.ip, "/rtx/")
                    .sendJsonObject(new JsonObject().put("rTx", Hex.encodeHex(tx.serialize())))
                    .onSuccess(res -> {
                        Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
                        if(res.statusCode() != 201 && res.statusCode() != 200){
                            lgr.log(Level.SEVERE, "Error sending registry tx");
                        }
                        else{
                            lgr.log(Level.FINE, "Successfully sent registry tx to " + ep.ip);
                        }
                    });
        }
    }

    public void broadcast_new_block(MvpBlockContainer container){
        System.out.println("Broadcasting block: " + container.block.getHash());
        for(Endpoint ep: endpoints){
            client
                    .post(ep.port, ep.ip, "/new_block/")
                    .sendJsonObject(new JsonObject(container.toJson()))
                    .onSuccess(res -> {
                        Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
                        if(res.statusCode() != 201 && res.statusCode() != 200){
                            lgr.log(Level.SEVERE, "Error sending block container");
                        }
                        else{
                            lgr.log(Level.FINE, "Successfully sent block container to " + ep.ip);
                        }
                    });
        }
    }

    public MvpBlockContainer obtain_block(Integer slot){
        for(Endpoint ep: endpoints){
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(new URI(
                        "http",
                        null,
                        ep.ip,
                        ep.port,
                        "/block/" + slot.toString() + "/",
                        null,
                        null
                )).build();
                java.net.http.HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                MvpBlockContainer c;
                try{
                    c = new MvpBlockContainer(response.body());
                }
                catch (InvalidParameter e){
                    c = null;
                }
                if(c != null){
                    return c;
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void run(){
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.route(HttpMethod.POST, "/new_block/").handler(ctx -> {
            HttpServerResponse response = ctx.response();

            String json = ctx.getBodyAsString().strip();
            response.putHeader("content-type", "application/json");
            response.end("{\"status\": \"ok\"}");

            MvpBlockContainer container = new MvpBlockContainer(json);
            System.out.println("Got remote block: " + container.block.getHash());
            blockchain.handleNewRemoteBlock(container);
        });

        router.route(HttpMethod.POST, "/stx/").handler(ctx -> {
            HttpServerResponse response = ctx.response();

            String json = ctx.getBodyAsString().strip();
            response.putHeader("content-type", "application/json");
            response.end("{\"status\": \"ok\"}");

            try {
                byte[] stx = Hex.decodeHex(new JSONObject(json).getString("sTx"));
                MvpStateTx stateTx = new MvpStateTx(stx);
                db.addTransaction(stateTx);
            } catch (DecoderException e) {
                e.printStackTrace();
            }
        });

        router.route(HttpMethod.POST, "/rtx/").handler(ctx -> {
            HttpServerResponse response = ctx.response();

            String json = ctx.getBodyAsString().strip();
            response.putHeader("content-type", "application/json");
            response.end("{\"status\": \"ok\"}");

            try {
                byte[] rtx = Hex.decodeHex(new JSONObject(json).getString("rTx"));
                MvpRegistryTx registryTx = new MvpRegistryTx(rtx);
                db.addRegistryTx(registryTx);
            } catch (DecoderException e) {
                e.printStackTrace();
            }
        });

        router.route(HttpMethod.GET, "/block/:slot/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            Integer slot = Integer.parseInt(ctx.request().getParam("slot"));

            MvpBlockContainer container = new MvpBlockContainer();
            container.block = db.getBlockBySlot(slot);
            container.state = db.getState(container.block.getStateHash());
            container.registry = db.getRegistry(container.block.getRegistryHash());
            ArrayList<MvpStateTx> stateTxList = new ArrayList<>();
            for(HashSum _h: container.block.getStateTxList()){
                stateTxList.add(db.getTransaction(_h));
            }
            ArrayList<MvpRegistryTx> registryTxList = new ArrayList<>();
            for(HashSum _h: container.block.getRegistryTxList()){
                registryTxList.add(db.getRegistryTransaction(_h));
            }
            container.sTx = new MvpStateTxContainer(stateTxList);
            container.rTx = new MvpRegistryTxContainer(registryTxList);

            response.putHeader("content-type", "application/json");
            response.end(container.toJson());
        });

        server.requestHandler(router).listen(config.general.ledgerApiPort);
    }

}
