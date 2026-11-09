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
package com.cloud.hypervisor.kvm.resource.disconnecthook;

import org.junit.Assert;
import org.junit.Test;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.Mockito;

public class MigrationCancelHookTest {

    @Test
    public void testAbortedJobIsACancellation() throws Exception {
        final Domain domain = Mockito.mock(Domain.class);
        Mockito.when(domain.getName()).thenReturn("i-2-3-VM");
        Mockito.when(domain.abortJob()).thenReturn(0);

        final MigrationCancelHook hook = new MigrationCancelHook(domain);
        hook.run();

        Assert.assertTrue(hook.stoppedTheWork());
    }

    @Test
    public void testJobThatFinishedBeforeTheAbortIsNotACancellation() throws Exception {
        final Domain domain = Mockito.mock(Domain.class);
        Mockito.when(domain.getName()).thenReturn("i-2-3-VM");
        final LibvirtException domainGone = Mockito.mock(LibvirtException.class);
        Mockito.when(domainGone.getMessage()).thenReturn("Domain not found: no domain with matching name 'i-2-3-VM'");
        Mockito.when(domainGone.getStackTrace()).thenReturn(new StackTraceElement[0]);
        Mockito.when(domain.abortJob()).thenThrow(domainGone);

        final MigrationCancelHook hook = new MigrationCancelHook(domain);
        hook.run();

        Assert.assertFalse(hook.stoppedTheWork());

        final Domain stillThere = Mockito.mock(Domain.class);
        Mockito.when(stillThere.getName()).thenReturn("i-2-3-VM");
        Mockito.when(stillThere.abortJob()).thenReturn(-1);
        final MigrationCancelHook refused = new MigrationCancelHook(stillThere);
        refused.run();
        Assert.assertFalse(refused.stoppedTheWork());
    }
}
