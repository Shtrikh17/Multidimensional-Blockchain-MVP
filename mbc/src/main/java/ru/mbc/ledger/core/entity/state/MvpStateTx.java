package ru.mbc.ledger.core.entity.state;

import org.web3j.crypto.Sign;
import ru.mbc.ledger.core.entity.HashableEntity;
import ru.mbc.ledger.core.entity.SerializableEntity;
import ru.mbc.ledger.core.error.structure.IncompleteObject;
import ru.mbc.ledger.util.HashSum;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class MvpStateTx implements HashableEntity, SerializableEntity {
    Integer nonce;
    String from, to;
    Integer value;
    MvpStateTxSignature signature;
    MvpStateTxType type;
    boolean isVerified;
    boolean isIncluded;

    public MvpStateTx(){}

    public MvpStateTx(Integer nonce, String from, String to, Integer value){
        set(nonce,from,to,value,null, MvpStateTxType.INTERNAL,false,false);
    }

    public MvpStateTx(Integer nonce, String from, String to, Integer value, MvpStateTxType type, boolean verified){
        set(nonce,from,to,value,null,type,verified,false);
    }

    public MvpStateTx(Integer nonce, String from, String to, Integer value, Integer type){
        MvpStateTxType _type;
        switch (type){
            case 2:
                _type = MvpStateTxType.EXTERNAL_IN;
                break;
            case 1:
                _type = MvpStateTxType.EXTERNAL_OUT;
                break;
            default:
                _type = MvpStateTxType.INTERNAL;
        }
        set(nonce,from,to,value, null, _type,false,false);
    }

    public MvpStateTx(Integer nonce, String from, String to, Integer value, MvpStateTxSignature signature, MvpStateTxType type){
        set(nonce,from,to,value,signature, type,false,false);
    }

    public MvpStateTx(Integer nonce, String from, String to, Integer value, MvpStateTxSignature signature){
        set(nonce,from,to,value,signature, MvpStateTxType.INTERNAL,false,false);
    }

    public MvpStateTx(Integer nonce, String from, String to, Integer value, MvpStateTxSignature signature, MvpStateTxType type, boolean verified, boolean included){
        set(nonce,from,to,value,signature,type,verified,included);
    }

    public MvpStateTx(Integer nonce, String from, String to, Integer value, MvpStateTxSignature signature, int type, boolean verified, boolean included){
        MvpStateTxType t;
        switch (type){
            case 2:
                t = MvpStateTxType.EXTERNAL_IN;
                break;
            case 1:
                t = MvpStateTxType.EXTERNAL_OUT;
                break;
            default:
                t = MvpStateTxType.INTERNAL;
        }

        set(nonce,from,to,value,signature,t,verified,included);
    }

    public MvpStateTx(Integer nonce, String from, String to, Integer value, MvpStateTxSignature signature, MvpStateTxType type, boolean verified){
        set(nonce,from,to,value,signature,type,verified,false);
    }

    public MvpStateTx(MvpStateTx oldTx){
        nonce = oldTx.nonce;
        from = oldTx.from;
        to = oldTx.to;
        value = oldTx.value;
        signature = new MvpStateTxSignature(oldTx.signature);
        type = oldTx.type;
        isVerified = oldTx.isVerified;
        isIncluded = oldTx.isIncluded;
    }

    public MvpStateTx(byte[] data){
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);
        try{
            while(din.available() > 0){
                nonce = din.readInt();
                int j = din.readInt();
                from = new String(din.readNBytes(j));
                j = din.readInt();
                to = new String(din.readNBytes(j));
                value = din.readInt();
                int k = din.readInt();
                switch (k){
                    case 2:
                        type = MvpStateTxType.EXTERNAL_IN;
                        break;
                    case 1:
                        type = MvpStateTxType.EXTERNAL_OUT;
                        break;
                    default:
                        type = MvpStateTxType.INTERNAL;
                }
                k = din.readInt();
                signature = new MvpStateTxSignature(din.readNBytes(k));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void set(Integer nonce, String from, String to, Integer amount, MvpStateTxSignature signature, MvpStateTxType type, boolean verified, boolean included){
        this.nonce = nonce;
        this.from = from;
        this.to = to;
        this.value = amount;
        this.signature = signature;
        this.isIncluded = included;
        this.isVerified = verified;
        this.type = type;
    }

    public void set(byte[] data){
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);
        try {
            nonce = din.readInt();
            int j = din.readInt();
            from = new String(din.readNBytes(j));
            j = din.readInt();
            to = new String(din.readNBytes(j));
            value = din.readInt();

            Integer k = din.readInt();
            switch (k){
                case 2:
                    type = MvpStateTxType.EXTERNAL_IN;
                    break;
                case 1:
                    type = MvpStateTxType.EXTERNAL_OUT;
                    break;
                default:
                    type = MvpStateTxType.INTERNAL;
            }
            k = din.readInt();
            signature = new MvpStateTxSignature(din.readNBytes(k));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] serialize(){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try {
            dout.writeInt(nonce);
            dout.writeInt(from.length());
            dout.write(from.getBytes(StandardCharsets.UTF_8));
            dout.writeInt(to.length());
            dout.write(to.getBytes(StandardCharsets.UTF_8));
            dout.writeInt(value);
            switch (type){
                case EXTERNAL_IN:
                    dout.writeInt(2);
                    break;
                case EXTERNAL_OUT:
                    dout.writeInt(1);
                    break;
                default:
                    dout.writeInt(0);
            }
            byte[] v = signature.serialize();
            dout.writeInt(v.length);
            dout.write(v);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }


    public byte[] serializePayload() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try {
            dout.writeInt(nonce);
            dout.writeInt(from.length());
            dout.write(from.getBytes(StandardCharsets.UTF_8));
            dout.writeInt(to.length());
            dout.write(to.getBytes(StandardCharsets.UTF_8));
            dout.writeInt(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public HashSum getHashForSignature(){
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(serializePayload());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new HashSum(hash);
    }

    public HashSum getHash() throws IncompleteObject {
        byte[] hash = null;
        if(signature == null){
            throw new IncompleteObject("Transaction must be signed to be hashed");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try {
            dout.writeInt(nonce);
            dout.writeInt(from.length());
            dout.write(from.getBytes(StandardCharsets.UTF_8));
            dout.writeInt(to.length());
            dout.write(to.getBytes(StandardCharsets.UTF_8));
            dout.writeInt(value);
            dout.write(signature.getHash().getArray());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(out.toByteArray());

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new HashSum(hash);
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public MvpStateTxSignature getSignature() {
        return signature;
    }

    public Integer getValue() {
        return value;
    }

    public boolean getIncluded() {
        return isIncluded;
    }

    public boolean getExternal() {
        return type == MvpStateTxType.EXTERNAL_IN || type == MvpStateTxType.EXTERNAL_OUT;
    }

    public boolean getExternalIn(){
        return type == MvpStateTxType.EXTERNAL_IN;
    }

    public boolean getExternalOut(){
        return type == MvpStateTxType.EXTERNAL_OUT;
    }

    public boolean getInternal(){
        return type == MvpStateTxType.INTERNAL;
    }

    public MvpStateTxType getType(){
        return type;
    }

    public boolean getVerified() {
        return isVerified;
    }

    public Integer getNonce() {
        return nonce;
    }

    public void setVerified(){
        isVerified = true;
    }

    public void setIncluded(){
        isIncluded = true;
    }

    public boolean isSigned(){
        return signature != null;
    }

    public void setSignature(Sign.SignatureData sign){
        signature = new MvpStateTxSignature(sign);
    }

    public int getRawType(){
        if(type == MvpStateTxType.EXTERNAL_IN)
            return 2;
        else if(type == MvpStateTxType.EXTERNAL_OUT)
            return 1;
        else
            return 0;
    }

}
