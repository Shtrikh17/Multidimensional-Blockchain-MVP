package ru.mbc.manager.verification;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TxDescriptor {
    private String TxHash;
    private String LedgerAddress;

    public String getTxHash(){
        return TxHash;
    }

    public String getLedgerAddress(){
        return LedgerAddress;
    }

    public TxDescriptor(String jsonString){
        JSONObject json = new JSONObject(jsonString);
        String tx = json.getString("tx");
        ArrayList<String> tokens = new ArrayList(Arrays.asList(tx.split("/")));
        TxHash = tokens.get(tokens.size()-1);
        tokens.remove(tokens.size()-1);
        LedgerAddress = String.join("/", tokens);
    }
}
