package com.ibm.lab.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

@Component
public class CommandLineRunnerBean implements CommandLineRunner {
	
	public static String USER_DIR = System.getProperty("user.dir") + File.pathSeparator;
	
	@Value("${user.privateKeyFolder}")
	private String keyDir;
	
	private String keyFile;
	
	@Value("${user.signedCert}")
	private String certFile;
	
	@Value("${channel.channel_id}")
	private String channelName;	
	
	@Value("${members.user_id}")
	private String userName;
	
	@Value("${members.msp_id}")
	private String mspId;
	
	@Value("${members.peer_server_hostname}")
	private String peerName;
	
	@Value("${members.peer_url}")
	private String peerURL;
	
	@Value("${members.peer_tls_cacerts}")
	private String peerTLSCert;	
	
	@Value("${orderers.orderer_server_hostname}")
	private String ordererName;
	
	@Value("${orderers.orderer_url}")
	private String ordererURL;
	
	@Value("${orderers.orderer_tls_cacerts}")
	private String ordererTLSCert;

	@Value("${chaincode.chaincode_id}")
	private String chaincodeName;
	
	private static SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
    private static final Logger logger = LoggerFactory.getLogger(CommandLineRunnerBean.class);
    public void run(String... args) throws Exception {
        String strArgs = Arrays.stream(args).collect(Collectors.joining("|"));
        logger.info("==============Application started with arguments:" + strArgs);
        
        if (args.length > 0) {
        	if (args[0].equalsIgnoreCase("invoke")) {
        		invoke(args[0]);
        	} else if (args[0].equalsIgnoreCase("query")) {
        		invoke(args[0]);
        	}
        }
        //invoke();
    }


	public void invoke(String func) throws Exception{
        HFClient client = HFClient.createNewInstance();

        //load parameters

        logger.info("UserDir:{}, keyDir{}",USER_DIR , keyDir);
        String keyFile = getKeyFilesInDir(new File(keyDir)).toString();

        //create user object
        FabricUser user = new FabricUser(userName, mspId, keyFile, certFile);

        //encryption suite
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(user);

        //create channel object
        Channel channel = client.newChannel(channelName);

        //create peer
        Properties peer_properties = new Properties();
        peer_properties.put("pemBytes", Files.readAllBytes(Paths.get(peerTLSCert)));
        peer_properties.setProperty("sslProvider", "openSSL");
        peer_properties.setProperty("negotiationType", "TLS");
        Peer peer = client.newPeer(peerName, peerURL,peer_properties);
        channel.addPeer(peer);

        /*//create EvenHub
        String event_url = "grpcs://peer0.org1.richfit.com:7051"; // ensure that port is of event hub
        EventHub eventHub = client.newEventHub(peerName, event_url, peer_properties);
        channel.addEventHub(eventHub);*/

        //orderer
        Properties orderer_properties = new Properties();
        orderer_properties.put("pemBytes", Files.readAllBytes(Paths.get(ordererTLSCert)));
        orderer_properties.setProperty("sslProvider", "openSSL");
        orderer_properties.setProperty("negotiationType", "TLS");
        Orderer orderer = client.newOrderer(ordererName, ordererURL,orderer_properties);
        channel.addOrderer(orderer);

        //init channel
        channel.initialize();

        //query
        if (func.equalsIgnoreCase("query")) {
            queryByChaincode(client, channel);
        } else if (func.equalsIgnoreCase("invoke")) {
        	invokeByChaincode(client, channel);
        } 
	}
    /**
    * @description query
    * @params [client, configurations, channel]
    * @return  void
    * @author  adder
    * @date  2020/1/20 14:23
    *
    */
    public void queryByChaincode(HFClient client,Channel channel) throws FileNotFoundException, ProposalException, InvalidArgumentException, UnsupportedEncodingException {
        
        ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(chaincodeName).build();
        
        //build args
        ArrayList<String> argsList = new ArrayList<>();
        argsList.add("a");


        //build query request
        QueryByChaincodeRequest request = client.newQueryProposalRequest();
        request.setChaincodeID(chaincodeID);
        request.setFcn("query");
        request.setArgs(argsList);
        Collection<ProposalResponse> responses = channel.queryByChaincode(request);
        ProposalResponse response = (ProposalResponse) responses.toArray()[0];

        //analyse response
        if (response.getStatus().toString().equals("SUCCESS")){
        	logger.info("result status:{}",response.getChaincodeActionResponseStatus());
            String result = new String(response.getChaincodeActionResponsePayload(), StandardCharsets.UTF_8);
            logger.info("result value:{}",result);
        }
    }

    /**
    * @description
    * @params [client, configurations, channel, message]
    * @return  void
    * @author  adder
    * @date  2020/1/21 15:42
    *
    */
    public void invokeByChaincode(HFClient client,Channel channel) throws FileNotFoundException, InvalidArgumentException, ProposalException, ExecutionException, InterruptedException {
        
        ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(chaincodeName).build();
   
        //build args
        ArrayList<String> argsList = new ArrayList<>();
        argsList.add("a");
        argsList.add("b");
        argsList.add("10");
        
        //build insert request
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setChaincodeLanguage(TransactionRequest.Type.JAVA);
        request.setChaincodeID(chaincodeID);
        request.setArgs(argsList);
        request.setFcn("invoke");
        request.setProposalWaitTime(3000);

        Collection<ProposalResponse> responses = channel.sendTransactionProposal(request);
        for (ProposalResponse response : responses) {
            if (response.getChaincodeActionResponseStatus()==200){
            	logger.info("Successfully sent Proposal and received ProposalResponse: status:"+response.getChaincodeActionResponseStatus()+",TransactionInformation: "+response.getProposalResponse().getResponse().getPayload().toStringUtf8());
            }
        }
        CompletableFuture<TransactionEvent> transactionEvent =  channel.sendTransaction(responses);
        TransactionEvent event = transactionEvent.get();

        logger.info("TransationID: "+event.getTransactionID());
        if (event.isValid()){
        	logger.info("Successfully committed the change to the ledger by the peer "+event.getPeer());
        }
    }



    /**
    * @description get private key from key dir
    * @params [filePath]
    * @return  java.io.File
    * @author  adder
    * @date  2020/1/20 11:02
    *
    */
    private static File getKeyFilesInDir(File filePath) {
        File keyFile = null;
        File[] listFiles = filePath.listFiles();
        if(listFiles != null) {
            for(File file:listFiles) {
                if(file.isFile()) {
                    if(file.getName().endsWith("_sk")) {
                        keyFile = file;
                        break;
                    }
                }
            }
        }
        return keyFile;
    }
}
