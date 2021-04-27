package ru.mbc.ledger.consensus;

import ru.mbc.ledger.core.entity.block.MvpBlock;
import ru.mbc.ledger.core.error.db.NoSuchEntity;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;
import ru.mbc.ledger.util.HashSum;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Stack;

public class BeaconConsensusChainSelector {
    private Hashtable<HashSum, Integer> blockSet;
    private ledgerDbPostgre db; // It will be needed for bootstrap process - enough to get blocks with nonce=MAX(nonce)-10 and get their heights
    Integer maxHeight;
    HashSum highestBlock;

    public BeaconConsensusChainSelector(ledgerDbPostgre _db){
        blockSet = new Hashtable<>();
        db = _db;
        maxHeight = 0;
        highestBlock = null;
    }

    public void notifyGenesisBlock(MvpBlock newBlock){
        try{
            blockSet.clear();
            blockSet.put(newBlock.getHash(), 0);
            maxHeight = 0;
            highestBlock = newBlock.getHash();
        }catch (NoSuchEntity ex){}
    }

    public HashSum notifyNewBlock(MvpBlock newBlock){
        // Check if parent is inside and substitute it
        if(newBlock.getSlot() > maxHeight){
            maxHeight = newBlock.getSlot();
            highestBlock = newBlock.getHash();
        }
        Integer N = 0;
        for(HashSum h: blockSet.keySet()){
            if(h.equals(newBlock.getPrevHash())){
                N = blockSet.get(h) + 1;
            }
        }

        blockSet.put(newBlock.getHash(), 0);

        return highestBlock;
    }

    public HashSum getCurrentTopBlockHash(){
        return highestBlock;
    }

    public MvpBlock getCurrentTopBlock() throws NoSuchEntity{
        return db.getBlock(highestBlock);
    }

}
