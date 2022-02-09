package org.apache.cloudstack.api.response;

import java.text.DecimalFormat;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StatsResponseTest {

    @Spy
    StatsResponse statsResponseMock;

    final char decimalSeparator = ((DecimalFormat) DecimalFormat.getInstance()).getDecimalFormatSymbols().getDecimalSeparator();

    @Test
    public void setDiskIOReadTestWithAnyInput() {
        statsResponseMock.setDiskIORead(1L);

        Mockito.verify(statsResponseMock).accumulateDiskIopsTotal(Mockito.anyLong());
    }

    @Test
    public void setDiskIOWriteTestWithAnyInput() {
        statsResponseMock.setDiskIOWrite(1L);

        Mockito.verify(statsResponseMock).accumulateDiskIopsTotal(Mockito.anyLong());
    }

    @Test
    public void accumulateDiskIopsTotalTestWithNullInput() {
        Long expected = 0L;

        statsResponseMock.accumulateDiskIopsTotal(null);

        Assert.assertEquals(expected, statsResponseMock.diskIopsTotal);
    }

    @Test
    public void accumulateDiskIopsTotalTestWithZeroAsInput() {
        Long expected = 0L;

        statsResponseMock.accumulateDiskIopsTotal(0L);

        Assert.assertEquals(expected, statsResponseMock.diskIopsTotal);
    }

    @Test
    public void accumulateDiskIopsTotalTestWithInputGreatherThanZero() {
        Long expected = 1L;

        statsResponseMock.accumulateDiskIopsTotal(1L);

        Assert.assertEquals(expected, statsResponseMock.diskIopsTotal);
    }

    @Test
    public void setDiskIOWriteTestWithInputNotNullAndNullDiskIopsTotal() {
        Long expected = 1L;

        statsResponseMock.setDiskIOWrite(expected);

        Assert.assertEquals(expected, statsResponseMock.diskIOWrite);
        Assert.assertEquals(expected, statsResponseMock.diskIopsTotal);
    }

    @Test
    public void setDiskIOWriteTestWithInputNotNullAndDiskIopsTotalNotNull() {
        statsResponseMock.diskIopsTotal = 1L;
        Long expectedDiskIOWrite = 1L, expectedDiskIopsTotal = 2L;

        statsResponseMock.setDiskIOWrite(1L);

        Assert.assertEquals(expectedDiskIOWrite, statsResponseMock.diskIOWrite);
        Assert.assertEquals(expectedDiskIopsTotal, statsResponseMock.diskIopsTotal);
    }

    @Test
    public void setNetworkKbsReadTestWithNullInput() {
        statsResponseMock.setNetworkKbsRead(null);

        Assert.assertEquals(null, statsResponseMock.networkKbsRead);
        Assert.assertEquals(null, statsResponseMock.networkRead);
    }

    @Test
    public void setNetworkKbsReadTestWithInputNotNull() {
        Long expectedNetworkKbsRead = Long.valueOf("100");
        String expectedNetworkRead = String.format("0%s10 MB", decimalSeparator); // the actual result is 0.097 but the value is rounded to 0.10

        statsResponseMock.setNetworkKbsRead(expectedNetworkKbsRead);

        Assert.assertEquals(expectedNetworkKbsRead, statsResponseMock.networkKbsRead);
        Assert.assertEquals(expectedNetworkRead, statsResponseMock.networkRead);
    }

    @Test
    public void setNetworkKbsWriteTestWithNullInput() {
        statsResponseMock.setNetworkKbsWrite(null);

        Assert.assertEquals(null, statsResponseMock.networkKbsWrite);
        Assert.assertEquals(null, statsResponseMock.networkWrite);
    }

    @Test
    public void setNetworkKbsWriteTestWithInputNotNull() {
        Long expectedNetworkKbsWrite = Long.valueOf("100");
        String expectedNetworkWrite = String.format("0%s10 MB", decimalSeparator); // the actual result is 0.097 but the value is rounded to 0.10

        statsResponseMock.setNetworkKbsWrite(expectedNetworkKbsWrite);

        Assert.assertEquals(expectedNetworkKbsWrite, statsResponseMock.networkKbsWrite);
        Assert.assertEquals(expectedNetworkWrite, statsResponseMock.networkWrite);
    }
}