package ru.mbc.ledger.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import ru.mbc.ledger.core.error.api.InvalidParameter;

import java.util.Arrays;

public final class HashSum {
    private byte[] array;

    public HashSum(){
        array = new byte[32];
        for(int i = 0; i < 32; i++){
            array[i] = 0;
        }
    }

    public HashSum(String data){
        try {
            array = Hex.decodeHex(data);
        } catch (DecoderException e) {
            e.printStackTrace();
            throw new InvalidParameter("Wrong state id value");
        }
    }

    public HashSum(byte[] array) {
        this.array = array;
    }

    public HashSum(HashSum t){
        this.array = t.array.clone();
    }

    public byte[] getArray() {
        return array.clone();
    }

    public String toString(){
        return Hex.encodeHexString(array);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashSum sum = (HashSum) o;
        return Arrays.equals(array, sum.array);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(array);
    }

    @Override
    public HashSum clone() {
        return new HashSum(array.clone());
    }
}
