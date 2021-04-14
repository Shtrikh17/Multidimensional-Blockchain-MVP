package ru.mbc.ledger.core.security;

import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.entity.state.MvpStateTxSignature;
import ru.mbc.ledger.util.HashSum;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

public class secUtil {
    public static boolean checkTxSignature(MvpStateTx tx){
        MvpStateTxSignature signature = tx.getSignature();
        Sign.SignatureData sign = signature.unpack();

        byte[] msgHash = tx.getHashForSignature().getArray();
        Boolean result = false;
        try {
            BigInteger pubKeyRecovered = Sign.signedMessageHashToKey(msgHash, sign);
            HashSum recAddress = new HashSum(Hash.sha256(pubKeyRecovered.toByteArray()));
            if(recAddress.equals(new HashSum(tx.getFrom()))){
                result = true;
            }
        } catch (SignatureException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static HashSum pubKeyToAddress(BigInteger pubKey){
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(pubKey.toByteArray());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new HashSum(hash);
    }

}
