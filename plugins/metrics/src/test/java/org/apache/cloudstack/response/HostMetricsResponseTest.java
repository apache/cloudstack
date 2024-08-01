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

package org.apache.cloudstack.response;

import java.text.DecimalFormat;

import org.junit.Assert;
import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;

public class HostMetricsResponseTest {

    final char decimalSeparator = ((DecimalFormat) DecimalFormat.getInstance()).getDecimalFormatSymbols().getDecimalSeparator();

    @Test
    public void testSetCpuAllocatedWithZeroCpu() {
        final HostMetricsResponse hostResponse = new HostMetricsResponse();
        hostResponse.setCpuAllocated(String.format("50%s25%%", decimalSeparator), 0, 1000L);
        Assert.assertEquals(String.format("0%s00 Ghz", decimalSeparator), hostResponse.getCpuAllocatedGhz());
    }

    @Test
    public void testSetCpuAllocatedWithInfiniteCpuAllocated() {
        final HostMetricsResponse hostResponse = new HostMetricsResponse();
        hostResponse.setCpuAllocated("∞%", 10, 1000L);
        Assert.assertEquals("Infinity Ghz", hostResponse.getCpuAllocatedGhz());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testSetCpuAllocatedWithInvalidCpu() {
        final HostMetricsResponse hostResponse = new HostMetricsResponse();
        hostResponse.setCpuAllocated("abc", 10, 1000L);
    }

    @Test
    public void testSetCpuAllocatedWithValidCpu() {
        String expected = String.format("5%s03 Ghz", decimalSeparator);
        final HostMetricsResponse hostResponse = new HostMetricsResponse();
        hostResponse.setCpuAllocated(String.format("50%s25%%", decimalSeparator), 10, 1000L);
        Assert.assertEquals(expected, hostResponse.getCpuAllocatedGhz());
    }

    @Test
    public void testSetCpuAllocatedWithNullCpu() {
        String expected = null;
        final HostMetricsResponse hostResponse = new HostMetricsResponse();
        hostResponse.setCpuAllocated(null, 10, 1000L);
        Assert.assertEquals(expected, hostResponse.getCpuAllocatedGhz());
    }

    @Test
    public void testSetCpuAllocatedWithNullCpuNumber() {
        String expected = null;
        final HostMetricsResponse hostResponse = new HostMetricsResponse();
        hostResponse.setCpuAllocated(String.format("50%s25%%", decimalSeparator), null, 1000L);
        Assert.assertEquals(expected, hostResponse.getCpuAllocatedGhz());
    }

    @Test
    public void testSetCpuAllocatedWithNullCpuSpeed() {
        String expected = null;
        final HostMetricsResponse hostResponse = new HostMetricsResponse();
        hostResponse.setCpuAllocated(String.format("50%s25%%", decimalSeparator), 10, null);
        Assert.assertEquals(expected, hostResponse.getCpuAllocatedGhz());
    }
}
