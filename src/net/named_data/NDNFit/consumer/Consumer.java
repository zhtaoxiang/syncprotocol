/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.consumer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterest;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;
import net.named_data.jndn.transport.Transport;
import net.named_data.jndn.util.Blob;

/**
 *
 * @author zht
 */
public class Consumer {

    public static final Name originalDataPrefix
            = new Name("/org/openmhealth/haitao/raw/walking");
    public static final Name groupedDataPrefix
            = new Name("/org/openmhealth/haitao/grouped/walking");
    public static final Name confirmDataPrefix
            = new Name("/org/openmhealth/haitao/confirm/walking");
    public static final String confirmContent = "confirm";
    public static final Logger logger = Logger.getLogger(Consumer.class.getName());
    public static long seqNo = 0;
    public static final long interestLifeTimeOut = 10000;

    public static Map<Name, Data> confirmMap = new HashMap<>();

    public static void main(String[] args) throws net.named_data.jndn.security.SecurityException, IOException, InterruptedException, EncodingException {
        final Face face = new Face();

        // leave this for further test
        MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
        MemoryPrivateKeyStorage privateKeyStorage
                = new MemoryPrivateKeyStorage();
        final KeyChain keyChain = new KeyChain(
                new IdentityManager(identityStorage, privateKeyStorage),
                new SelfVerifyPolicyManager(identityStorage));
        Name identityName = new Name("/org/openmhealth/haitao/");
        Name keyName = keyChain.generateRSAKeyPairAsDefault(identityName);
        final Name certificateName = keyName.getSubName(0, keyName.size() - 1)
                .append("KEY").append(keyName.get(-1)).append("ID-CERT")
                .append("0");

        face.setCommandSigningInfo(keyChain, certificateName);
        // leave this for further test
        Interest interest = new Interest(
                new Name(groupedDataPrefix).appendSequenceNumber(seqNo++));
        interest.setMustBeFresh(true);
        interest.setInterestLifetimeMilliseconds(interestLifeTimeOut);
        final OnTimeout onTimeout = new OnTimeout() {

            @Override
            public void onTimeout(Interest interest) {
                System.out.println("time out for I: " + interest.toUri());
            }

        };

        OnRegisterFailed onRegisterFailed = new OnRegisterFailed() {

            @Override
            public void onRegisterFailed(Name prefix) {
                System.out.println("Failed to register prefix" + prefix.toUri());
            }

        };

        OnData onData = new OnData() {

            @Override
            public void onData(Interest interest, Data data) {
                System.out.println("<< D: " + data.getContent().toString());
                if (data.getName().toUri().contains("grouped")) {
                    //generate the confirmation data
                    Data confirmation = new Data();
                    confirmation.setName(new Name(confirmDataPrefix).append(data.getName().get(-1)));
                    confirmation.setContent(new Blob(confirmContent));
                    try {
                        keyChain.sign(confirmation, certificateName);
                        confirmMap.put(confirmation.getName(), confirmation);
                    } catch (SecurityException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }

                    //parse contents and fetch the corresponding data
                    String[] rawDataNames = data.getContent().toString().split("##");
                    System.out.println("The number of raw data names packed in this grouped data is " + rawDataNames.length);
                    for (String one : rawDataNames) {
                        Interest interestForRawData = new Interest(new Name(one));
                        interestForRawData.setMustBeFresh(true);
                        interestForRawData.setInterestLifetimeMilliseconds(interestLifeTimeOut);
                        try {
                            face.expressInterest(interestForRawData, this, onTimeout);
                            System.out.println(">> I: " + interestForRawData.toUri());
                        } catch (IOException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    }
                    //fetch the next grouped data packet
                    Interest nextGroupedInterest = new Interest(
                            new Name(groupedDataPrefix).appendSequenceNumber(seqNo++));
                    nextGroupedInterest.setMustBeFresh(true);
                    nextGroupedInterest.setInterestLifetimeMilliseconds(interestLifeTimeOut);
                    try {
                        face.expressInterest(nextGroupedInterest, this, onTimeout);
                        System.out.println(">> I: " + nextGroupedInterest.toUri());
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        };

        OnInterestCallback onInterestCallback = new OnInterestCallback() {

            @Override
            public void onInterest(Name prefix, Interest interest, Face face,
                    long interestFilterId, InterestFilter filter) {
                System.out.println("<< I: " + interest.toUri());
                Data data = confirmMap.get(interest.getName());
                if (data != null) {
                    System.out.println(">> D: " + data.getContent().toString());
                    try {
                        face.putData(data);
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }

        };

        face.registerPrefix(confirmDataPrefix, onInterestCallback, onRegisterFailed);

        face.expressInterest(interest, onData, onTimeout);
        System.out.println(">> I: " + interest.toUri());

        while (true) {
            face.processEvents();
            Thread.sleep(5);
        }
    }
}
