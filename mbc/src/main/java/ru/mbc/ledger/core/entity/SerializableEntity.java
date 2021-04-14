package ru.mbc.ledger.core.entity;

public interface SerializableEntity {
    byte[] serialize();
    void set(byte[] data);
}
