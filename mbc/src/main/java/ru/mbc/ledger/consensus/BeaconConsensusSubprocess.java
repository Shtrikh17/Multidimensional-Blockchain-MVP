package ru.mbc.ledger.consensus;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.json.JSONObject;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import ru.mbc.ledger.core.entity.block.MvpBlock;
import ru.mbc.ledger.core.entity.block.MvpBlockContainer;
import ru.mbc.ledger.core.entity.blockchain.MvpBlockchain;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerUser;
import ru.mbc.ledger.util.Config;
import ru.mbc.ledger.util.HashSum;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;

import java.beans.beancontext.BeanContext;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.URI;
import java.security.SignatureException;
import java.net.http.HttpResponse;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.ext.web.client.WebClient;

public class BeaconConsensusSubprocess extends Thread{
    ConsensusBlockNotify consensusNotify;
    ledgerDbPostgre db;
    WebClient client;
    Config config;
    Vertx vertx;
    MvpBlockchain blockchain;
    Boolean toCreate;
    Integer slot;

    Integer lastSlot;

    public BeaconConsensusSubprocess(ConsensusBlockNotify cbn, ledgerDbPostgre _db, Config c, MvpBlockchain bc){
        consensusNotify = cbn;
        db = _db;
        vertx = Vertx.vertx();
        client = WebClient.create(vertx);
        config = c;
        blockchain = bc;
        toCreate = false;

        lastSlot = 0;
    }

    public void register(){
        if(config.consensus.miningAllowed){
            client
                    .post(config.consensus.oraclePort, config.consensus.oracleHost, "/register")
                    .sendJsonObject(new JsonObject().put("address", config.consensus.user.getAddress().toString()))
                    .onSuccess(res -> {
                        Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
                        if(res.statusCode() != 201){
                            lgr.log(Level.SEVERE, "Error in registration at beacon");
                            System.exit(1);
                        }
                        lgr.log(Level.FINE, "Successfully registered to beacon");
                    });
        }
    }

    public boolean checkBlockConsensusValidity(MvpBlock block){
        BeaconConsensusStructure consensusStructure = block.getConsensus();
        Sign.SignatureData sign = consensusStructure.unpack();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try {
            dout.write(block.getHashForConsensus().getArray());
            dout.writeLong(consensusStructure.getTimestamp());
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] msgHash = Hash.sha256(out.toByteArray());

        Boolean result = false;
        try {
            BigInteger pubKeyRecovered = Sign.signedMessageHashToKey(msgHash, sign);
            HashSum recAddress = new HashSum(Hash.sha256(pubKeyRecovered.toByteArray()));
            if(recAddress.equals(consensusStructure.getOwner())){
                result = true;
            }
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return result;
    }

    public BeaconConsensusStructure findBlockConsensus(MvpBlock block, MvpLedgerUser user){
        Long timestamp = System.currentTimeMillis() / 1000L;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try {
            dout.write(block.getHashForConsensus().getArray());
            dout.writeLong(timestamp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Sign.SignatureData signature = user.signRawData(out.toByteArray());
        return new BeaconConsensusStructure(signature, timestamp, user.getAddress());
    }


    public boolean checkSlotOrder(HashSum owner, Integer slot_number){
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(
                    "http",
                    null,
                    config.consensus.oracleHost,
                    config.consensus.oraclePort,
                    "/slot/" + slot_number.toString() + "/",
                    null,
                    null
            )).build();
            java.net.http.HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = new JsonObject(response.body());
            if(json.getInteger("slot") != null){
                if(new HashSum(json.getString("address")).equals(owner)){
                    System.out.println("Ready to acknowledge block in slot #" + json.getInteger("slot").toString());
                    return true;
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Integer getCurrentSlot(){
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(
                    "http",
                    null,
                    config.consensus.oracleHost,
                    config.consensus.oraclePort,
                    "/current",
                    null,
                    null
                    )).build();
            java.net.http.HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject jo = new JsonObject(response.body());
            return jo.getInteger("slot");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void run(){
        while(true){
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = null;
            try {
                request = HttpRequest.newBuilder().uri(new URI(
                        "http",
                        null,
                        config.consensus.oracleHost,
                        config.consensus.oraclePort,
                        "/current",
                        null,
                        null
                )).build();
                java.net.http.HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject json = new JsonObject(response.body());
                if(json.getInteger("slot") != null && json.getInteger("slot") > lastSlot){
                    if(new HashSum(json.getString("address")).equals(config.consensus.user.getAddress())){
                        System.out.println("Ready to create block in slot #" + json.getInteger("slot").toString());
                        toCreate = true;
                        slot = json.getInteger("slot");
                    }
                }
            } catch (URISyntaxException | IOException | InterruptedException e) {
                e.printStackTrace();
            }

            try {
                if(toCreate){
                    MvpBlockContainer container = blockchain.prepareNewBlock(slot);
                    BeaconConsensusStructure conStructure = findBlockConsensus(container.block, config.consensus.user);
                    container.block.setConsensus(conStructure);
                    blockchain.handleNewLocalBlock(container);
                    toCreate = false;
                    lastSlot = slot;
                }
                sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
