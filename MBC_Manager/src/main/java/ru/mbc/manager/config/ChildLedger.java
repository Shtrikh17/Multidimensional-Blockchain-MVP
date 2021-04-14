package ru.mbc.manager.config;

import java.util.ArrayList;

public class ChildLedger {
    public static ArrayList<NodeConfig> nodes;
    public static String ledgerAddress;

    public ChildLedger(){
        nodes = new ArrayList<>();
    }
}
