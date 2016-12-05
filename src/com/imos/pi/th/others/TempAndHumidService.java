package com.imos.pi.th.others;

import com.imos.pi.th.model.TempHumidData;
import com.imos.pi.th.model.TimeTempHumidData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.imos.common.utils.ProcessExecutor;
import static com.imos.pi.th.others.TempAndHumidConstant.CELCIUS;
import static com.imos.pi.th.others.TempAndHumidConstant.HUMIDITY;
import static com.imos.pi.th.others.TempAndHumidConstant.PERCENTAGE;
import static com.imos.pi.th.others.TempAndHumidConstant.TEMPERATURE;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pintu
 */
public class TempAndHumidService {

    private final Logger LOGGER = LoggerFactory.getLogger(TempAndHumidService.class);

    private String data;
    private double temp, humid;
    private final int tempLength = TEMPERATURE.length(), humidLength = HUMIDITY.length();

    private ProcessExecutor executor;

    private final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseFolder;

    private final DatabaseList databaseList;

    @Getter
    private final List<String> command;

    @Inject
    public TempAndHumidService(DatabaseList databaseList) {
        this.databaseList = databaseList;

        command = new ArrayList<>();
        command.add("sudo");
        command.add("python");
        command.add("/home/pi/Adafruit_Python_DHT/examples/AdafruitDHT.py");
        command.add("22");
        command.add("4");

        baseFolder = "/home/pi/NetBeansProjects/TempAndHumidModule/dist/";
    }

    public void executeTheSensor() {
        if (!System.getProperty("os.name").equals("Linux")) {
            LOGGER.info("OS is not Linux");
            return;
        }
        try {
            data = executeCommand(command);

            temp = Double.parseDouble(data.substring(tempLength, data.indexOf(CELCIUS)));
            humid = Double.parseDouble(data.substring(data.indexOf(HUMIDITY) + humidLength, data.indexOf(PERCENTAGE)));

            TempHumidData tempHumidData = new TempHumidData();
            tempHumidData.setHumidity(humid);
            tempHumidData.setTemperature(temp);

            TimeTempHumidData jsonData = new TimeTempHumidData();
            jsonData.setData(tempHumidData);
            jsonData.setTime(System.currentTimeMillis());
            jsonData.setDate(LocalDate.now().toString());

            databaseList.addData(jsonData);
            databaseList.setCurrentValue(jsonData);

            saveToLocalDB(jsonData);

            LOGGER.info(DateTimeFormatter.ofPattern("dd/MMM/yyyy").format(LocalDate.now()) + " " + DateTimeFormatter.ofPattern("hh:mm:ss a").format(LocalTime.now()));
        } catch (NumberFormatException | JSONException | StringIndexOutOfBoundsException e) {
            LOGGER.error(data + " : " + e.getMessage());
        }
    }

    public void saveDataAsJSON() throws IOException {
        String fileName;
        final JSONArray arrayData = new JSONArray();
        LocalDate today = LocalDate.now();

        fileName = today.minusDays(1).toString();
        long yesterdayTime = LocalTime.now().atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        File file = new File(baseFolder + fileName + ".json");

        if (file.exists()) {
            tempData = new ArrayList<>(Arrays.asList(MAPPER.readValue(file, TimeTempHumidData[].class)));
        } else {
            tempData = new ArrayList<>();
        }

        tempData.addAll(databaseList.getOneDayData(yesterdayTime));

        tempData.stream()
                .sorted((d1, d2) -> Long.compare(d1.getTime(), d2.getTime()))
                .distinct()
                .forEach(d -> {
                    try {
                        arrayData.put(new JSONObject(MAPPER.writeValueAsString(d)));
                    } catch (JsonProcessingException | JSONException e) {
                        LOGGER.error(e.getMessage());
                    }
                });

        try {
            Files.write(Paths.get(baseFolder + fileName + ".json"), arrayData.toString().getBytes(),
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
        }

        Files.deleteIfExists(Paths.get(fileName));

        List<TimeTempHumidData> list = DatabaseList.getInstance()
                .getAllData()
                .stream()
                .sorted((d1, d2) -> Long.compare(d1.getTime(), d2.getTime()))
                .collect(Collectors.toList());

        list.stream()
                .distinct()
                .forEach(d -> {
                    try {
                        arrayData.put(new JSONObject(MAPPER.writeValueAsString(d)));
                    } catch (JSONException | JsonProcessingException e) {
                        LOGGER.error(e.getMessage());
                    }
                });
        try {
            Files.write(Paths.get(baseFolder + "allData.json"), arrayData.toString().getBytes(),
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            LOGGER.info(DateTimeFormatter.ofPattern("dd_MM_yyyy").format(today.minusDays(1)) + " data is saved");
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
        }
    }
    List<TimeTempHumidData> tempData;

    private void saveToLocalDB(TimeTempHumidData data) {
        String fileName = LocalDate.now().toString();
        String filePath = baseFolder + fileName + ".json";
        File file = new File(filePath);
        try {
            if (file.exists()) {
                tempData = new ArrayList<>(Arrays.asList(MAPPER.readValue(file, TimeTempHumidData[].class)));
            } else {
                tempData = new ArrayList<>();
            }
            tempData.add(data);
            JSONArray arrayData = new JSONArray();
            tempData.stream()
                    .forEach(d -> {
                        try {
                            arrayData.put(new JSONObject(MAPPER.writeValueAsString(d)));
                        } catch (JsonProcessingException | JSONException e) {
                            LOGGER.error(e.getMessage());
                        }
                    });
            Files.write(Paths.get(filePath), arrayData.toString().getBytes(),
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    public String executeCommand(List<String> command) {
        String value = "";
        try {
            executor = new ProcessExecutor(command);
            value = executor.startExecution().getInputMsg();

        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
        }
        return value;
    }

}
