package com.cloud.api;

import java.util.HashMap;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.dispatch.DispatchChain;
import com.cloud.api.dispatch.DispatchTask;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;

public class ApiDispatcherTest {
    protected static final Long resourceId = 1L;
    protected static final ApiCommandResourceType resourceType = ApiCommandResourceType.Account;

    ApiDispatcher apiDispatcher;

    @Before
    public void injectMocks() throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        apiDispatcher = new ApiDispatcher();
        DispatchChain dispatchChain = Mockito.mock(DispatchChain.class);
        Mockito.doNothing().when(dispatchChain).dispatch(Mockito.any(DispatchTask.class));
        ReflectionTestUtils.setField(apiDispatcher, "standardDispatchChain", dispatchChain);
    }

    @Test
    public void testBaseCmdDispatchCallContext() {
        TesBaseCmd cmd = new TesBaseCmd();
        try {
            apiDispatcher.dispatch(cmd, new HashMap<String, String>(), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertEquals(CallContext.current().getEventResourceId(), resourceId);
        Assert.assertEquals(CallContext.current().getEventResourceType(), resourceType);
    }

    protected class TesBaseCmd extends BaseCmd {

        @Override
        public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {

        }

        @Override
        public String getCommandName() {
            return "testCommand";
        }

        @Override
        public long getEntityOwnerId() {
            return 1L;
        }

        @Override
        public Long getApiResourceId() {
            return resourceId;
        }

        @Override
        public ApiCommandResourceType getApiResourceType() {
            return resourceType;
        }
    }
}