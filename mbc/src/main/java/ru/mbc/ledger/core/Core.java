package ru.mbc.ledger.core;

import ru.mbc.ledger.api.ApiHandler;
import ru.mbc.ledger.api.ApiRestUi;
import ru.mbc.ledger.consensus.BeaconConsensusChainSelector;
import ru.mbc.ledger.consensus.BeaconConsensusSubprocess;
import ru.mbc.ledger.consensus.ConsensusBlockNotify;
import ru.mbc.ledger.core.logic.mbcLogic;
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
    private mbcLogic mbc;

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
        mbc = new mbcLogic("127.0.0.1", config.general.mbcVerifierPort, config.general.mbcModulePort, db);
        handler = new ApiHandler(db, sLogic, chainSelector, network, mbc);                                                    // UI for observation & tx submission
    }

    @Override
    public void run() {
        ApiRestUi ui = new ApiRestUi(handler, config);
        Thread uiThread = new Thread(ui);
        uiThread.start();
        Thread networkThread = new Thread(network);
        Thread mbcThread = new Thread(mbc);

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
        }
        networkThread.start();
        mbcThread.start();

        try {
            uiThread.join();
            networkThread.join();
            mbcThread.join();
            consensusThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }







}
