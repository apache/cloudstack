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
package org.apache.cloudstack.api.command.offering;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.LongFunction;

import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.db.EntityManager;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.junit.Assert;
import org.junit.Test;

public class DomainAndZoneIdResolverTest {
    static class TestCmd extends BaseCmd implements DomainAndZoneIdResolver {
        @Override
        public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
            // No implementation needed for tests
        }

        @Override
        public String getCommandName() {
            return "test";
        }

        @Override
        public long getEntityOwnerId() {
            return 1L;
        }
    }

    private void setEntityMgr(final BaseCmd cmd, final EntityManager entityMgr) throws Exception {
        Field f = BaseCmd.class.getDeclaredField("_entityMgr");
        f.setAccessible(true);
        f.set(cmd, entityMgr);
    }

    @Test
    public void resolveDomainIds_usesDefaultProviderWhenEmpty() {
        TestCmd cmd = new TestCmd();

        final LongFunction<List<Long>> defaultsProvider = id -> Arrays.asList(100L, 200L);

        List<Long> result = cmd.resolveDomainIds("", 42L, defaultsProvider, "offering");
        Assert.assertEquals(Arrays.asList(100L, 200L), result);
    }

    @Test
    public void resolveDomainIds_resolvesValidUuids() throws Exception {
        TestCmd cmd = new TestCmd();

        EntityManager em = mock(EntityManager.class);
        setEntityMgr(cmd, em);

        Domain d1 = mock(Domain.class);
        when(d1.getId()).thenReturn(10L);
        Domain d2 = mock(Domain.class);
        when(d2.getId()).thenReturn(20L);

        when(em.findByUuid(Domain.class, "uuid1")).thenReturn(d1);
        when(em.findByUuid(Domain.class, "uuid2")).thenReturn(d2);

        List<Long> ids = cmd.resolveDomainIds("uuid1, public, uuid2", null, null, "template");
        Assert.assertEquals(Arrays.asList(10L, 20L), ids);
    }

    @Test
    public void resolveDomainIds_invalidUuid_throws() throws Exception {
        TestCmd cmd = new TestCmd();

        EntityManager em = mock(EntityManager.class);
        setEntityMgr(cmd, em);

        when(em.findByUuid(Domain.class, "bad-uuid")).thenReturn(null);

        Assert.assertThrows(InvalidParameterValueException.class,
            () -> cmd.resolveDomainIds("bad-uuid", null, null, "offering"));
    }

    @Test
    public void resolveZoneIds_usesDefaultProviderWhenEmpty() {
        TestCmd cmd = new TestCmd();

        final LongFunction<List<Long>> defaultsProvider = id -> Collections.singletonList(300L);

        List<Long> result = cmd.resolveZoneIds("", 99L, defaultsProvider, "offering");
        Assert.assertEquals(Collections.singletonList(300L), result);
    }

    @Test
    public void resolveZoneIds_resolvesValidUuids() throws Exception {
        TestCmd cmd = new TestCmd();

        EntityManager em = mock(EntityManager.class);
        setEntityMgr(cmd, em);

        DataCenter z1 = mock(DataCenter.class);
        when(z1.getId()).thenReturn(30L);
        DataCenter z2 = mock(DataCenter.class);
        when(z2.getId()).thenReturn(40L);

        when(em.findByUuid(DataCenter.class, "zone-1")).thenReturn(z1);
        when(em.findByUuid(DataCenter.class, "zone-2")).thenReturn(z2);

        List<Long> ids = cmd.resolveZoneIds("zone-1, all, zone-2", null, null, "service");
        Assert.assertEquals(Arrays.asList(30L, 40L), ids);
    }

    @Test
    public void resolveZoneIds_invalidUuid_throws() throws Exception {
        TestCmd cmd = new TestCmd();

        EntityManager em = mock(EntityManager.class);
        setEntityMgr(cmd, em);

        when(em.findByUuid(DataCenter.class, "bad-zone")).thenReturn(null);

        Assert.assertThrows(InvalidParameterValueException.class,
            () -> cmd.resolveZoneIds("bad-zone", null, null, "offering"));
    }
}
