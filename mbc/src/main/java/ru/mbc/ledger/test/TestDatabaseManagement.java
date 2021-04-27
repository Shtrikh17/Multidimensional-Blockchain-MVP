package ru.mbc.ledger.test;

import ru.mbc.ledger.consensus.BeaconConsensusStructure;
import ru.mbc.ledger.consensus.BeaconConsensusSubprocess;
import ru.mbc.ledger.core.entity.block.MvpBlock;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerAccount;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerDescriptor;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerUser;
import ru.mbc.ledger.core.entity.registry.MvpRegistry;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTx;
import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;
import ru.mbc.ledger.util.HashSum;

import java.util.Hashtable;
import java.util.LinkedHashMap;

public class TestDatabaseManagement {

    public static void main(String[] args){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        // Create users
        MvpLedgerUser user1 = new MvpLedgerUser();
        MvpLedgerUser user2 = new MvpLedgerUser();
        MvpLedgerAccount account1 = new MvpLedgerAccount(100);
        MvpLedgerAccount account2 = new MvpLedgerAccount(50);

        // Init state
        LinkedHashMap<HashSum, MvpLedgerAccount> balances = new LinkedHashMap<>();
        balances.put(user1.getAddress(), account1);
        balances.put(user2.getAddress(), account2);
        MvpState s1 = new MvpState(balances);

        // Create signed transaction
        MvpStateTx tx = new MvpStateTx(1, user1.getAddress().toString(), user2.getAddress().toString(), 50);
        user1.signTX(tx);
        db.addTransaction(tx);
        System.out.println("State transaction hash sum: " + tx.getHash().toString());

        // Create registry transaction
        MvpRegistryTx rtx = new MvpRegistryTx(1, new MvpLedgerDescriptor("test"));
        db.addRegistryTx(rtx);
        System.out.println("Registry transaction hash sum: " + tx.getHash().toString());

        // Apply tx and store new state
        s1.applyTX(tx);
        db.addState(s1);
        System.out.println("State hash sum: " + s1.getHash().toString());

        // Apply rtx and store new registry
        MvpRegistry registry = new MvpRegistry();
        registry.applyTX(rtx);
        db.addRegistry(registry);
        System.out.println("Registry hash: " + registry.getHash().toString());

        // Create new block
        MvpBlock block = new MvpBlock(null, s1.getHash(), registry.getHash(), null, null);
//        BeaconConsensusSubprocess consensus = new BeaconConsensusSubprocess();
//        BeaconConsensusStructure cons = consensus.findBlockConsensus(block, user1);
//        block.setConsensus(cons);


        db.addBlock(block);
        System.out.println("Block hash: " + block.getHash().toString());

    }

    public static void drop(String[] args){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.dropLedger();
    }

}
