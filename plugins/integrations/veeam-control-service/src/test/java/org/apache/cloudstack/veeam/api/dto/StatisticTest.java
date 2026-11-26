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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class StatisticTest {

    private static Statistic memoryTotal() {
        return Statistic.hostGauge(Ref.of("/ovirt-engine/api/hosts/h1", "h1"), "memory.total", "Total memory",
                Statistic.TYPE_INTEGER, Statistic.UNIT_BYTES, BigDecimal.valueOf(540171829248L));
    }

    @Test
    public void idForName_MatchesOvirt() {
        assertEquals("7816602b-c05c-3db7-a4da-3769f7ad8896", Statistic.idForName("memory.total"));
    }

    @Test
    public void hostGauge_SetsFields() {
        Statistic statistic = memoryTotal();
        assertEquals("memory.total", statistic.getName());
        assertEquals("gauge", statistic.getKind());
        assertEquals("/ovirt-engine/api/hosts/h1/statistics/7816602b-c05c-3db7-a4da-3769f7ad8896", statistic.getHref());
        assertEquals("h1", statistic.getHost().getId());
        assertEquals(BigDecimal.valueOf(540171829248L), statistic.getValues().getItems().get(0).getDatum());
    }

    @Test
    public void hostJson_FollowStatistics_MatchesOvirtShape() throws Exception {
        Mapper mapper = new Mapper();
        Host host = new Host();
        host.setStatistics(NamedList.of("statistic", List.of(memoryTotal(),
                Statistic.hostGauge(Ref.of("/ovirt-engine/api/hosts/h1", "h1"), "cpu.current.idle", "Idle CPU usage",
                        Statistic.TYPE_DECIMAL, Statistic.UNIT_PERCENT, new BigDecimal("96.51")))));

        JsonNode statistics = mapper.jsonMapper().readTree(mapper.toJson(host)).path("statistics").path("statistic");

        assertEquals(2, statistics.size());
        JsonNode total = statistics.get(0);
        assertEquals("memory.total", total.path("name").asText());
        assertEquals("integer", total.path("type").asText());
        assertEquals("bytes", total.path("unit").asText());
        JsonNode totalDatum = total.path("values").path("value").get(0).path("datum");
        assertTrue(totalDatum.isNumber());
        assertEquals(540171829248L, totalDatum.asLong());
        assertEquals("h1", total.path("host").path("id").asText());

        JsonNode idleDatum = statistics.get(1).path("values").path("value").get(0).path("datum");
        assertTrue(idleDatum.isNumber());
        assertEquals(96.51, idleDatum.asDouble(), 0.0001);
    }

    @Test
    public void xml_ContainsDatum() throws Exception {
        Mapper mapper = new Mapper();
        String xml = mapper.toXml(NamedList.of("statistic", List.of(memoryTotal())));
        assertTrue(xml, xml.contains("<name>memory.total</name>"));
        assertTrue(xml, xml.contains("<datum>540171829248</datum>"));
    }
}
