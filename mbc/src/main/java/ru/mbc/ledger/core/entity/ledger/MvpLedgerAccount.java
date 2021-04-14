package ru.mbc.ledger.core.entity.ledger;

import ru.mbc.ledger.core.error.LedgerAccountValueError;

import java.io.*;

public class MvpLedgerAccount {
    private Integer balance;
    private Integer nonce;

    public MvpLedgerAccount(Integer _nonce, Integer _balance){
        nonce = _nonce;
        balance = _balance;
    }

    public MvpLedgerAccount(){
        nonce = 0;
        balance = 0;
    }

    public MvpLedgerAccount(Integer _b){
        nonce = 0;
        balance = _b;
    }

    public MvpLedgerAccount(byte[] data){
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);
        try {
            nonce = din.readInt();
            balance = din.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MvpLedgerAccount(MvpLedgerAccount account){
        balance = account.balance;
        nonce = account.nonce;
    }

    public Integer getNonce(){
        return nonce;
    }

    public void incNonce(){nonce += 1;}

    public Integer getBalance(){
        return balance;
    }

    public void addValue(Integer value) throws LedgerAccountValueError {
        if(value < 0){
            throw new LedgerAccountValueError("Value must be greater than 0");
        }
        balance += value;
    }

    public void decValue(Integer value) throws LedgerAccountValueError {
        if(balance < value){
            throw new LedgerAccountValueError("Value must be less or equal to balance");
        }
        balance -= value;
    }

    public byte[] serialize(){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try {
            dout.writeInt(nonce);
            dout.writeInt(balance);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

}
