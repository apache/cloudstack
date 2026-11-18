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
package com.cloud.host;

import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.vm.VirtualMachine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HostVOTest {
    HostVO host;
    ServiceOfferingVO offering;

    @Before
    public void setUp() throws Exception {
        host = new HostVO();
        offering = new ServiceOfferingVO("TestSO", 0, 0, 0, 0, 0,
                false, "TestSO", false, VirtualMachine.Type.User, false);
    }

    @Test
    public void testNoSO() {
        assertFalse(host.checkHostServiceOfferingTags(null));
    }

    @Test
    public void testNoTag() {
        assertTrue(host.checkHostServiceOfferingTags(offering));
    }

    @Test
    public void testRightTag() {
        host.setHostTags(Arrays.asList("tag1", "tag2"), false);
        offering.setHostTag("tag2,tag1");
        assertTrue(host.checkHostServiceOfferingTags(offering));
    }

    @Test
    public void testWrongTag() {
        host.setHostTags(Arrays.asList("tag1", "tag2"), false);
        offering.setHostTag("tag2,tag4");
        assertFalse(host.checkHostServiceOfferingTags(offering));
    }

    @Test
    public void checkHostServiceOfferingTagsTestRuleTagWithServiceTagThatMatches() {
        host.setHostTags(List.of("tags[0] == 'A'"), true);
        offering.setHostTag("A");
        assertTrue(host.checkHostServiceOfferingTags(offering));
    }

    @Test
    public void checkHostServiceOfferingTagsTestRuleTagWithServiceTagThatDoesNotMatch() {
        host.setHostTags(List.of("tags[0] == 'A'"), true);
        offering.setHostTag("B");
        assertFalse(host.checkHostServiceOfferingTags(offering));
    }

    @Test
    public void checkHostServiceOfferingTagsTestRuleTagWithNullServiceTag() {
        host.setHostTags(List.of("tags[0] == 'A'"), true);
        offering.setHostTag(null);
        assertFalse(host.checkHostServiceOfferingTags(offering));
    }

    @Test
    public void testEitherNoSOOrTemplate() {
        assertFalse(host.checkHostServiceOfferingAndTemplateTags(null, Mockito.mock(VirtualMachineTemplate.class), null));
        assertFalse(host.checkHostServiceOfferingAndTemplateTags(Mockito.mock(ServiceOffering.class), null, null));
    }

    @Test
    public void testNoTagOfferingTemplate() {
        assertTrue(host.checkHostServiceOfferingAndTemplateTags(offering, Mockito.mock(VirtualMachineTemplate.class), Collections.emptySet()));
        assertTrue(host.getHostServiceOfferingAndTemplateMissingTags(offering, Mockito.mock(VirtualMachineTemplate.class), Collections.emptySet()).isEmpty());
        assertTrue(host.checkHostServiceOfferingAndTemplateTags(offering, Mockito.mock(VirtualMachineTemplate.class), Set.of("tag1", "tag2")));
        assertTrue(host.getHostServiceOfferingAndTemplateMissingTags(offering, Mockito.mock(VirtualMachineTemplate.class), Set.of("tag1", "tag2")).isEmpty());
    }

    @Test
    public void testRightTagOfferingTemplate() {
        host.setHostTags(Arrays.asList("tag1", "tag2"), false);
        offering.setHostTag("tag2,tag1");
        assertTrue(host.checkHostServiceOfferingAndTemplateTags(offering, Mockito.mock(VirtualMachineTemplate.class), Set.of("tag1")));
        Set<String> actualMissingTags = host.getHostServiceOfferingAndTemplateMissingTags(offering, Mockito.mock(VirtualMachineTemplate.class), Set.of("tag1"));
        assertTrue(actualMissingTags.isEmpty());

        host.setHostTags(Arrays.asList("tag1", "tag2", "tag3"), false);
        offering.setHostTag("tag2,tag1");
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getTemplateTag()).thenReturn("tag3");
        assertTrue(host.checkHostServiceOfferingAndTemplateTags(offering, template, Set.of("tag2", "tag3")));
        actualMissingTags = host.getHostServiceOfferingAndTemplateMissingTags(offering, template, Set.of("tag2", "tag3"));
        assertTrue(actualMissingTags.isEmpty());
        host.setHostTags(List.of("tag3"), false);
        offering.setHostTag(null);
        assertTrue(host.checkHostServiceOfferingAndTemplateTags(offering, template, Set.of("tag3")));
        actualMissingTags = host.getHostServiceOfferingAndTemplateMissingTags(offering, template, Set.of("tag3"));
        assertTrue(actualMissingTags.isEmpty());

        assertTrue(host.checkHostServiceOfferingAndTemplateTags(offering, template, Set.of("tag2", "tag1")));
        actualMissingTags = host.getHostServiceOfferingAndTemplateMissingTags(offering, template, Set.of("tag2", "tag1"));
        assertTrue(actualMissingTags.isEmpty());
    }

    @Test
    public void testWrongOfferingTag() {
        host.setHostTags(Arrays.asList("tag1", "tag2"), false);
        offering.setHostTag("tag2,tag4");
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getTemplateTag()).thenReturn("tag1");
        assertFalse(host.checkHostServiceOfferingAndTemplateTags(offering, template, Set.of("tag1", "tag2", "tag3", "tag4")));
        Set<String> actualMissingTags = host.getHostServiceOfferingAndTemplateMissingTags(offering, template, Set.of("tag1", "tag2", "tag3", "tag4"));
        assertEquals(Set.of("tag4"), actualMissingTags);

        offering.setHostTag("tag1,tag2");
        template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getTemplateTag()).thenReturn("tag3");
        actualMissingTags = host.getHostServiceOfferingAndTemplateMissingTags(offering, template, Set.of("tag1", "tag2", "tag3", "tag4"));
        assertFalse(host.checkHostServiceOfferingAndTemplateTags(offering, template, Set.of("tag1", "tag2", "tag3", "tag4")));
        assertEquals(Set.of("tag3"), actualMissingTags);
    }
}
