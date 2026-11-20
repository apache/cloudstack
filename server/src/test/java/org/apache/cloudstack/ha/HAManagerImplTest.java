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
package org.apache.cloudstack.ha;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.ha.provider.HAProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HAManagerImplTest {

    private static final String HA_PROVIDER_NAME = "kvmhaprovider";

    @Mock
    private HAConfig haConfig;

    @Mock
    private HAResource resource;

    @Mock
    private HAProvider<HAResource> haProvider;

    private HAManagerImpl haManager;

    @Before
    public void setUp() throws Exception {
        haManager = Mockito.spy(new HAManagerImpl());

        final Map<String, HAProvider<HAResource>> haProviderMap = new HashMap<>();
        haProviderMap.put(HA_PROVIDER_NAME, haProvider);
        final Field field = HAManagerImpl.class.getDeclaredField("haProviderMap");
        field.setAccessible(true);
        field.set(haManager, haProviderMap);

        Mockito.when(haConfig.getHaProvider()).thenReturn(HA_PROVIDER_NAME);
    }

    @Test
    public void validateAndFindHAProviderFencedHostDoesNotAttemptIneligibleTransition() {
        // A fenced host is put in maintenance mode by the fencing flow and is therefore
        // ineligible; the FSM has no Fenced -> Ineligible transition, so none must be attempted
        // (it would log a NoTransitionException warning on every poll round otherwise).
        Mockito.when(haProvider.isEligible(resource)).thenReturn(false);
        Mockito.when(haConfig.getState()).thenReturn(HAConfig.HAState.Fenced);

        assertNull(haManager.validateAndFindHAProvider(haConfig, resource));

        Mockito.verify(haManager, Mockito.never()).transitionHAState(Mockito.any(HAConfig.Event.class), Mockito.any(HAConfig.class));
    }

    @Test
    public void validateAndFindHAProviderIneligibleResourceTransitionsToIneligible() {
        Mockito.when(haProvider.isEligible(resource)).thenReturn(false);
        Mockito.when(haConfig.getState()).thenReturn(HAConfig.HAState.Available);
        Mockito.doReturn(true).when(haManager).transitionHAState(Mockito.any(HAConfig.Event.class), Mockito.any(HAConfig.class));

        assertNull(haManager.validateAndFindHAProvider(haConfig, resource));

        Mockito.verify(haManager).transitionHAState(HAConfig.Event.Ineligible, haConfig);
    }

    @Test
    public void validateAndFindHAProviderAlreadyIneligibleDoesNotRetransition() {
        Mockito.when(haProvider.isEligible(resource)).thenReturn(false);
        Mockito.when(haConfig.getState()).thenReturn(HAConfig.HAState.Ineligible);

        assertNull(haManager.validateAndFindHAProvider(haConfig, resource));

        Mockito.verify(haManager, Mockito.never()).transitionHAState(Mockito.any(HAConfig.Event.class), Mockito.any(HAConfig.class));
    }

    @Test
    public void validateAndFindHAProviderEligibleResourceTransitionsBackFromIneligible() {
        Mockito.when(haProvider.isEligible(resource)).thenReturn(true);
        Mockito.when(haConfig.getState()).thenReturn(HAConfig.HAState.Ineligible);
        Mockito.doReturn(true).when(haManager).transitionHAState(Mockito.any(HAConfig.Event.class), Mockito.any(HAConfig.class));

        assertEquals(haProvider, haManager.validateAndFindHAProvider(haConfig, resource));

        Mockito.verify(haManager).transitionHAState(HAConfig.Event.Eligible, haConfig);
    }
}
