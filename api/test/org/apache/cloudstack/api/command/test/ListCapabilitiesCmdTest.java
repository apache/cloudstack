package org.apache.cloudstack.api.command.test;


import com.cloud.server.ManagementService;
import com.cloud.utils.Pair;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.command.admin.config.ListCfgsByCmd;
import org.apache.cloudstack.api.command.user.config.ListCapabilitiesCmd;
import org.apache.cloudstack.api.response.CapabilitiesResponse;
import org.apache.cloudstack.api.response.ConfigurationResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.config.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListCapabilitiesCmdTest extends TestCase {

    private ListCapabilitiesCmd listCapabilitiesCmd;
    private ManagementService mgr;
    private ResponseGenerator responseGenerator;

    @Override
    @Before
    public void setUp() {
        responseGenerator = Mockito.mock(ResponseGenerator.class);
        mgr = Mockito.mock(ManagementService.class);
        listCapabilitiesCmd = new ListCapabilitiesCmd();
    }

    @Test
    public void testCreateSuccess() {

        listCapabilitiesCmd._mgr = mgr;
        listCapabilitiesCmd._responseGenerator = responseGenerator;

        Map<String, Object> result = new HashMap<String, Object>();

        try {
            Mockito.when(mgr.listCapabilities(listCapabilitiesCmd)).thenReturn(result);
        } catch (Exception e) {
            Assert.fail("Received exception when success expected " + e.getMessage());
        }

        CapabilitiesResponse capResponse = new CapabilitiesResponse();
        Mockito.when(responseGenerator.createCapabilitiesResponse(result)).thenReturn(capResponse);

        listCapabilitiesCmd.execute();
        Mockito.verify(responseGenerator).createCapabilitiesResponse(result);

        CapabilitiesResponse actualResponse = (CapabilitiesResponse) listCapabilitiesCmd.getResponseObject();

        Assert.assertEquals(capResponse, actualResponse);
    }

}