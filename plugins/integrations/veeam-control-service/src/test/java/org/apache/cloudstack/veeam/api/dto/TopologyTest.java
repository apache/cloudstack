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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class TopologyTest {

    @Test
    public void constructor_IntValues_ConvertsToStrings() {
        Topology topology = new Topology(2, 4, 1);

        assertEquals("2", topology.getSockets());
        assertEquals("4", topology.getCores());
        assertEquals("1", topology.getThreads());
    }

    @Test
    public void defaultConstructor_NullFields() {
        Topology topology = new Topology();

        assertNull(topology.getSockets());
        assertNull(topology.getCores());
        assertNull(topology.getThreads());
    }

    @Test
    public void cpuWithTopology_Serializes() throws Exception {
        Mapper mapper = new Mapper();
        Cpu cpu = new Cpu();
        cpu.setName("Intel Xeon");
        cpu.setArchitecture("x86_64");
        Topology topology = new Topology(4, 8, 2);
        cpu.setTopology(topology);

        String json = mapper.toJson(cpu);

        assertNotNull(json);
        // Mapper uses SNAKE_CASE, so field names become snake_case
        assertEquals("Intel Xeon", cpu.getName());
        assertEquals("x86_64", cpu.getArchitecture());
        assertEquals(topology, cpu.getTopology());
    }

    @Test
    public void topologyJson_OmitsNullFields() throws Exception {
        Mapper mapper = new Mapper();
        Topology topology = new Topology();
        topology.setSockets("2");

        String json = mapper.toJson(topology);

        assertNotNull(json);
        // only sockets is set
        assertEquals("2", topology.getSockets());
        assertNull(topology.getCores());
    }
}
