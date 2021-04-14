package ru.mbc.ledger.core.entity.registry;

import ru.mbc.ledger.core.entity.HashableEntity;
import ru.mbc.ledger.core.entity.SerializableEntity;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;
import ru.mbc.ledger.util.HashSum;
import ru.mbc.ledger.util.transform;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class MvpRegistryTxContainer implements SerializableEntity, HashableEntity {
    private ArrayList<MvpRegistryTx> storage;

    public ArrayList<MvpRegistryTx> getStorage(){
        return storage;
    }

    public MvpRegistryTxContainer(ArrayList<MvpRegistryTx> _tx){
        storage = transform.deepCopy2(_tx);
    }

    public MvpRegistryTxContainer(byte[] data){
        set(data);
    }

    public ArrayList<HashSum> getHashList(){
        ArrayList<HashSum> result = new ArrayList<>();
        for(MvpRegistryTx tx: storage){
            result.add(tx.getHash());
        }
        return result;
    }

    public void set(byte[] data) {
        storage = new ArrayList<>();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);

        try {
            while (din.available() > 0) {
                int k = din.readInt();
                MvpRegistryTx tx = new MvpRegistryTx(din.readNBytes(k));
                storage.add(tx);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] serialize() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try {
            for(MvpRegistryTx tx: storage){
                byte[] v = tx.serialize();
                dout.writeInt(v.length);
                dout.write(v);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    public HashSum getHash(){
        byte[] hash = null;

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            for(MvpRegistryTx tx: storage){
                out.write(tx.getHash().getArray());
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(out.toByteArray());

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new HashSum(hash);
    }

    public void markAsIncluded(ledgerDbPostgre db){
        for(MvpRegistryTx tx: storage){
            db.markRegistryTxIncluded(tx.getHash());
        }
    }
}
