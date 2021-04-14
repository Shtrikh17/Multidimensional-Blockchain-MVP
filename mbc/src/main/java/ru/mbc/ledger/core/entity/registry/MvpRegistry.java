package ru.mbc.ledger.core.entity.registry;

import ru.mbc.ledger.core.entity.HashableEntity;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerDescriptor;
import ru.mbc.ledger.core.entity.SerializableEntity;
import ru.mbc.ledger.core.error.tx.AliasError;
import ru.mbc.ledger.core.error.tx.NonceError;
import ru.mbc.ledger.util.HashSum;
import ru.mbc.ledger.util.transform;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;

public class MvpRegistry implements HashableEntity, SerializableEntity {
    Hashtable<HashSum, MvpLedgerDescriptor> storage;
    Integer registryNonce;

    public MvpRegistry(){
        storage = new Hashtable<>();
        registryNonce = 0;
    }

    public MvpRegistry(MvpRegistry oldRegistry){
        storage = transform.deepCopy(oldRegistry.getStorage());
        registryNonce = oldRegistry.registryNonce;
    }

    public MvpRegistry(Hashtable<HashSum, MvpLedgerDescriptor> oldStorage){
        storage = transform.deepCopy(oldStorage);
        registryNonce = 0;
    }

    public MvpRegistry(Hashtable<HashSum, MvpLedgerDescriptor> oldStorage, Integer _nonce){
        set(oldStorage, _nonce);
    }

    public MvpRegistry(byte[] data){
        set(data);
    }

    public void set(Hashtable<HashSum, MvpLedgerDescriptor> oldStorage, Integer _nonce){
        storage = transform.deepCopy(oldStorage);
        registryNonce = _nonce;
    }

    public void set(byte[] data){
        storage = new Hashtable<>();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);

        try {
            registryNonce = din.readInt();
            while (din.available() > 0) {
                HashSum hash = new HashSum(din.readNBytes(32));   // TODO: set define, currently - hardcode for sha-256
                Integer k = din.readInt();
                byte[] v = din.readNBytes(k);
                storage.put(hash, new MvpLedgerDescriptor(v));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Hashtable<HashSum, MvpLedgerDescriptor> getStorage() {
        return storage;
    }

    public Integer getRegistryNonce(){
        return registryNonce;
    }

    public boolean checkTX(MvpRegistryTx tx) throws AliasError, NonceError{
        // TODO: check if ledger is permitted to register here if such restrictions exist
        // TODO: check if counter is correct
        // TODO: check if alias not yet registered
        // Alias taken
        for(MvpLedgerDescriptor descriptor: storage.values()){
            if(descriptor.getAlias().equals(tx.getLedgerDescriptor().getAlias())){
                throw new AliasError("Alias already taken");
            }
        }
        // Check nonce
        if(registryNonce + 1 != tx.getNonce()){
            throw new NonceError("Invalid registry counter value");
        }
        return true;
    }

    public boolean verifyTX(MvpRegistryTx tx) throws AliasError, NonceError{
        return checkTX(tx);
    }

    public boolean applyTX(MvpRegistryTx _tx) throws AliasError, NonceError{
        if(!checkTX(_tx)){
            return false;
        }
        synchronized (storage){
            storage.put(_tx.getLedgerHash(), _tx.getLedgerDescriptor());
            registryNonce += 1;
        }
        return true;
    }

    public byte[] serialize(){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);

        try {
            dout.writeInt(registryNonce);
            for(HashSum hash: storage.keySet()){
                dout.write(hash.getArray());
                byte[] v = storage.get(hash).serialize();
                dout.writeInt(v.length);
                dout.write(v);
            }
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
