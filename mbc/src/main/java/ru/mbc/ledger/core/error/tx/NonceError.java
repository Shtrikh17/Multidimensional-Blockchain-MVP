package ru.mbc.ledger.core.error.tx;

public class NonceError extends Error{
    public NonceError(String errorMessage) {
        super(errorMessage);
    }
}
