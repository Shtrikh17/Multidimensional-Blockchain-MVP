package ru.mbc.manager.config;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Config {
    public MBC_API mbc_api;
    public BC_API bc_api;
    public ParentLedgerConfig parent;
    public ChildLedgerConfig children;
    public General general;

    public Config(){
        mbc_api = new MBC_API();
        bc_api = new BC_API();
        parent = new ParentLedgerConfig();
        children = new ChildLedgerConfig();
        general = new General();
    }

    public void parse(String filename) throws ConfigError{
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
        general.ledgerAddress = json.getString("ledger");
        ArrayList<String> tmp = new ArrayList<>(Arrays.asList(general.ledgerAddress.split("/")));
        tmp.remove(tmp.size() - 1);
        parent.ledgerAddress = String.join("/", tmp);

        // MBC API
        jsonElement = json.getJSONObject("mbc_api");
        mbc_api.node.ip = jsonElement.getString("ip");
        mbc_api.node.port = jsonElement.getInt("port");

        // BC API
        jsonElement = json.getJSONObject("bc_api");
        bc_api.node.ip = jsonElement.getString("ip");
        bc_api.node.port = jsonElement.getInt("port");

        // Parent
        jsonArray = json.getJSONArray("parent_nodes");
        for(int i = 0; i < jsonArray.length(); i++){
            jsonElement = jsonArray.getJSONObject(i);
            NodeConfig node = new NodeConfig();
            node.ip = jsonElement.getString("ip");
            node.port = jsonElement.getInt("port");
            parent.nodes.add(node);
        }

        // Children
        jsonArray = json.getJSONArray("children");
        for(int i = 0; i < jsonArray.length(); i++){
            jsonElement = jsonArray.getJSONObject(i);
            ChildLedger child = new ChildLedger();
            child.ledgerAddress = jsonElement.getString("ledger");
            JSONArray nodes = jsonElement.getJSONArray("nodes");
            for(int j = 0; j < nodes.length(); j++){
                NodeConfig node = new NodeConfig();
                JSONObject jo = nodes.getJSONObject(j);
                node.ip = jo.getString("ip");
                node.port = jo.getInt("port");
                child.nodes.add(node);
            }
        }
    }

}
