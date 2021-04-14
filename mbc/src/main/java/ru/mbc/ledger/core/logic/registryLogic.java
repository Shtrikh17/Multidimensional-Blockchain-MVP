package ru.mbc.ledger.core.logic;

import ru.mbc.ledger.core.entity.registry.MvpRegistry;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTx;
import ru.mbc.ledger.core.error.LedgerAccountValueError;
import ru.mbc.ledger.core.error.tx.AliasError;
import ru.mbc.ledger.core.error.tx.SignatureError;
import ru.mbc.ledger.util.HashSum;

import java.util.ArrayList;

public class registryLogic {
    public MvpRegistry buildState(MvpRegistry registry, ArrayList<MvpRegistryTx> txList, ArrayList<MvpRegistryTx> txApplied, ArrayList<HashSum> txError){
        MvpRegistry newRegistry = new MvpRegistry(registry);

        for(MvpRegistryTx tx: txList){
            try{
                newRegistry.applyTX(tx);
                txApplied.add(tx);
            }
            catch (LedgerAccountValueError | SignatureError | AliasError e){
                txError.add(tx.getHash());
            }
        }

        return newRegistry;
    }

    public MvpRegistry createGenesisRegistry(){
        return new MvpRegistry();
    }
}
