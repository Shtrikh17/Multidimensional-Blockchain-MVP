package ru.mbc.ledger.core.error;

public class LedgerAccountValueError extends Error {
    public LedgerAccountValueError(String errorMessage) {
        super(errorMessage);
    }
}
