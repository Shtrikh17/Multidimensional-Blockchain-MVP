package ru.mbc.ledger.core.entity.blockchain;

import ru.mbc.ledger.consensus.BeaconConsensusChainSelector;
import ru.mbc.ledger.consensus.BeaconConsensusSubprocess;
import ru.mbc.ledger.consensus.ConsensusBlockNotify;
import ru.mbc.ledger.core.entity.block.MvpBlock;
import ru.mbc.ledger.core.entity.block.MvpBlockContainer;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerAccount;
import ru.mbc.ledger.core.entity.registry.MvpRegistry;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTx;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTxContainer;
import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.entity.state.MvpStateTxContainer;
import ru.mbc.ledger.core.logic.registryLogic;
import ru.mbc.ledger.core.logic.stateLogic;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;
import ru.mbc.ledger.network.MvpConfigNetwork;
import ru.mbc.ledger.util.Account;
import ru.mbc.ledger.util.Config;
import ru.mbc.ledger.util.HashSum;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

public class MvpBlockchain {
    private ledgerDbPostgre db;
    private stateLogic sLogic;
    private registryLogic rLogic;
    private final ReentrantLock lock = new ReentrantLock();
    private BeaconConsensusChainSelector chainSelector;
    private ConsensusBlockNotify consensusNotify;
    private BeaconConsensusSubprocess consensus;
    private Config config;
    private MvpConfigNetwork network;

    private MvpBlockContainer currentBlockContainer;
    // TODO: bootstrap - blockchain must load and check the sequence
    // TODO: bootstrap constructor

    public MvpBlockchain(ledgerDbPostgre _db, stateLogic sLogic, registryLogic rLogic, BeaconConsensusChainSelector selector, ConsensusBlockNotify cn, Config c){
        db = _db;
        this.sLogic = sLogic;
        this.rLogic = rLogic;
        chainSelector = selector;
        consensusNotify = cn;
        config = c;

        if(c.general.genesis){
            db.dropLedger();
            db.initLedger();
            Hashtable<HashSum, MvpLedgerAccount> accounts = new Hashtable<>();
            ArrayList<HashSum> superusers = new ArrayList<>();
            if(c.state.accounts.size() > 0){
                for(Account account: c.state.accounts){
                    accounts.put(account.address, new MvpLedgerAccount(0, account.value));
                }
            }
            if(c.state.superusers.size() > 0){
                for(HashSum h: c.state.superusers){
                    superusers.add(h);
                }
            }
            MvpState firstState = sLogic.createGenesisState(accounts, superusers);
            MvpRegistry firstRegistry = rLogic.createGenesisRegistry();
            db.addState(firstState);
            db.addRegistry(firstRegistry);
            MvpBlock block = new MvpBlock(new HashSum(), firstState.getHash(), firstRegistry.getHash(),0, new ArrayList<>(), new ArrayList<>());
            chainSelector.notifyGenesisBlock(block);
            db.addBlock(block);
            consensusNotify.setMarker();
        }
    }

    public void loadBlock(Integer slot){
        MvpBlockContainer blockContainer = network.obtain_block(slot);
        if(blockContainer != null)
            handleNewRemoteBlock(blockContainer);
    }

    public void loadDatabase(Integer last_slot, Integer max_slot){
        for(Integer i = last_slot; i <= max_slot; i++){
            loadBlock(i);
        }
    }

    public void setConsensus(BeaconConsensusSubprocess con){
        consensus = con;
    }

    public void setNetwork(MvpConfigNetwork nwk){
        network = nwk;
    }

