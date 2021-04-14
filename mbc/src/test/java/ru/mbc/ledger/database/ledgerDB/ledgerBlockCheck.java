package ru.mbc.ledger.database.ledgerDB;

import org.junit.jupiter.api.Test;
import ru.mbc.ledger.consensus.BeaconConsensusStructure;
import ru.mbc.ledger.core.entity.block.MvpBlock;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerAccount;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerDescriptor;
import ru.mbc.ledger.core.entity.registry.MvpRegistry;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTx;
import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerUser;
import ru.mbc.ledger.util.HashSum;
import ru.mbc.ledger.util.helper;

import java.util.ArrayList;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class ledgerBlockCheck {
    @Test
    public void checkBlock(){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        MvpLedgerDescriptor descriptor1 = new MvpLedgerDescriptor("ledger 1");

        MvpLedgerUser user1 = new MvpLedgerUser();
        MvpLedgerUser user2 = new MvpLedgerUser();
        MvpLedgerAccount account1 = new MvpLedgerAccount(100);
        MvpLedgerAccount account2 = new MvpLedgerAccount(100);
        Hashtable<HashSum, MvpLedgerAccount> balances = new Hashtable<>();
        balances.put(user1.getAddress(), account1);
        balances.put(user2.getAddress(), account2);

        MvpState state = new MvpState(balances);
        MvpRegistry registry = new MvpRegistry();
        HashSum sTx1 = helper.generateHash();
        HashSum rTx1 = helper.generateHash();
        BeaconConsensusStructure c1 = new BeaconConsensusStructure(null, 0L, user1.getAddress());
        MvpBlock block1 = new MvpBlock(null, state.getHash(), registry.getHash(), 0, new ArrayList<HashSum>(), new ArrayList<HashSum>(), c1);


        HashSum stateHash1 = state.getHash();
        HashSum registryHash1 = registry.getHash();
        db.addState(state);
        db.addRegistry(registry);
        db.addBlock(block1);

        MvpStateTx tx = new MvpStateTx(1, user1.getAddress().toString(), user2.getAddress().toString(), 10);
        user1.signTX(tx);
        state.applyTX(tx);

        HashSum stateHash2 = state.getHash();
        db.addTransaction(tx);
        db.addState(state);

        MvpBlock block2 = new MvpBlock(block1.getHash(), state.getHash(), registry.getHash(), 1, new ArrayList<HashSum>(), new ArrayList<HashSum>(), c1);
        db.addBlock(block2);

        MvpLedgerDescriptor descriptor = new MvpLedgerDescriptor("child ledger");
        MvpRegistryTx regTX = new MvpRegistryTx(1, descriptor);
        registry.applyTX(regTX);

        HashSum registryHash2 = registry.getHash();
        db.addRegistryTx(regTX);
        db.addRegistry(registry);

        MvpBlock block3 = new MvpBlock(block2.getHash(), state.getHash(), registry.getHash(), 2, new ArrayList<HashSum>(), new ArrayList<HashSum>(), c1);
        db.addBlock(block3);

        MvpBlock b1 = db.getBlock(block1.getHash());
        MvpBlock b2 = db.getBlock(block2.getHash());
        MvpBlock b3 = db.getBlock(block3.getHash());

        assertEquals(b1.getHash(), b2.getPrevHash());
        assertEquals(b2.getHash(), b3.getPrevHash());
        assertEquals(b2.getStateHash(), b3.getStateHash());
        assertEquals(b1.getRegistryHash(), b2.getRegistryHash());

        db.deleteBlock(block1.getHash());
        db.deleteBlock(block2.getHash());
        db.deleteBlock(block3.getHash());
        db.dropLedger();
    }
}