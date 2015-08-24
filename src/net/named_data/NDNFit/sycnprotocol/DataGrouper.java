/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.sycnprotocol;

import java.util.concurrent.TimeUnit;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 * For each data type, a DataGrouper should be created and initiated, and it
 * should be called periodically to do Data grouping.
 *
 * @author zht
 */
public class DataGrouper implements Runnable {

    public static final long INIT_GROUP_TIME_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    public static final int INIT_GROUP_SIZE = 100;

    public DataGrouper(Repo repo, Name originalDataPrefix,
            Name groupedDataPrefix) {
        this.repo = repo;
        this.origialDataPrefix = originalDataPrefix;
        this.groupedDataPreifx = groupedDataPrefix;
        timeInterval = INIT_GROUP_TIME_INTERVAL;
        groupSize = INIT_GROUP_SIZE;
        nameSplitter = Common.INIT_NAME_SPLITTER;
    }

    public long getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(long timeInterval) {
        this.timeInterval = timeInterval;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(int groupSize) {
        this.groupSize = groupSize;
    }

    public String getNameSplitter() {
        return nameSplitter;
    }

    public void setNameSplitter(String nameSplitter) {
        this.nameSplitter = nameSplitter;
    }

    public long getSeq() {
        return seq;
    }

    public long getLastRunTime() {
        return lastRunTime;
    }

    public Name getOrigialDataPrefix() {
        return origialDataPrefix;
    }

    public Name getGroupedDataPreifx() {
        return groupedDataPreifx;
    }

    public void groupData() {
        System.out.println("GroupData is called");
        if (lastRunTime == 0) {
            //This indicates that it is the first time to run the code, do some
            //initiation
            long theStartTime = repo.getTheStartTime(origialDataPrefix);
            if (theStartTime == 0) { // No such type of data
                return;
            }
            lastRunTime = (theStartTime / timeInterval) * timeInterval;
        }
        // Group all the data till the timepoint (this timepoint should be a 
        // multiple of timeInterval) before the current time
        while (lastRunTime + timeInterval < System.currentTimeMillis()) {
            // get all the data falling in the time interval and 
            Name[] dataNameArray = repo.getDataNameArray(origialDataPrefix, lastRunTime,
                    timeInterval);
            lastRunTime += timeInterval;

            int size = dataNameArray.length;
            // no data falls into this time interval
            if (size == 0) {
                return;
            }

            // group the data and write into the repo
            for (int i = 0; i < size; i = i + groupSize) {
                // prepare the content
                StringBuilder stringBuilder
                        = new StringBuilder(dataNameArray[i].toUri());
                int thisGroupSize = i + groupSize < size ? groupSize : size - i;
                for (int j = 1; j < thisGroupSize; j++) {
                    stringBuilder.append(nameSplitter)
                            .append(dataNameArray[i + j].toUri());
                }
                //generate name and data packet
                Data groupedData = new Data();

                Name groupedDataName = new Name(groupedDataPreifx)
                        .appendSequenceNumber(seq++);
                groupedData.setName(groupedDataName);

                String content = stringBuilder.toString();
                groupedData.setContent(new Blob(content));

                repo.writeData(groupedData);
            }
        }
    }

    @Override
    public void run() {
        groupData();
    }

    private Repo repo;
    private long timeInterval;
    private int groupSize;
    private String nameSplitter;
    private Name origialDataPrefix;
    private Name groupedDataPreifx;

    private long seq = 0;
    // What is the last time to group the data
    private long lastRunTime = 0;
}
