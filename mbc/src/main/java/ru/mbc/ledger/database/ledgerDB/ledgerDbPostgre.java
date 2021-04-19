package ru.mbc.ledger.database.ledgerDB;

import ru.mbc.ledger.consensus.BeaconConsensusStructure;
import ru.mbc.ledger.core.entity.block.MvpBlock;
import ru.mbc.ledger.core.entity.ledger.MvpLedgerDescriptor;
import ru.mbc.ledger.core.entity.registry.MvpRegistry;
import ru.mbc.ledger.core.entity.registry.MvpRegistryTx;
import ru.mbc.ledger.core.entity.state.MvpState;
import ru.mbc.ledger.core.entity.state.MvpStateTx;
import ru.mbc.ledger.core.entity.state.MvpStateTxSignature;
import ru.mbc.ledger.core.error.db.NoSuchEntity;
import ru.mbc.ledger.util.HashSum;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ledgerDbPostgre {
    String url = "jdbc:postgresql://localhost:5432/";
    String user = "mbc";
    String password = "Passw0rd";
    String database;

    private void dropById(String hashSum, String entity) throws NoSuchElementException{
        String dbQuery = "DELETE FROM " + entity + " WHERE id=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setString(1, hashSum);
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchElementException();
        }
    }

    private void dropByHash(HashSum hashSum, String entity) throws NoSuchEntity{
        String dbQuery = "DELETE FROM " + entity + " WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No hash found or some other error occurred");
        }
    }

    public ledgerDbPostgre(){
        this.database = "ledger_test";
    }

    public ledgerDbPostgre(String ledgerId){
        this.database = "ledger_" + ledgerId;
    }

    public void initLedger(){

        boolean toInit = true;
        try {
            Connection c = DriverManager.getConnection(url + "mbcdb", user, password);
            String query = "SELECT datname FROM pg_database WHERE datistemplate = false";
            PreparedStatement pst = c.prepareStatement(query);
            ResultSet resultSet = pst.executeQuery();

            //iterate each catalog in the ResultSet
            while (resultSet.next()) {
                // Get the database name, which is at position 1
                String databaseName = resultSet.getString(1);
                if(databaseName.equals(this.database)){
                    toInit = false;
                    break;
                }
            }
            resultSet.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        if(!toInit)
            return;

        String[] initQueries = {
                "DROP DATABASE IF EXISTS " + database,
                "CREATE DATABASE " + database
        };

        String[] dbQueries = {
                "CREATE TABLE IF NOT EXISTS STATES (id serial PRIMARY KEY, hash bytea UNIQUE, storage bytea)",
                "CREATE TABLE IF NOT EXISTS BLOCKS (id serial PRIMARY KEY, hash bytea UNIQUE, parentHash bytea, stateHash bytea, registryHash bytea, sTxHash bytea, rTxHash bytea, number INTEGER, consensus bytea, sTx bytea[], rTx bytea[])",
                "CREATE TABLE IF NOT EXISTS STX (id serial PRIMARY KEY, hash bytea UNIQUE, nonce INTEGER, verify_ctr INTEGER, addressFrom bytea, addressTo bytea, value INTEGER, signature bytea, type INTEGER, verified BOOLEAN, included BOOLEAN)",
                "CREATE TABLE IF NOT EXISTS REGISTRY (id serial PRIMARY KEY, hash bytea UNIQUE, storage bytea)",
                "CREATE TABLE IF NOT EXISTS RTX (id serial PRIMARY KEY, hash bytea UNIQUE, nonce INTEGER, descriptor bytea, included BOOLEAN)"
        };

        String dbQuery = String.join("; ", initQueries);

        try{
            Connection c = DriverManager.getConnection(url + "mbcdb", user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }

        dbQuery = String.join("; ", dbQueries);
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void addState(MvpState state){
        String dbQuery = "INSERT INTO STATES (hash, storage) VALUES (?,?) ON CONFLICT(hash) DO NOTHING";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(state.getHash().getArray()));
            pst.setBinaryStream(2, new ByteArrayInputStream(state.serialize()));
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public MvpState getState() throws NoSuchEntity {
        MvpState state = null;
        String dbQuery = "SELECT * FROM STATES ORDER BY id DESC LIMIT 1";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                state = new MvpState(rs.getBinaryStream("storage").readAllBytes());
            }
            else{
                throw new NoSuchEntity("No state found");
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return state;
    }

    public MvpState getState(HashSum hash) throws NoSuchEntity{
        MvpState state;
        String dbQuery = "SELECT * FROM STATES WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(hash.getArray()));
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                state = new MvpState(rs.getBinaryStream("storage").readAllBytes());
            }
            else{
                throw new NoSuchEntity("No state found");
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No state found");
        }
        return state;
    }

    public void deleteState(HashSum hashSum) throws NoSuchElementException{
        dropByHash(hashSum, "STATES");
    }

    public void dropLedger(){
        String dbQuery = "DROP DATABASE IF EXISTS " + database;

        try{
            Connection c = DriverManager.getConnection(url + "mbcdb", user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void addTransaction(MvpStateTx tx){
        String dbQuery = "INSERT INTO STX (hash, nonce, addressFrom, addressTo, value, signature, included, type, verified, verify_ctr) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(tx.getHash().getArray()));
            pst.setInt(2, tx.getNonce());
            pst.setBinaryStream(3, new ByteArrayInputStream(tx.getFrom().getBytes(StandardCharsets.UTF_8)));
            pst.setBinaryStream(4, new ByteArrayInputStream(tx.getTo().getBytes(StandardCharsets.UTF_8)));
            pst.setInt(5, tx.getValue());
            pst.setBinaryStream(6, new ByteArrayInputStream(tx.getSignature().serialize()));
            pst.setBoolean(7, tx.getIncluded());
            pst.setInt(8, tx.getRawType());
            pst.setBoolean(9, tx.getVerified());
            pst.setInt(10, 0);
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public MvpStateTx getTransaction(HashSum hashSum) throws NoSuchEntity{
        MvpStateTx tx;
        String dbQuery = "SELECT * FROM STX WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                tx = new MvpStateTx(
                        rs.getInt("nonce"),
                        new String(rs.getBinaryStream("addressFrom").readAllBytes()),
                        new String(rs.getBinaryStream("addressTo").readAllBytes()),
                        rs.getInt("value"),
                        new MvpStateTxSignature(rs.getBinaryStream("signature").readAllBytes()),
                        rs.getInt("type"),
                        rs.getBoolean("verified"),
                        rs.getBoolean("included")
                );
            }
            else{
                throw new NoSuchEntity("No tx found");
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No tx found");
        }
        return tx;
    }

    public ArrayList<MvpStateTx> getTransactions(ArrayList<HashSum> hashSums) throws NoSuchEntity{
        ArrayList<MvpStateTx> txList = new ArrayList<>();
        String dbQuery = "SELECT * FROM STX WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);

            for(HashSum hashSum: hashSums){
                PreparedStatement pst = c.prepareStatement(dbQuery);
                pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
                ResultSet rs = pst.executeQuery();
                if(rs.next()){
                    txList.add(new MvpStateTx(
                            rs.getInt("nonce"),
                            new String(rs.getBinaryStream("addressFrom").readAllBytes()),
                            new String(rs.getBinaryStream("addressTo").readAllBytes()),
                            rs.getInt("value"),
                            new MvpStateTxSignature(rs.getBinaryStream("signature").readAllBytes()),
                            rs.getInt("type"),
                            rs.getBoolean("verified"),
                            rs.getBoolean("included")
                    ));
                }
                else{
                    throw new NoSuchEntity("No tx found");
                }
                rs.close();
                pst.close();
            }

            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No tx found");
        }
        return txList;
    }

    public void deleteTransaction(HashSum hashSum) {
        dropByHash(hashSum, "STX");
    }

    private boolean getBooleanField(HashSum hashSum, String field) throws  NoSuchEntity{
        boolean result = false;
        String dbQuery = "SELECT " + field + " FROM STX WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                result = rs.getBoolean(field);
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("Not found boolean entity");
        }
        return result;
    }

    public boolean txIsExternal(HashSum hashSum) throws NoSuchEntity {
        return getBooleanField(hashSum, "external");
    }

    public boolean txIsVerified(HashSum hashSum) throws NoSuchEntity {
        return getBooleanField(hashSum, "verified");
    }

    private void updateBooleanField(HashSum hashSum, String field) throws NoSuchEntity{
        String dbQuery = "UPDATE STX SET " + field + " = 'true' WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No transaction found");
        }
    }

    public void txMarkAsVerified(HashSum hashSum) throws NoSuchEntity{
        updateBooleanField(hashSum, "verified");
        String dbQuery = "UPDATE STX SET verify_ctr = 1 WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No transaction found");
        }
    }

    public void txMarkAsIncluded(HashSum hashSum) throws NoSuchEntity {
        updateBooleanField(hashSum, "included");
    }

    public ArrayList<MvpStateTx> obtainTx(){
        return obtainTx(10);
    }

    public ArrayList<MvpStateTx> obtainTx(Integer N){
        ArrayList<MvpStateTx> txList = new ArrayList<MvpStateTx>();
        String dbQuery = "SELECT * FROM STX WHERE included = false and (type = 0 or type = 1 or type = 2 and verified = true and verify_ctr = 0) LIMIT " + N.toString();
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            ResultSet rs = pst.executeQuery();
            while(rs.next()){
                MvpStateTx tx = new MvpStateTx(
                        rs.getInt("nonce"),
                        new String(rs.getBinaryStream("addressFrom").readAllBytes()),
                        new String(rs.getBinaryStream("addressTo").readAllBytes()),
                        rs.getInt("value"),
                        new MvpStateTxSignature(rs.getBinaryStream("signature").readAllBytes()),
                        rs.getInt("type"),
                        rs.getBoolean("verified"),
                        rs.getBoolean("included")
                );
                txList.add(tx);
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }

        return txList;
    }

    public void modifyVerifyCounters(){
        String dbQuery1 = "SELECT hash, verify_ctr FROM STX WHERE verify_ctr > 0";
        Hashtable<HashSum, Integer> table = new Hashtable<>();
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery1);
            ResultSet rs = pst.executeQuery();
            while(rs.next()){
                table.put(new HashSum(rs.getBinaryStream("hash").readAllBytes()), rs.getInt("verify_ctr"));
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No transaction found");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String dbQuery2 = "UPDATE STX SET verify_ctr = (?) WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            for(HashSum h: table.keySet()){
                PreparedStatement pst = c.prepareStatement(dbQuery2);
                pst.setInt(1, table.get(h) - 1);
                pst.setBinaryStream(2, new ByteArrayInputStream(h.getArray()));
                pst.execute();
                pst.close();
            }
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No transaction found");
        }
    }

    public ArrayList<MvpStateTx> getPendingStateTx(){
        ArrayList<MvpStateTx> txList = new ArrayList<MvpStateTx>();
        String dbQuery = "SELECT * FROM STX WHERE included = false";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            ResultSet rs = pst.executeQuery();
            while(rs.next()){
                MvpStateTx tx = new MvpStateTx(
                        rs.getInt("nonce"),
                        new String(rs.getBinaryStream("addressFrom").readAllBytes()),
                        new String(rs.getBinaryStream("addressTo").readAllBytes()),
                        rs.getInt("value"),
                        new MvpStateTxSignature(rs.getBinaryStream("signature").readAllBytes()),
                        rs.getInt("type"),
                        rs.getBoolean("verified"),
                        rs.getBoolean("included")
                );
                txList.add(tx);
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return txList;
    }

    public void addBlock(MvpBlock block) {
        String dbQuery = "INSERT INTO BLOCKS (hash, parentHash, stateHash, registryHash, sTxHash, rTxHash, number, consensus) VALUES (?,?,?,?,?,?,?,?)";

        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(block.getHash().getArray()));
            if (block.getPrevHash() != null){
                pst.setBinaryStream(2, new ByteArrayInputStream(block.getPrevHash().getArray()));
            }
            else{
                byte[] t = new byte[32];
                for(int i = 0; i< 32; i++)
                    t[i] = 0;
                pst.setBinaryStream(2, new ByteArrayInputStream(t));
            }
            pst.setBinaryStream(3, new ByteArrayInputStream(block.getStateHash().getArray()));
            pst.setBinaryStream(4, new ByteArrayInputStream(block.getRegistryHash().getArray()));
            pst.setBinaryStream(5, new ByteArrayInputStream(block.getSTxHash().getArray()));
            pst.setBinaryStream(6, new ByteArrayInputStream(block.getRTxHash().getArray()));
            pst.setInt(7, block.getSlot());
            if(block.getConsensus() != null){
                pst.setBinaryStream(8, new ByteArrayInputStream(block.getConsensus().serialize()));
            }
            else{
                byte[] t = new byte[32];
                for(int i = 0; i< 32; i++)
                    t[i] = 0;
                pst.setBinaryStream(8, new ByteArrayInputStream(t));
            }
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void deleteBlock(HashSum hashSum) {
        dropByHash(hashSum, "BLOCKS");
    }

    public MvpBlock getBlock(HashSum hashSum) throws NoSuchEntity {
        MvpBlock block;
        String dbQuery = "SELECT * FROM BLOCKS WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                block = new MvpBlock(
                        new HashSum(rs.getBinaryStream("parentHash").readAllBytes()),
                        new HashSum(rs.getBinaryStream("stateHash").readAllBytes()),
                        new HashSum(rs.getBinaryStream("registryHash").readAllBytes()),
                        rs.getInt("number"),
                        rs.getBinaryStream("sTxHash").readAllBytes(),
                        rs.getBinaryStream("rTxHash").readAllBytes(),
                        new BeaconConsensusStructure(rs.getBinaryStream("consensus").readAllBytes()),
                        rs.getInt("number") == 0
                );
            }
            else{
                throw new NoSuchEntity("No block found");
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No such block or some SQL error occurred");
        }
        return block;
    }

    public MvpBlock getBlockBySlot(Integer slot) throws NoSuchEntity {
        MvpBlock block;
        String dbQuery = "SELECT * FROM BLOCKS WHERE number=?";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            pst.setInt(1, slot.intValue());
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                block = new MvpBlock(
                        new HashSum(rs.getBinaryStream("parentHash").readAllBytes()),
                        new HashSum(rs.getBinaryStream("stateHash").readAllBytes()),
                        new HashSum(rs.getBinaryStream("registryHash").readAllBytes()),
                        rs.getInt("number"),
                        rs.getBinaryStream("sTxHash").readAllBytes(),
                        rs.getBinaryStream("rTxHash").readAllBytes(),
                        new BeaconConsensusStructure(rs.getBinaryStream("consensus").readAllBytes()),
                        rs.getInt("number") == 0
                );
            }
            else{
                throw new NoSuchEntity("No block found");
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No such block or some SQL error occurred");
        }
        return block;
    }

    public MvpBlock getBlock() throws NoSuchEntity {
        MvpBlock block;
        String dbQuery = "SELECT * FROM BLOCKS order by number desc";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                BeaconConsensusStructure consensus = null;
                try{
                    consensus = new BeaconConsensusStructure(rs.getBinaryStream("consensus").readAllBytes());
                }
                catch (EOFException ex){
                    consensus = new BeaconConsensusStructure();
                }
                block = new MvpBlock(
                        new HashSum(rs.getBinaryStream("parentHash").readAllBytes()),
                        new HashSum(rs.getBinaryStream("stateHash").readAllBytes()),
                        new HashSum(rs.getBinaryStream("registryHash").readAllBytes()),
                        rs.getInt("number"),
                        rs.getBinaryStream("sTxHash").readAllBytes(),
                        rs.getBinaryStream("rTxHash").readAllBytes(),
                        consensus,
                        rs.getInt("number") == 0
                );
            }
            else{
                throw new NoSuchEntity("No block found");
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No such block or some SQL error occurred");
        }
        return block;
    }

    public boolean checkIfBlockPresent(HashSum hashSum) {
        String dbQuery = "SELECT * FROM BLOCKS WHERE hash=(?)";
        boolean result;
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                result = true;
            }
            else{
                result = false;
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            result = false;
        }
        return result;
    }

    public MvpBlock getPrevBlock(HashSum hashSum) throws NoSuchEntity {
        MvpBlock block;
        String dbQuery = "SELECT parentHash FROM BLOCKS WHERE hash=(?)";
        HashSum prevHash;
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                prevHash = new HashSum(rs.getBinaryStream("prev_hash").readAllBytes());
            }
            else{
                throw new NoSuchEntity("No given block found");
            }
            rs.close();
            pst.close();
            c.close();
            block = getBlock(prevHash);
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchElementException();
        }
        return block;
    }

    public void addRegistry(MvpRegistry registry){
        String dbQuery = "INSERT INTO REGISTRY (hash, storage) VALUES (?,?) ON CONFLICT(hash) DO NOTHING";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(registry.getHash().getArray()));
            pst.setBinaryStream(2, new ByteArrayInputStream(registry.serialize()));
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public MvpRegistry getRegistry() throws NoSuchEntity{
        MvpRegistry registry = null;
        String dbQuery = "SELECT * FROM REGISTRY ORDER BY id DESC LIMIT 1";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                registry = new MvpRegistry(rs.getBinaryStream("storage").readAllBytes());
            }
            else{
                throw new NoSuchEntity("No registry found");
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No registry found");
        }
        return registry;
    }

    public MvpRegistry getRegistry(HashSum hash) throws NoSuchEntity{
        MvpRegistry registry = null;
        String dbQuery = "SELECT * FROM REGISTRY WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(hash.getArray()));
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                registry = new MvpRegistry(rs.getBinaryStream("storage").readAllBytes());
            }
            else{
                throw new NoSuchEntity("No registry found");
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No registry found");
        }
        return registry;
    }

    public void deleteRegistry(HashSum hashSum) throws NoSuchEntity{
        dropByHash(hashSum, "REGISTRY");
    }

    public MvpRegistryTx getRegistryTransaction(HashSum hashSum) throws NoSuchElementException{
        MvpRegistryTx tx = new MvpRegistryTx();
        String dbQuery = "SELECT * FROM RTX WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                tx.set(
                        rs.getInt("nonce"),
                        rs.getBinaryStream("descriptor").readAllBytes(),
                        rs.getBoolean("included")
                );
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchElementException();
        }
        return tx;
    }

    public ArrayList<MvpRegistryTx> getRegistryTransactions(ArrayList<HashSum> hashSums) throws NoSuchElementException{
        ArrayList<MvpRegistryTx> txList = new ArrayList<>();
        String dbQuery = "SELECT * FROM RTX WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);

            for(HashSum hashSum: hashSums){
                PreparedStatement pst = c.prepareStatement(dbQuery);

                pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
                ResultSet rs = pst.executeQuery();
                if(rs.next()){
                    txList.add(new MvpRegistryTx(
                            rs.getInt("nonce"),
                            new MvpLedgerDescriptor(rs.getBinaryStream("descriptor").readAllBytes()),
                            rs.getBoolean("included")
                    ));
                }
                rs.close();
                pst.close();
            }

            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchElementException();
        }
        return txList;
    }

    public void addRegistryTx(MvpRegistryTx tx){
        String dbQuery = "INSERT INTO RTX (hash, nonce, descriptor, included) VALUES (?,?,?,?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(tx.getHash().getArray()));
            pst.setInt(2, tx.getNonce());
            pst.setBinaryStream(3, new ByteArrayInputStream(tx.getLedgerDescriptor().serialize()));
            pst.setBoolean(4,tx.getIncluded());
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void deleteRegistryTx(HashSum hashSum) {
        dropByHash(hashSum, "RTX");
    }

    public void markRegistryTxIncluded(HashSum hashSum){
        String dbQuery = "UPDATE RTX SET included = 'true' WHERE hash=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            pst.setBinaryStream(1, new ByteArrayInputStream(hashSum.getArray()));
            pst.execute();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchElementException();
        }
    }

    public ArrayList<MvpRegistryTx> obtainRegistryTx(){
        return obtainRegistryTx(10);
    }

    public ArrayList<MvpRegistryTx> obtainRegistryTx(Integer N){
        ArrayList<MvpRegistryTx> txList = new ArrayList<>();
        String dbQuery = "SELECT * FROM RTX WHERE included = false LIMIT " + N.toString();
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            ResultSet rs = pst.executeQuery();
            while(rs.next()){
                MvpRegistryTx tx = new MvpRegistryTx(
                        rs.getInt("nonce"),
                        new MvpLedgerDescriptor(rs.getBinaryStream("descriptor").readAllBytes())
                );
                txList.add(tx);
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchElementException();
        }
        return txList;
    }

    public ArrayList<MvpRegistryTx> getPendingRegistryTx(){
        ArrayList<MvpRegistryTx> txList = new ArrayList<>();
        String dbQuery = "SELECT * FROM RTX WHERE included = false";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            ResultSet rs = pst.executeQuery();
            while(rs.next()){
                MvpRegistryTx tx = new MvpRegistryTx(
                        rs.getInt("nonce"),
                        new MvpLedgerDescriptor(rs.getBinaryStream("descriptor").readAllBytes())
                );
                txList.add(tx);
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException | IOException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return txList;
    }

    public Integer getUserNonce(HashSum address) throws NoSuchEntity{
        Integer result;
        String dbQuery = "SELECT COUNT(*) as COUNT FROM STX WHERE addressFrom=(?)";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);

            pst.setBinaryStream(1, new ByteArrayInputStream(address.getArray()));
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                result = rs.getInt("COUNT") + 1;
            }
            else{
                result = 1;
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No address or db error occurred");
        }
        return result;
    }

    public Integer getLedgerNonce() throws NoSuchEntity{
        Integer result;
        String dbQuery = "SELECT COUNT(*) as COUNT FROM RTX where included = false";
        try{
            Connection c = DriverManager.getConnection(url + database, user, password);
            PreparedStatement pst = c.prepareStatement(dbQuery);
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                result = rs.getInt("COUNT") + 1;
            }
            else{
                result = 1;
            }
            rs.close();
            pst.close();
            c.close();
        }
        catch (SQLException ex){
            Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
            lgr.log(Level.SEVERE, ex.getMessage(), ex);
            throw new NoSuchEntity("No address or db error occurred");
        }
        return result;
    }

    public void addMultipleStateTx(ArrayList<MvpStateTx> txList){
        for(MvpStateTx tx: txList){
            String dbQuery = "INSERT INTO STX (hash, nonce, addressFrom, addressTo, value, signature, included, type, verified) VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT (hash) DO NOTHING";
            try{
                Connection c = DriverManager.getConnection(url + database, user, password);
                PreparedStatement pst = c.prepareStatement(dbQuery);

                pst.setBinaryStream(1, new ByteArrayInputStream(tx.getHash().getArray()));
                pst.setInt(2, tx.getNonce());
                pst.setBinaryStream(3, new ByteArrayInputStream(tx.getFrom().getBytes(StandardCharsets.UTF_8)));
                pst.setBinaryStream(4, new ByteArrayInputStream(tx.getTo().getBytes(StandardCharsets.UTF_8)));
                pst.setInt(5, tx.getValue());
                pst.setBinaryStream(6, new ByteArrayInputStream(tx.getSignature().serialize()));
                pst.setBoolean(7, tx.getIncluded());
                pst.setInt(8, tx.getRawType());
                pst.setBoolean(9, tx.getVerified());
                pst.execute();
                pst.close();
                c.close();
            }
            catch (SQLException ex){
                Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
                lgr.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    public void addMultipleRegistryTx(ArrayList<MvpRegistryTx> txList){
        for(MvpRegistryTx tx: txList){
            String dbQuery = "INSERT INTO RTX (hash, nonce, descriptor, included) VALUES (?,?,?,?) ON CONFLICT (hash) DO NOTHING";
            try{
                Connection c = DriverManager.getConnection(url + database, user, password);
                PreparedStatement pst = c.prepareStatement(dbQuery);

                pst.setBinaryStream(1, new ByteArrayInputStream(tx.getHash().getArray()));
                pst.setInt(2, tx.getNonce());
                pst.setBinaryStream(3, new ByteArrayInputStream(tx.getLedgerDescriptor().serialize()));
                pst.setBoolean(4,tx.getIncluded());
                pst.execute();
                pst.close();
                c.close();
            }
            catch (SQLException ex){
                Logger lgr = Logger.getLogger(ledgerDbPostgre.class.getName());
                lgr.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }
}
