package ru.mbc.ledger.core.entity.ledger;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import ru.mbc.ledger.core.entity.state.MvpStateTxSignature;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.util.HashSum;

import java.math.BigInteger;
import java.security.*;
import java.sql.BatchUpdateException;

public class MvpLedgerUser {
    private HashSum account;
    private ECKeyPair keyPair;

    public MvpLedgerUser(){
        try {
            BigInteger privKey = Keys.createEcKeyPair().getPrivateKey();
            BigInteger pubKey = Sign.publicKeyFromPrivate(privKey);
            keyPair = new ECKeyPair(privKey, pubKey);
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        account = new HashSum(Hash.sha256(keyPair.getPublicKey().toByteArray()));
    }

    public MvpLedgerUser(HashSum address, byte[] privKey, byte[] pubKey){
        account = address;
        keyPair = new ECKeyPair(new BigInteger(privKey), new BigInteger(pubKey));
    }

    public byte[] getPrivKey(){
        return keyPair.getPrivateKey().toByteArray();
    }

    public byte[] getPubKey(){
        return keyPair.getPublicKey().toByteArray();
    }

    public MvpStateTxSignature signTX(MvpStateTx tx){
        return signTX(tx, true);
    }

    public MvpStateTxSignature signTX(MvpStateTx tx, boolean apply){
        byte[] msgHash = tx.getHashForSignature().getArray();
        Sign.SignatureData signature = Sign.signMessage(msgHash, keyPair, false);
        if(apply){
            tx.setSignature(signature);
        }
        return new MvpStateTxSignature(signature);
    }

    public Sign.SignatureData signRawData(byte[] data){
        byte[] msgHash = Hash.sha256(data);
        return Sign.signMessage(msgHash, keyPair, false);
    }

    public HashSum getAddress(){
        return account;
    }


}
