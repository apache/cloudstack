package org.apache.cloudstack.storage.datastore.driver;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;


@RunWith(PowerMockRunner.class)
@PrepareForTest(StorPoolUtil.class)
public class StorPoolPrimaryDataStoreDriverTest {

    @Mock
    private VMInstanceDao vmInstanceDao;

    @Mock
    private ResourceTagDao _resourceTagDao;

    @Mock
    private AsyncCompletionCallback<CopyCommandResult> callback;

    @InjectMocks
    private StorPoolPrimaryDataStoreDriver storPoolPrimaryDataStoreDriver;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(StorPoolUtil.class, RETURNS_MOCKS);
    }

    @Test
    public void testMigrateVolume() {

        DataStore srcStore = mock(DataStore.class);
        DataStore destStore = mock(DataStore.class);

        DataObject srcObj = mock(VolumeInfo.class);
        DataObject destObj = mock(VolumeInfo.class);

        VolumeObjectTO srcTO = mock(VolumeObjectTO.class);
        VolumeObjectTO dstTO = mock(VolumeObjectTO.class);

        PrimaryDataStoreTO dstPrimaryTo = mock(PrimaryDataStoreTO.class);

        VMInstanceVO vm = mock(VMInstanceVO.class);

        setReturnsWhenSourceAndDestinationAreVolumes(srcStore, destStore, srcObj, destObj, srcTO, dstTO, dstPrimaryTo, vm);
        when(vm.getState()).thenReturn(State.Running);


        storPoolPrimaryDataStoreDriver.copyAsync(srcObj, destObj, callback);
    }

    @Test
    public void testCopyVolumeAttachedToVm() {

        DataStore srcStore = mock(DataStore.class);
        DataStore destStore = mock(DataStore.class);

        DataObject srcObj = mock(VolumeInfo.class);
        DataObject destObj = mock(VolumeInfo.class);

        VolumeObjectTO srcTO = mock(VolumeObjectTO.class);
        VolumeObjectTO dstTO = mock(VolumeObjectTO.class);

        PrimaryDataStoreTO dstPrimaryTo = mock(PrimaryDataStoreTO.class);

        VMInstanceVO vm = mock(VMInstanceVO.class);

        setReturnsWhenSourceAndDestinationAreVolumes(srcStore, destStore, srcObj, destObj, srcTO, dstTO, dstPrimaryTo, vm);
        when(vm.getState()).thenReturn(State.Stopped);

        storPoolPrimaryDataStoreDriver.copyAsync(srcObj, destObj, callback);
    }

    @Test
    public void testCopyVolumeNotAttachedToVm() {

        DataStore srcStore = mock(DataStore.class);
        DataStore destStore = mock(DataStore.class);

        DataObject srcObj = mock(VolumeInfo.class);
        DataObject destObj = mock(VolumeInfo.class);

        VolumeObjectTO srcTO = mock(VolumeObjectTO.class);
        VolumeObjectTO dstTO = mock(VolumeObjectTO.class);

        PrimaryDataStoreTO dstPrimaryTo = mock(PrimaryDataStoreTO.class);

        setReturnsWhenSourceAndDestinationAreVolumes(srcStore, destStore, srcObj, destObj, srcTO, dstTO, dstPrimaryTo, null);

        storPoolPrimaryDataStoreDriver.copyAsync(srcObj, destObj, callback);
    }
    private void setReturnsWhenSourceAndDestinationAreVolumes(DataStore srcStore, DataStore destStore, DataObject srcObj, DataObject destObj, VolumeObjectTO srcTO, VolumeObjectTO dstTO, PrimaryDataStoreTO dstPrimaryTo, VMInstanceVO vm) {
        when(srcStore.getRole()).thenReturn(DataStoreRole.Primary);
        when(destStore.getRole()).thenReturn(DataStoreRole.Primary);
        when(srcObj.getDataStore()).thenReturn(srcStore);
        when(destObj.getDataStore()).thenReturn(destStore);
        when(srcObj.getType()).thenReturn(DataObjectType.VOLUME);
        when(destObj.getType()).thenReturn(DataObjectType.VOLUME);
        when(destObj.getTO()).thenReturn(dstTO);
        when(srcObj.getTO()).thenReturn(srcTO);

        when(srcObj.getDataStore().getDriver()).thenReturn(storPoolPrimaryDataStoreDriver);
        when(destObj.getTO().getDataStore()).thenReturn(dstPrimaryTo);

        when(srcTO.getPath()).thenReturn("/dev/storpool-byid/t.t.t");
        when(srcTO.getUuid()).thenReturn(UUID.randomUUID().toString());
        when(dstPrimaryTo.getPoolType()).thenReturn(StoragePoolType.StorPool);
        when(vmInstanceDao.findById(anyLong())).thenReturn(vm);
    }
}
