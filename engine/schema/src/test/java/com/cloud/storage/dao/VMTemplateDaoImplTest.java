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
package com.cloud.storage.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.cpu.CPU;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class VMTemplateDaoImplTest {

    @Mock
    HostDao hostDao;

    @Mock
    VMTemplateZoneDao templateZoneDao;

    @Spy
    @InjectMocks
    VMTemplateDaoImpl templateDao = new VMTemplateDaoImpl();

    @Test
    public void testFindLatestTemplateByName_ReturnsTemplate() {
        VMTemplateVO expectedTemplate = new VMTemplateVO();
        List<VMTemplateVO> returnedList = Collections.singletonList(expectedTemplate);
        doReturn(returnedList).when(templateDao).listBy(any(SearchCriteria.class), any(Filter.class));
        VMTemplateVO result = templateDao.findLatestTemplateByName("test", CPU.CPUArch.getDefault());
        assertNotNull("Expected a non-null template", result);
        assertEquals("Expected the returned template to be the first element", expectedTemplate, result);
    }

    @Test
    public void testFindLatestTemplateByName_ReturnsNullWhenNoTemplateFound() {
        List<VMTemplateVO> emptyList = Collections.emptyList();
        doReturn(emptyList).when(templateDao).listBy(any(SearchCriteria.class), any(Filter.class));
        VMTemplateVO result = templateDao.findLatestTemplateByName("test", CPU.CPUArch.getDefault());
        assertNull("Expected null when no templates are found", result);
    }

    @Test
    public void testFindLatestTemplateByName_NullArch() {
        VMTemplateVO expectedTemplate = new VMTemplateVO();
        List<VMTemplateVO> returnedList = Collections.singletonList(expectedTemplate);
        doReturn(returnedList).when(templateDao).listBy(any(SearchCriteria.class), any(Filter.class));
        VMTemplateVO result = templateDao.findLatestTemplateByName("test", null);
        assertNotNull("Expected a non-null template even if arch is null", result);
        assertEquals("Expected the returned template to be the first element", expectedTemplate, result);
    }

    @Test
    public void testGetSortedTemplatesListWithPreferredArch_PreferredProvided() {
        VMTemplateVO templatePreferred = Mockito.mock(VMTemplateVO.class);
        when(templatePreferred.getArch()).thenReturn(CPU.CPUArch.amd64);
        VMTemplateVO templateOther = Mockito.mock(VMTemplateVO.class);
        when(templateOther.getArch()).thenReturn(CPU.CPUArch.arm64);

        Map<Pair<Hypervisor.HypervisorType, CPU.CPUArch>, VMTemplateVO> uniqueTemplates = new HashMap<>();
        uniqueTemplates.put(new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64), templatePreferred);
        uniqueTemplates.put(new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.arm64), templateOther);
        List<VMTemplateVO> sortedList = templateDao.getSortedTemplatesListWithPreferredArch(uniqueTemplates,
                CPU.CPUArch.amd64.getType());
        assertEquals(2, sortedList.size());
        assertEquals(templatePreferred, sortedList.get(0));
        assertEquals(templateOther, sortedList.get(1));
    }

    @Test
    public void testGetSortedTemplatesListWithPreferredArch_NoPreferred() {
        VMTemplateVO template1 = Mockito.mock(VMTemplateVO.class);
        when(template1.getId()).thenReturn(1L);
        VMTemplateVO template2 = Mockito.mock(VMTemplateVO.class);
        when(template2.getId()).thenReturn(2L);
        Map<Pair<Hypervisor.HypervisorType, CPU.CPUArch>, VMTemplateVO> uniqueTemplates = new HashMap<>();
        uniqueTemplates.put(new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64), template1);
        uniqueTemplates.put(new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.arm64), template2);
        List<VMTemplateVO> sortedList = templateDao.getSortedTemplatesListWithPreferredArch(uniqueTemplates, "");
        assertEquals(2, sortedList.size());
        assertEquals(template2, sortedList.get(0));
        assertEquals(template1, sortedList.get(1));
    }

    @Test
    public void testFindSystemVMReadyTemplates() {
        long zoneId = 1L;
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        String preferredArch = CPU.CPUArch.arm64.getType();
        List<Pair<Hypervisor.HypervisorType, CPU.CPUArch>> availableHypervisors = new ArrayList<>();
        availableHypervisors.add(new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.amd64));
        availableHypervisors.add(new Pair<>(Hypervisor.HypervisorType.KVM, CPU.CPUArch.arm64));
        doReturn(availableHypervisors).when(hostDao).listDistinctHypervisorArchTypes(zoneId);
        VMTemplateVO template1 = Mockito.mock(VMTemplateVO.class);
        when(template1.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(template1.getArch()).thenReturn(CPU.CPUArch.amd64);
        VMTemplateVO template2 = Mockito.mock(VMTemplateVO.class);
        when(template2.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(template2.getArch()).thenReturn(CPU.CPUArch.arm64);
        List<VMTemplateVO> templatesFromDb = Arrays.asList(template1, template2);
        doReturn(templatesFromDb).when(templateDao).listBy(any(), any());
        SearchBuilder<VMTemplateVO> sb = mock(SearchBuilder.class);
        templateDao.readySystemTemplateSearch = sb;
        when(sb.create()).thenReturn(mock(SearchCriteria.class));
        List<VMTemplateVO> result = templateDao.findSystemVMReadyTemplates(zoneId, hypervisorType, preferredArch);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(template2, result.get(0));
        assertEquals(template1, result.get(1));
    }

    @Test
    public void testFindRoutingTemplates() {
        Hypervisor.HypervisorType hType = Hypervisor.HypervisorType.KVM;
        String templateName = "TestRouting";
        String preferredArch = CPU.CPUArch.amd64.getType();
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(template.getArch()).thenReturn(CPU.CPUArch.amd64);
        List<VMTemplateVO> templatesFromDb = Collections.singletonList(template);
        doReturn(templatesFromDb).when(templateDao).listBy(any(), any());
        SearchBuilder<VMTemplateVO> sb = mock(SearchBuilder.class);
        when(sb.create()).thenReturn(mock(SearchCriteria.class));
        templateDao.tmpltTypeHyperSearch2 = sb;
        List<VMTemplateVO> result = templateDao.findRoutingTemplates(hType, templateName, preferredArch);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(template, result.get(0));
    }

    @Test
    public void testFindLatestTemplateByTypeAndHypervisorAndArch_Found() {
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        CPU.CPUArch arch = CPU.CPUArch.amd64;
        Storage.TemplateType type = Storage.TemplateType.SYSTEM;
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        List<VMTemplateVO> templatesFromDb = Collections.singletonList(template);
        doReturn(templatesFromDb).when(templateDao).listBy(any(), any());
        VMTemplateVO result = templateDao.findLatestTemplateByTypeAndHypervisorAndArch(hypervisorType, arch, type);
        assertNotNull(result);
        assertEquals(template, result);
    }

    @Test
    public void testFindLatestTemplateByTypeAndHypervisorAndArch_NotFound() {
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        CPU.CPUArch arch = CPU.CPUArch.x86;
        Storage.TemplateType type = Storage.TemplateType.SYSTEM;
        doReturn(Collections.emptyList()).when(templateDao).listBy(any(), any());
        VMTemplateVO result = templateDao.findLatestTemplateByTypeAndHypervisorAndArch(hypervisorType, arch, type);
        assertNull(result);
    }

    private void mockTemplateZoneJoin() {
        VMTemplateZoneVO templateZoneVO = mock(VMTemplateZoneVO.class);
        SearchBuilder<VMTemplateZoneVO> templateZoneVOSearchBuilder = mock(SearchBuilder.class);
        when(templateZoneVOSearchBuilder.entity()).thenReturn(templateZoneVO);
        when(templateZoneDao.createSearchBuilder()).thenReturn(templateZoneVOSearchBuilder);
    }

    @Test
    public void testListTemplateIsoByArchAndZone_WithDataCenterId() {
        Long dataCenterId = 1L;
        CPU.CPUArch arch = CPU.CPUArch.getDefault();
        Boolean isIso = true;
        VMTemplateVO templateVO = mock(VMTemplateVO.class);
        GenericSearchBuilder<VMTemplateVO, Long> searchBuilder = mock(GenericSearchBuilder.class);
        when(searchBuilder.entity()).thenReturn(templateVO);
        SearchCriteria<Long>searchCriteria = mock(SearchCriteria.class);
        when(templateDao.createSearchBuilder(Long.class)).thenReturn(searchBuilder);
        when(searchBuilder.create()).thenReturn(searchCriteria);
        mockTemplateZoneJoin();
        doReturn(new ArrayList<>()).when(templateDao).customSearch(searchCriteria, null);
        List<Long> result = templateDao.listTemplateIsoByArchVnfAndZone(dataCenterId, arch, isIso, false);
        assertNotNull(result);
        verify(searchBuilder, times(1)).select(null, SearchCriteria.Func.DISTINCT, templateVO.getGuestOSId());
        verify(searchBuilder, times(1)).and(eq("state"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder, times(1)).and(eq("type"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder, times(1)).and(eq("arch"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder, times(1)).and(eq("isIso"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder, times(1)).join(eq("templateZoneSearch"), any(), any(), any(), eq(JoinBuilder.JoinType.INNER));
        verify(templateDao, times(1)).customSearch(searchCriteria, null);
    }

    @Test
    public void testListTemplateIsoByArchAndZone_WithoutDataCenterId() {
        Long dataCenterId = null;
        CPU.CPUArch arch = CPU.CPUArch.getDefault();
        Boolean isIso = false;
        VMTemplateVO templateVO = mock(VMTemplateVO.class);
        GenericSearchBuilder<VMTemplateVO, Long> searchBuilder = mock(GenericSearchBuilder.class);
        when(searchBuilder.entity()).thenReturn(templateVO);
        SearchCriteria<Long>searchCriteria = mock(SearchCriteria.class);
        when(templateDao.createSearchBuilder(Long.class)).thenReturn(searchBuilder);
        when(searchBuilder.create()).thenReturn(searchCriteria);
        doReturn(new ArrayList<>()).when(templateDao).customSearch(searchCriteria, null);
        List<Long> result = templateDao.listTemplateIsoByArchVnfAndZone(dataCenterId, arch, isIso, false);
        assertNotNull(result);
        verify(searchBuilder, times(1)).select(null, SearchCriteria.Func.DISTINCT, templateVO.getGuestOSId());
        verify(searchBuilder, times(1)).and(eq("state"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder, times(1)).and(eq("type"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder, times(1)).and(eq("arch"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder, times(1)).and(eq("isIso"), any(), eq(SearchCriteria.Op.NEQ));
        verify(searchBuilder, never()).join(eq("templateZoneSearch"), any(), any(), any(), eq(JoinBuilder.JoinType.INNER));
        verify(templateDao, times(1)).customSearch(searchCriteria, null);
    }

    @Test
    public void testListTemplateIsoByArchAndZone_WithoutArch() {
        Long dataCenterId = 1L;
        CPU.CPUArch arch = null;
        Boolean isIso = true;
        VMTemplateVO templateVO = mock(VMTemplateVO.class);
        GenericSearchBuilder<VMTemplateVO, Long> searchBuilder = mock(GenericSearchBuilder.class);
        when(searchBuilder.entity()).thenReturn(templateVO);
        SearchCriteria<Long>searchCriteria = mock(SearchCriteria.class);
        when(templateDao.createSearchBuilder(Long.class)).thenReturn(searchBuilder);
        when(searchBuilder.create()).thenReturn(searchCriteria);
        mockTemplateZoneJoin();
        doReturn(new ArrayList<>()).when(templateDao).customSearch(searchCriteria, null);
        List<Long> result = templateDao.listTemplateIsoByArchVnfAndZone(dataCenterId, arch, isIso, false);
        assertNotNull(result);
        verify(searchBuilder, times(1)).select(null, SearchCriteria.Func.DISTINCT, templateVO.getGuestOSId());
        verify(searchBuilder, times(1)).and(eq("state"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder, times(1)).and(eq("type"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder, times(1)).and(eq("arch"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder, times(1)).and(eq("isIso"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder, times(1)).join(eq("templateZoneSearch"), any(), any(), any(), eq(JoinBuilder.JoinType.INNER));
        verify(templateDao, times(1)).customSearch(searchCriteria, null);
    }

    @Test
    public void testListTemplateIsoByArchAndZone_WithoutIsIso() {
        Long dataCenterId = 1L;
        CPU.CPUArch arch = CPU.CPUArch.getDefault();
        Boolean isIso = null;
        VMTemplateVO templateVO = mock(VMTemplateVO.class);
        GenericSearchBuilder<VMTemplateVO, Long> searchBuilder = mock(GenericSearchBuilder.class);
        when(searchBuilder.entity()).thenReturn(templateVO);
        SearchCriteria<Long>searchCriteria = mock(SearchCriteria.class);
        when(templateDao.createSearchBuilder(Long.class)).thenReturn(searchBuilder);
        when(searchBuilder.create()).thenReturn(searchCriteria);
        mockTemplateZoneJoin();
        doReturn(new ArrayList<>()).when(templateDao).customSearch(searchCriteria, null);
        List<Long> result = templateDao.listTemplateIsoByArchVnfAndZone(dataCenterId, arch, isIso, false);
        assertNotNull(result);
        verify(searchBuilder, times(1)).select(null, SearchCriteria.Func.DISTINCT, templateVO.getGuestOSId());
        verify(searchBuilder, times(1)).and(eq("state"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder, times(1)).and(eq("type"), any(), eq(SearchCriteria.Op.IN));
        verify(searchBuilder, times(1)).and(eq("arch"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder, never()).and(eq("isIso"), any(), eq(SearchCriteria.Op.NEQ));
        verify(searchBuilder, never()).and(eq("isIso"), any(), eq(SearchCriteria.Op.EQ));
        verify(searchBuilder, times(1)).join(eq("templateZoneSearch"), any(), any(), any(), eq(JoinBuilder.JoinType.INNER));
        verify(templateDao, times(1)).customSearch(searchCriteria, null);
    }
}
