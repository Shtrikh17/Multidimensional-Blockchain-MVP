package ru.mbc.ledger.consensus;

public class ConsensusBlockNotify {
    private Boolean marker;

    public ConsensusBlockNotify(){
        marker = false;
    }

    public boolean isNotified(){
        boolean result = false;
        synchronized (marker){
            result = marker.booleanValue();
        }
        return result;
    }

    public void setMarker(){
        synchronized (marker){
            marker = true;
        }
    }

    public void unsetMarker(){
        synchronized (marker){
            marker = false;
        }
    }



}
