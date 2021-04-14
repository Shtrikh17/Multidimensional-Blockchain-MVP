package ru.mbc.ledger.api;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import ru.mbc.ledger.api.ApiHandler;
import ru.mbc.ledger.util.Config;

public class ApiRestUi implements Runnable {
    private ApiHandler handler;
    private Config config;


    public ApiRestUi(ApiHandler handler, Config c){
        this.handler = handler; config = c;
    }

    @Override
    public void run() {
        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route(HttpMethod.GET, "/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            response.putHeader("content-type", "application/json");
            response.end("{\"status\": \"ok\"}");
        });

        router.route(HttpMethod.GET, "/state/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String result = handler.getLastState();
            if(result == null){
                response.setStatusCode(404);
                response.end("No such state");
            }
            else{
                response.putHeader("content-type", "application/json");
                response.end(result);
            }
        });

        router.route(HttpMethod.GET, "/state/:stateId/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String id = ctx.request().getParam("stateId");
            String result = handler.getState(id);
            if(result == null){
                response.setStatusCode(404);
                response.end("No such state");
            }
            else{
                response.putHeader("content-type", "application/json");
                response.end(result);
            }
        });

        router.route(HttpMethod.GET, "/registry/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String result = handler.getLastRegistry();
            if(result == null){
                response.setStatusCode(404);
                response.end("No such registry");
            }
            else{
                response.putHeader("content-type", "application/json");
                response.end(result);
            }
        });

        router.route(HttpMethod.GET, "/registry/:registryId/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String id = ctx.request().getParam("registryId");
            String result = handler.getRegistry(id);
            if(result == null){
                response.setStatusCode(404);
                response.end("No such registry");
            }
            else{
                response.putHeader("content-type", "application/json");
                response.end(result);
            }
        });

        router.route(HttpMethod.GET, "/stx/:txId/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String id = ctx.request().getParam("txId");
            String result = handler.getStateTx(id);
            if(result == null){
                response.setStatusCode(404);
                response.end("No such state tx");
            }
            else{
                response.putHeader("content-type", "application/json");
                response.end(result);
            }
        });

        router.route(HttpMethod.GET, "/rtx/:txId/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String id = ctx.request().getParam("txId");
            String result = handler.getRegistryTx(id);
            if(result == null){
                response.setStatusCode(404);
                response.end("No such registry tx");
            }
            else{
                response.putHeader("content-type", "application/json");
                response.end(result);
            }
        });


        router.route(HttpMethod.GET, "/block/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String result = handler.getLastBlock();
            if(result == null){
                response.setStatusCode(404);
                response.end("No such block");
            }
            else{
                response.putHeader("content-type", "application/json");
                response.end(result);
            }
        });

        router.route(HttpMethod.GET, "/block/ex/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String result = handler.getLastExBlock();
            if(result == null){
                response.setStatusCode(404);
                response.end("No such block");
            }
            else{
                response.putHeader("content-type", "application/json");
                response.end(result);
            }
        });


        router.route(HttpMethod.GET, "/block/:blockId/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String id = ctx.request().getParam("blockId");
            String result = handler.getBlock(id);
            if(result == null){
                response.setStatusCode(404);
                response.end("No such block");
            }
            else{
                response.putHeader("content-type", "application/json");
                response.end(result);
            }
        });

        router.route(HttpMethod.GET, "/block/:blockId/ex/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String id = ctx.request().getParam("blockId");
            String result = handler.getExBlock(id);
            if(result == null){
                response.setStatusCode(404);
                response.end("No such block");
            }
            else{
                response.putHeader("content-type", "application/json");
                response.end(result);
            }
        });

        // [+] VERIFIED
        router.route(HttpMethod.POST, "/stx/").handler(ctx -> {
            HttpServerResponse response = ctx.response();

            String json = ctx.getBodyAsString().strip();
            handler.addStateTx(json);

            response.putHeader("content-type", "application/json");
            response.end("{\"status\": \"ok\"}");
        });

        // [+] VERIFIED
        router.route(HttpMethod.GET, "/stx/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String result = handler.getPendingStateTx();
            response.putHeader("content-type", "application/json");
            response.end(result);
        });

        // [+] VERIFIED
        router.route(HttpMethod.GET, "/rtx/").handler(ctx -> {
            HttpServerResponse response = ctx.response();
            String result = handler.getPendingRegistryTx();
            response.putHeader("content-type", "application/json");
            response.end(result);
        });

        // [+] VERIFIED
        router.route(HttpMethod.POST, "/rtx/").handler(ctx -> {
            HttpServerResponse response = ctx.response();

            String json = ctx.getBodyAsString().strip();
            handler.addRegistryTx(json);

            response.putHeader("content-type", "application/json");
            response.end("{\"status\": \"ok\"}");
        });


        server.requestHandler(router).listen(config.general.apiPort);
    }
}
