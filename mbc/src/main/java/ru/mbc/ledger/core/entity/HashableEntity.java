package ru.mbc.ledger.core.entity;

import ru.mbc.ledger.util.HashSum;

public interface HashableEntity {
    public HashSum getHash();
}
