package ru.mbc.ledger.core.entity.block;

import ru.mbc.ledger.consensus.BeaconConsensusStructure;
import ru.mbc.ledger.core.entity.HashableEntity;
import ru.mbc.ledger.core.entity.SerializableEntity;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.error.structure.IncompleteObject;
import ru.mbc.ledger.util.HashSum;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Objects;

public class MvpBlock implements HashableEntity, SerializableEntity {
    HashSum prevBlockHash;
    HashSum stateHash;
    HashSum registryHash;
    Integer number;
    BeaconConsensusStructure consensus;
    Boolean genesis;

    ArrayList<HashSum> sTxList;
    ArrayList<HashSum> rTxList;

    public HashSum getPrevHash(){
        return prevBlockHash;
    }

    public HashSum getStateHash(){
        return stateHash;
    }

    public HashSum getRegistryHash(){return registryHash;}

    public Integer getSlot(){
        return number;
    }

    public BeaconConsensusStructure getConsensus(){
        return consensus;
    }

    public ArrayList<HashSum> getStateTxList(){
        return sTxList;
    }

    public ArrayList<HashSum> getRegistryTxList(){
        return rTxList;
    }

    public HashSum getRTxHash(){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (HashSum h : rTxList) {
                out.write(h.getArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(out.toByteArray());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new HashSum(hash);
    }

    public HashSum getSTxHash(){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (HashSum h : sTxList) {
                out.write(h.getArray());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(out.toByteArray());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new HashSum(hash);
    }

    public MvpBlock(){}

    public MvpBlock(HashSum parentHash, HashSum _stateHash, HashSum _reg, ArrayList<HashSum> sTxList, ArrayList<HashSum> rTxList){
        set(parentHash, _stateHash, _reg, 0, sTxList, rTxList, null, false);
    }

    public MvpBlock(HashSum parentHash, HashSum _stateHash, HashSum _reg, Integer _number, ArrayList<HashSum> sTxList, ArrayList<HashSum> rTxList, Boolean genesis){
        set(parentHash, _stateHash, _reg, _number, sTxList, rTxList, null, genesis);
    }

    public MvpBlock(HashSum parentHash, HashSum _stateHash, HashSum _reg, Integer _number, ArrayList<HashSum> sTxList, ArrayList<HashSum> rTxList){
        set(parentHash, _stateHash, _reg, _number, sTxList, rTxList, null, false);
    }

    public MvpBlock(HashSum _prevBlockHash, HashSum _stateHash, HashSum _reg, Integer _number, ArrayList<HashSum> sTxList, ArrayList<HashSum> rTxList, BeaconConsensusStructure _consensus){
        set(_prevBlockHash, _stateHash, _reg, _number, sTxList, rTxList, _consensus, false);
    }

    public MvpBlock(HashSum _prevBlockHash, HashSum _stateHash, HashSum _reg, Integer _number, byte[] sTxList, byte[] rTxList, BeaconConsensusStructure _consensus, Boolean genesis){
        ArrayList<HashSum> stateTxList = new ArrayList<>();
        ByteArrayInputStream s1 = new ByteArrayInputStream(sTxList);
        DataInputStream d1 = new DataInputStream(s1);
        int k;
        try {
            k = d1.readInt();
            for(int i = 0; i < k; i++){
                stateTxList.add(new HashSum(d1.readNBytes(32)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<HashSum> registryTxList = new ArrayList<>();
        ByteArrayInputStream s2 = new ByteArrayInputStream(rTxList);
        DataInputStream d2 = new DataInputStream(s2);
        try {
            k = d2.readInt();
            for(int i = 0; i < k; i++){
                registryTxList.add(new HashSum(d2.readNBytes(32)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        set(_prevBlockHash, _stateHash, _reg, _number, stateTxList, registryTxList, _consensus, genesis);
    }

    public void set(HashSum prevHash, HashSum stateHash, HashSum _reg, Integer _number, ArrayList<HashSum> sTxList, ArrayList<HashSum> rTxList, BeaconConsensusStructure _consensus, Boolean genesis){
        this.prevBlockHash = prevHash;
        this.stateHash = stateHash;
        this.registryHash = _reg;
        this.number = _number;
        this.consensus = _consensus;
        this.genesis = genesis;

        this.sTxList = Objects.requireNonNullElseGet(sTxList, ArrayList::new);
        this.rTxList = Objects.requireNonNullElseGet(rTxList, ArrayList::new);
    }

    public MvpBlock(byte[] data){
        set(data);
    }

    public void set(byte[] data){
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(in);
        try {
            prevBlockHash = new HashSum(din.readNBytes(32));
            stateHash = new HashSum(din.readNBytes(32));
            registryHash = new HashSum(din.readNBytes(32));
            int k = din.readInt();
            sTxList = new ArrayList<>();
            for(int i = 0; i < k; i++){
                sTxList.add(new HashSum(din.readNBytes(32)));
            }
            k = din.readInt();
            rTxList = new ArrayList<>();
            for(int i = 0; i < k; i++){
                rTxList.add(new HashSum(din.readNBytes(32)));
            }
            number = din.readInt();
            k = din.readInt();
            consensus = new BeaconConsensusStructure(din.readNBytes(k));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setConsensus(BeaconConsensusStructure c){
        consensus = c;
    }

    public byte[] serialize() throws IncompleteObject{
        if(consensus == null){
            throw new IncompleteObject("Consensus must be initialized before serialization");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        DataOutputStream dout = new DataOutputStream(outputStream);
        try {
            if(prevBlockHash != null)
                dout.write( prevBlockHash.getArray() );
            else{
                for(int i = 0; i < 32; i++)
                    dout.writeByte(0);
            }
            dout.write( stateHash.getArray() );
            dout.write( registryHash.getArray() );
            dout.writeInt(sTxList.size());
            for(HashSum h: sTxList)
                dout.write(h.getArray());
            dout.writeInt(rTxList.size());
            for(HashSum h: rTxList)
                dout.write(h.getArray());
            dout.writeInt( number );
            byte[] v = consensus.serialize();
            dout.writeInt(v.length);
            dout.write(v);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public byte[] serializeForHash() throws IncompleteObject{
        if(number > 0 && consensus == null){
            throw new IncompleteObject("Consensus must be initialized before serialization");
        }
        if(consensus == null){
            consensus = new BeaconConsensusStructure();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        DataOutputStream dout = new DataOutputStream(outputStream);
        try {
            if(prevBlockHash != null)
                dout.write( prevBlockHash.getArray() );
            else{
                for(int i = 0; i < 32; i++)
                    dout.writeByte(0);
            }
            dout.write( stateHash.getArray() );
            dout.write( registryHash.getArray() );
            dout.write( getSTxHash().getArray() );
            dout.write( getRTxHash().getArray() );
            byte[] v = consensus.serialize();
            dout.writeInt(v.length);
            dout.write(v);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public byte[] serializeForConsensus(){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        DataOutputStream dout = new DataOutputStream(outputStream);
        try {
            if(prevBlockHash != null)
                dout.write( prevBlockHash.getArray() );
            else{
                for(int i = 0; i < 32; i++)
                    dout.writeByte(0);
            }
            dout.write( stateHash.getArray() );
            dout.write( registryHash.getArray() );
            dout.write( getSTxHash().getArray() );
            dout.write( getRTxHash().getArray() );
            dout.writeInt( number );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public HashSum getHash() throws IncompleteObject {
        byte[] hash = null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(serializeForHash());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new HashSum(hash);
    }

    public HashSum getHashForConsensus(){
        byte[] hash = null;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(serializeForConsensus());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new HashSum(hash);
    }

}
