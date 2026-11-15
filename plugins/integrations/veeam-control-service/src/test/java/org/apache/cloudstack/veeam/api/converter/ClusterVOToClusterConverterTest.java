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

package org.apache.cloudstack.veeam.api.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.veeam.api.dto.Cluster;
import org.junit.Test;

import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.cpu.CPU;
import com.cloud.dc.ClusterVO;

public class ClusterVOToClusterConverterTest {

    @Test
    public void testToCluster_MapsDefaultsAndResolvedDataCenter() {
        final ClusterVO vo = mock(ClusterVO.class);
        when(vo.getUuid()).thenReturn("cluster-1");
        when(vo.getName()).thenReturn("cluster-a");
        when(vo.getArch()).thenReturn(CPU.CPUArch.amd64);
        when(vo.getDataCenterId()).thenReturn(11L);

        final DataCenterJoinVO zone = mock(DataCenterJoinVO.class);
        when(zone.getUuid()).thenReturn("dc-1");

        final Cluster cluster = ClusterVOToClusterConverter.toCluster(vo, id -> zone);

        assertEquals("cluster-1", cluster.getId());
        assertEquals("cluster-a", cluster.getName());
        assertEquals("x86_64", cluster.getCpu().getArchitecture());
        assertEquals("dc-1", cluster.getDataCenter().getId());
        assertEquals("urandom", cluster.getRequiredRngSources().requiredRngSource.get(0));
        assertEquals("networks", cluster.getLink().get(0).getRel());
        assertNotNull(cluster.getSchedulingPolicy());
        assertNotNull(cluster.getMacPool());
        assertTrue(cluster.getHref().contains("/api/clusters/cluster-1"));
    }

    @Test
    public void testToClusterList_ConvertsAllItems() {
        final ClusterVO first = mock(ClusterVO.class);
        when(first.getUuid()).thenReturn("c1");
        when(first.getName()).thenReturn("c1");
        when(first.getArch()).thenReturn(CPU.CPUArch.x86);
        when(first.getDataCenterId()).thenReturn(1L);

        final ClusterVO second = mock(ClusterVO.class);
        when(second.getUuid()).thenReturn("c2");
        when(second.getName()).thenReturn("c2");
        when(second.getArch()).thenReturn(CPU.CPUArch.amd64);
        when(second.getDataCenterId()).thenReturn(2L);

        final List<Cluster> clusters = ClusterVOToClusterConverter.toClusterList(List.of(first, second), id -> null);

        assertEquals(2, clusters.size());
        assertEquals("c1", clusters.get(0).getId());
        assertEquals("c2", clusters.get(1).getId());
    }
}
