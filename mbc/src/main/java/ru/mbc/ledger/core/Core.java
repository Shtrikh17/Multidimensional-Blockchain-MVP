package ru.mbc.ledger.core;

import ru.mbc.ledger.api.ApiHandler;
import ru.mbc.ledger.api.ApiRestUi;
import ru.mbc.ledger.consensus.BeaconConsensusChainSelector;
import ru.mbc.ledger.consensus.BeaconConsensusSubprocess;
import ru.mbc.ledger.consensus.ConsensusBlockNotify;
import ru.mbc.ledger.core.logic.registryLogic;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;
import ru.mbc.ledger.core.logic.stateLogic;
import ru.mbc.ledger.core.entity.blockchain.MvpBlockchain;
import ru.mbc.ledger.network.MvpConfigNetwork;
import ru.mbc.ledger.util.Config;

public class Core implements Runnable {
    private Config config;

    private ledgerDbPostgre db;
    private stateLogic sLogic;
    private MvpBlockchain blockchain;
    private ApiHandler handler;
    private registryLogic rLogic;
    private BeaconConsensusSubprocess consensus;
    private MvpConfigNetwork network;

    public Core(Config c){
        config = c;

        db = new ledgerDbPostgre(config.general.nodeId);
        sLogic = new stateLogic();
        rLogic = new registryLogic();
        BeaconConsensusChainSelector chainSelector = new BeaconConsensusChainSelector(db);                      // To make it possible for API to query last block
        ConsensusBlockNotify consensusNotify = new ConsensusBlockNotify();                                      // Restart for consensus (update block)
        blockchain = new MvpBlockchain(db, sLogic, rLogic, chainSelector, consensusNotify, config);             // Blockchain
        network = new MvpConfigNetwork(config, blockchain, db);
        consensus = new BeaconConsensusSubprocess(consensusNotify, db, config, blockchain);                     // Thread for mining
        blockchain.setConsensus(consensus);                                                                     // fix dependency
        blockchain.setNetwork(network);
        handler = new ApiHandler(db, sLogic, chainSelector, network);                                                    // UI for observation & tx submission
    }

    @Override
    public void run() {
        ApiRestUi ui = new ApiRestUi(handler, config);
        Thread uiThread = new Thread(ui);
        uiThread.start();
        Thread networkThread = new Thread(network);

        Integer last_slot = 0;
        while(true){
            System.out.println("Starting bootstrap procedure");
            Integer slot = consensus.getCurrentSlot();
            if(slot == null || slot.equals(last_slot)){
                break;
            }
            blockchain.loadDatabase(last_slot + 1, slot);
            last_slot = slot;
        }
        System.out.println("Bootstrap complete");

        Thread consensusThread = new Thread(consensus);
        if(config.consensus.miningAllowed){
            consensus.register();
            consensusThread.start();
            networkThread.start();
        }

        try {
            uiThread.join();
            networkThread.join();
            consensusThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }







}