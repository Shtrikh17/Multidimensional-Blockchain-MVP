package ru.mbc.ledger.core.entity.state;

import ru.mbc.ledger.core.entity.HashableEntity;
import ru.mbc.ledger.core.entity.SerializableEntity;
import ru.mbc.ledger.core.entity.block.MvpBlockContainer;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;
import ru.mbc.ledger.util.HashSum;
import ru.mbc.ledger.util.transform;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class MvpStateTxContainer implements SerializableEntity, HashableEntity {
    private ArrayList<MvpStateTx> storage;

    public MvpStateTxContainer(ArrayList<MvpStateTx> _tx){
        storage = transform.deepCopy2(_tx);
    }

    public ArrayList<MvpStateTx> getStorage(){
        return storage;
    }

    public MvpStateTxContainer(byte[] data){
        set(data);
    }

    public ArrayList<HashSum> getHashList(){
        ArrayList<HashSum> result = new ArrayList<>();
        for(MvpStateTx tx: storage){
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
                MvpStateTx tx = new MvpStateTx(din.readNBytes(k));
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
            for(MvpStateTx tx: storage){
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
            for(MvpStateTx tx: storage){
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
        for(MvpStateTx tx: storage){
            db.txMarkAsIncluded(tx.getHash());
        }
    }

}
