/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.sycnprotocol;

import java.io.IOException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnTimeout;

/**
 *
 * @author zht
 */
public class ReceiveInterest implements OnInterestCallback {

    public ReceiveInterest(Repo repo, Face face) {
        this.repo = repo;
        this.face = face;
        onData = new ReceiveData(repo);
        onTimeout = new RequestDataTimeOut();
    }

    @Override
    public void onInterest(Name prefix, Interest interest, Face face,
            long interestFilterId, InterestFilter filter) {
        System.out.println("<< I: " + interest.toUri());
        try {
            Name dataName = interest.getName();
            Data data = repo.readData(dataName);
            if (data != null) {
                face.putData(data);
                System.out.println(">> D: " + data.getContent().toString());
                // If the cosumer requests for grouped data, then send Interest
                // for confirmation
                if (dataName.toUri().contains(Common.GROUP_DATA_COMPONENT)) {
                    Interest interestForConfirm = new Interest(new Name(
                            NetworkConnection.confirmDataPrefix)
                            .append(interest.getName().get(-1)));
                    interestForConfirm.setInterestLifetimeMilliseconds(4000);
                    interestForConfirm.setMustBeFresh(true);
                    // Wait some time for the remote client to take some action
                    Thread.sleep(100);
                    face.expressInterest(interestForConfirm, onData, onTimeout);
                    System.out.println(">> I: " + interestForConfirm.toUri());
                }
            }
        } catch (IOException | InterruptedException ex) {
            System.out.println("exception: " + ex.getMessage());
        }
    }

    private final Repo repo;
    private final Face face;
    private final OnData onData;
    private final OnTimeout onTimeout;
}
