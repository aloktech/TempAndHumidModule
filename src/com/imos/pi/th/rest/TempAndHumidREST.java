/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.pi.th.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.imos.pi.th.TempAndHumidModule;
import com.imos.pi.th.others.DatabaseList;
import static com.imos.pi.th.others.TempAndHumidConstant.DATA;
import static com.imos.pi.th.others.TempAndHumidConstant.FAILURE;
import static com.imos.pi.th.others.TempAndHumidConstant.STATUS;
import static com.imos.pi.th.others.TempAndHumidConstant.SUCCESS;
import static com.imos.pi.th.others.TempAndHumidConstant.TIME;
import com.imos.pi.th.others.TempAndHumidUtil;
import java.util.Date;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Pintu
 */
@Path("temphumid")
public class TempAndHumidREST {

    private final Logger LOGGER = LoggerFactory.getLogger(TempAndHumidREST.class);

    private final Injector injector = TempAndHumidModule.INJECTOR;

    private final ObjectMapper MAPPER = new ObjectMapper();

    private final DatabaseList databaseList;

    public TempAndHumidREST() {
        this.databaseList = injector.getInstance(DatabaseList.class);
    }

    @Inject
    public TempAndHumidREST(DatabaseList databaseList) {
        this.databaseList = databaseList;
    }

    @Path("data/day/{time}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOnedayData(@PathParam(value = TIME) Long time) {
        JSONObject status = new JSONObject();
        Date date = new Date();
        status.put("time", date);
        try {
            JSONArray arrayData = new JSONArray();
            arrayData = TempAndHumidUtil.listToJSONArray(databaseList, time, arrayData);
            status.put(DATA, arrayData);
            status.put(STATUS, SUCCESS);
            LOGGER.info("succeed to get a day data : " + date);
        } catch (Exception e) {
            status.put(STATUS, "FAILURE");
            LOGGER.error("failed to get a day data : " + new Date(time) + " " + e.getMessage());
        }

        return Response.status(Response.Status.CREATED).entity(status.toString()).build();
    }

    @Path("data/current")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentData() {
        JSONObject status = new JSONObject();
        Date date = new Date();
        status.put("time", date);
        try {
            status.put(DATA, new JSONObject(MAPPER.writeValueAsString(databaseList.getCurrentValue())));
            status.put(STATUS, SUCCESS);
            LOGGER.info("succeed to get currentData : " + date);
        } catch (JsonProcessingException | JSONException e) {
            status.put(STATUS, FAILURE);
            LOGGER.error("failed to get currentData " + date + " " + e.getMessage());
        }

        return Response.status(Response.Status.CREATED).entity(status.toString()).build();
    }
}