    public void handleNewRemoteBlock(MvpBlockContainer container){
        // Checks if block is correct for inclusion into database - i.e. handleNewRemoteBlock
        // 0. check if not already present
        if(db.checkIfBlockPresent(container.block.getHash()))
            return;

        // 1. check block hash sums
        if(!container.block.getStateHash().equals(container.state.getHash()))
            return;
        if(!container.block.getRegistryHash().equals(container.registry.getHash()))
            return;
        if(!container.block.getSTxHash().equals(container.sTx.getHash()))
            return;
        if(!container.block.getRTxHash().equals(container.rTx.getHash()))
            return;

        // 2. check block consensus + timestamp
        if(!consensus.checkBlockConsensusValidity(container.block))
            return;

        // 2.1. check if it was the node's turn
        if(!consensus.checkSlotOrder(container.block.getConsensus().getOwner(), container.block.getSlot())){
            return;
        };

        MvpBlock parentBlock = db.getBlock(container.block.getPrevHash());
        if(parentBlock == null)
            return;
        // 3. check state apply
        ArrayList<HashSum> txListReturn = new ArrayList<>();
        ArrayList<HashSum> txListError = new ArrayList<>();
        ArrayList<MvpStateTx> txAdded = new ArrayList<>();
        ArrayList<MvpStateTx> txSet = container.sTx.getStorage();
        MvpState prevState = db.getState(parentBlock.getStateHash());

        MvpState testState = sLogic.buildState(prevState, txSet, txAdded, txListReturn, txListError);
        if(txListError.size() > 0 || txListReturn.size() > 0)
            return;

        // 4. check that result state hash equals hash in the block
        if(!testState.getHash().equals(container.block.getStateHash()))
            return;

        // 5. check registry apply
        ArrayList<HashSum> regTxListError = new ArrayList<>();
        ArrayList<MvpRegistryTx> regTxAdded = new ArrayList<>();
        ArrayList<MvpRegistryTx> regTxSet = container.rTx.getStorage();
        MvpRegistry prevRegistry = db.getRegistry(parentBlock.getRegistryHash());

        MvpRegistry testRegistry = rLogic.buildState(prevRegistry, regTxSet, regTxAdded, regTxListError);
        if(regTxListError.size() > 0)
            return;

        // 6. check that result registry hash equals hash in the block
        if(!testRegistry.getHash().equals(container.block.getRegistryHash()))
            return;

        lock.lock();
        try{
            // 7. save stx (if not present)
            // 8. save rtx (if not present)
            db.addMultipleStateTx(container.sTx.getStorage());
            db.addMultipleRegistryTx(container.rTx.getStorage());

            // 9. save state
            db.addState(container.state);

            // 10. save registry
            db.addRegistry(container.registry);

            // 11. save block
            db.addBlock(container.block);

            // 12. notify chain selector
            chainSelector.notifyNewBlock(container.block);

            // 13. notify consensus
            consensusNotify.setMarker();

            // 14. Fix counters
            db.modifyVerifyCounters();
        }
        finally{
            lock.unlock();
        }
    }

    public void handleNewLocalBlock(MvpBlockContainer container){
        // Handles local block after consensus
        // check block - not necessary, created it myself
        // check state - not necessary, created it myself
        // store transactions - not necessary, created the state myself from local transactions
        // mark transactions as included
        // store block and state
        // notify chain selection (consensus) - direct call
        // notify consensus - new block - use NewBlockNotify

        lock.lock();
        try{
            container.sTx.markAsIncluded(db);
            container.rTx.markAsIncluded(db);
            db.addState(container.state);
            db.addRegistry(container.registry);
            db.addBlock(container.block);
            chainSelector.notifyNewBlock(container.block);
            consensusNotify.setMarker();
            network.broadcast_new_block(container);
            db.modifyVerifyCounters();

        }finally {
            lock.unlock();
        }
    }

    public MvpBlockContainer prepareNewBlock(Integer slot){
        // run chain selector
        currentBlockContainer = new MvpBlockContainer();                                    // Create container
        MvpBlock parentBlock = chainSelector.getCurrentTopBlock();                    // Get parent block
        MvpState state = db.getState(parentBlock.getStateHash());                     // Restore new state
        MvpRegistry reg = db.getRegistry(parentBlock.getRegistryHash());              // Restore registry

        // Perform state transition
        ArrayList<HashSum> txListReturn = new ArrayList<>();
        ArrayList<HashSum> txListError = new ArrayList<>();
        ArrayList<MvpStateTx> txAdded = new ArrayList<>();
        ArrayList<MvpStateTx> txSet = db.obtainTx();
        currentBlockContainer.state = sLogic.buildState(state, txSet, txAdded, txListReturn, txListError);
        currentBlockContainer.sTx = new MvpStateTxContainer(txAdded);

        // Temporary: remove error tx
        for(HashSum tx: txListError){
            db.deleteTransaction(tx);
        }
        for(HashSum tx: txListReturn){
            db.deleteTransaction(tx);
        }

        // Perform registry transition
        ArrayList<HashSum> regTxListError = new ArrayList<>();
        ArrayList<MvpRegistryTx> regTxAdded = new ArrayList<>();
        ArrayList<MvpRegistryTx> regTxSet = db.obtainRegistryTx();
        currentBlockContainer.registry = rLogic.buildState(reg, regTxSet, regTxAdded, regTxListError);
        currentBlockContainer.rTx = new MvpRegistryTxContainer(regTxAdded);
        for(HashSum tx: regTxListError){
            db.deleteRegistryTx(tx);
        }

        // Create block
        currentBlockContainer.block = new MvpBlock(
                parentBlock.getHash(),
                currentBlockContainer.state.getHash(),
                currentBlockContainer.registry.getHash(),
                slot,
                currentBlockContainer.sTx.getHashList(),
                currentBlockContainer.rTx.getHashList()
        );

        return currentBlockContainer;
    }



}
