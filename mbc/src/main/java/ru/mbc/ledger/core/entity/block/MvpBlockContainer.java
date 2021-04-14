package ru.mbc.ledger.core.entity.block;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONException;
import org.json.JSONObject;
import ru.mbc.ledger.core.entity.SerializableEntity;
import ru.mbc.ledger.core.entity.registry.MvpRegistry;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTxContainer;
import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTxContainer;
import ru.mbc.ledger.core.error.api.InvalidParameter;

import java.io.*;

public class MvpBlockContainer implements SerializableEntity {
    public MvpBlock block;
    public MvpState state;
    public MvpRegistry registry;
    public MvpStateTxContainer sTx;
    public MvpRegistryTxContainer rTx;

    public MvpBlockContainer(){};
    public MvpBlockContainer(byte[] data){
        set(data);
    }

    public MvpBlockContainer(String json){
        JSONObject jo;
        try{
             jo = new JSONObject(json);
        }catch (JSONException e){
            throw new InvalidParameter("Invalid JSON");
        }
        try {
            block = new MvpBlock(Hex.decodeHex(jo.getString("block")));
            state = new MvpState(Hex.decodeHex(jo.getString("state")));
            registry = new MvpRegistry(Hex.decodeHex(jo.getString("registry")));
            sTx = new MvpStateTxContainer(Hex.decodeHex(jo.getString("sTx")));
            rTx = new MvpRegistryTxContainer(Hex.decodeHex(jo.getString("rTx")));
        } catch (DecoderException e) {
            e.printStackTrace();
        }
    }

    public String toJson(){
        JSONObject jo = new JSONObject();
        jo.put("block", Hex.encodeHexString(block.serialize()));
        jo.put("state", Hex.encodeHexString(state.serialize()));
        jo.put("registry", Hex.encodeHexString(registry.serialize()));
        jo.put("sTx", Hex.encodeHexString(sTx.serialize()));
        jo.put("rTx", Hex.encodeHexString(rTx.serialize()));
        return jo.toString();
    }

    public void set(byte[] data){
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);

        try {
            int k = din.readInt();
            block = new MvpBlock(din.readNBytes(k));
            k = din.readInt();
            state = new MvpState(din.readNBytes(k));
            k = din.readInt();
            registry = new MvpRegistry(din.readNBytes(k));
            k = din.readInt();
            sTx = new MvpStateTxContainer(din.readNBytes(k));
            k = din.readInt();
            rTx = new MvpRegistryTxContainer(din.readNBytes(k));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] serialize(){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(out);
        try{
            byte[] v = block.serialize();
            dout.writeInt(v.length);
            dout.write(v);
            v = state.serialize();
            dout.writeInt(v.length);
            dout.write(v);
            v = registry.serialize();
            dout.writeInt(v.length);
            dout.write(v);
            v = sTx.serialize();
            dout.writeInt(v.length);
            dout.write(v);
            v = rTx.serialize();
            dout.writeInt(v.length);
            dout.write(v);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }


}
