package ru.mbc.ledger.consensus;

import org.web3j.crypto.Sign;
import ru.mbc.ledger.util.HashSum;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BeaconConsensusStructure {
    private Long timestamp;
    Sign.SignatureData signature;
    private HashSum owner;

    public BeaconConsensusStructure(Sign.SignatureData s, Long ts, HashSum o){
        timestamp = ts;
        signature = s;
        owner = o;
    }

    public Long getTimestamp(){
        return timestamp;
    }

    public HashSum getOwner(){
        return owner;
    }

    public Sign.SignatureData unpack(){
        return signature;
    }

    public byte[] serialize(){
        if(signature == null || owner == null){
            return new byte[]{};
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try {
            dout.write(owner.getArray());
            dout.writeLong(timestamp);
            dout.writeInt(signature.getV().length);
            dout.write(signature.getV());
            dout.writeInt(signature.getR().length);
            dout.write(signature.getR());
            dout.writeInt(signature.getS().length);
            dout.write(signature.getS());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public BeaconConsensusStructure(){
        signature = null;
        owner = null;
        timestamp = null;
    }

    public BeaconConsensusStructure(byte[] data){
        if(data == null || data.length == 0){
            signature = null;
            owner = null;
            timestamp = null;
            return;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);
        try {
            owner = new HashSum(din.readNBytes(32));
            timestamp = din.readLong();
            Integer k = din.readInt();
            byte[] v = din.readNBytes(k);
            k = din.readInt();
            byte[] r = din.readNBytes(k);
            k = din.readInt();
            byte[] s = din.readNBytes(k);
            signature = new Sign.SignatureData(v,r,s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BeaconConsensusStructure(BeaconConsensusStructure oldSignature){
        signature = oldSignature.signature;
    }

    public BeaconConsensusStructure(Sign.SignatureData oldSignature){
        signature = oldSignature;
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
