/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.imos.pi.th.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Pintu
 */
@Getter
@Setter
@JsonPropertyOrder({"data", "date", "time"})
@EqualsAndHashCode(exclude = {"data", "date"})
public class TimeTempHumidData implements Serializable, Comparable<TimeTempHumidData> {

    @JsonProperty("data")
    private TempHumidData data;

    @JsonProperty("time")
    private long time;

    @JsonProperty("date")
    private String date;

    @Override
    public int compareTo(TimeTempHumidData o) {
        if (o == this) {
            return 0;
        }
        return this.time < o.time ? -1 : 1;
    }
}
