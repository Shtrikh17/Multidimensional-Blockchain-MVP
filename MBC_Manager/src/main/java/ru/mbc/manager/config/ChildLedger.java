package ru.mbc.manager.config;

import java.util.ArrayList;

public class ChildLedger {
    public ArrayList<NodeConfig> nodes;
    public String ledgerAddress;

    public ChildLedger(){
        nodes = new ArrayList<>();
    }
}
