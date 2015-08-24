/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.sycnprotocol;

import net.named_data.jndn.Name;
import net.named_data.jndn.OnRegisterFailed;

/**
 *
 * @author zht
 */
public class RegisterFailure implements OnRegisterFailed {

    @Override
    public void onRegisterFailed(Name prefix) {
        System.out.println("Failed to register prefix" + prefix.toUri());
    }

}
