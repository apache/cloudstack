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
//

package com.cloud.template;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateService.TemplateApiResult;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventDistributor;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.secstorage.heuristics.HeuristicType;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.heuristics.HeuristicRuleHelper;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.org.Grouping;
import com.cloud.server.StatsCollector;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class HypervisorTemplateAdapterTest {
    @Mock
    EventDistributor eventDistributor;
    List<Event> events = new ArrayList<>();

    @Mock
    TemplateManager _templateMgr;

    @Mock
    TemplateService _templateService;

    @Mock
    TemplateDataFactory _dataFactory;

    @Mock
    VMTemplateZoneDao _templateZoneDao;

    @Mock
    TemplateDataStoreDao _templateStoreDao;

    @Mock
    UsageEventDao _usageEventDao;

    @Mock
    ResourceLimitService _resourceManager;

    @Mock
    MessageBus _messageBus;

    @Mock
    AccountDao _accountDao;

    @Mock
    DataCenterDao _dcDao;

    @Mock
    ConfigurationDao _configDao;

    @Mock
    DataStoreManager dataStoreManagerMock;

    @Mock
    HeuristicRuleHelper heuristicRuleHelperMock;

    @Mock
    StatsCollector statsCollectorMock;

    @Mock
    Logger loggerMock;

    @Spy
    @InjectMocks
    HypervisorTemplateAdapter _adapter = new HypervisorTemplateAdapter();

    //UsageEventUtils reflection abuse helpers
    private Map<String, Object> oldFields = new HashMap<>();
    private List<UsageEventVO> usageEvents = new ArrayList<>();

    private MockedStatic<ComponentContext> componentContextMocked;

    private AutoCloseable closeable;

    private static final long zoneId = 1L;

    @Before
    public void before() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        if (componentContextMocked != null) {
            componentContextMocked.close();
        }
        closeable.close();
    }

    public UsageEventUtils setupUsageUtils() {
        Mockito.when(_configDao.getValue(eq("publish.usage.events"))).thenReturn("true");
        Mockito.when(_usageEventDao.persist(Mockito.any(UsageEventVO.class))).then(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock invocation) throws Throwable {
                UsageEventVO vo = (UsageEventVO)invocation.getArguments()[0];
                usageEvents.add(vo);
                return null;
            }
        });

        Mockito.when(_usageEventDao.listAll()).thenReturn(usageEvents);

        doAnswer((Answer<Void>) invocation -> {
            Event event = (Event)invocation.getArguments()[0];
            events.add(event);
            return null;
        }).when(eventDistributor).publish(any(Event.class));

        componentContextMocked = Mockito.mockStatic(ComponentContext.class);
        when(ComponentContext.getComponent(eq(EventDistributor.class))).thenReturn(eventDistributor);

        UsageEventUtils utils = new UsageEventUtils();

        Map<String, String> usageUtilsFields = new HashMap<String, String>();
        usageUtilsFields.put("usageEventDao", "_usageEventDao");
        usageUtilsFields.put("accountDao", "_accountDao");
        usageUtilsFields.put("dcDao", "_dcDao");
        usageUtilsFields.put("configDao", "_configDao");

        for (String fieldName : usageUtilsFields.keySet()) {
            try {
                Field f = UsageEventUtils.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                //Remember the old fields for cleanup later (see cleanupUsageUtils)
                Field staticField = UsageEventUtils.class.getDeclaredField("s_" + fieldName);
                staticField.setAccessible(true);
                oldFields.put(f.getName(), staticField.get(null));
                f.set(utils,
                        this.getClass()
                                .getDeclaredField(
                                        usageUtilsFields.get(fieldName))
                                .get(this));
            } catch (IllegalArgumentException | IllegalAccessException
                    | NoSuchFieldException | SecurityException e) {
                e.printStackTrace();
            }

        }
        try {
            Method method = UsageEventUtils.class.getDeclaredMethod("init");
            method.setAccessible(true);
            method.invoke(utils);
        } catch (SecurityException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException e) {
            e.printStackTrace();
        }

        return utils;
    }

    public void cleanupUsageUtils() {
        UsageEventUtils utils = new UsageEventUtils();

        for (String fieldName : oldFields.keySet()) {
            try {
                Field f = UsageEventUtils.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(utils, oldFields.get(fieldName));
            } catch (IllegalArgumentException | IllegalAccessException
                    | NoSuchFieldException | SecurityException e) {
                e.printStackTrace();
            }

        }
        try {
            Method method = UsageEventUtils.class.getDeclaredMethod("init");
            method.setAccessible(true);
            method.invoke(utils);
        } catch (SecurityException | NoSuchMethodException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void testEmitDeleteEventUuid() throws InterruptedException, ExecutionException {
        //All the mocks required for this test to work.
        ImageStoreEntity store = mock(ImageStoreEntity.class);
        when(store.getId()).thenReturn(1l);
        when(store.getDataCenterId()).thenReturn(1l);
        when(store.getName()).thenReturn("Test Store");

        TemplateDataStoreVO dataStoreVO = mock(TemplateDataStoreVO.class);
        when(dataStoreVO.getDownloadState()).thenReturn(Status.DOWNLOADED);

        TemplateInfo info = mock(TemplateInfo.class);
        when(info.getDataStore()).thenReturn(store);

        VMTemplateVO template = mock(VMTemplateVO.class);
        when(template.getId()).thenReturn(1l);
        when(template.getName()).thenReturn("Test Template");
        when(template.getFormat()).thenReturn(ImageFormat.QCOW2);
        when(template.getAccountId()).thenReturn(1l);
        when(template.getUuid()).thenReturn("Test UUID"); //TODO possibly return this from method for comparison, if things work how i want

        TemplateProfile profile = mock(TemplateProfile.class);
        when(profile.getTemplate()).thenReturn(template);
        when(profile.getZoneIdList()).thenReturn(null);

        TemplateApiResult result = mock(TemplateApiResult.class);
        when(result.isSuccess()).thenReturn(true);
        when(result.isFailed()).thenReturn(false);

        @SuppressWarnings("unchecked")
        AsyncCallFuture<TemplateApiResult> future = mock(AsyncCallFuture.class);
        when(future.get()).thenReturn(result);

        AccountVO acct = mock(AccountVO.class);
        when(acct.getId()).thenReturn(1l);
        when(acct.getDomainId()).thenReturn(1l);

        when(_templateMgr.getImageStoreByTemplate(anyLong(), anyLong())).thenReturn(Collections.singletonList((DataStore)store));
        when(_templateStoreDao.listByTemplateStore(anyLong(), anyLong())).thenReturn(Collections.singletonList(dataStoreVO));
        when(_dataFactory.getTemplate(anyLong(), any(DataStore.class))).thenReturn(info);
        when(_dataFactory.listTemplateOnCache(anyLong())).thenReturn(Collections.singletonList(info));
        when(_templateService.deleteTemplateAsync(any(TemplateInfo.class))).thenReturn(future);
        when(_accountDao.findById(anyLong())).thenReturn(acct);
        when(_accountDao.findByIdIncludingRemoved(anyLong())).thenReturn(acct);

        //Test actually begins here.
        setupUsageUtils();

        _adapter.delete(profile);
        Assert.assertNotNull(usageEvents);
        Assert.assertNotNull(events);
        Assert.assertEquals(1, events.size());

        Event event = events.get(0);
        Assert.assertNotNull(event);
        Assert.assertNotNull(event.getResourceType());
        Assert.assertEquals(VirtualMachineTemplate.class.getName(), event.getResourceType());
        Assert.assertNotNull(event.getResourceUUID());
        Assert.assertEquals("Test UUID",  event.getResourceUUID());
        Assert.assertEquals(EventTypes.EVENT_TEMPLATE_DELETE, event.getEventType());


        cleanupUsageUtils();
    }

    @Test
    public void createTemplateWithinZonesTestZoneIdsNullShouldCallListAllZones() {
        TemplateProfile templateProfileMock = Mockito.mock(TemplateProfile.class);
        VMTemplateVO vmTemplateVOMock = Mockito.mock(VMTemplateVO.class);

        Mockito.when(templateProfileMock.getZoneIdList()).thenReturn(null);

        _adapter.createTemplateWithinZones(templateProfileMock, vmTemplateVOMock);

        Mockito.verify(_dcDao, Mockito.times(1)).listAllZones();
    }

    @Test
    public void createTemplateWithinZonesTestZoneIdsNotNullShouldNotCallListAllZones() {
        TemplateProfile templateProfileMock = Mockito.mock(TemplateProfile.class);
        VMTemplateVO vmTemplateVOMock = Mockito.mock(VMTemplateVO.class);
        List<Long> zoneIds = List.of(1L);

        Mockito.when(templateProfileMock.getZoneIdList()).thenReturn(zoneIds);
        Mockito.doReturn(null).when(_adapter).getImageStoresThrowsExceptionIfNotFound(Mockito.any(Long.class), Mockito.any(TemplateProfile.class));
        Mockito.doReturn(null).when(_adapter).verifyHeuristicRulesForZone(Mockito.any(VMTemplateVO.class), Mockito.anyLong());
        Mockito.doNothing().when(_adapter).standardImageStoreAllocation(Mockito.isNull(), Mockito.any(VMTemplateVO.class));

        _adapter.createTemplateWithinZones(templateProfileMock, vmTemplateVOMock);

        Mockito.verify(_dcDao, Mockito.times(0)).listAllZones();
    }

    @Test
    public void createTemplateWithinZonesTestZoneDoesNotHaveActiveHeuristicRulesShouldCallStandardImageStoreAllocation() {
        TemplateProfile templateProfileMock = Mockito.mock(TemplateProfile.class);
        VMTemplateVO vmTemplateVOMock = Mockito.mock(VMTemplateVO.class);
        List<Long> zoneIds = List.of(zoneId);

        Mockito.when(templateProfileMock.getZoneIdList()).thenReturn(zoneIds);
        Mockito.doReturn(null).when(_adapter).getImageStoresThrowsExceptionIfNotFound(Mockito.any(Long.class), Mockito.any(TemplateProfile.class));
        Mockito.doReturn(null).when(_adapter).verifyHeuristicRulesForZone(Mockito.any(VMTemplateVO.class), Mockito.anyLong());
        Mockito.doNothing().when(_adapter).standardImageStoreAllocation(Mockito.isNull(), Mockito.any(VMTemplateVO.class));

        _adapter.createTemplateWithinZones(templateProfileMock, vmTemplateVOMock);

        Mockito.verify(_adapter, Mockito.times(1)).standardImageStoreAllocation(Mockito.isNull(), Mockito.any(VMTemplateVO.class));
    }

    @Test
    public void createTemplateWithinZonesTestZoneWithHeuristicRuleShouldCallValidateSecondaryStorageAndCreateTemplate() {
        TemplateProfile templateProfileMock = Mockito.mock(TemplateProfile.class);
        VMTemplateVO vmTemplateVOMock = Mockito.mock(VMTemplateVO.class);
        DataStore dataStoreMock = Mockito.mock(DataStore.class);
        List<Long> zoneIds = List.of(1L);

        Mockito.when(templateProfileMock.getZoneIdList()).thenReturn(zoneIds);
        Mockito.doReturn(dataStoreMock).when(_adapter).verifyHeuristicRulesForZone(Mockito.any(VMTemplateVO.class), Mockito.anyLong());
        Mockito.doNothing().when(_adapter).validateSecondaryStorageAndCreateTemplate(Mockito.any(List.class), Mockito.any(VMTemplateVO.class), Mockito.isNull());

        _adapter.createTemplateWithinZones(templateProfileMock, vmTemplateVOMock);

        Mockito.verify(_adapter, Mockito.times(1)).validateSecondaryStorageAndCreateTemplate(Mockito.any(List.class), Mockito.any(VMTemplateVO.class), Mockito.isNull());
    }

    @Test(expected = CloudRuntimeException.class)
    public void getImageStoresThrowsExceptionIfNotFoundTestNullImageStoreShouldThrowCloudRuntimeException() {
        TemplateProfile templateProfileMock = Mockito.mock(TemplateProfile.class);

        Mockito.when(dataStoreManagerMock.getImageStoresByZoneIds(Mockito.anyLong())).thenReturn(null);

        _adapter.getImageStoresThrowsExceptionIfNotFound(zoneId, templateProfileMock);
    }

    @Test(expected = CloudRuntimeException.class)
    public void getImageStoresThrowsExceptionIfNotFoundTestEmptyImageStoreShouldThrowCloudRuntimeException() {
        TemplateProfile templateProfileMock = Mockito.mock(TemplateProfile.class);
        List<DataStore> imageStoresList = new ArrayList<>();

        Mockito.when(dataStoreManagerMock.getImageStoresByZoneIds(Mockito.anyLong())).thenReturn(imageStoresList);

        _adapter.getImageStoresThrowsExceptionIfNotFound(zoneId, templateProfileMock);
    }

    @Test
    public void getImageStoresThrowsExceptionIfNotFoundTestNonEmptyImageStoreShouldNotThrowCloudRuntimeException() {
        TemplateProfile templateProfileMock = Mockito.mock(TemplateProfile.class);
        DataStore dataStoreMock = Mockito.mock(DataStore.class);
        List<DataStore> imageStoresList = List.of(dataStoreMock);

        Mockito.when(dataStoreManagerMock.getImageStoresByZoneIds(Mockito.anyLong())).thenReturn(imageStoresList);

        _adapter.getImageStoresThrowsExceptionIfNotFound(zoneId, templateProfileMock);
    }

    @Test
    public void verifyHeuristicRulesForZoneTestTemplateIsISOFormatShouldCheckForISOHeuristicType() {
        VMTemplateVO vmTemplateVOMock = Mockito.mock(VMTemplateVO.class);

        Mockito.when(vmTemplateVOMock.getFormat()).thenReturn(ImageFormat.ISO);
        _adapter.verifyHeuristicRulesForZone(vmTemplateVOMock, 1L);

        Mockito.verify(heuristicRuleHelperMock, Mockito.times(1)).getImageStoreIfThereIsHeuristicRule(1L, HeuristicType.ISO, vmTemplateVOMock);
    }

    @Test
    public void verifyHeuristicRulesForZoneTestTemplateNotISOFormatShouldCheckForTemplateHeuristicType() {
        VMTemplateVO vmTemplateVOMock = Mockito.mock(VMTemplateVO.class);

        Mockito.when(vmTemplateVOMock.getFormat()).thenReturn(ImageFormat.QCOW2);
        _adapter.verifyHeuristicRulesForZone(vmTemplateVOMock, 1L);

        Mockito.verify(heuristicRuleHelperMock, Mockito.times(1)).getImageStoreIfThereIsHeuristicRule(1L, HeuristicType.TEMPLATE, vmTemplateVOMock);
    }

    @Test
    public void isZoneAndImageStoreAvailableTestZoneIdIsNullShouldReturnFalse() {
        DataStore dataStoreMock = Mockito.mock(DataStore.class);
        Long zoneId = null;
        Set<Long> zoneSet = null;
        boolean isTemplatePrivate = false;

        boolean result = _adapter.isZoneAndImageStoreAvailable(dataStoreMock, zoneId, zoneSet, isTemplatePrivate);

        Mockito.verify(loggerMock, Mockito.times(1)).warn(String.format("Zone ID is null, cannot allocate ISO/template in image store [%s].", dataStoreMock));
        Assert.assertFalse(result);
    }

    @Test
    public void isZoneAndImageStoreAvailableTestZoneIsNullShouldReturnFalse() {
        DataStore dataStoreMock = Mockito.mock(DataStore.class);
        Long zoneId = 1L;
        Set<Long> zoneSet = null;
        boolean isTemplatePrivate = false;
        DataCenterVO dataCenterVOMock = null;

        Mockito.when(_dcDao.findById(Mockito.anyLong())).thenReturn(dataCenterVOMock);
        Mockito.when(dataStoreMock.getId()).thenReturn(2L);

        boolean result = _adapter.isZoneAndImageStoreAvailable(dataStoreMock, zoneId, zoneSet, isTemplatePrivate);

        Mockito.verify(loggerMock, Mockito.times(1)).warn(String.format("Unable to find zone by id [%s], so skip downloading template to its image store [%s].",
                zoneId, dataStoreMock.getId()));
        Assert.assertFalse(result);
    }

    @Test
    public void isZoneAndImageStoreAvailableTestZoneIsDisabledShouldReturnFalse() {
        DataStore dataStoreMock = Mockito.mock(DataStore.class);
        Long zoneId = 1L;
        Set<Long> zoneSet = null;
        boolean isTemplatePrivate = false;
        DataCenterVO dataCenterVOMock = Mockito.mock(DataCenterVO.class);

        Mockito.when(_dcDao.findById(Mockito.anyLong())).thenReturn(dataCenterVOMock);
        Mockito.when(dataCenterVOMock.getAllocationState()).thenReturn(Grouping.AllocationState.Disabled);
        Mockito.when(dataStoreMock.getId()).thenReturn(2L);

        boolean result = _adapter.isZoneAndImageStoreAvailable(dataStoreMock, zoneId, zoneSet, isTemplatePrivate);

        Mockito.verify(loggerMock, Mockito.times(1)).info(String.format("Zone [%s] is disabled. Skip downloading template to its image store [%s].", zoneId, dataStoreMock.getId()));
        Assert.assertFalse(result);
    }

    @Test
    public void isZoneAndImageStoreAvailableTestImageStoreDoesNotHaveEnoughCapacityShouldReturnFalse() {
        DataStore dataStoreMock = Mockito.mock(DataStore.class);
        Long zoneId = 1L;
        Set<Long> zoneSet = null;
        boolean isTemplatePrivate = false;
        DataCenterVO dataCenterVOMock = Mockito.mock(DataCenterVO.class);

        Mockito.when(_dcDao.findById(Mockito.anyLong())).thenReturn(dataCenterVOMock);
        Mockito.when(dataCenterVOMock.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(dataStoreMock.getId()).thenReturn(2L);
        Mockito.when(statsCollectorMock.imageStoreHasEnoughCapacity(any(DataStore.class))).thenReturn(false);

        boolean result = _adapter.isZoneAndImageStoreAvailable(dataStoreMock, zoneId, zoneSet, isTemplatePrivate);

        Mockito.verify(loggerMock, times(1)).info(String.format("Image store doesn't have enough capacity. Skip downloading template to this image store [%s].",
                dataStoreMock.getId()));
        Assert.assertFalse(result);
    }

    @Test
    public void isZoneAndImageStoreAvailableTestImageStoreHasEnoughCapacityAndZoneSetIsNullShouldReturnTrue() {
        DataStore dataStoreMock = Mockito.mock(DataStore.class);
        Long zoneId = 1L;
        Set<Long> zoneSet = null;
        boolean isTemplatePrivate = false;
        DataCenterVO dataCenterVOMock = Mockito.mock(DataCenterVO.class);

        Mockito.when(_dcDao.findById(Mockito.anyLong())).thenReturn(dataCenterVOMock);
        Mockito.when(dataCenterVOMock.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(statsCollectorMock.imageStoreHasEnoughCapacity(any(DataStore.class))).thenReturn(true);

        boolean result = _adapter.isZoneAndImageStoreAvailable(dataStoreMock, zoneId, zoneSet, isTemplatePrivate);

        Mockito.verify(loggerMock, times(1)).info(String.format("Zone set is null; therefore, the ISO/template should be allocated in every secondary storage " +
                "of zone [%s].", dataCenterVOMock));
        Assert.assertTrue(result);
    }

    @Test
    public void isZoneAndImageStoreAvailableTestTemplateIsPrivateAndItIsAlreadyAllocatedToTheSameZoneShouldReturnFalse() {
        DataStore dataStoreMock = Mockito.mock(DataStore.class);
        Long zoneId = 1L;
        Set<Long> zoneSet = Set.of(1L);
        boolean isTemplatePrivate = true;
        DataCenterVO dataCenterVOMock = Mockito.mock(DataCenterVO.class);

        Mockito.when(_dcDao.findById(Mockito.anyLong())).thenReturn(dataCenterVOMock);
        Mockito.when(dataCenterVOMock.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(statsCollectorMock.imageStoreHasEnoughCapacity(any(DataStore.class))).thenReturn(true);

        boolean result = _adapter.isZoneAndImageStoreAvailable(dataStoreMock, zoneId, zoneSet, isTemplatePrivate);

        Mockito.verify(loggerMock, times(1)).info(String.format("The template is private and it is already allocated in a secondary storage in zone [%s]; " +
                "therefore, image store [%s] will be skipped.", dataCenterVOMock, dataStoreMock));
        Assert.assertFalse(result);
    }

    @Test
    public void isZoneAndImageStoreAvailableTestTemplateIsPrivateAndItIsNotAlreadyAllocatedToTheSameZoneShouldReturnTrue() {
        DataStore dataStoreMock = Mockito.mock(DataStore.class);
        Long zoneId = 1L;
        Set<Long> zoneSet = new HashSet<>();
        boolean isTemplatePrivate = true;
        DataCenterVO dataCenterVOMock = Mockito.mock(DataCenterVO.class);

        Mockito.when(_dcDao.findById(Mockito.anyLong())).thenReturn(dataCenterVOMock);
        Mockito.when(dataCenterVOMock.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(statsCollectorMock.imageStoreHasEnoughCapacity(any(DataStore.class))).thenReturn(true);

        boolean result = _adapter.isZoneAndImageStoreAvailable(dataStoreMock, zoneId, zoneSet, isTemplatePrivate);

        Mockito.verify(loggerMock, times(1)).info(String.format("Private template will be allocated in image store [%s] in zone [%s].",
                dataStoreMock, dataCenterVOMock));
        Assert.assertTrue(result);
    }

    @Test
    public void testCheckZoneImageStoresDirectDownloadTemplate() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        Mockito.when(templateVO.isDirectDownload()).thenReturn(true);
        _adapter.checkZoneImageStores(templateVO, List.of(1L));
    }

    @Test
    public void testCheckZoneImageStoresRegularTemplateWithStore() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        Mockito.when(templateVO.isDirectDownload()).thenReturn(false);
        Mockito.when(dataStoreManagerMock.getImageStoresByScope(Mockito.any())).thenReturn(List.of(Mockito.mock(DataStore.class)));
        _adapter.checkZoneImageStores(templateVO, List.of(1L));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckZoneImageStoresRegularTemplateNoStore() {
        VMTemplateVO templateVO = Mockito.mock(VMTemplateVO.class);
        Mockito.when(templateVO.isDirectDownload()).thenReturn(false);
        Mockito.when(dataStoreManagerMock.getImageStoresByScope(Mockito.any())).thenReturn(new ArrayList<>());
        _adapter.checkZoneImageStores(templateVO, List.of(1L));
    }
}
