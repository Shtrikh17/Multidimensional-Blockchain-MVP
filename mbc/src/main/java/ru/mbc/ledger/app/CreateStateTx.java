package ru.mbc.ledger.app;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerUser;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.error.api.InvalidParameter;
import ru.mbc.ledger.util.HashSum;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.io.File;
import java.nio.file.Files;

public class CreateStateTx {

    private static MvpLedgerUser readUserConfig(String filename) throws InvalidParameter{
        File file = new File(filename);
        String jsonData;
        try {
            jsonData = Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new InvalidParameter("Wrong data");
        }

        try {
            JSONObject jo = new JSONObject(jsonData);
            HashSum address = new HashSum(jo.getString("address"));
            byte[] pubKey = Hex.decodeHex(jo.getString("pubKey"));
            byte[] privKey = Hex.decodeHex(jo.getString("privKey"));
            return new MvpLedgerUser(address, privKey, pubKey);
        } catch (DecoderException e) {
            e.printStackTrace();
            throw new InvalidParameter("Wrong json in config");
        }
    }


    public static void main(String[] args){
        String filename = "/home/user/IdeaProjects/mbc/config/user2.json";
        String output = "/home/user/IdeaProjects/mbc/tx/tx3.json";

        MvpLedgerUser user = readUserConfig(filename);

        Scanner sc = new Scanner(System.in);

        System.out.println("From: " + user.getAddress().toString());
        System.out.print("To: ");
        String to = sc.nextLine();

        System.out.print("Value: ");
        Integer value = sc.nextInt();

        System.out.print("Nonce: ");
        Integer nonce = sc.nextInt();

        System.out.print("Type [0 - internal, 1 - external out, 2 - external in]: ");
        Integer type = sc.nextInt();

        MvpStateTx tx = new MvpStateTx(nonce, user.getAddress().toString(), to, value, type);
        user.signTX(tx);

        JSONObject jo = new JSONObject();
        jo.put("nonce", tx.getNonce());
        jo.put("from", tx.getFrom().toString());
        jo.put("to", tx.getTo());
        jo.put("value", tx.getValue());
        switch (tx.getRawType()){
            case 2:
                jo.put("type", "external in");
                break;
            case 1:
                jo.put("type", "external out");
                break;
            default:
                jo.put("type", "internal");
        }
        jo.put("signature", Hex.encodeHexString(tx.getSignature().serialize()));

        String result = jo.toString();

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), "utf-8"))) {
            writer.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("HashSum: " + tx.getHash().toString());
    }
}
