package ru.mbc.ledger.util;

import ru.mbc.ledger.core.entity.ledger.MvpLedgerAccount;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerUser;
import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;

import java.security.SecureRandom;
import java.util.Hashtable;

public class helper {
    public static HashSum generateHash(){
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[32];                // TODO: define - currently for sha-256
        random.nextBytes(bytes);
        return new HashSum(bytes);
    }



}
