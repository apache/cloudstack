package com.cloud.hypervisor.xenserver.resource.wrapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmDiskStatsCommand;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.UpgradeSnapshotCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.storage.ResizeVolumeCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostEnvironment;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.hypervisor.xenserver.resource.XsHost;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.vm.DiskProfile;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Marshalling;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Connection.class, Host.Record.class})
public class CitrixRequestWrapperTest {

    @Mock
    private CitrixResourceBase citrixResourceBase;
    @Mock
    private RebootAnswer rebootAnswer;
    @Mock
    private CreateAnswer createAnswer;

    @Test
    public void testWrapperInstance() {
        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);
    }

    @Test
    public void testUnknownCommand() {
        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        try {
            wrapper.execute(new NotAValidCommand(), citrixResourceBase);
        } catch (final Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    public void testExecuteRebootRouterCommand() {
        final RebootRouterCommand rebootRouterCommand = new RebootRouterCommand("Test", "127.0.0.1");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(rebootRouterCommand, citrixResourceBase);

        verify(citrixResourceBase, times(2)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testExecuteCreateCommand() {
        final StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);
        final DiskProfile diskProfile = Mockito.mock(DiskProfile.class);
        final CreateCommand createCommand = new CreateCommand(diskProfile, "", poolVO, false);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(createCommand, citrixResourceBase);

        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testCheckConsoleProxyLoadCommand() {
        final CheckConsoleProxyLoadCommand consoleProxyCommand = new CheckConsoleProxyLoadCommand();

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(consoleProxyCommand, citrixResourceBase);

        assertFalse(answer.getResult());
    }

    @Test
    public void testWatchConsoleProxyLoadCommand() {
        final WatchConsoleProxyLoadCommand watchConsoleProxyCommand = new WatchConsoleProxyLoadCommand(0, 0, "", "", 0);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(watchConsoleProxyCommand, citrixResourceBase);

        assertFalse(answer.getResult());
    }

    @Test
    public void testReadyCommand() {
        final ReadyCommand readyCommand = new ReadyCommand();

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(readyCommand, citrixResourceBase);

        assertFalse(answer.getResult());
    }

    @Test
    public void testGetHostStatsCommand() {
        final GetHostStatsCommand statsCommand = new GetHostStatsCommand(null, null, 0);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(statsCommand, citrixResourceBase);

        assertTrue(answer.getResult());
    }

    @Test
    public void testGetVmStatsCommand() {
        final GetVmStatsCommand statsCommand = new GetVmStatsCommand(new ArrayList<String>(), null, null);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(statsCommand, citrixResourceBase);

        assertTrue(answer.getResult());
    }

    @Test
    public void testGetVmDiskStatsCommand() {
        final GetVmDiskStatsCommand statsCommand = new GetVmDiskStatsCommand(new ArrayList<String>(), null, null);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(statsCommand, citrixResourceBase);

        assertTrue(answer.getResult());
    }

    @Test
    public void testCheckHealthCommand() {
        final CheckHealthCommand statsCommand = new CheckHealthCommand();

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(statsCommand, citrixResourceBase);

        assertFalse(answer.getResult());
    }

    @Test
    public void testStopCommandCommand() {
        final StopCommand statsCommand = new StopCommand("Test", false, false);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(statsCommand, citrixResourceBase);

        assertFalse(answer.getResult());
    }

    @Test
    public void testRebootCommand() {
        final RebootCommand rebootCommand = new RebootCommand("Test");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(rebootCommand, citrixResourceBase);

        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testCheckVirtualMachineCommand() {
        final CheckVirtualMachineCommand virtualMachineCommand = new CheckVirtualMachineCommand("Test");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(virtualMachineCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertTrue(answer.getResult());
    }

    @Test
    public void testPrepareForMigrationCommand() {
        final VirtualMachineTO machineTO = Mockito.mock(VirtualMachineTO.class);
        final PrepareForMigrationCommand prepareCommand = new PrepareForMigrationCommand(machineTO);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(prepareCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testMigrateCommand() {
        final VirtualMachineTO machineTO = Mockito.mock(VirtualMachineTO.class);
        final MigrateCommand migrateCommand = new MigrateCommand("Test", "127.0.0.1", false, machineTO, false);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(migrateCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testDestroyCommand() {

        final VMTemplateStorageResourceAssoc templateStorage = Mockito.mock(VMTemplateStorageResourceAssoc.class);
        final StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);

        final DestroyCommand destroyCommand = new DestroyCommand(poolVO, templateStorage);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(destroyCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testCreateStoragePoolCommand() {
        final StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);
        final XsHost xsHost = Mockito.mock(XsHost.class);

        final CreateStoragePoolCommand destroyCommand = new CreateStoragePoolCommand(false, poolVO);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(citrixResourceBase.getHost()).thenReturn(xsHost);

        final Answer answer = wrapper.execute(destroyCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testModifyStoragePoolCommand() {
        final StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);
        final XsHost xsHost = Mockito.mock(XsHost.class);

        final ModifyStoragePoolCommand destroyCommand = new ModifyStoragePoolCommand(false, poolVO);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(citrixResourceBase.getHost()).thenReturn(xsHost);

        final Answer answer = wrapper.execute(destroyCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testDeleteStoragePoolCommand() {
        final StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);
        final XsHost xsHost = Mockito.mock(XsHost.class);

        final DeleteStoragePoolCommand destroyCommand = new DeleteStoragePoolCommand(poolVO);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(citrixResourceBase.getHost()).thenReturn(xsHost);

        final Answer answer = wrapper.execute(destroyCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertTrue(answer.getResult());
    }

    @Test
    public void testResizeVolumeCommand() {
        final StorageFilerTO pool = Mockito.mock(StorageFilerTO.class);

        final ResizeVolumeCommand resizeCommand = new ResizeVolumeCommand("Test", pool, 1l, 3l, false, "Tests-1");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(resizeCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testAttachVolumeCommand() {
        final AttachVolumeCommand destroyCommand = new AttachVolumeCommand(false, true, "Test", StoragePoolType.LVM, "/", "DATA", 100l, 1l, "123");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(destroyCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testAttachIsoCommand() {
        final AttachIsoCommand destroyCommand = new AttachIsoCommand("Test", "/", true);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(destroyCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testUpgradeSnapshotCommand() {
        final StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);

        final UpgradeSnapshotCommand destroyCommand = new UpgradeSnapshotCommand(poolVO, "http", 1l, 1l, 1l, 1l, 1l, "/", "58c5778b-7dd1-47cc-a7b5-f768541bf278", "Test", "2.1");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(destroyCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertTrue(answer.getResult());
    }

    @Test
    public void testUpgradeSnapshotCommandNo21() {
        final StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);

        final UpgradeSnapshotCommand destroyCommand = new UpgradeSnapshotCommand(poolVO, "http", 1l, 1l, 1l, 1l, 1l, "/", "58c5778b-7dd1-47cc-a7b5-f768541bf278", "Test", "3.1");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(destroyCommand, citrixResourceBase);
        verify(citrixResourceBase, times(0)).getConnection();

        assertTrue(answer.getResult());
    }

    @Test
    public void testGetStorageStatsCommand() {
        final XsHost xsHost = Mockito.mock(XsHost.class);
        final DataStoreTO store = Mockito.mock(DataStoreTO.class);

        final GetStorageStatsCommand storageStatsCommand = new GetStorageStatsCommand(store);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(citrixResourceBase.getHost()).thenReturn(xsHost);

        final Answer answer = wrapper.execute(storageStatsCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testPrimaryStorageDownloadCommand() {
        final XsHost xsHost = Mockito.mock(XsHost.class);
        final StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);

        final PrimaryStorageDownloadCommand storageDownloadCommand = new PrimaryStorageDownloadCommand("Test", "http://127.0.0.1", ImageFormat.VHD, 1l, poolVO, 200);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(citrixResourceBase.getHost()).thenReturn(xsHost);

        final Answer answer = wrapper.execute(storageDownloadCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testGetVncPortCommand() {
        final GetVncPortCommand storageDownloadCommand = new GetVncPortCommand(1l, "Test");

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(storageDownloadCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testSetupCommand() {
        final XsHost xsHost = Mockito.mock(XsHost.class);
        final HostEnvironment env = Mockito.mock(HostEnvironment.class);

        final SetupCommand storageDownloadCommand = new SetupCommand(env);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(citrixResourceBase.getHost()).thenReturn(xsHost);

        final Answer answer = wrapper.execute(storageDownloadCommand, citrixResourceBase);
        verify(citrixResourceBase, times(1)).getConnection();

        assertFalse(answer.getResult());
    }

    @Test
    public void testMaintainCommand() {
        // This test needs further work.

        final String uuid = "befc4dcd-f5c6-4015-8791-3c18622b7c7f";

        final Connection conn = Mockito.mock(Connection.class);
        final XsHost xsHost = Mockito.mock(XsHost.class);
        final XmlRpcClient client = Mockito.mock(XmlRpcClient.class);

        //        final Host.Record hr = PowerMockito.mock(Host.Record.class);
        //        final Host host = PowerMockito.mock(Host.class);

        final MaintainCommand storageDownloadCommand = new MaintainCommand();

        final Map<String, Object> map = new Hashtable<String, Object>();
        map.put("Value", "Xen");

        final Map<String, Object> spiedMap = spy(map);

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        assertNotNull(wrapper);

        when(citrixResourceBase.getConnection()).thenReturn(conn);
        when(citrixResourceBase.getHost()).thenReturn(xsHost);
        when(xsHost.getUuid()).thenReturn(uuid);
        when(conn.getSessionReference()).thenReturn("befc4dcd");

        try {
            final Object [] params = {Marshalling.toXMLRPC("befc4dcd"), Marshalling.toXMLRPC(uuid)};
            when(client.execute("host.get_by_uuid", new Object[]{"befc4dcd", uuid})).thenReturn(spiedMap);
            PowerMockito.when(conn, "dispatch", "host.get_by_uuid", params).thenReturn(spiedMap);
        } catch (final Exception e) {
        }

        //        try {
        //            PowerMockito.mockStatic(Host.class);
        //            //BDDMockito.given(Host.getByUuid(conn, xsHost.getUuid())).willReturn(host);
        //            PowerMockito.when(Host.getByUuid(conn, xsHost.getUuid())).thenReturn(host);
        //            PowerMockito.verifyStatic(times(1));
        //        } catch (final BadServerResponse e) {
        //            fail(e.getMessage());
        //        } catch (final XenAPIException e) {
        //            fail(e.getMessage());
        //        } catch (final XmlRpcException e) {
        //            fail(e.getMessage());
        //        }
        //
        //        PowerMockito.mockStatic(Types.class);
        //        PowerMockito.when(Types.toHostRecord(spiedMap)).thenReturn(hr);
        //        PowerMockito.verifyStatic(times(1));
        //
        //        try {
        //            PowerMockito.mockStatic(Host.Record.class);
        //            when(host.getRecord(conn)).thenReturn(hr);
        //            verify(host, times(1)).getRecord(conn);
        //        } catch (final BadServerResponse e) {
        //            fail(e.getMessage());
        //        } catch (final XenAPIException e) {
        //            fail(e.getMessage());
        //        } catch (final XmlRpcException e) {
        //            fail(e.getMessage());
        //        }

        final Answer answer = wrapper.execute(storageDownloadCommand, citrixResourceBase);

        assertFalse(answer.getResult());
    }
}

class NotAValidCommand extends Command {

    @Override
    public boolean executeInSequence() {
        return false;
    }

}