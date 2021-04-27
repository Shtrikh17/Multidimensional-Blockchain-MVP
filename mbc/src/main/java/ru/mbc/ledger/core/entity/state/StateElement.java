package ru.mbc.ledger.core.entity.state;

import ru.mbc.ledger.core.entity.ledger.MvpLedgerAccount;
import ru.mbc.ledger.util.HashSum;

public class StateElement {
    public HashSum hash;
    public MvpLedgerAccount account;
}
