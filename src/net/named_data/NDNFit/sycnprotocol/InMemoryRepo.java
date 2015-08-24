/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.named_data.NDNFit.sycnprotocol;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;

/**
 *
 * @author zht
 */
public class InMemoryRepo implements Repo {

    private TimeStampedName toTimeStampedName(Name name, boolean hasTimeStamp) {
        return new TimeStampedName(name, hasTimeStamp);
    }

    @Override
    public Name[] getDataNameArray(Name prefix, long startTime, long duration) {
        if (prefix.toUri().contains(Common.GROUP_DATA_COMPONENT)) {
            return null;
        }
        Name startName = new Name(prefix).appendTimestamp(startTime);
        Name endName = new Name(prefix).appendTimestamp(startTime + duration);
        Object[] result
                = originalData.subMap(toTimeStampedName(startName, true),
                        toTimeStampedName(endName, true)).keySet().toArray();
        TimeStampedName[] convertedResult
                = Arrays.copyOf(result, result.length, TimeStampedName[].class);
        int length = convertedResult.length;
        Name namesResult[] = new Name[length];
        for (int i = 0; i < length; i++) {
            namesResult[i] = new Name(convertedResult[i].getPrefix())
                    .appendTimestamp(convertedResult[i].getTimeStamp());
        }
        return namesResult;
    }

    @Override
    public void writeData(Data data) {
        Name name = data.getName();
        if (name.toUri().contains(Common.GROUP_DATA_COMPONENT)) {
            groupedData.put(name, data);
        } else {
            originalData.put(toTimeStampedName(name, true), data);
        }
    }

    @Override
    public Data readData(Name name) {
        if (name.toUri().contains(Common.GROUP_DATA_COMPONENT)) {
            return groupedData.get(name);
        } else {
            return originalData.get(toTimeStampedName(name, true));
        }
    }

    @Override
    public long getTheStartTime(Name originalDataPrefix) {
        return originalData.ceilingKey(
                toTimeStampedName(originalDataPrefix, false)).getTimeStamp();
//        Name firstDataName = originalData.ceilingKey(toTimeStampedName(originalDataPrefix));
//        if (firstDataName == null) {
//            return 0;
//        }
//        Component timeStampComponent = firstDataName.get(-1);
//        try {
//            return timeStampComponent.toTimestamp();
//        } catch (EncodingException ex) {
//            Logger.getLogger(InMemoryRepo.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return 0;
    }

    @Override
    public int getOriginalDataSize() {
        return originalData.size();
    }

    @Override
    public int getGroupedDataSize() {
        return groupedData.size();
    }

    @Override
    public void writeOriginalData(Data data) {
        originalData.put(toTimeStampedName(data.getName(), true), data);
    }

    @Override
    public void writeGroupedlData(Data data) {
        groupedData.put(data.getName(), data);

    }

    @Override
    public Data readOriginalData(Name name) {
        return originalData.get(toTimeStampedName(name, true));
    }

    @Override
    public Data readGroupedData(Name name) {
        return groupedData.get(name);
    }

    private final ConcurrentSkipListMap<TimeStampedName, Data> originalData
            = new ConcurrentSkipListMap<>();

    private final ConcurrentSkipListMap<Name, Data> groupedData
            = new ConcurrentSkipListMap<>();
}

/**
 *
 * Since Name uses NDN canonical ordering to do comparison, it will give wrong
 * order for the timestamp, so I write this for the InMemoryRepo.
 *
 * Other practical repos also need to consider the same problem.
 *
 * @author zht
 */
class TimeStampedName implements Comparable {

    public TimeStampedName(Name name, boolean hasTimeStamp) {
        if (hasTimeStamp) {

            try {
                this.timeStamp = name.get(-1).toTimestamp();
                this.prefix = name.getPrefix(-1);
            } catch (EncodingException e) {
                this.prefix = name;
                this.timeStamp = 0;
            }
        } else {
            this.prefix = name;
            this.timeStamp = 0;
        }
    }

    public Name getPrefix() {
        return prefix;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public int compareTo(Object o) {
        TimeStampedName other = (TimeStampedName) (o);
        if (prefix.equals(other.getPrefix())) {
            if (timeStamp < other.getTimeStamp()) {
                return -1;
            }
            if (timeStamp > other.getTimeStamp()) {
                return 1;
            }
            return 0;
        } else {
            return prefix.compareTo(other.prefix);
        }
    }

    private Name prefix;
    private long timeStamp;
}
