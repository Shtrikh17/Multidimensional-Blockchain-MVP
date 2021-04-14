package ru.mbc.ledger.core.entity.state;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.web3j.crypto.Hash;
import ru.mbc.ledger.core.entity.HashableEntity;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerAccount;
import ru.mbc.ledger.core.entity.SerializableEntity;
import ru.mbc.ledger.core.error.LedgerAccountValueError;
import ru.mbc.ledger.core.error.tx.BalanceError;
import ru.mbc.ledger.core.error.tx.NonceError;
import ru.mbc.ledger.core.error.tx.SignatureError;
import ru.mbc.ledger.util.Config;
import ru.mbc.ledger.util.HashSum;
import ru.mbc.ledger.util.transform;
import ru.mbc.ledger.core.security.secUtil;

public class MvpState implements SerializableEntity, HashableEntity {
    private Hashtable<HashSum, MvpLedgerAccount> storage;
    private ArrayList<HashSum> superusers;

    public MvpState(){
        storage = new Hashtable<>();
    }

    public MvpState(MvpState oldState){
        storage = transform.deepCopy(oldState.storage);
        superusers = transform.deepCopy2(oldState.superusers);
    }

    public MvpState(Hashtable<HashSum, MvpLedgerAccount> _balances){
        storage = transform.deepCopy2(_balances);
    }

    public MvpState(Hashtable<HashSum, MvpLedgerAccount> _balances, ArrayList<HashSum> _superusers){
        storage = transform.deepCopy2(_balances);
        superusers = transform.deepCopy2(_superusers);
    }

    public MvpState(byte[] data){
        set(data);
    }

    public void set(Hashtable<HashSum, MvpLedgerAccount> _balances){
        this.storage = transform.deepCopy2(_balances);
    }

    public void set(byte[] data){
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);
        this.storage = new Hashtable<>();
        this.superusers = new ArrayList<>();

        try {
            int N = din.readInt();
            for(int i = 0; i < N; i++){
                HashSum hash = new HashSum(din.readNBytes(32));   // TODO: set define, currently - hardcode for sha-256
                Integer k = din.readInt();
                byte[] res = din.readNBytes(k);
                this.storage.put(hash, new MvpLedgerAccount(res));
            }
            N = din.readInt();
            for(int i = 0; i < N; i++){
                HashSum hash = new HashSum(din.readNBytes(32));
                this.superusers.add(hash);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Hashtable<HashSum, MvpLedgerAccount> getAccounts() {
        return storage;
    }

    public void setInitialState(Hashtable<HashSum, MvpLedgerAccount> _storage, ArrayList<HashSum> _superusers){
        storage = _storage;
        superusers = _superusers;
    }


    public boolean checkTX(MvpStateTx tx) throws LedgerAccountValueError, SignatureError, NonceError, BalanceError {
        HashSum fromAddress = new HashSum(tx.getFrom());
        // From address exists
        if(!storage.containsKey(fromAddress)){
            throw new LedgerAccountValueError("No from address in the ledger");
        }

        // Check signature
        if(!secUtil.checkTxSignature(tx)){
            throw new SignatureError("Invalid signature");
        }

        if(storage.get(fromAddress).getNonce() + 1 != tx.getNonce()){
            throw new NonceError("Incorrect nonce value");
        }

        // Balance is enough to perform operation or user is superuser
        if(!superusers.contains(fromAddress)){
            if(storage.get(fromAddress).getBalance() < tx.getValue()){
                throw new BalanceError("From account balance less than value transmitted");
            }
        }

        return true;
    }

    public boolean verifyTX(MvpStateTx _tx) throws LedgerAccountValueError, SignatureError, NonceError, BalanceError{
        return checkTX(_tx);
    }

    public boolean applyTX(MvpStateTx _tx) throws LedgerAccountValueError, SignatureError, NonceError, BalanceError{
        if(!checkTX(_tx)){
            // Unreachable if errors operate correctly
            return false;
        }
        synchronized (storage){
            MvpLedgerAccount account1;
            MvpLedgerAccount account2;

            if(_tx.getRawType() == 1 || _tx.getRawType() == 0){      // EXTERNAL OUT or INTERNAL
                account1 = storage.get(new HashSum(_tx.getFrom()));
                account1.decValue(_tx.getValue());
                account1.incNonce();
                storage.put(new HashSum(_tx.getFrom()), account1);
            }

            if(_tx.getRawType() == 0 || _tx.getRawType() == 2){      // INTERNAL or EXTERNAL IN
                if(storage.containsKey(new HashSum(_tx.getTo()))) {
                    account2 = storage.get(new HashSum(_tx.getTo()));
                }
                else {
                    account2 = new MvpLedgerAccount();
                }
                account2.addValue(_tx.getValue());
                storage.put(new HashSum(_tx.getTo()), account2);
            }

        }
        return true;
    }

    public byte[] serialize(){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try {
            dout.writeInt(storage.size());
            for(HashSum hash: storage.keySet()){
                    byte[] data = storage.get(hash).serialize();
                    dout.write(hash.getArray());
                    dout.writeInt(data.length);
                    dout.write(data);
            }
            dout.writeInt(superusers.size());
            for(HashSum hash: superusers){
                dout.write(hash.getArray());
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

    public boolean equals(MvpState testState){
        for(HashSum k: storage.keySet()){
            if(storage.get(k).getBalance() != testState.storage.get(k).getBalance())
                return false;
        }
        return true;
    }
}