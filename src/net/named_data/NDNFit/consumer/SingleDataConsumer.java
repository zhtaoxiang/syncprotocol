/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.consumer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.SecurityException;

/**
 *
 * @author zht
 */
public class SingleDataConsumer {

    public static Name originalDataPrefix
            = new Name("/org/openmhealth/haitao/raw/walking");
    public static Name groupedDataPrefix
            = new Name("/org/openmhealth/haitao/grouped/walking");
    public static Name confirmDataPrefix
            = new Name("/org/openmhealth/haitao/confirm/walking");
    public static String confirmContent = "confirm";

    public static void main(String[] args) throws SecurityException, IOException, InterruptedException, EncodingException {
        final Face face = new Face();
        
        Interest interest = new Interest(
                new Name(groupedDataPrefix).appendSequenceNumber(0));
        interest.setMustBeFresh(true);
        interest.setInterestLifetimeMilliseconds(1000);
        final OnTimeout onTimeout = new OnTimeout() {

            @Override
            public void onTimeout(Interest interest) {
                System.out.println("time out");
            }

        };
        OnData onData = new OnData() {

            @Override
            public void onData(Interest interest, Data data) {
                try {
                    face.expressInterest(interest, this, onTimeout);
                } catch (IOException ex) {
                    Logger.getLogger(SingleDataConsumer.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("receive data");
            }
        };
        
        face.expressInterest(interest, onData, onTimeout);

        while (true) {
            face.processEvents();
            Thread.sleep(5);
        }
    }
}
