package ru.mbc.ledger.core.entity.ledger;

import ru.mbc.ledger.util.HashSum;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MvpLedgerDescriptor {
    private String alias;

    public MvpLedgerDescriptor(){}

    public MvpLedgerDescriptor(String v){
        alias = v;
    }

    public MvpLedgerDescriptor(byte[] v){
        alias = new String(v, StandardCharsets.UTF_8);
    }

    public MvpLedgerDescriptor(MvpLedgerDescriptor descriptor){
        alias = descriptor.alias;
    }

    public void deserialize(byte[] v){
        alias = new String(v, StandardCharsets.UTF_8);
    }

    public String getAlias(){
        return alias;
    }

    public byte[] serialize(){
        return alias.getBytes(StandardCharsets.UTF_8);
    }

    public HashSum getHash(){
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(serialize());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new HashSum(hash);
    }
}
