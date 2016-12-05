/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.pi.th.others;

import com.imos.pi.th.model.TimeTempHumidData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pintu
 */
@Singleton
public final class DatabaseList {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseList.class);

    private static final ConcurrentMap<MonthlyClockTime, Set<TimeTempHumidData>> ALL_DATA = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, TimeTempHumidData> CURRENT_MAP = new ConcurrentHashMap<>();

    public static final ConcurrentMap<String, ListDayIndex> DAY_LIST_INDEX_MAP = new ConcurrentHashMap<>();

    public final Calendar CALENDAR = GregorianCalendar.getInstance();

    private static DatabaseList INSTANCE;

    private final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseFolder;

    @Inject
    public DatabaseList() {
        baseFolder = "/home/pi/NetBeansProjects/TempAndHumidModule/dist/";
    }

    public void uploadData() {
        TimeTempHumidData[] array;
        try {
            array = MAPPER.readValue(new File(baseFolder + "allData.json"), TimeTempHumidData[].class);
            for (TimeTempHumidData value : array) {
                addData(value);
            }
            LOGGER.info("All data are uploaded");
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
        }

        String fileName = LocalDate.now().toString();
        try {
            array = MAPPER.readValue(new File(baseFolder + fileName + ".json"), TimeTempHumidData[].class);
            for (TimeTempHumidData value : array) {
                addData(value);
            }
            LOGGER.info("Today data is uploaded");
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    public int size() {
        return ALL_DATA.size();
    }

    public static DatabaseList getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DatabaseList();
        }

        return INSTANCE;
    }

    public void setCurrentValue(TimeTempHumidData data) {
        CURRENT_MAP.put("CURRENT", data);
    }

    public TimeTempHumidData getCurrentValue() {
        return CURRENT_MAP.get("CURRENT") == null ? new TimeTempHumidData() : CURRENT_MAP.get("CURRENT");
    }

    public void addData(TimeTempHumidData data) {

        synchronized (ALL_DATA) {
            CALENDAR.setTimeInMillis(data.getTime());
            MonthlyClockTime mct = new MonthlyClockTime(CALENDAR.get(Calendar.HOUR_OF_DAY),
                    CALENDAR.get(Calendar.MINUTE),
                    CALENDAR.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            Set<TimeTempHumidData> list = ALL_DATA.get(mct);
            if (list == null) {
                list = new HashSet<>();
                ALL_DATA.put(mct, list);
            }
            list.add(data);
        }
    }

    public List<TimeTempHumidData> getOneDayData(Long time) {

        CALENDAR.setTimeInMillis(time);

        CALENDAR.set(Calendar.HOUR_OF_DAY, 0);
        CALENDAR.set(Calendar.MINUTE, 0);
        CALENDAR.set(Calendar.SECOND, 0);
        CALENDAR.set(Calendar.MILLISECOND, 0);

        MonthlyClockTime startMCT = new MonthlyClockTime(CALENDAR.get(Calendar.HOUR_OF_DAY),
                CALENDAR.get(Calendar.MINUTE),
                CALENDAR.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        CALENDAR.set(Calendar.HOUR_OF_DAY, 23);
        CALENDAR.set(Calendar.MINUTE, 59);
        CALENDAR.set(Calendar.SECOND, 59);
        CALENDAR.set(Calendar.MILLISECOND, 999);

        MonthlyClockTime endMCT = new MonthlyClockTime(CALENDAR.get(Calendar.HOUR_OF_DAY),
                CALENDAR.get(Calendar.MINUTE),
                CALENDAR.getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        return ALL_DATA.entrySet()
                .stream()
                .filter(p -> (p.getKey().getHour() >= startMCT.getHour()
                && p.getKey().getHour() <= endMCT.getHour()
                && p.getKey().getMinute() >= startMCT.getMinute()
                && p.getKey().getMinute() <= endMCT.getMinute()
                && p.getKey().getDate().equals(startMCT.getDate())
                && p.getKey().getDate().equals(endMCT.getDate())))
                .map(m -> {
                    return m.getValue().iterator().next();
                })
                .sorted()
                .collect(Collectors.toList());
    }

    public List<TimeTempHumidData> getAllData() {

        return ALL_DATA.entrySet()
                .stream()
                .map(m -> {
                    return m.getValue().iterator().next();
                })
                //                .sorted()
                .collect(Collectors.toList());
    }

    public ListDayIndex getDayIndex(Long dayTime) {
        return DAY_LIST_INDEX_MAP.get(Instant.ofEpochMilli(dayTime).atZone(ZoneId.systemDefault()).toLocalDate().toString());
    }
}

@Getter
class MonthlyClockTime implements Comparable<MonthlyClockTime> {

    private final int hour;
    private final int minute;
    private final LocalDate date;

    public MonthlyClockTime(int hour, int minute, LocalDate date) {
        this.hour = hour;
        this.minute = minute;
        this.date = date;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MonthlyClockTime) {
            MonthlyClockTime ct = (MonthlyClockTime) obj;
            return ct.getMinute() == this.getMinute() && ct.getHour() == this.getHour() && ct.getDate().equals(this.getDate());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + this.hour;
        hash = 29 * hash + this.minute;
        return hash;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    @Override
    public int compareTo(MonthlyClockTime ct) {
        if (ct == this) {
            return 0;
        }
        if (ct.getDate().equals(this.getDate())) {
            if (ct.getHour() == this.getHour()) {
                return ct.getMinute() - this.getMinute();
            } else {
                return ct.getHour() - this.getHour();
            }
        } else {
            return ct.getDate().compareTo(this.getDate());
        }
    }

}
