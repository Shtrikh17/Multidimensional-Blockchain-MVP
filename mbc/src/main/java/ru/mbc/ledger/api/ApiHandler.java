package ru.mbc.ledger.api;

import ru.mbc.ledger.consensus.BeaconConsensusChainSelector;
import ru.mbc.ledger.core.entity.block.MvpBlock;
import ru.mbc.ledger.core.entity.registry.MvpRegistry;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTx;
import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.error.api.InvalidParameter;
import ru.mbc.ledger.core.error.db.NoSuchEntity;
import ru.mbc.ledger.core.logic.stateLogic;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;
import ru.mbc.ledger.network.MvpConfigNetwork;
import ru.mbc.ledger.util.HashSum;

import java.util.ArrayList;

import static ru.mbc.ledger.api.ApiHelper.*;

public class ApiHandler {
    private ledgerDbPostgre db;
    private stateLogic logic;
    private BeaconConsensusChainSelector chainSelector;
    private MvpConfigNetwork network;

    public ApiHandler(ledgerDbPostgre _db, stateLogic _logic, BeaconConsensusChainSelector cs, MvpConfigNetwork nwk){
        db = _db;
        logic = _logic;
        chainSelector = cs;
        network = nwk;
    }

    public String getLastState(){
        String result;
        try{
            MvpState state = db.getState();
            result = serializeState(state);
        }
        catch(NoSuchEntity exception){
            result = null;
        }
        return result;
    }

    public String getState(String hash){
        String result;
        try{
            MvpState state = db.getState(new HashSum(hash));
            result = serializeState(state);
        }
        catch(NoSuchEntity | InvalidParameter e){
            result = null;
        }
        return result;
    }

    public String getStateTx(String hash){
        String result;
        try{
            MvpStateTx tx = db.getTransaction(new HashSum(hash));
            result = serializeStateTx(tx);
        }
        catch(NoSuchEntity | InvalidParameter e){
            result = null;
        }
        return result;
    }

    public String getPendingStateTx(){
        ArrayList<MvpStateTx> list = db.getPendingStateTx();
        return serializeStateTxList(list);
    }

    public String getPendingRegistryTx(){
        ArrayList<MvpRegistryTx> list = db.getPendingRegistryTx();
        return serializeRegistryTxList(list);
    }

    public String getRegistryTx(String hash){
        String result;
        try{
            MvpRegistryTx tx = db.getRegistryTransaction(new HashSum(hash));
            result = serializeRegistryTx(tx);
        }
        catch(NoSuchEntity | InvalidParameter e){
            result = null;
        }
        return result;
    }

    public String getRegistry(String hash){
        String result;
        try{
            MvpRegistry registry = db.getRegistry(new HashSum(hash));
            result = serializeRegistry(registry);
        }
        catch(NoSuchEntity | InvalidParameter e){
            result = null;
        }
        return result;
    }

    public String getLastRegistry(){
        String result;
        try{
            MvpRegistry registry = db.getRegistry();
            result = serializeRegistry(registry);
        }
        catch(NoSuchEntity exception){
            result = null;
        }
        return result;
    }

    public String getBlock(String hash){
        String result;
        try{
            MvpBlock block = db.getBlock(new HashSum(hash));
            result = serializeBlock(block);
        }
        catch(NoSuchEntity exception){
            result = null;
        }
        return result;
    }

    public String getExBlock(String hash){
        String result;
        try{
            MvpBlock block = db.getBlock(new HashSum(hash));
            MvpState state = db.getState(block.getStateHash());
            MvpRegistry registry = db.getRegistry(block.getRegistryHash());
            ArrayList<MvpStateTx> sTx = db.getTransactions(block.getStateTxList());
            ArrayList<MvpRegistryTx> rTx = db.getRegistryTransactions(block.getRegistryTxList());

            result = serializeBlockEx(block, state, registry, sTx, rTx);
        }
        catch(NoSuchEntity exception){
            result = null;
        }
        return result;
    }

    public String getLastBlock(){
        String result;
        HashSum hash = chainSelector.getCurrentTopBlockHash();
        try{
            MvpBlock block = db.getBlock(hash);
            result = serializeBlock(block);
        }
        catch(NoSuchEntity exception){
            result = null;
        }
        return result;
    }

    public String getLastExBlock(){
        String result;
        HashSum hash = chainSelector.getCurrentTopBlockHash();
        try{
            MvpBlock block = db.getBlock(hash);
            MvpState state = db.getState(block.getStateHash());
            MvpRegistry registry = db.getRegistry(block.getRegistryHash());
            ArrayList<MvpStateTx> sTx = db.getTransactions(block.getStateTxList());
            ArrayList<MvpRegistryTx> rTx = db.getRegistryTransactions(block.getRegistryTxList());

            result = serializeBlockEx(block, state, registry, sTx, rTx);
        }
        catch(NoSuchEntity exception){
            result = null;
        }
        return result;
    }

    public void addStateTx(String json){
        try{
            MvpStateTx tx = makeStateTx(json);
            // TODO: check state tx validity
            db.addTransaction(tx);
            network.broadcast_state_tx(tx);
        }
        catch (InvalidParameter e){}
    }

    public void addRegistryTx(String json){
        MvpRegistryTx tx = makeRegistryTx(json);
        db.addRegistryTx(tx);
        network.broadcast_registry_tx(tx);
    }



}
