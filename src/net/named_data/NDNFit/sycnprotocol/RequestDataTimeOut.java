/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.sycnprotocol;

import net.named_data.jndn.Interest;
import net.named_data.jndn.OnTimeout;

/**
 *
 * @author zht
 */
public class RequestDataTimeOut implements OnTimeout{

    @Override
    public void onTimeout(Interest interest) {
        System.out.println("Time out for interest " + interest.getName().toUri());
    }
    
}
