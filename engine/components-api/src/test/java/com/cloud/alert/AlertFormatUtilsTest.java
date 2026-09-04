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
package com.cloud.alert;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Pod;
import com.cloud.host.Host;

@RunWith(MockitoJUnitRunner.class)
public class AlertFormatUtilsTest {

    @Mock
    Host host;
    @Mock
    DataCenter zone;
    @Mock
    Pod pod;

    @Test
    public void describeHostLocationIncludesNameIdUuidZoneAndPod() {
        setUpHost();
        setUpZone();
        setUpPod();

        String result = AlertFormatUtils.describeHostLocation(host, zone, pod);

        assertEquals("name: cs-kvm06 (id: 37, uuid: host-uuid), availability zone: Milton1, pod: Milton1-Pod1", result);
    }

    @Test
    public void describeHostLocationFallsBackToUnknownForNullZone() {
        setUpHost();
        setUpPod();

        String result = AlertFormatUtils.describeHostLocation(host, null, pod);

        assertEquals("name: cs-kvm06 (id: 37, uuid: host-uuid), availability zone: unknown, pod: Milton1-Pod1", result);
    }

    @Test
    public void describeHostLocationFallsBackToUnknownForNullPod() {
        setUpHost();
        setUpZone();

        String result = AlertFormatUtils.describeHostLocation(host, zone, null);

        assertEquals("name: cs-kvm06 (id: 37, uuid: host-uuid), availability zone: Milton1, pod: unknown", result);
    }

    @Test
    public void describeHostLocationFallsBackToUnknownForNullZoneAndPod() {
        setUpHost();

        String result = AlertFormatUtils.describeHostLocation(host, null, null);

        assertEquals("name: cs-kvm06 (id: 37, uuid: host-uuid), availability zone: unknown, pod: unknown", result);
    }

    @Test
    public void describeHostLocationDescribesZoneAndPodForNullHost() {
        setUpZone();
        setUpPod();

        String result = AlertFormatUtils.describeHostLocation(null, zone, pod);

        assertEquals("No host to describe for availability zone: Milton1, pod: Milton1-Pod1", result);
    }

    @Test
    public void describeHostLocationFallsBackToUnknownForNullHostAndNullZoneAndPod() {
        String result = AlertFormatUtils.describeHostLocation(null, null, null);

        assertEquals("No host to describe for availability zone: unknown, pod: unknown", result);
    }

    private void setUpHost() {
        when(host.getName()).thenReturn("cs-kvm06");
        when(host.getId()).thenReturn(37L);
        when(host.getUuid()).thenReturn("host-uuid");
    }

    private void setUpZone() {
        when(zone.getName()).thenReturn("Milton1");
    }

    private void setUpPod() {
        when(pod.getName()).thenReturn("Milton1-Pod1");
    }
}
