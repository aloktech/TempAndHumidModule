///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package com.imos.pi.th.others;
//
//import com.imos.pi.th.model.TimeTempHumidData;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.inject.Inject;
//import com.google.inject.Singleton;
//import java.io.File;
//import java.io.IOException;
//import java.time.Instant;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Calendar;
//import java.util.Collections;
//import java.util.Date;
//import java.util.GregorianCalendar;
//import java.util.List;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//import java.util.stream.Collectors;
//import lombok.Getter;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// *
// * @author Pintu
// */
//@Singleton
//public final class DatabaseListBackup {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseListBackup.class);
//
//    private final static List<TimeTempHumidData> ALL_DATA = Collections.synchronizedList(new ArrayList<>());
//
//    private static final ConcurrentMap<MonthlyClockTime, List<TimeTempHumidData>> ALL_DATAs = new ConcurrentHashMap<>();
//
//    private static final ConcurrentMap<String, TimeTempHumidData> CURRENT_MAP = new ConcurrentHashMap<>();
//
//    public static final ConcurrentMap<String, ListDayIndex> DAY_LIST_INDEX_MAP = new ConcurrentHashMap<>();
//
//    public final Calendar CALENDAR = GregorianCalendar.getInstance();
//
//    private static DatabaseListBackup INSTANCE;
//
//    private final ObjectMapper MAPPER = new ObjectMapper();
//
//    private final String baseFolder;
//
//    @Inject
//    public DatabaseListBackup() {
//        baseFolder = "/home/pi/NetBeansProjects/TempAndHumidModule/dist/";
//    }
//
//    public void uploadData() {
//        TimeTempHumidData[] array;
//        try {
//            array = MAPPER.readValue(new File(baseFolder + "allData.json"), TimeTempHumidData[].class);
//            for (TimeTempHumidData value : array) {
//                addData(value);
//            }
//            LOGGER.info("All data are uploaded");
//        } catch (IOException ex) {
//            LOGGER.error(ex.getMessage());
//        }
//
//        String fileName = LocalDate.now().toString();
//        try {
//            array = MAPPER.readValue(new File(baseFolder + fileName + ".json"), TimeTempHumidData[].class);
//            for (TimeTempHumidData value : array) {
//                addData(value);
//            }
//            LOGGER.info("Today data is uploaded");
//        } catch (IOException ex) {
//            LOGGER.error(ex.getMessage());
//        }
//    }
//
//    public int size() {
//        return ALL_DATA.size();
//    }
//
//    public static DatabaseListBackup getInstance() {
//        if (INSTANCE == null) {
//            INSTANCE = new DatabaseListBackup();
//        }
//
//        return INSTANCE;
//    }
//
//    public void setCurrentValue(TimeTempHumidData data) {
//        CURRENT_MAP.put("CURRENT", data);
//    }
//
//    public TimeTempHumidData getCurrentValue() {
//        return CURRENT_MAP.get("CURRENT") == null ? new TimeTempHumidData() : CURRENT_MAP.get("CURRENT");
//    }
//
//    public void addData(TimeTempHumidData data) {
//
//        synchronized (ALL_DATA) {
//            CALENDAR.setTimeInMillis(data.getTime());
//            MonthlyClockTime mct = new MonthlyClockTime(CALENDAR.get(Calendar.HOUR_OF_DAY), 
//                    CALENDAR.get(Calendar.MINUTE), CALENDAR.getTime());
//            List<TimeTempHumidData> list = ALL_DATAs.get(mct);
//            if (list == null) {
//                list = new ArrayList<>();
//                ALL_DATAs.put(mct, list);
//            }
//            list.add(data);
//            if (!ALL_DATA.contains(data)) {
//                ALL_DATA.add(data);
//
//                int size = ALL_DATA.size();
//
//                ListDayIndex listDayIndex = DAY_LIST_INDEX_MAP.get(data.getDate());
//
//                if (listDayIndex == null) {
//                    listDayIndex = new ListDayIndex();
//                    listDayIndex.setStartIndex(size - 1);
//                }
//                listDayIndex.setEndIndex(size - 1);
//                DAY_LIST_INDEX_MAP.put(data.getDate(), listDayIndex);
//            }
//        }
//
//    }
//
//    public List<TimeTempHumidData> getOneDayData(Long time) {
//        String date = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate().toString();
//
//        ListDayIndex listDayIndex = DAY_LIST_INDEX_MAP.get(date);
//
//        if (listDayIndex != null) {
//            List<TimeTempHumidData> TEMP_ALL_DATA = new ArrayList<>(ALL_DATA);
//            List<TimeTempHumidData> dayList = new ArrayList<>();
//            dayList.addAll(TEMP_ALL_DATA.subList(listDayIndex.getStartIndex(), listDayIndex.getEndIndex()));
//            LOGGER.info("START : " + listDayIndex.getStartIndex() + " END : " + listDayIndex.getEndIndex()
//                    + " DIFF : " + (listDayIndex.getEndIndex() - listDayIndex.getStartIndex()) + " Date : " + CALENDAR.getTime());
//            try {
//                dayList.addAll(Arrays.asList(MAPPER.readValue(new File(baseFolder + date + ".json"),
//                        TimeTempHumidData[].class)));
//            } catch (IOException ex) {
//                LOGGER.error("failed to get one day data : " + ex.getMessage());
//            }
//            return dayList
//                    .stream()
//                    .distinct()
//                    .sorted((d1, d2) -> Long.compare(d1.getTime(), d2.getTime()))
//                    .collect(Collectors.toList());
//        } else {
//            return new ArrayList<>();
//        }
//    }
//
//    public List<TimeTempHumidData> getAllData() {
//        synchronized (ALL_DATA) {
//            ALL_DATA.sort((d1, d2) -> Long.compare(d1.getTime(), d2.getTime()));
//        }
//
//        return ALL_DATA;
//    }
//
//    public ListDayIndex getDayIndex(Long dayTime) {
//        return DAY_LIST_INDEX_MAP.get(Instant.ofEpochMilli(dayTime).atZone(ZoneId.systemDefault()).toLocalDate().toString());
//    }
//}
//
//@Getter
//class MonthlyClockTime {
//
//    int hour;
//    int minute;
//    Date date;
//
//    public MonthlyClockTime(int hour, int minute, Date date) {
//        this.hour = hour;
//        this.minute = minute;
//        this.date = date;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (obj == this) {
//            return true;
//        }
//        if (obj instanceof MonthlyClockTime) {
//            MonthlyClockTime ct = (MonthlyClockTime) obj;
//            return ct.getMinute() == this.getMinute() && ct.getHour() == this.getHour() && ct.getDate().equals(this.getDate());
//        }
//        return false;
//    }
//
//    @Override
//    public int hashCode() {
//        int hash = 7;
//        hash = 29 * hash + this.hour;
//        hash = 29 * hash + this.minute;
//        return hash;
//    }
//
//    public Date getDate() {
//        return date;
//    }
//
//    public int getHour() {
//        return hour;
//    }
//
//    public int getMinute() {
//        return minute;
//    }
//
//}
