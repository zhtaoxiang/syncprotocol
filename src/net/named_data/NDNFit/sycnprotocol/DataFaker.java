/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.sycnprotocol;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 *
 * @author zht
 */
public class DataFaker implements Runnable {

    public DataFaker(Repo repo, Name dataPrefix) {
        this.repo = repo;
        this.dataPrefix = dataPrefix;
        content = 0;
    }

    private void insertFakedDataWithinTheLastHalfHour() {
        long halfHourInMillis = TimeUnit.MINUTES.toMillis(30);
        long dataNumber = TimeUnit.MINUTES.toSeconds(30);
        long startTime = System.currentTimeMillis() - halfHourInMillis;

        while (content < dataNumber) {
            long timeStamp = startTime + TimeUnit.SECONDS.toMillis(content);
            Name name = new Name(dataPrefix).appendTimestamp(timeStamp);
            Data data = new Data();
            data.setName(name);
            data.setContent(new Blob(Long.toString(content)));
            repo.writeOriginalData(data);
            content++;
        }
    }

    @Override
    public void run() {
        insertFakedDataWithinTheLastHalfHour();
        while(true) {
            try {
                Thread.sleep((long)(5000 * Math.random()));
            } catch (InterruptedException ex) {
                Logger.getLogger(DataFaker.class.getName()).log(Level.SEVERE, null, ex);
            }
            Name name = new Name(dataPrefix).appendTimestamp(System.currentTimeMillis());
            Data data = new Data();
            data.setName(name);
            data.setContent(new Blob(Long.toString(content)));
            repo.writeOriginalData(data);
            content++;
        }
    }

    private Repo repo;
    private long content;
    private Name dataPrefix;
}

