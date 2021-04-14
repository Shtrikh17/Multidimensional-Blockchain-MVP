package ru.mbc.ledger.database.ledgerDB;

import org.junit.jupiter.api.Test;
import ru.mbc.ledger.core.entity.state.MvpStateTxType;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerAccount;
import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerUser;
import ru.mbc.ledger.util.HashSum;

import java.util.ArrayList;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ledgerStateTxCheck {

    @Test
    public void checkSignature(){
        MvpLedgerUser user1 = new MvpLedgerUser();
        MvpLedgerUser user2 = new MvpLedgerUser();

        MvpLedgerAccount address1 = new MvpLedgerAccount(100);
        MvpLedgerAccount address2 = new MvpLedgerAccount(0);

        Hashtable<HashSum, MvpLedgerAccount> balances = new Hashtable<>();
        balances.put(user1.getAddress(), address1);
        balances.put(user2.getAddress(), address2);
        MvpState state = new MvpState(balances);

        MvpStateTx tx = new MvpStateTx(1, user1.getAddress().toString(), user2.getAddress().toString(), 20);
        user1.signTX(tx);
        state.applyTX(tx);

        assertEquals(80, state.getAccounts().get(user1.getAddress()).getBalance());
        assertEquals(20, state.getAccounts().get(user2.getAddress()).getBalance());
    }

    @Test
    public void checkTransactionAddition(){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        MvpLedgerUser user1 = new MvpLedgerUser();
        MvpLedgerUser user2 = new MvpLedgerUser();
        HashSum address1 = user1.getAddress();
        HashSum address2 = user2.getAddress();
        Integer amount = 100;

        // Check transaction addition
        MvpStateTx tx = new MvpStateTx(1, address1.toString(), address2.toString(), amount);
        user1.signTX(tx);
        db.addTransaction(tx);
        MvpStateTx tx2 = db.getTransaction(tx.getHash());

        assertEquals(tx.getFrom(), tx2.getFrom());
        assertEquals(tx.getTo(), tx2.getTo());
        assertEquals(tx.getValue(), tx2.getValue());
        assertEquals(tx.getIncluded(), tx2.getIncluded());

        db.deleteTransaction(tx.getHash());
        db.dropLedger();
    }

    @Test
    public void checkExternalTransaction(){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        MvpLedgerUser user1 = new MvpLedgerUser();
        MvpLedgerUser user2 = new MvpLedgerUser();
        HashSum address1 = user1.getAddress();
        HashSum address2 = user2.getAddress();
        Integer amount = 100;

        // External transaction
        MvpStateTx tx = new MvpStateTx(2, address1.toString(), address2.toString(), amount, MvpStateTxType.EXTERNAL_IN, false);
        user1.signTX(tx);
        db.addTransaction(tx);
        MvpStateTx restoredTx = db.getTransaction(tx.getHash());

        assertEquals(tx.getIncluded(), restoredTx.getIncluded());
        assertEquals(tx.getExternal(), restoredTx.getExternal());
        assertEquals(tx.getVerified(), restoredTx.getVerified());

        // Set verified
        tx.setVerified();
        db.txMarkAsVerified(tx.getHash());
        MvpStateTx restoredTx2 = db.getTransaction(tx.getHash());

        assertEquals(tx.getVerified(), restoredTx2.getVerified());

        // Set included
        tx.setIncluded();
        db.txMarkAsIncluded(tx.getHash());
        MvpStateTx restoredTx3 = db.getTransaction(tx.getHash());
        db.deleteTransaction(tx.getHash());

        assertEquals(restoredTx3.getIncluded(), restoredTx3.getIncluded());

        db.dropLedger();
    }

    @Test
    public void checkObtain(){
        MvpLedgerUser user1 = new MvpLedgerUser();
        MvpLedgerUser user2 = new MvpLedgerUser();
        HashSum address1 = user1.getAddress();
        HashSum address2 = user2.getAddress();
        Integer amount = 100;

        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        MvpStateTx tx1 = new MvpStateTx(1, address1.toString(), address2.toString(), amount, MvpStateTxType.INTERNAL, false);
        MvpStateTx tx2 = new MvpStateTx(2, address1.toString(), address2.toString(), amount, MvpStateTxType.EXTERNAL_IN, false);
        MvpStateTx tx3 = new MvpStateTx(3, address1.toString(), address2.toString(), amount, MvpStateTxType.EXTERNAL_IN, false);
        user1.signTX(tx1);
        user1.signTX(tx2);
        user1.signTX(tx3);
        db.addTransaction(tx1);
        db.addTransaction(tx2);
        db.addTransaction(tx3);

        ArrayList<MvpStateTx> tx = db.obtainTx();
        assertEquals(1, tx.size());

        db.txMarkAsVerified(tx2.getHash());
        tx = db.obtainTx();
        assertEquals(2, tx.size());

        db.txMarkAsIncluded(tx2.getHash());
        tx = db.obtainTx();
        assertEquals(1, tx.size());

        db.dropLedger();
    }
}
