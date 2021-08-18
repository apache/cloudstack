package org.apache.cloudstack.response;

import org.junit.Assert;
import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;

public class HostMetricsResponseTest {

    @Test
    public void testSetCpuAllocatedWithZeroCpu() {
        final HostMetricsResponse hostResponse = new HostMetricsResponse();
        hostResponse.setCpuAllocated("50.25%", 0, 1000L);
        Assert.assertEquals("0.00 Ghz", hostResponse.getCpuAllocatedGhz());
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
        final HostMetricsResponse hostResponse = new HostMetricsResponse();
        hostResponse.setCpuAllocated("50.25%", 10, 1000L);
        Assert.assertEquals("5.03 Ghz", hostResponse.getCpuAllocatedGhz());
    }

}
