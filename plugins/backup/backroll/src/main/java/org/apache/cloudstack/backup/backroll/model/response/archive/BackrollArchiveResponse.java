package org.apache.cloudstack.backup.backroll.model.response.archive;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackrollArchiveResponse {
    @JsonProperty("archive")
    public String archive;

    @JsonProperty("barchive")
    public String barchive;

    @JsonProperty("id")
    public String id;

    @JsonProperty("name")
    public String name;

    @JsonProperty("start")
    public DateTime start;

    @JsonProperty("time")
    public DateTime time;
}
