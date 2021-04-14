package ru.mbc.ledger.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import ru.mbc.ledger.core.error.config.ConfigValueError;

public class Account {
    public HashSum address;
    public Integer value;

    public Account(){
        address = new HashSum();
        value = 0;
    }

    public Account(String hexAddress, Integer val) throws ConfigValueError {
        try {
            address = new HashSum(Hex.decodeHex(hexAddress));
            value = val;
        } catch (DecoderException e) {
            throw new ConfigValueError("Invalid address");
        }
    }
}
