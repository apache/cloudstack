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

package org.apache.cloudstack.network.contrail.management;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiConnectorFactory;
import net.juniper.contrail.api.ApiObjectBase;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;

import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.dc.DataCenter;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.db.Merovingian2;
import com.cloud.utils.mgmt.JmxUtil;

import junit.framework.TestCase;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:/publicNetworkContext.xml")

public class PublicNetworkTest extends TestCase {
    private static final Logger s_logger =
            Logger.getLogger(PublicNetworkTest.class);

    @Inject public ContrailManager _contrailMgr;
    @Inject public NetworkDao _networksDao;

    private static boolean _initDone = false;
    private static int _mysql_server_port;
    private static long _msId;
    private static Merovingian2 _lockMaster;
    private ManagementServerMock _server;
    private ApiConnector _spy;
    

    @BeforeClass
    public static void globalSetUp() throws Exception {
        ApiConnectorFactory.setImplementation(ApiConnectorMockito.class);
        s_logger.info("mysql server is getting launched ");
        _mysql_server_port = TestDbSetup.init(null);
        s_logger.info("mysql server launched on port " + _mysql_server_port);
        _msId = ManagementServerNode.getManagementServerId();
        _lockMaster = Merovingian2.createLockMaster(_msId);
    }

    @AfterClass
    public static void globalTearDown() throws Exception {
        _lockMaster.cleanupForServer(_msId);
        JmxUtil.unregisterMBean("Locks", "Locks");
        _lockMaster = null;

        AbstractApplicationContext ctx = (AbstractApplicationContext) ComponentContext.getApplicationContext();
        Map<String, ComponentLifecycle> lifecycleComponents = ctx.getBeansOfType(ComponentLifecycle.class);
        for (ComponentLifecycle bean: lifecycleComponents.values()) {
            bean.stop();
        }
        ctx.close();

        s_logger.info("destroying mysql server instance running at port <" + _mysql_server_port + ">");
        TestDbSetup.destroy(_mysql_server_port, null);
    }

    @Before
    public void setUp() throws Exception {
        try {
            ComponentContext.initComponentsLifeCycle();
        } catch (Exception ex) {
            ex.printStackTrace();
            s_logger.error(ex.getMessage());
        }
        _server = ComponentContext.inject(new ManagementServerMock());

        _server.initialize(!_initDone);
        _initDone = false;
        _spy = ((ApiConnectorMockito)_contrailMgr.getApiConnector()).getSpy();
    }

    @After
    public void tearDown() throws Exception {
        _server.shutdown();
    }

    @Test
    public void testPublicNetwork() throws IOException {
    	DataCenter zone = _server.getZone();
        List<NetworkVO> networks = _networksDao.listByZoneAndTrafficType(zone.getId(), TrafficType.Public);
        assertNotNull(networks);
        assertFalse(networks.isEmpty());
        UserVm vm1 = _server.createVM("test", networks.get(0));

        ArgumentCaptor<ApiObjectBase> createArg = ArgumentCaptor.forClass(ApiObjectBase.class);
        verify(_spy, times(4)).create(createArg.capture());
        
        List<ApiObjectBase> argumentList = createArg.getAllValues();
        ApiObjectBase vmObj = argumentList.get(0);
        assertEquals(VirtualNetwork.class, vmObj.getClass());
        assertEquals("__default_Public__", vmObj.getName());
        
        String vmiName = null;
        for (ApiObjectBase obj: argumentList) {
            if (obj.getClass() == VirtualMachineInterface.class) {
                vmiName = obj.getName();
            }
        }
        assertEquals("test-0", vmiName);
    }
}
