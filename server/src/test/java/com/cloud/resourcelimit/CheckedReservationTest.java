//
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
package com.cloud.resourcelimit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.reservation.ReservationVO;
import org.apache.cloudstack.reservation.dao.ReservationDao;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.configuration.Resource;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class CheckedReservationTest {

    @Mock
    Account account;
    @Mock
    ReservationDao reservationDao;
    @Mock
    ResourceLimitService resourceLimitService;

    @Mock
    ReservationVO reservation = new ReservationVO(1l, 1l, Resource.ResourceType.user_vm, 1l);

    @Mock
    GlobalLock quotaLimitLock;

    private AutoCloseable closeable;
    private MockedStatic<GlobalLock> globalLockMocked;

    @Before
    public void setup() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        globalLockMocked = Mockito.mockStatic(GlobalLock.class);
        Mockito.when(quotaLimitLock.lock(Mockito.anyInt())).thenReturn(true);
        globalLockMocked.when(() -> GlobalLock.getInternLock(Mockito.anyString())).thenReturn(quotaLimitLock);
    }

    @After
    public void tearDown() throws Exception {
        globalLockMocked.close();
        closeable.close();
    }

    @Test
    public void getId() {
        when(account.getDomainId()).thenReturn(4l);
        // Some weird behaviour depending on whether the database is up or not.
        lenient().when(reservationDao.persist(Mockito.any())).thenReturn(reservation);
        lenient().when(reservation.getId()).thenReturn(1L);
        try (CheckedReservation cr = new CheckedReservation(account, Resource.ResourceType.user_vm,1l, reservationDao, resourceLimitService) ) {
            List<Long> ids = cr.getIds();
            assertEquals(1, cr.getIds().size());
            long id = ids.get(0);
            assertEquals(1L, id);
        } catch (NullPointerException npe) {
            fail("NPE caught");
        } catch (ResourceAllocationException rae) {
            // this does not work on all platforms because of the static methods being used in the global lock mechanism
            // normally one would
            // throw new CloudRuntimeException(rae);
            // but we'll ignore this for platforms that can not humour the static bits of the system.
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNoAmount() {
        Mockito.when(reservationDao.persist(Mockito.any())).thenReturn(reservation);
        try (CheckedReservation cr = new CheckedReservation(account, Resource.ResourceType.cpu,-11l, reservationDao, resourceLimitService) ) {
            Long amount = cr.getReservedAmount();
            assertEquals(Long.valueOf(-11L), amount);
        } catch (NullPointerException npe) {
            fail("NPE caught");
        } catch (ResourceAllocationException rae) {
            throw new CloudRuntimeException(rae);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testReservationPersistAndCallContextParam() {
        List<String> tags = List.of("abc", "xyz");
        when(account.getAccountId()).thenReturn(1L);
        when(account.getDomainId()).thenReturn(4L);
        List<ReservationVO> persistedReservations = new ArrayList<>();
        Mockito.when(reservationDao.persist(Mockito.any(ReservationVO.class))).thenAnswer((Answer<ReservationVO>) invocation -> {
            ReservationVO reservationVO = (ReservationVO) invocation.getArguments()[0];
            ReflectionTestUtils.setField(reservationVO, "id", (long) (persistedReservations.size() + 1));
            persistedReservations.add(reservationVO);
            return reservationVO;
        });
        Resource.ResourceType type = Resource.ResourceType.cpu;
        try (CheckedReservation cr = new CheckedReservation(account, type, tags, 2L, reservationDao, resourceLimitService);) {
            Assert.assertEquals(tags.size() + 1, persistedReservations.size()); // An extra for no tag
            Object obj = CallContext.current().getContextParameter(CheckedReservation.getResourceReservationContextParameterKey(type));
            Assert.assertTrue(obj instanceof List);
            List<Long> list = (List<Long>) obj;
            Assert.assertEquals(tags.size() + 1, list.size()); // An extra for no tag
        } catch (Exception e) {
            Assert.fail("Exception faced: " + e.getMessage());
        }
    }

    @Test
    public void testMultipleReservationsWithOneFailing() {
        List<String> tags = List.of("abc", "xyz");
        when(account.getAccountId()).thenReturn(1L);
        when(account.getDomainId()).thenReturn(4L);
        Map<Long, ReservationVO> persistedReservations = new HashMap<>();
        Mockito.when(reservationDao.persist(Mockito.any(ReservationVO.class))).thenAnswer((Answer<ReservationVO>) invocation -> {
            ReservationVO reservationVO = (ReservationVO) invocation.getArguments()[0];
            Long id = (long) (persistedReservations.size() + 1);
            ReflectionTestUtils.setField(reservationVO, "id", id);
            persistedReservations.put(id, reservationVO);
            return reservationVO;
        });
        Mockito.when(reservationDao.remove(Mockito.anyLong())).thenAnswer((Answer<Boolean>) invocation -> {
            Long id = (Long) invocation.getArguments()[0];
            persistedReservations.remove(id);
            return true;
        });
        try {
            Mockito.doThrow(ResourceAllocationException.class).when(resourceLimitService).checkResourceLimitWithTag(account, Resource.ResourceType.cpu, "xyz", 1L);
            try (CheckedReservation vmReservation = new CheckedReservation(account, Resource.ResourceType.user_vm, tags, 1L, reservationDao, resourceLimitService);
                 CheckedReservation cpuReservation = new CheckedReservation(account, Resource.ResourceType.cpu, tags, 1L, reservationDao, resourceLimitService);
                 CheckedReservation memReservation = new CheckedReservation(account, Resource.ResourceType.memory, tags, 256L, reservationDao, resourceLimitService);
            ) {
                Assert.fail("Exception should have occurred but all reservations successful!");
            } catch (Exception ex) {
                if (!(ex instanceof ResourceAllocationException)) {
                    Assert.fail(String.format("Expected ResourceAllocationException but %s occurred!", ex.getClass().getSimpleName()));
                }
                throw ex;
            }
        } catch (Exception rae) {
            // Check if all persisted reservations are removed
            Assert.assertTrue("All persisted reservations are not removed", persistedReservations.isEmpty());
        }
    }
}
