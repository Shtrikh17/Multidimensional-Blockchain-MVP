package ru.mbc.ledger.core.entity.registry;

import ru.mbc.ledger.core.entity.HashableEntity;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerDescriptor;
import ru.mbc.ledger.core.entity.SerializableEntity;
import ru.mbc.ledger.util.HashSum;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MvpRegistryTx implements HashableEntity, SerializableEntity {
    private Integer nonce;
    private MvpLedgerDescriptor descriptor;
    private boolean isIncluded;

    public MvpRegistryTx(){
        nonce = 0;
        descriptor = new MvpLedgerDescriptor();
        isIncluded = false;
    }

    public MvpRegistryTx(MvpRegistryTx oldTx){
        nonce = oldTx.nonce;
        descriptor = new MvpLedgerDescriptor(oldTx.descriptor);
        isIncluded = false;
    }

    public MvpRegistryTx(Integer _nonce, MvpLedgerDescriptor _descriptor){
        set(_nonce, _descriptor, false);
    }

    public MvpRegistryTx(Integer _nonce, MvpLedgerDescriptor _descriptor, boolean included){
        set(_nonce, _descriptor, included);
    }

    public MvpRegistryTx(byte[] data){
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);
        try{
            nonce = din.readInt();
            int k = din.readInt();
            descriptor = new MvpLedgerDescriptor(din.readNBytes(k));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void set(Integer _nonce, byte[] _descriptor, boolean included){
        nonce = _nonce;
        descriptor = new MvpLedgerDescriptor(_descriptor);
        isIncluded = included;
    }

    public void set(Integer _nonce, MvpLedgerDescriptor _descriptor, boolean included){
        nonce = _nonce;
        descriptor = new MvpLedgerDescriptor(_descriptor);
        isIncluded = included;
    }

    public Integer getNonce(){
        return nonce;
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

    public HashSum getLedgerHash(){
        return descriptor.getHash();
    }

    public MvpLedgerDescriptor getLedgerDescriptor(){
        return descriptor;
    }

    public boolean getIncluded(){
        return isIncluded;
    }

    public void setIncluded(){
        isIncluded = true;
    }

    public byte[] serialize(){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        DataOutputStream dout = new DataOutputStream(outputStream);
        try {
            dout.writeInt(nonce);
            byte[] v = descriptor.serialize();
            dout.writeInt(v.length);
            dout.write(v);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public void set(byte[] data){
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);
        try {
            nonce = din.readInt();
            Integer k = din.readInt();
            descriptor = new MvpLedgerDescriptor(din.readNBytes(k));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
