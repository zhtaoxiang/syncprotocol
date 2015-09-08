/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.sycnprotocol;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;

/**
 *
 * @author zht
 */
public class NetworkConnection {

    public static Name originalDataPrefix 
            = new Name("/org/openmhealth/haitao/raw/walking");
    public static Name groupedDataPrefix 
            = new Name("/org/openmhealth/haitao/grouped/walking");
    public static Name confirmDataPrefix 
            = new Name("/org/openmhealth/haitao/confirm/walking");

    public static void main(String[] args) throws InterruptedException {
        // Create repo and write faked data in.
        Repo repo = new InMemoryRepo();

        // Periodically group data
        ScheduledExecutorService scheduler
                = Executors.newScheduledThreadPool(2);

        // Write faked data into repo
        DataFaker dataFacker = new DataFaker(repo, originalDataPrefix);
        scheduler.schedule(dataFacker, 0, TimeUnit.MILLISECONDS);

        // wait 5 senconds for data inserting
        Thread.sleep(5000);

        // Periodically group data
        DataGrouper walkingDataGrouper = new DataGrouper(repo,
                originalDataPrefix, groupedDataPrefix);
        scheduler.scheduleAtFixedRate(
                walkingDataGrouper, 2, 150, TimeUnit.SECONDS);

        // deal with data requests
        try {
            Face face = new Face();

            MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
            MemoryPrivateKeyStorage privateKeyStorage
                    = new MemoryPrivateKeyStorage();
            KeyChain keyChain = new KeyChain(
                    new IdentityManager(identityStorage, privateKeyStorage),
                    new SelfVerifyPolicyManager(identityStorage));
            Name identityName = new Name("/org/openmhealth/haitao/");
            Name keyName = keyChain.generateRSAKeyPairAsDefault(identityName);
            Name certificateName = keyName.getSubName(0, keyName.size() - 1)
                    .append("KEY").append(keyName.get(-1)).append("ID-CERT")
                    .append("0");

            face.setCommandSigningInfo(keyChain, certificateName);
            
            ReceiveInterest receiveInterest = new ReceiveInterest(repo, face);
            RegisterFailure registerFailure = new RegisterFailure();

            face.registerPrefix(originalDataPrefix, receiveInterest,
                    registerFailure);
            face.registerPrefix(groupedDataPrefix, receiveInterest,
                    registerFailure);

            while (true) {
                face.processEvents();
                Thread.sleep(5);
            }

        } catch (SecurityException | IOException | EncodingException 
                | InterruptedException e) {
            System.out.println("exception: " + e.getMessage());
        }
    }
}
