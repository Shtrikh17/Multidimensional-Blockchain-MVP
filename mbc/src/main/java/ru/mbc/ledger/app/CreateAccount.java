package ru.mbc.ledger.app;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerUser;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CreateAccount {

    public static void main(String[] argv){
        MvpLedgerUser user = new MvpLedgerUser();
        JSONObject jo = new JSONObject();

        jo.put("address", user.getAddress().toString());
        jo.put("pubKey",Hex.encodeHexString(user.getPubKey()));
        jo.put("privKey", Hex.encodeHexString(user.getPrivKey()));

        String result = jo.toString();

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/home/user/IdeaProjects/mbc/config/test/ledger3/user/user_3_3.json"), "utf-8"))) {
            writer.write(result);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
