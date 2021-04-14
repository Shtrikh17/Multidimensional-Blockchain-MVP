package ru.mbc.ledger.util;

import java.util.ArrayList;

public class State {
    public ArrayList<Account> accounts;
    public ArrayList<HashSum> superusers;

    public State(){
        accounts = new ArrayList<>();
        superusers = new ArrayList<>();
    }
}
