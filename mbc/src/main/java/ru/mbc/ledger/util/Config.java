package ru.mbc.ledger.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerUser;
import ru.mbc.ledger.core.error.config.ConfigError;
import ru.mbc.ledger.core.error.config.ConfigValueError;
import ru.mbc.ledger.network.Endpoint;


public class Config {
    public final Consensus consensus;
    public final Network network;
    public final State state;
    public final General general;

    public Config(){
        consensus = new Consensus();
        network = new Network();
        state = new State();
        general = new General();
    }

    public void parse(String filename) throws ConfigError {
        String jsonText;
        try {
             jsonText = new String(Files.readAllBytes(Path.of(filename)));
        } catch (IOException e) {
            throw new ConfigError("Not found config file");
        }

        JSONObject json = new JSONObject(jsonText);
        JSONObject jsonElement;
        JSONArray jsonArray;

        // General
        jsonElement = json.getJSONObject("general");
        general.genesis = jsonElement.getBoolean("genesis");
        general.nodeId = jsonElement.getString("node_id");
        general.apiPort = jsonElement.getInt("api_port");
        general.bcPort = jsonElement.getInt("bc_port");
        general.mbcPort = jsonElement.getInt("mbc_server");
        general.mbcManager = jsonElement.getInt("mbc_manager");

        // Consensus
        jsonElement = json.getJSONObject("consensus");
        consensus.oracleHost = jsonElement.getString("host");
        consensus.oraclePort = jsonElement.getInt("port");
        consensus.slot = jsonElement.getInt("slot");
        consensus.miningAllowed = jsonElement.getBoolean("miningAllowed");
        jsonElement = jsonElement.getJSONObject("user");
        try {
            consensus.user = new MvpLedgerUser(
                    new HashSum(jsonElement.getString("address")),
                    Hex.decodeHex(jsonElement.getString("privKey")),
                    Hex.decodeHex(jsonElement.getString("pubKey"))
            );
        } catch (DecoderException e) {
            e.printStackTrace();
        }

        // Network
        jsonElement = json.getJSONObject("network");
        try{
            jsonArray = jsonElement.getJSONArray("peers");
        }
        catch (JSONException e){
            throw new ConfigError("No peers config found");
        }
        network.peers = new ArrayList<>();
        for(int i = 0; i < jsonArray.length(); i++){
            JSONObject nJO = jsonArray.getJSONObject(i);
            network.peers.add(new Endpoint(nJO.getString("ip"), nJO.getInt("port")));
        }

        // State
        jsonElement = json.getJSONObject("state");
        try{
            jsonArray = jsonElement.getJSONArray("accounts");
            for(int i = 0; i < jsonArray.length(); i++){
                try{
                    JSONObject o = jsonArray.getJSONObject(i);
                    state.accounts.add(new Account(o.getString("address"), o.getInt("value")));
                }
                catch(JSONException | ConfigValueError e){}
            }
        }
        catch (JSONException e){}
        try{
            jsonArray = jsonElement.getJSONArray("superusers");
            for(int i = 0; i < jsonArray.length(); i++){
                try{
                    state.superusers.add(new HashSum(Hex.decodeHex(jsonArray.getString(i))));
                }
                catch(JSONException | DecoderException e){}
            }
        }catch (JSONException e){}
    }
}
