package ru.mbc.ledger.core.error.tx;

public class SignatureError extends Error {
    public SignatureError(String errorMessage) {
        super(errorMessage);
    }
}
