package com.ibm.lab.demo;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Set;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoPrimitives;


/**
 * @author adder
 * @date 2020/1/20 15:43
 */
public class FabricUser implements User {
    private String name;
    private String mspId;
    private Enrollment enrollment;
    private String keyFile;
    private String certFile;

    FabricUser(String name, String mspId, String keyFile, String certFile) {
        this.name = name;
        this.mspId = mspId;
        this.keyFile=keyFile;
        this.certFile=certFile;

        try{
            enrollment=loadFromPemFile(keyFile, certFile);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }
    
    

    private Enrollment loadFromPemFile(String keyFile,String certFile) throws Exception{
        byte[] keyPem = Files.readAllBytes(Paths.get(keyFile));     //load private key text
        byte[] certPem = Files.readAllBytes(Paths.get(certFile));   //load certificate text
        CryptoPrimitives suite = new CryptoPrimitives();            //load the cryptography suite
        PrivateKey privateKey = suite.bytesToPrivateKey(keyPem);    //convert private key text to object
        return new X509Enrollment(privateKey,new String(certPem));  //create X509Enrollment object
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getMspId() {
        return mspId;
    }

    @Override
    public Enrollment getEnrollment() {
        return enrollment;
    }

    @Override
    public String getAccount() {
        return null;
    }

    @Override
    public String getAffiliation() {
        return null;
    }

    @Override
    public Set<String> getRoles() {
        return null;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public String getCertFile() {
        return certFile;
    }

    public void setCertFile(String certFile) {
        this.certFile = certFile;
    }

}
