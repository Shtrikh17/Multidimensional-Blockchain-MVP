package ru.mbc.manager.config;

import java.util.ArrayList;

public class ParentLedgerConfig {
    public ArrayList<NodeConfig> nodes;
    public String ledgerAddress;

    public ParentLedgerConfig(){
        nodes = new ArrayList<>();
    }
}
