package org.apache.cloudstack.api.command.admin.vlan;

import junit.framework.TestCase;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VlanIpRangeResponse;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.configuration.ConfigurationService;
import com.cloud.dc.Vlan;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

public class UpdateVlanIpRangeCmdTest extends TestCase {

    private UpdateVlanIpRangeCmd updateVlanIpRangeCmd;
    private ResponseGenerator responseGenerator;

    @Test
    public void testUpdateSuccess() throws Exception {

        ConfigurationService configService = Mockito.mock(ConfigurationService.class);
        Vlan result = Mockito.mock(Vlan.class);

        responseGenerator = Mockito.mock(ResponseGenerator.class);
        updateVlanIpRangeCmd = new UpdateVlanIpRangeCmd();

        Mockito.when(configService.updateVlanAndPublicIpRange(updateVlanIpRangeCmd)).thenReturn(result);
        updateVlanIpRangeCmd._configService = configService;

        VlanIpRangeResponse ipRes = Mockito.mock(VlanIpRangeResponse.class);
        Mockito.when(responseGenerator.createVlanIpRangeResponse(result)).thenReturn(ipRes);

        updateVlanIpRangeCmd._responseGenerator = responseGenerator;
        try {
            updateVlanIpRangeCmd.execute();
        } catch (ServerApiException ex) {
            assertEquals("Failed to Update vlan ip range", ex.getMessage());
        }
    }

    @Test
    public void testUpdateFailure() throws ResourceAllocationException, ResourceUnavailableException {

        ConfigurationService configService = Mockito.mock(ConfigurationService.class);

        responseGenerator = Mockito.mock(ResponseGenerator.class);
        updateVlanIpRangeCmd = new UpdateVlanIpRangeCmd();
        updateVlanIpRangeCmd._configService = configService;

        Mockito.when(configService.updateVlanAndPublicIpRange(updateVlanIpRangeCmd)).thenReturn(null);

        try {
            updateVlanIpRangeCmd.execute();
        } catch (ServerApiException ex) {
            assertEquals("Failed to Update vlan ip range", ex.getMessage());
        }

    }
}