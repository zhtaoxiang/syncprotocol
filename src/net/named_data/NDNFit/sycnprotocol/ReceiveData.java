/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.sycnprotocol;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;

/**
 *
 * @author zht
 */
public class ReceiveData implements OnData {
    
    public ReceiveData(Repo repo) {
        this.repo  = repo;
    }

    @Override
    public void onData(Interest interest, Data data) {
        System.out.println("<< D: " + data.getName().toUri());
        // Delete grouped data
        
        //
    }

    private Repo repo;
}
