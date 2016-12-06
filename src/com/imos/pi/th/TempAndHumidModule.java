package com.imos.pi.th;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imos.pi.th.services.TemperatureSensorAction;
import com.imos.pi.th.services.TempAndHumidJettyServer;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.imos.pi.th.model.TimeTempHumidData;
import com.imos.pi.th.utils.DatabaseList;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pintu
 */
public class TempAndHumidModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempAndHumidModule.class);

    public static final Injector INJECTOR = Guice.createInjector(new AbstractModule() {
        @Override
        protected void configure() {
        }
    });

    private final DatabaseList databaseList;
    private final TemperatureSensorAction action;
    private final TempAndHumidJettyServer server;

    @Inject
    public TempAndHumidModule(DatabaseList databaseList, TemperatureSensorAction action, TempAndHumidJettyServer server) {
        this.databaseList = databaseList;
        this.action = action;
        this.server = server;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            LOGGER.info("STARTED ! ...");

            new Thread(() -> {
                try {
                    String baseFolder = "/home/pi/NetBeansProjects/TempAndHumidModule/dist/";
                    long time = lastAccessTime(baseFolder);
                    List<String> status = Files.readAllLines(Paths.get(baseFolder + "status.txt"), Charset.defaultCharset());
                    status.add("Stop  : " + DateTimeFormatter.ofPattern("dd/MMM/yyyy").format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()) + " "
                            + DateTimeFormatter.ofPattern("hh:mm:ss a").format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalTime()));
                    String timeStr = "Start : " + DateTimeFormatter.ofPattern("dd/MMM/yyyy").format(LocalDate.now()) + " " + DateTimeFormatter.ofPattern("hh:mm:ss a").format(LocalTime.now());
                    status.add(timeStr);
                    String data = status.stream()
                            .collect(Collectors.joining("\n"));
                    Files.write(Paths.get(baseFolder + "status.txt"), data.getBytes(), StandardOpenOption.CREATE);
                } catch (IOException ex) {
                    java.util.logging.Logger.getLogger(TempAndHumidModule.class.getName()).log(Level.SEVERE, null, ex);
                }
            }).start();

            TempAndHumidModule module = INJECTOR.getInstance(TempAndHumidModule.class);

            module.configure();

            LOGGER.info("CONTINUES !  ...");
        } catch (Exception ex) {
            LOGGER.info("Failed to start!  ..." + ex.getMessage());
        }
    }

    private void configure() {
        Executors.newSingleThreadExecutor().execute(() -> {
            databaseList.uploadData();
        });

        action.detectSensorSignalInEveryMinutes();
        action.saveDataAsJSONIn24Hours();

        server.configure();
        server.start();
    }

    private static long lastAccessTime(String filePath) {
        long maxTime = 0;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(filePath))) {
            ObjectMapper MAPPER = new ObjectMapper();

            for (Path path : directoryStream) {
                File file = path.toFile();
                if (file.isFile() && file.getName().endsWith(".json") && file.getName().matches("\\d{4}-\\d{2}-\\d{2}.json")) {
                    TimeTempHumidData[] arrays = MAPPER.readValue(file, TimeTempHumidData[].class);
                    long time = arrays[arrays.length - 1].getTime();
                    if (maxTime < time) {
                        maxTime = time;
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return maxTime;
    }
}
