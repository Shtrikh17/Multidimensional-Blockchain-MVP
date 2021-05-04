package ru.mbc.manager.verification;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class TxDescriptor {
    private String TxHash;
    private String LedgerAddress;
    private String toAddress;
    private Integer value;
    private String TxInHash;

    public String getTxHash(){
        return TxHash;
    }

    public String getTxInHash() {return TxInHash;}

    public String getLedgerAddress(){
        return LedgerAddress;
    }

    public String getToAddress(){
        return toAddress;
    }

    public Integer getValue(){
        return value;
    }

    public TxDescriptor(String jsonString, String currentLedger){
        JSONObject json = new JSONObject(jsonString);
        String tx = json.getString("tx_out");
        ArrayList<String> tokens = new ArrayList(Arrays.asList(tx.split("/")));
        TxHash = tokens.get(tokens.size()-1);
        tokens.remove(tokens.size()-1);
        LedgerAddress = String.join("/", tokens);
        toAddress = currentLedger + "/" + json.getString("to");
        value = json.getInt("value");
        TxInHash = json.getString("tx_in");
    }
}
