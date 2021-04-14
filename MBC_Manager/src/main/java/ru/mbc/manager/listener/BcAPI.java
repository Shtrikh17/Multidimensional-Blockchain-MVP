package ru.mbc.manager.listener;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import ru.mbc.manager.verification.TxDescriptor;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class BcAPI extends Thread {
    Vertx vertx;
    HttpServer server;
    Router router;
    ArrayBlockingQueue<TxDescriptor> queue;
    String host;
    Integer port;

    public BcAPI(String host, Integer port, ArrayBlockingQueue<TxDescriptor> _q){
        vertx = Vertx.vertx();
        server = vertx.createHttpServer();
        queue = _q;
        this.host = host;
        this.port = port;
    }

    @Override
    public void run(){
        router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.route(HttpMethod.POST, "/verify/").handler(ctx -> {
            HttpServerResponse response = ctx.response();

            String json = ctx.getBodyAsString().strip();
            queue.add(new TxDescriptor(json));

            response.putHeader("content-type", "application/json");
            response.end("{\"status\": \"ok\"}");
        });

        server.requestHandler(router).listen(port);
    }
}
