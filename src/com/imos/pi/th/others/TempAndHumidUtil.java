/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.pi.th.others;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pintu
 */
public class TempAndHumidUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempAndHumidUtil.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static JSONArray listToJSONArray(DatabaseList databaseList, Long time, JSONArray arrayData) throws Exception {
        databaseList.getOneDayData(time)
                .stream()
                .distinct()
                .forEach(d -> {
                    try {
                        arrayData.put(new JSONObject(MAPPER.writeValueAsString(d)));
                    } catch (JsonProcessingException | JSONException e) {
                        LOGGER.error(e.getMessage());
                    }
                });
        if (arrayData.length() == 0) {
            throw new Exception("Empty");
        }
        return arrayData;
    }
}
