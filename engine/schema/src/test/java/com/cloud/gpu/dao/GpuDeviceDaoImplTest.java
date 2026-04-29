/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.gpu.dao;

import com.cloud.gpu.GpuDeviceVO;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GpuDeviceDaoImplTest {

    @Spy
    @InjectMocks
    GpuDeviceDaoImpl gpuDeviceDao = new GpuDeviceDaoImpl();

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void listByIds_emptyList() {
        List<GpuDeviceVO> devices = gpuDeviceDao.listByIds(null);
        Assert.assertTrue("Expected empty list", devices.isEmpty());
        devices = gpuDeviceDao.listByIds(Collections.emptyList());
        Assert.assertTrue("Expected empty list", devices.isEmpty());
    }

    @Test
    public void listByIds() {
        doReturn(List.of(mock(GpuDeviceVO.class))).when(gpuDeviceDao).listBy(any(SearchCriteria.class));

        List<GpuDeviceVO> devices = gpuDeviceDao.listByIds(List.of(1L, 2L, 3L));

        Assert.assertFalse("Expected non empty list", devices.isEmpty());

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuDeviceDao).listBy(scCaptor.capture());
        SearchCriteria<GpuDeviceVO> sc = scCaptor.getValue();
        Assert.assertEquals("Expected correct where clause", "gpu_device.id IN (?,?,?)", sc.getWhereClause().trim());
    }

    @Test
    public void findByHostIdAndBusAddress() {
        doReturn(mock(GpuDeviceVO.class)).when(gpuDeviceDao).findOneBy(any(SearchCriteria.class));

        GpuDeviceVO device = gpuDeviceDao.findByHostIdAndBusAddress(1L, "0000:00:1f.6");

        Assert.assertNotNull("Expected non-null device", device);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuDeviceDao).findOneBy(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "gpu_device.host_id = ?  AND gpu_device.bus_address = ?",
                scCaptor.getValue().getWhereClause().trim());
    }

    @Test
    public void listByHostId() {
        doReturn(List.of(mock(GpuDeviceVO.class))).when(gpuDeviceDao).listBy(any(SearchCriteria.class));

        List<GpuDeviceVO> devices = gpuDeviceDao.listByHostId(1L);

        Assert.assertFalse("Expected non empty list", devices.isEmpty());

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuDeviceDao).listBy(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "gpu_device.host_id = ?",
                scCaptor.getValue().getWhereClause().trim());
    }

    @Test
    public void listByVmId() {
        doReturn(List.of(mock(GpuDeviceVO.class))).when(gpuDeviceDao).listBy(any(SearchCriteria.class));

        List<GpuDeviceVO> devices = gpuDeviceDao.listByVmId(1L);

        Assert.assertFalse("Expected non empty list", devices.isEmpty());
        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuDeviceDao).listBy(scCaptor.capture());

        Assert.assertEquals("Expected correct where clause", "gpu_device.vm_id = ?",
                scCaptor.getValue().getWhereClause().trim());
    }

    @Test
    public void isVgpuProfileInUse() {
        doReturn(1).when(gpuDeviceDao).getCount(any(SearchCriteria.class));

        boolean vgpuProfileInUse = gpuDeviceDao.isVgpuProfileInUse(1L);

        Assert.assertTrue("Expected vGPU profile to be in use", vgpuProfileInUse);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuDeviceDao).getCount(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "gpu_device.vgpu_profile_id = ?",
                scCaptor.getValue().getWhereClause().trim());
    }

    @Test
    public void isGpuCardInUse() {
        doReturn(1).when(gpuDeviceDao).getCount(any(SearchCriteria.class));

        boolean vgpuProfileInUse = gpuDeviceDao.isGpuCardInUse(1L);

        Assert.assertTrue("Expected GPU Card to be in use", vgpuProfileInUse);

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuDeviceDao).getCount(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "gpu_device.card_id = ?",
                scCaptor.getValue().getWhereClause().trim());
    }

    @Test
    public void listByHostAndVm() {
        doReturn(List.of(mock(GpuDeviceVO.class))).when(gpuDeviceDao).search(any(SearchCriteria.class), any());

        List<GpuDeviceVO> devices = gpuDeviceDao.listByHostAndVm(1L, 2L);

        Assert.assertFalse("Expected non empty list", devices.isEmpty());

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
        verify(gpuDeviceDao).search(scCaptor.capture(), filterCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "gpu_device.host_id = ?  AND gpu_device.vm_id = ?",
                scCaptor.getValue().getWhereClause().trim());
        Assert.assertNull("Expected no filter", filterCaptor.getValue());
    }

    @Test
    public void listDevicesForAllocation() {
        doReturn(List.of(mock(GpuDeviceVO.class))).when(gpuDeviceDao).search(any(SearchCriteria.class), any());

        List<GpuDeviceVO> devices = gpuDeviceDao.listDevicesForAllocation(1L, 2L);

        Assert.assertFalse("Expected non empty list", devices.isEmpty());

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
        verify(gpuDeviceDao).search(scCaptor.capture(), filterCaptor.capture());
        Assert.assertEquals("Expected correct where clause",
                "gpu_device.host_id = ?  AND gpu_device.vgpu_profile_id=? AND gpu_device.state = ?  AND gpu_device"
                + ".managed_state = ?  AND gpu_device.type != ?",
                scCaptor.getValue().getWhereClause().trim());
        Assert.assertNull("Expected no filter", filterCaptor.getValue());
    }

    @Test
    public void searchAndCountGpuDevices() {
    }

    @Test
    public void getDistinctGpuCardIds_no_devices() {
        doReturn(null).when(gpuDeviceDao).listBy(any(SearchCriteria.class));

        List<Long> cardIds = gpuDeviceDao.getDistinctGpuCardIds();

        Assert.assertTrue("Expected empty list", cardIds.isEmpty());

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuDeviceDao).listBy(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "", scCaptor.getValue().getWhereClause().trim());
    }


    @Test
    public void getDistinctGpuCardIds() {
        GpuDeviceVO device1 = mock(GpuDeviceVO.class);
        GpuDeviceVO device2 = mock(GpuDeviceVO.class);
        GpuDeviceVO device3 = mock(GpuDeviceVO.class);
        when(device1.getCardId()).thenReturn(1L);
        when(device2.getCardId()).thenReturn(2L);
        when(device3.getCardId()).thenReturn(1L);

        doReturn(List.of(device1, device2, device3)).when(gpuDeviceDao).listBy(any(SearchCriteria.class));

        List<Long> cardIds = gpuDeviceDao.getDistinctGpuCardIds();

        Assert.assertEquals("Expected 2 card IDs", 2, cardIds.size());

        Assert.assertTrue("Expected card ID 1 in list", cardIds.contains(1L));
        Assert.assertTrue("Expected card ID 2 in list", cardIds.contains(2L));

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuDeviceDao).listBy(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "", scCaptor.getValue().getWhereClause().trim());
    }

    @Test
    public void getDistinctVgpuProfileIds_no_devices() {
        doReturn(null).when(gpuDeviceDao).listBy(any(SearchCriteria.class));

        List<Long> cardIds = gpuDeviceDao.getDistinctVgpuProfileIds();

        Assert.assertTrue("Expected empty list", cardIds.isEmpty());

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuDeviceDao).listBy(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "", scCaptor.getValue().getWhereClause().trim());
    }


    @Test
    public void getDistinctVgpuProfileIds() {
        GpuDeviceVO device1 = mock(GpuDeviceVO.class);
        GpuDeviceVO device2 = mock(GpuDeviceVO.class);
        GpuDeviceVO device3 = mock(GpuDeviceVO.class);
        when(device1.getVgpuProfileId()).thenReturn(1L);
        when(device2.getVgpuProfileId()).thenReturn(2L);
        when(device3.getVgpuProfileId()).thenReturn(1L);

        doReturn(List.of(device1, device2, device3)).when(gpuDeviceDao).listBy(any(SearchCriteria.class));

        List<Long> cardIds = gpuDeviceDao.getDistinctVgpuProfileIds();

        Assert.assertEquals("Expected 2 VgpuProfile IDs", 2, cardIds.size());

        Assert.assertTrue("Expected VgpuProfile ID 1 in list", cardIds.contains(1L));
        Assert.assertTrue("Expected VgpuProfile ID 2 in list", cardIds.contains(2L));

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuDeviceDao).listBy(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "", scCaptor.getValue().getWhereClause().trim());
    }


    @Test
    public void listByParentGpuDeviceId() {
        doReturn(List.of(mock(GpuDeviceVO.class))).when(gpuDeviceDao).listBy(any(SearchCriteria.class));

        List<GpuDeviceVO> devices = gpuDeviceDao.listByParentGpuDeviceId(1L);

        Assert.assertFalse("Expected non empty list", devices.isEmpty());

        ArgumentCaptor<SearchCriteria> scCaptor = ArgumentCaptor.forClass(SearchCriteria.class);
        verify(gpuDeviceDao).listBy(scCaptor.capture());
        Assert.assertEquals("Expected correct where clause", "gpu_device.parent_gpu_device_id = ?",
                scCaptor.getValue().getWhereClause().trim());
    }
}
