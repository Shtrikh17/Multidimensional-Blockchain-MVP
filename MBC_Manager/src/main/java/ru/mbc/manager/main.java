package ru.mbc.manager;

import ru.mbc.manager.config.Config;
import ru.mbc.manager.listener.BcAPI;
import ru.mbc.manager.listener.MbcAPI;
import ru.mbc.manager.verification.MbcLogic;
import ru.mbc.manager.verification.TxDescriptor;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class main {

    public static void main(String[] args){
        if(args.length == 0){
            System.out.println("Provide config as an argument");
            System.exit(-1);
        }
        String filename = args[0];
        Config config = new Config();
        config.parse(filename);

        ArrayBlockingQueue<TxDescriptor> txQueue = new ArrayBlockingQueue<TxDescriptor>(10);
        BcAPI bcAPI = new BcAPI(config.bc_api.node.ip, config.bc_api.node.port, txQueue, config.general.ledgerAddress);
        MbcLogic logic = new MbcLogic(config, txQueue, config.general.bcVerifierPort);
        MbcAPI mbcAPI = new MbcAPI(config.mbc_api.node.ip, config.mbc_api.node.port, logic, config.general.bcVerifierPort);

        Thread thread1 = new Thread(logic);
        Thread thread2 = new Thread(mbcAPI);
        Thread thread3 = new Thread(bcAPI);
        thread1.start();
        thread2.start();
        thread3.start();
        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
