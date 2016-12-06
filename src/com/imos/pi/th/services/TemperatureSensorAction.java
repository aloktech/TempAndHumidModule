/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.pi.th.services;

import com.imos.pi.th.services.TempAndHumidService;
import com.google.inject.Inject;
import com.imos.common.utils.Scheduler;
import com.imos.common.utils.SchedulerTask;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alok Ranjan
 */
public class TemperatureSensorAction {

    private final Logger LOGGER = LoggerFactory.getLogger(TemperatureSensorAction.class);

    private final TempAndHumidService service;

    @Inject
    public TemperatureSensorAction(TempAndHumidService service) {
        this.service = service;
    }

    public void detectSensorSignalInEveryMinutes() {
        try {
            Calendar calendar = GregorianCalendar.getInstance();
            int minute = calendar.get(Calendar.MINUTE) + 2;
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
            delay = delay < 0 ? 0 : delay;

            new Timer().scheduleAtFixedRate(new SchedulerTask<>((o) -> {
                o.executeTheSensor();
            }, service),
                    delay,
                    getDelayed(new Scheduler.SchedulerBuilder()
                            .minute("1")
                            .build()));
            LOGGER.info("Task detectSensorSignalInEveryMinutes scheduled");
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    public void saveDataAsJSONIn24Hours() {
        try {

            Calendar todayTime = GregorianCalendar.getInstance();
            int today = todayTime.get(Calendar.DAY_OF_MONTH);
            int tomorrow = today + 1;

            Calendar tomorrowTime = GregorianCalendar.getInstance();
            tomorrowTime.set(Calendar.DAY_OF_MONTH, tomorrow);
            tomorrowTime.set(Calendar.HOUR_OF_DAY, 0);
            tomorrowTime.set(Calendar.MINUTE, 0);
            tomorrowTime.set(Calendar.SECOND, 0);
            tomorrowTime.set(Calendar.MILLISECOND, 0);

            long delay = tomorrowTime.getTimeInMillis() - todayTime.getTimeInMillis();

            delay = delay < 0 ? 0 : delay;

            new Timer().scheduleAtFixedRate(new SchedulerTask<>((o) -> {
                try {
                    o.saveDataAsJSON();
                } catch (IOException ex) {
                    LOGGER.error(ex.getMessage());
                }
            }, service),
                    delay,
                    new Scheduler.SchedulerBuilder()
                            .hour("24")
                            .build()
                            .getDelayed());
            LOGGER.info("Task saveDataAsJSONIn24Hours scheduled");
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    public long getDelayed(Scheduler scheduler) {
        long time = 0, interval = 1000L;
        if (!scheduler.getMilliSecond().equals("0")) {
            time += Long.parseLong(scheduler.getMilliSecond());
        }
        if (!scheduler.getSecond().equals("0")) {
            time += interval * Long.parseLong(scheduler.getSecond());
        }
        if (!scheduler.getMinute().equals("0")) {
            time += interval * 60 * Long.parseLong(scheduler.getMinute());
        }
        if (!scheduler.getHour().equals("0")) {
            time += interval * 60 * 60 * Long.parseLong(scheduler.getHour());
        }
        return time;
    }

}
