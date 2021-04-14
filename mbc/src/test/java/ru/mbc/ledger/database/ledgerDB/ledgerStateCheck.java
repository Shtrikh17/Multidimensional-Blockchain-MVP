package ru.mbc.ledger.database.ledgerDB;
import org.junit.jupiter.api.Test;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerAccount;
import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerUser;
import ru.mbc.ledger.util.HashSum;

import java.util.ArrayList;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

public class ledgerStateCheck {
    @Test
    public void checkStatesSave(){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        Hashtable<HashSum, MvpLedgerAccount> balances = new Hashtable<>();
        MvpLedgerUser user1 = new MvpLedgerUser();
        MvpLedgerUser user2 = new MvpLedgerUser();

        MvpLedgerAccount account1 = new MvpLedgerAccount(100);
        MvpLedgerAccount account2 = new MvpLedgerAccount(50);

        balances.put(user1.getAddress(), account1);
        balances.put(user2.getAddress(), account2);

        MvpState s1 = new MvpState(balances);
        db.addState(s1);
        MvpState s2 = db.getState(s1.getHash());

        assertEquals(s2.getAccounts().size(), s1.getAccounts().size());

        for(HashSum k: s2.getAccounts().keySet()){
            assertEquals(s2.getAccounts().get(k).getBalance(), s1.getAccounts().get(k).getBalance());
        }

        db.deleteState(s1.getHash());
        db.dropLedger();
    }

    @Test
    public void checkStateApplyTX(){
        MvpLedgerUser user1 = new MvpLedgerUser();
        MvpLedgerUser user2 = new MvpLedgerUser();
        MvpLedgerAccount account1 = new MvpLedgerAccount(100);
        MvpLedgerAccount account2 = new MvpLedgerAccount(50);

        Hashtable<HashSum, MvpLedgerAccount> balances = new Hashtable<>();
        balances.put(user1.getAddress(), account1);
        balances.put(user2.getAddress(), account2);
        MvpState s1 = new MvpState(balances);

        MvpStateTx tx = new MvpStateTx(1, user1.getAddress().toString(), user2.getAddress().toString(), 20);
        user1.signTX(tx);
        s1.applyTX(tx);

        Hashtable<HashSum, MvpLedgerAccount> balances2 = new Hashtable<>();
        balances2.put(user1.getAddress(), new MvpLedgerAccount(80));
        balances2.put(user2.getAddress(), new MvpLedgerAccount(70));

        assertEquals(balances2.size(), s1.getAccounts().size());
        for(HashSum k: s1.getAccounts().keySet()){
            assertEquals(s1.getAccounts().get(k).getBalance(), balances2.get(k).getBalance());
        }
    }

    @Test
    public void checkStateApplyDb(){
        ledgerDbPostgre db = new ledgerDbPostgre();
        db.initLedger();

        // Create users
        MvpLedgerUser user1 = new MvpLedgerUser();
        MvpLedgerUser user2 = new MvpLedgerUser();
        MvpLedgerAccount account1 = new MvpLedgerAccount(100);
        MvpLedgerAccount account2 = new MvpLedgerAccount(50);

        // Init state
        Hashtable<HashSum, MvpLedgerAccount> balances = new Hashtable<>();
        balances.put(user1.getAddress(), account1);
        balances.put(user2.getAddress(), account2);
        MvpState s1 = new MvpState(balances);

        // Create signed transaction
        MvpStateTx tx = new MvpStateTx(1, user1.getAddress().toString(), user2.getAddress().toString(), 50);
        user1.signTX(tx);
        db.addTransaction(tx);

        // Apply tx and store new state
        s1.applyTX(tx);
        db.addState(s1);

        // Create test state
        Hashtable<HashSum, MvpLedgerAccount> balances2 = new Hashtable<>();
        MvpLedgerAccount _account1 = new MvpLedgerAccount(50);
        MvpLedgerAccount _account2 = new MvpLedgerAccount(100);
        balances2.put(user1.getAddress(), _account1);
        balances2.put(user2.getAddress(), _account2);
        ArrayList<MvpStateTx> txList = new ArrayList<>();
        txList.add(tx);
        MvpState testState = new MvpState(balances2);
        MvpState restoredState = db.getState(s1.getHash());

        // Test balances
        assertEquals(testState.getAccounts().size(), restoredState.getAccounts().size());
        for(HashSum k: testState.getAccounts().keySet()){
            assertEquals(testState.getAccounts().get(k).getBalance(), restoredState.getAccounts().get(k).getBalance());
        }

        // Clear
        db.deleteState(s1.getHash());
        db.dropLedger();
    }
}
