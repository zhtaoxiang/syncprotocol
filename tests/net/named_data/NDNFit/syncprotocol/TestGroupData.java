/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.syncprotocol;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.named_data.NDNFit.sycnprotocol.Common;
import net.named_data.NDNFit.sycnprotocol.DataFaker;
import net.named_data.NDNFit.sycnprotocol.DataGrouper;
import net.named_data.NDNFit.sycnprotocol.InMemoryRepo;
import net.named_data.NDNFit.sycnprotocol.Repo;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author zht
 */
public class TestGroupData {

    Name originalDataPrefix = new Name("/org/openmhealth/haitao/raw/walking");
    Name groupedDataPrefix = new Name("/org/openmhealth/haitao/grouped/walking");

    @Test
    public void GroupAndReadData() throws InterruptedException {

        Repo repo = new InMemoryRepo();
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

        // wait 5 senconds for data grouping
        Thread.sleep(5000);

        long seq = 0;
        long content = 0;
        Name interestName = new Name(groupedDataPrefix)
                .appendSequenceNumber(seq);
        while (true) {
            Data groupedData = repo.readData(interestName);
            if (groupedData != null) {
                String groupedDataContent = groupedData.getContent().toString();
                String[] dataNames = groupedDataContent
                        .split(Common.INIT_NAME_SPLITTER);
                for (String dataName : dataNames) {
                    Name oneName = new Name(dataName);
                    Data oneData = repo.readOriginalData(oneName);
                    String originalDataContent = oneData.getContent().toString();
                    assertEquals(content++, Long.parseLong(originalDataContent));
                }
                interestName = new Name(groupedDataPrefix)
                        .appendSequenceNumber(++seq);
            }
            Thread.sleep(1000);
        }
    }
}

