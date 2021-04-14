package ru.mbc.ledger.database.ledgerDB;

import org.junit.jupiter.api.Test;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerDescriptor;
import ru.mbc.ledger.core.entity.registry.MvpRegistry;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTx;
import ru.mbc.ledger.util.HashSum;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ledgerRegistryCheck {
    @Test
    public void checkRegistrySave(){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        String alias1 = "ledger 1";
        Hashtable<HashSum, MvpLedgerDescriptor> storage = new Hashtable<>();
        MvpLedgerDescriptor descriptor = new MvpLedgerDescriptor(alias1);
        storage.put(descriptor.getHash(), descriptor);
        MvpRegistry registry1 = new MvpRegistry(storage);
        db.addRegistry(registry1);
        MvpRegistry registry2 = db.getRegistry(registry1.getHash());

        assertEquals(registry1.getStorage().size(), registry2.getStorage().size());

        for(HashSum k: registry1.getStorage().keySet()){
            assertEquals(registry1.getStorage().get(k).getAlias(), registry2.getStorage().get(k).getAlias());
        }

        db.deleteRegistry(registry1.getHash());
        db.dropLedger();
    }

    @Test
    public void checkRegistryApplyTX(){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        String alias1 = "ledger 1";
        String alias2 = "ledger 2";
        MvpLedgerDescriptor descriptor1 = new MvpLedgerDescriptor(alias1);
        Hashtable<HashSum, MvpLedgerDescriptor> storage = new Hashtable<>();
        storage.put(descriptor1.getHash(), descriptor1);
        MvpRegistry registry1 = new MvpRegistry(storage);
        db.addRegistry(registry1);

        MvpLedgerDescriptor descriptor2 = new MvpLedgerDescriptor(alias2);
        MvpRegistryTx tx1 = new MvpRegistryTx(1, descriptor2);
        registry1.applyTX(tx1);
        db.addRegistryTx(tx1);
        db.addRegistry(registry1);

        MvpRegistry registry2 = db.getRegistry(registry1.getHash());

        assertEquals(registry1.getStorage().size(), registry2.getStorage().size());

        for(HashSum k: registry1.getStorage().keySet()){
            assertEquals(registry1.getStorage().get(k).getAlias(), registry2.getStorage().get(k).getAlias());
        }

        db.dropLedger();
    }

    @Test
    public void checkLedgerTransactions(){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        String alias1 = "ledger 1";
        MvpLedgerDescriptor descriptor1 = new MvpLedgerDescriptor(alias1);
        MvpRegistryTx tx1 = new MvpRegistryTx(0, descriptor1);
        db.addRegistryTx(tx1);
        MvpRegistryTx tx2 = db.getRegistryTransaction(tx1.getHash());
        db.deleteRegistryTx(tx1.getHash());

        assertEquals(tx1.getLedgerDescriptor().getAlias(), tx2.getLedgerDescriptor().getAlias());

        db.dropLedger();
    }
}
