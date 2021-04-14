package ru.mbc.ledger.core.logic;

import ru.mbc.ledger.core.entity.ledger.MvpLedgerAccount;
import ru.mbc.ledger.core.error.LedgerAccountValueError;
import ru.mbc.ledger.core.error.tx.BalanceError;
import ru.mbc.ledger.core.error.tx.NonceError;
import ru.mbc.ledger.core.error.tx.SignatureError;
import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.util.Config;
import ru.mbc.ledger.util.HashSum;

import java.util.ArrayList;
import java.util.Hashtable;

public class stateLogic {

    public MvpState buildState(MvpState state, ArrayList<MvpStateTx> txList, ArrayList<MvpStateTx> txApplied, ArrayList<HashSum> txNotApplied, ArrayList<HashSum> txError){
        MvpState newState = new MvpState(state);

        for(MvpStateTx tx: txList){
            try{
                newState.applyTX(tx);
                txApplied.add(tx);
            }
            catch (LedgerAccountValueError e){
                System.out.println("Error applying transaction " + tx.getHash().toString() + ": ledger account value error (" + e.getMessage() + ")");
                txError.add(tx.getHash());
            }
            catch(SignatureError e){
                System.out.println("Error applying transaction " + tx.getHash().toString() + ": signature error");
                txError.add(tx.getHash());
            }
            catch (BalanceError e){
                System.out.println("Error applying transaction " + tx.getHash().toString() + ": balance error");
                txNotApplied.add(tx.getHash());
            }
            catch (NonceError e){
                System.out.println("Error applying transaction " + tx.getHash().toString() + ": nonce error");
                txNotApplied.add(tx.getHash());
            }
        }

        return newState;
    }

    public MvpState createGenesisState(Hashtable<HashSum, MvpLedgerAccount> initBalance, ArrayList<HashSum> superusers){
        MvpState newState = new MvpState(initBalance, superusers);
        return newState;
    }

}
