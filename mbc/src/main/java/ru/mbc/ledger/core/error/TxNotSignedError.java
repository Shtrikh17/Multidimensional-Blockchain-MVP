package ru.mbc.ledger.core.error;

public class TxNotSignedError extends Error {
    public TxNotSignedError(String errorMessage) {
        super(errorMessage);
    }
}
