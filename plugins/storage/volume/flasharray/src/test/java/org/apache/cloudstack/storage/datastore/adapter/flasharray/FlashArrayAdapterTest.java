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
package org.apache.cloudstack.storage.datastore.adapter.flasharray;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class FlashArrayAdapterTest {

    private FlashArrayAdapter adapter() {
        return mock(FlashArrayAdapter.class, Mockito.CALLS_REAL_METHODS);
    }

    @Test
    public void shortHostNameStripsDomain() {
        Assert.assertEquals("b08-36", FlashArrayAdapter.shortHostName("b08-36.lab.local"));
        Assert.assertEquals("b08-36", FlashArrayAdapter.shortHostName("b08-36"));
    }

    @Test
    public void getHostReturnsNullWhenHostIsMissing() {
        FlashArrayAdapter adapter = adapter();
        doReturn(null).when(adapter).findHost("b08-36");

        Assert.assertNull(adapter.getHost("b08-36"));
        Assert.assertFalse(adapter.canAccessHost(null, "b08-36"));
    }

    @Test
    public void getHostFallsBackToShortName() {
        FlashArrayAdapter adapter = adapter();
        FlashArrayHost host = new FlashArrayHost();
        doReturn(null).when(adapter).findHost("b08-36.lab.local");
        doReturn(host).when(adapter).findHost("b08-36");

        Assert.assertSame(host, adapter.getHost("b08-36.lab.local"));
        Assert.assertTrue(adapter.canAccessHost(null, "b08-36.lab.local"));
    }

    @Test
    public void getHostDoesNotRetryShortNameWithoutDomain() {
        FlashArrayAdapter adapter = adapter();
        FlashArrayHost host = new FlashArrayHost();
        doReturn(host).when(adapter).findHost("b08-36");

        Assert.assertSame(host, adapter.getHost("b08-36"));
        verify(adapter, Mockito.times(1)).findHost("b08-36");
        verify(adapter, never()).findHost("b08-36.lab.local");
    }
}
