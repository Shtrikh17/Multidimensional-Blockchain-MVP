package ru.mbc.ledger.consensus;

import ru.mbc.ledger.core.entity.block.MvpBlock;
import ru.mbc.ledger.core.error.db.NoSuchEntity;
import ru.mbc.ledger.database.ledgerDB.ledgerDbPostgre;
import ru.mbc.ledger.util.HashSum;

import java.util.ArrayList;
import java.util.Stack;

public class BeaconConsensusChainSelector {
    private ArrayList<MvpBlock> blockList;
    private ledgerDbPostgre db; // It will be needed for bootstrap process - enough to get blocks with nonce=MAX(nonce)-10 and get their heights
    Integer maxHeight;
    HashSum highestBlock;

    public BeaconConsensusChainSelector(ledgerDbPostgre _db){
        blockList = new ArrayList<>();
        db = _db;
        maxHeight = 0;
        highestBlock = null;
    }

    public void notifyGenesisBlock(MvpBlock newBlock){
        try{
            blockList.clear();
            blockList.add(newBlock);
            maxHeight = newBlock.getSlot();
            highestBlock = newBlock.getHash();
        }catch (NoSuchEntity ex){

        }
    }

    public HashSum notifyNewBlock(MvpBlock newBlock){
        // Too old blocks are removed
        if(maxHeight - newBlock.getSlot() > 10)
            return highestBlock;

        // Check if parent is inside and substitute it
        if(newBlock.getSlot() > maxHeight){
            maxHeight = newBlock.getSlot();
            highestBlock = newBlock.getHash();
        }
        for(int i = 0; i < blockList.size(); i++){
            if(blockList.get(i).getHash().equals(newBlock.getPrevHash())){
                blockList.set(i, newBlock);
                break;
            }
        }

        // Clean up cache
        Stack<MvpBlock> cleanup = new Stack<>();
        for(MvpBlock b: blockList){
            if(maxHeight - b.getSlot() > 10){
                cleanup.push(b);
            }
        }
        while(!cleanup.isEmpty()){
            blockList.remove(cleanup.pop());
        }

        return highestBlock;
    }

    public HashSum getCurrentTopBlockHash(){
        return highestBlock;
    }

    public MvpBlock getCurrentTopBlock() throws NoSuchEntity{
        for(MvpBlock block: blockList){
            if(highestBlock.equals(block.getHash())){
                return block;
            }
        }
        throw new NoSuchEntity("No top block found");
    }

}
