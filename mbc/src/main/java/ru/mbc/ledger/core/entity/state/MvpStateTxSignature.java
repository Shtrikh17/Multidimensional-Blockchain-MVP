package ru.mbc.ledger.core.entity.state;

import org.web3j.crypto.Sign;
import ru.mbc.ledger.util.HashSum;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MvpStateTxSignature {
    private Sign.SignatureData signature;

    public MvpStateTxSignature(byte[] data){
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);
        try {
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

    public MvpStateTxSignature(MvpStateTxSignature oldSignature){
        signature = oldSignature.signature;
    }

    public MvpStateTxSignature(Sign.SignatureData oldSignature){
        signature = oldSignature;
    }

    public Sign.SignatureData unpack(){
        return signature;
    }

    public byte[] serialize(){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try {
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
