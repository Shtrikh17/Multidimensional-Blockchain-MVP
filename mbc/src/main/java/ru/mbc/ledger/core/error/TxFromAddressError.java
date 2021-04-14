package ru.mbc.ledger.core.error;

public class TxFromAddressError extends Error{
    public TxFromAddressError(String errorMessage) {
        super(errorMessage);
    }
}
