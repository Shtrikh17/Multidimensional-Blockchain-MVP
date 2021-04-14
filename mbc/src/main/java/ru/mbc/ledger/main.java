package ru.mbc.ledger;

import ru.mbc.ledger.core.Core;
import ru.mbc.ledger.util.Config;

public class main {
    public static void main(String[] args){

        if(args.length == 0){
            System.out.println("Provide config as an argument");
            System.exit(-1);
        }
        //String filename = "/home/user/IdeaProjects/mbc/config/config.json";
        String filename = args[0];
        Config config = new Config();
        config.parse(filename);

        Core core = new Core(config);
        Thread thread = new Thread(core);
        thread.start();

        try{
            thread.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
