package ru.mbc.ledger.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Hashtable;

public class transform {

    public static <T> Hashtable<HashSum, T> deepCopy(Hashtable<HashSum, T> original){
        Hashtable<HashSum, T> copy = new Hashtable<>(original.size());
        for(HashSum key: original.keySet()){
            copy.put(key, original.get(key));
        }
        return copy;
    }

    public static <T> Hashtable<HashSum, T> deepCopy2(Hashtable<HashSum, T> original){
        Hashtable<HashSum, T> copy = new Hashtable<>(original.size());
        for(HashSum key: original.keySet()){
            T t = original.get(key);
            Class<?> clazz = t.getClass();
            try {
                Constructor<?> copyConstructor = clazz.getConstructor(clazz);
                copy.put(key, (T) copyConstructor.newInstance(t));
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return copy;
    }

    public static <T> ArrayList<T> deepCopy2(ArrayList<T> original){
        ArrayList<T> result = new ArrayList<>(original.size());
        for(T t: original){
            Class<?> clazz = t.getClass();
            try {
                Constructor<?> copyConstructor = clazz.getConstructor(clazz);
                result.add((T) copyConstructor.newInstance(t));
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

        }
        return result;
    }

    public static <T> Hashtable<HashSum, T> hashTransformDecode(Hashtable<String, T> original) {
        Hashtable<HashSum, T> result = new Hashtable<>(original.size());
        for (String k : original.keySet()) {
            try {
                result.put(new HashSum(Hex.decodeHex(k)), original.get(k));
            } catch (DecoderException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
