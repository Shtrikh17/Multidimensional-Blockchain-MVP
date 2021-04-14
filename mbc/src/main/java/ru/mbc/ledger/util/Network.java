package ru.mbc.ledger.util;

import java.util.ArrayList;
import ru.mbc.ledger.network.Endpoint;

public class Network {
    public ArrayList<Endpoint> peers;

    public Network(){
        peers = new ArrayList<>();
    }
}

