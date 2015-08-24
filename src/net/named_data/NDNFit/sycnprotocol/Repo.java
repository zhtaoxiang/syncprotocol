/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.sycnprotocol;

import net.named_data.jndn.Data;
import net.named_data.jndn.Name;

/**
 *
 * @author zht
 */
public interface Repo {

    public Name[] getDataNameArray(Name prefix, long start, long duration);

    public void writeData(Data data);

    public void writeOriginalData(Data data);

    public void writeGroupedlData(Data data);

    public Data readData(Name name);
    
    public Data readOriginalData(Name name);
    
    public Data readGroupedData(Name name);

    public int getOriginalDataSize();

    public int getGroupedDataSize();

    public long getTheStartTime(Name originalDataPrefix);
}
