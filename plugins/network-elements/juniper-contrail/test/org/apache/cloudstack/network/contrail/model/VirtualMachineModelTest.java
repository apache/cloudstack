package org.apache.cloudstack.network.contrail.model;
import java.util.UUID;

import java.io.IOException;

import org.apache.cloudstack.network.contrail.management.*;
import org.apache.cloudstack.network.contrail.model.*;

import org.apache.log4j.Logger;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;

import com.cloud.network.Network;
import com.cloud.network.dao.NetworkVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.uservm.UserVm;

import net.juniper.contrail.api.ApiConnectorMock;
import net.juniper.contrail.api.ApiConnectorFactory;
import net.juniper.contrail.api.ApiObjectBase;
import net.juniper.contrail.api.types.Domain;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.types.NetworkIpam;
import net.juniper.contrail.api.types.SubnetType;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VnSubnetsType;
import net.juniper.contrail.api.types.NetworkPolicy;
import net.juniper.contrail.api.types.Project;
import net.juniper.contrail.api.types.FloatingIp;
import net.juniper.contrail.api.types.VnSubnetsType;
import net.juniper.contrail.api.types.ServiceInstance;
import net.juniper.contrail.api.ApiConnector;

import org.junit.runner.RunWith;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import junit.framework.TestCase;

public class VirtualMachineModelTest extends TestCase {
    private static final Logger s_logger =
            Logger.getLogger(VirtualMachineModelTest.class);

    @Test
    public void testVirtualMachineDBLookup() {
        ModelDatabase db = new ModelDatabase();
        VMInstanceVO vm  = mock(VMInstanceVO.class);

        // Create 3 dummy Virtual Machine model objects
        // Add these models to database.
        // Each VM is identified by unique UUId.
        VirtualMachineModel  vm0 = new VirtualMachineModel(vm, "fbc1f8fa-4b78-45ee-bba0-b551dbf72353");
        db.getVirtualMachines().add(vm0);

        VirtualMachineModel  vm1 = new VirtualMachineModel(vm, "fbc1f8fa-4b78-45ee-bba0-b551dbf83464");
        db.getVirtualMachines().add(vm1);

        VirtualMachineModel  vm2 = new VirtualMachineModel(vm, "fbc1f8fa-4b78-45ee-bba0-b551dbf94575");
        db.getVirtualMachines().add(vm2);

        s_logger.debug("No of Vitual Machines added to database : " + db.getVirtualMachines().size());

        assertEquals(3, db.getVirtualMachines().size());

        assertSame(vm0, db.lookupVirtualMachine("fbc1f8fa-4b78-45ee-bba0-b551dbf72353"));
        assertSame(vm1, db.lookupVirtualMachine("fbc1f8fa-4b78-45ee-bba0-b551dbf83464"));
        assertSame(vm2, db.lookupVirtualMachine("fbc1f8fa-4b78-45ee-bba0-b551dbf94575"));
    }

    @Test
    public void testCreateVirtualMachine() throws IOException {

        String uuid = UUID.randomUUID().toString();
		ContrailManagerImpl contrailMgr = mock(ContrailManagerImpl.class);
		ModelController controller      = mock(ModelController.class);
        ApiConnector api = new ApiConnectorMock(null, 0);
		when(controller.getManager()).thenReturn(contrailMgr);
		when(controller.getApiAccessor()).thenReturn(api);

		// Create Virtual-Network (VN)
        NetworkVO network = mock(NetworkVO.class);
		when(network.getName()).thenReturn("testnetwork");
		when(network.getState()).thenReturn(Network.State.Allocated);
		when(network.getGateway()).thenReturn("10.1.1.1");
		when(network.getCidr()).thenReturn("10.1.1.0/24");
		when(network.getPhysicalNetworkId()).thenReturn(42L);
		when(network.getDomainId()).thenReturn(10L);
		when(network.getAccountId()).thenReturn(42L);

		when(contrailMgr.getCanonicalName(network)).thenReturn("testnetwork");
		when(contrailMgr.getProjectId(network.getDomainId(), network.getAccountId())).thenReturn("testProjectId");

		// Create Virtual-Machine (VM)
        VMInstanceVO vm = mock(VMInstanceVO.class);
		when(vm.getInstanceName()).thenReturn("testVM1");
		when(vm.getState()).thenReturn(VirtualMachine.State.Starting);
		when(vm.getDomainId()).thenReturn(10L);
		when(vm.getAccountId()).thenReturn(42L);

		UserVmDao VmDao      = mock(UserVmDao.class);
		when(VmDao.findById(anyLong())).thenReturn(null);
		when(controller.getVmDao()).thenReturn(VmDao);

        VirtualMachineModel vmModel = new VirtualMachineModel(vm, uuid);

        assertEquals(vmModel.getInstanceName(), "testVM1");
        assertEquals(vmModel.getUuid(), uuid);

        vmModel.build(controller, vm);
		try {
            vmModel.update(controller);
		} catch (Exception ex) {
			fail("virtual-network update failed ");
		}

		//verify
        assertTrue(vmModel.verify(controller));

    }

}
