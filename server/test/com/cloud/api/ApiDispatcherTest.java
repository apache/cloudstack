package com.cloud.api;

import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;

@RunWith(MockitoJUnitRunner.class)
public class ApiDispatcherTest {

    @Mock
    AccountManager accountManager;
    
    public static class TestCmd extends BaseCmd {

        @Parameter(name = "strparam1")
        String strparam1;
        
        @Parameter(name="intparam1", type=CommandType.INTEGER)
        int intparam1;

        @Parameter(name="boolparam1", type=CommandType.BOOLEAN)
        boolean boolparam1;

        @Override
        public void execute() throws ResourceUnavailableException,
                InsufficientCapacityException, ServerApiException,
                ConcurrentOperationException, ResourceAllocationException,
                NetworkRuleConflictException {
            // well documented nothing
        }

        @Override
        public String getCommandName() {
            return "test";
        }

        @Override
        public long getEntityOwnerId() {
            return 0;
        }

    }

    @Before
    public void setup() {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        new ApiDispatcher().init();
        ApiDispatcher.getInstance()._accountMgr = accountManager;
    }
    
    @After
    public void cleanup() {
        CallContext.unregister();
    }

    @Test
    public void processParameters() {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("strparam1", "foo");
        params.put("intparam1", "100");
        params.put("boolparam1", "true");
        TestCmd cmd = new TestCmd();
        //how lucky that field is not protected, this test would be impossible
        ApiDispatcher.processParameters(cmd, params);
        Assert.assertEquals("foo", cmd.strparam1);
        Assert.assertEquals(100, cmd.intparam1);
    }

}
