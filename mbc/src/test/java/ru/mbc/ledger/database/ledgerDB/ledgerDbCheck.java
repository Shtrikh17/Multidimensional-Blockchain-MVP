package ru.mbc.ledger.database.ledgerDB;
import org.junit.jupiter.api.Test;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerDescriptor;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTx;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerUser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ledgerDbCheck {
    @Test
    public void checkDatabase(){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();
        db.dropLedger();
    }

    @Test
    public void checkStateTxNonce(){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        MvpLedgerUser user1 = new MvpLedgerUser();
        MvpLedgerUser user2 = new MvpLedgerUser();
        MvpStateTx tx = new MvpStateTx(0, user1.getAddress().toString(), user2.getAddress().toString(), 10);
        user1.signTX(tx);
        db.addTransaction(tx);

        assertEquals(2, db.getUserNonce(user1.getAddress()));
        assertEquals(1, db.getUserNonce(user2.getAddress()));

        db.dropLedger();
    }

    @Test
    public void checkRegistryTxNonce(){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        assertEquals(1, db.getLedgerNonce());
        MvpRegistryTx tx = new MvpRegistryTx();
        db.addRegistryTx(tx);
        assertEquals(2, db.getLedgerNonce());
        MvpLedgerDescriptor descriptor = new MvpLedgerDescriptor("ledger 1");
        tx = new MvpRegistryTx(2, descriptor,true);
        db.addRegistryTx(tx);
        assertEquals(2, db.getLedgerNonce());

        db.dropLedger();
    }

}
