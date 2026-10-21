// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.veeam.api.dto;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Statistic extends BaseDto {
    public static final String KIND_GAUGE = "gauge";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_DECIMAL = "decimal";
    public static final String UNIT_BYTES = "bytes";
    public static final String UNIT_PERCENT = "percent";

    private String name;
    private String description;
    private String kind;
    private String type;
    private String unit;
    private NamedList<Value> values;
    private Ref host;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public NamedList<Value> getValues() {
        return values;
    }

    public void setValues(NamedList<Value> values) {
        this.values = values;
    }

    public Ref getHost() {
        return host;
    }

    public void setHost(Ref host) {
        this.host = host;
    }

    /**
     * oVirt derives the statistic id from its name only, so the same statistic has the same id on every host.
     */
    public static String idForName(final String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public static Statistic hostGauge(final Ref host, final String name, final String description, final String type,
            final String unit, final BigDecimal datum) {
        final Statistic statistic = withHrefAndId(new Statistic(), null, idForName(name));
        statistic.setHref(host.getHref() + "/statistics/" + statistic.getId());
        statistic.setName(name);
        statistic.setDescription(description);
        statistic.setKind(KIND_GAUGE);
        statistic.setType(type);
        statistic.setUnit(unit);
        statistic.setValues(NamedList.of("value", Collections.singletonList(Value.of(datum))));
        statistic.setHost(host);
        return statistic;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Value {
        private BigDecimal datum;

        public static Value of(final BigDecimal datum) {
            Value value = new Value();
            value.setDatum(datum);
            return value;
        }

        public BigDecimal getDatum() {
            return datum;
        }

        public void setDatum(BigDecimal datum) {
            this.datum = datum;
        }
    }
}
