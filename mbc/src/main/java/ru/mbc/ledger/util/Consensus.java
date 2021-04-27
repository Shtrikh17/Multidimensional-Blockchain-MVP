package ru.mbc.ledger.util;

import ru.mbc.ledger.core.entity.ledger.MvpLedgerUser;

public class Consensus{
    public String oracleHost;
    public Integer oraclePort;
    public MvpLedgerUser user;
    public Integer slot;
    public Boolean miningAllowed;
}
