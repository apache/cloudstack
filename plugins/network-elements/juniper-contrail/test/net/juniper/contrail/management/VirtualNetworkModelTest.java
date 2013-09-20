package net.juniper.contrail.management;

import java.util.UUID;

import net.juniper.contrail.model.VirtualNetworkModel;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkVO;

import junit.framework.TestCase;

public class VirtualNetworkModelTest extends TestCase {
    private static final Logger s_logger =
            Logger.getLogger(VirtualNetworkModelTest.class);

    @Test
    public void testDBLookup() {
        ModelDatabase db = new ModelDatabase();
        NetworkVO network = Mockito.mock(NetworkVO.class);
        VirtualNetworkModel storageModel = new VirtualNetworkModel(network, null, ContrailManager.managementNetworkName,
                TrafficType.Storage);
        db.getVirtualNetworks().add(storageModel);
        VirtualNetworkModel mgmtModel = new VirtualNetworkModel(network, null, ContrailManager.managementNetworkName,
                TrafficType.Management);
        db.getVirtualNetworks().add(mgmtModel);
        VirtualNetworkModel guestModel1 = new VirtualNetworkModel(network, UUID.randomUUID().toString(), "test",
                TrafficType.Guest);
        db.getVirtualNetworks().add(guestModel1);
        VirtualNetworkModel guestModel2 = new VirtualNetworkModel(network, UUID.randomUUID().toString(), "test",
                TrafficType.Guest);
        db.getVirtualNetworks().add(guestModel2);
        s_logger.debug("networks: " + db.getVirtualNetworks().size());
        assertEquals(4, db.getVirtualNetworks().size());
        assertSame(storageModel, db.lookupVirtualNetwork(null, storageModel.getName(), TrafficType.Storage));
        assertSame(mgmtModel, db.lookupVirtualNetwork(null, mgmtModel.getName(), TrafficType.Management));
        assertSame(guestModel1, db.lookupVirtualNetwork(guestModel1.getUuid(), null, TrafficType.Guest));
        assertSame(guestModel2, db.lookupVirtualNetwork(guestModel2.getUuid(), null, TrafficType.Guest));
    }

}
