package ru.mbc.ledger.core.error.tx;

public class BalanceError extends Error{
    public BalanceError(String errorMessage) {
        super(errorMessage);
    }
}
