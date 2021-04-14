package ru.mbc.ledger.core.error.db;

public class NoSuchEntity extends Error{
    public NoSuchEntity(String errorMessage) {
        super(errorMessage);
    }
}
