package ru.mbc.ledger.api;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.crypto.Sign;
import ru.mbc.ledger.core.entity.block.MvpBlock;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerAccount;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerDescriptor;
import ru.mbc.ledger.core.entity.registry.MvpRegistry;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTx;
import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.entity.state.MvpStateTxSignature;
import ru.mbc.ledger.core.entity.state.MvpStateTxType;
import ru.mbc.ledger.core.error.api.InvalidParameter;
import ru.mbc.ledger.util.HashSum;

import java.awt.image.Kernel;
import java.util.ArrayList;

public class ApiHelper {

    public static String serializeState(MvpState state){
        JSONObject jo = new JSONObject();
        for(HashSum address: state.getAccounts().keySet()){
            JSONObject account = new JSONObject();
            account.put("balance", state.getAccounts().get(address).getBalance());
            account.put("nonce", state.getAccounts().get(address).getNonce());
            jo.put(address.toString(), account);
        }
        return jo.toString();
    }

    public static String serializeStateTx(MvpStateTx tx){
        JSONObject jo = new JSONObject();
        jo.put("nonce", tx.getNonce());
        jo.put("from", tx.getFrom().toString());
        jo.put("to", tx.getTo().toString());
        jo.put("value", tx.getValue());
        switch(tx.getType()){
            case INTERNAL:
                jo.put("type", "internal");
                break;
            case EXTERNAL_IN:
                jo.put("type", "external incoming");
                break;
            case EXTERNAL_OUT:
                jo.put("type", "external outgoing");
                break;
        }
        JSONObject s = new JSONObject();
        s.put("v", Hex.encodeHexString(tx.getSignature().unpack().getV()));
        s.put("r", Hex.encodeHexString(tx.getSignature().unpack().getR()));
        s.put("s", Hex.encodeHexString(tx.getSignature().unpack().getS()));
        jo.put("signature", s);
        return jo.toString();
    }

    public static String serializeStateTxList(ArrayList<MvpStateTx> list){
        JSONArray ja = new JSONArray();
        for(MvpStateTx tx: list){
            ja.put(serializeStateTx(tx));
        }
        return ja.toString();
    }

    public static String serializeRegistryTx(MvpRegistryTx tx){
        JSONObject jo = new JSONObject();
        jo.put("nonce", tx.getNonce());
        JSONObject descriptor = new JSONObject();
        descriptor.put("alias", tx.getLedgerDescriptor().getAlias());
        jo.put("descriptor", descriptor);
        return jo.toString();
    }

    public static String serializeRegistryTxList(ArrayList<MvpRegistryTx> list){
        JSONArray ja = new JSONArray();
        for(MvpRegistryTx tx: list){
            ja.put(serializeRegistryTx(tx));
        }
        return ja.toString();
    }

    public static String serializeRegistry(MvpRegistry registry){
        JSONObject jo = new JSONObject();
        jo.put("nonce", registry.getRegistryNonce());
        for(HashSum address: registry.getStorage().keySet()){
            JSONObject record = new JSONObject();
            record.put("alias", registry.getStorage().get(address).getAlias());
            jo.put(address.toString(), record);
        }
        return jo.toString();
    }

    public static String serializeBlock(MvpBlock block){
        JSONObject jo = new JSONObject();
        jo.put("prevBlockHash", block.getPrevHash().toString());
        jo.put("stateHash", block.getStateHash().toString());
        jo.put("registryHash", block.getRegistryHash().toString());
        jo.put("sTxHash", block.getSTxHash().toString());
        jo.put("rTxHash", block.getRTxHash().toString());

        JSONObject consensus = new JSONObject();
        consensus.put("timestamp", block.getConsensus().getTimestamp());

        JSONObject signature = new JSONObject();
        signature.put("v", Hex.encodeHexString(block.getConsensus().unpack().getV()));
        signature.put("r", Hex.encodeHexString(block.getConsensus().unpack().getR()));
        signature.put("s", Hex.encodeHexString(block.getConsensus().unpack().getS()));
        consensus.put("signature", signature);

        consensus.put("owner", block.getConsensus().getOwner().toString());
        jo.put("consensus", consensus);
        return jo.toString();
    }

    public static String serializeBlockEx(MvpBlock block, MvpState state, MvpRegistry registry, ArrayList<MvpStateTx> sTx, ArrayList<MvpRegistryTx> rTx){
        JSONObject jo = new JSONObject();
        jo.put("prevBlockHash", block.getPrevHash().toString());

        JSONObject jState = new JSONObject();
        jState.put("hash", block.getStateHash().toString());
        JSONObject jStateValue = new JSONObject(serializeState(state));
        jState.put("contents", jStateValue);
        jo.put("state", jState);

        JSONObject jReg = new JSONObject();
        jReg.put("hash", block.getRegistryHash().toString());
        JSONObject jRegValue = new JSONObject(serializeRegistry(registry));
        jReg.put("contents", jRegValue);
        jo.put("registry", jReg);

        JSONArray jStx = new JSONArray();
        for(MvpStateTx tx: sTx){
            JSONObject jTx = new JSONObject(serializeStateTx(tx));
            jStx.put(jTx);
        }
        jo.put("sTx", jStx);

        JSONArray jRtx = new JSONArray();
        for(MvpRegistryTx tx: rTx){
            JSONObject jTx = new JSONObject(serializeRegistryTx(tx));
            jRtx.put(jTx);
        }
        jo.put("rTx", jRtx);

        JSONObject consensus = new JSONObject();
        consensus.put("timestamp", block.getConsensus().getTimestamp());

        JSONObject signature = new JSONObject();
        signature.put("v", Hex.encodeHexString(block.getConsensus().unpack().getV()));
        signature.put("r", Hex.encodeHexString(block.getConsensus().unpack().getR()));
        signature.put("s", Hex.encodeHexString(block.getConsensus().unpack().getS()));
        consensus.put("signature", signature);

        consensus.put("owner", block.getConsensus().getOwner().toString());
        jo.put("consensus", consensus);

        return jo.toString();
    }

    public static MvpStateTx makeStateTx(String json) throws InvalidParameter{
        JSONObject jo = new JSONObject(json);
        String from;
        String to;
        Integer nonce, value;
        byte[] sign;
        MvpStateTxType type;
        try {
            nonce = jo.getInt("nonce");
            from = jo.getString("from");
            to = jo.getString("to");
            value = jo.getInt("value");
            sign = Hex.decodeHex(jo.getString("signature"));
            switch (jo.getString("type")){
                case "external in":
                    type = MvpStateTxType.EXTERNAL_IN;
                    break;
                case "external out":
                    type = MvpStateTxType.EXTERNAL_OUT;
                    break;
                default:
                    type = MvpStateTxType.INTERNAL;
            }
        } catch (DecoderException e) {
            e.printStackTrace();
            throw new InvalidParameter("Wrong json");
        }

        return new MvpStateTx(nonce, from.toString(), to.toString(), value, new MvpStateTxSignature(sign), type);
    }

    public static MvpRegistryTx makeRegistryTx(String json) throws InvalidParameter{
        JSONObject jo = new JSONObject(json);
        Integer nonce;
        MvpLedgerDescriptor descriptor;
        nonce = jo.getInt("nonce");
        JSONObject s = jo.getJSONObject("descriptor");
        descriptor = new MvpLedgerDescriptor(s.getString("alias"));

        return new MvpRegistryTx(nonce, descriptor);
    }

}
