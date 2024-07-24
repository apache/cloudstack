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

package com.cloud.tags;

import com.cloud.exception.PermissionDeniedException;
import com.cloud.server.ResourceTag;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import junit.framework.TestCase;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class TaggedResourceManagerImplTest extends TestCase {

    @Mock
    AccountManager accountManager;


    @Spy
    @InjectMocks
    private final TaggedResourceManagerImpl taggedResourceManagerImplSpy = new TaggedResourceManagerImpl();

    private final List<ResourceTag.ResourceObjectType> listResourceObjectTypes = Arrays.asList(ResourceTag.ResourceObjectType.values());

    @Test
    public void validateGetTagsFromResourceMustReturnValues(){
        Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put("test1", "test1");
        expectedResult.put("test2", "test2");

        listResourceObjectTypes.forEach(resourceObjectType -> {
            List<ResourceTag> resourceTags = new ArrayList<>();
            expectedResult.entrySet().forEach(entry -> {
                resourceTags.add(new ResourceTagVO(entry.getKey(), entry.getValue(), 0, 0, 0, resourceObjectType, "test", "test"));
            });

            Mockito.doReturn(resourceTags).when(taggedResourceManagerImplSpy).listByResourceTypeAndId(Mockito.eq(resourceObjectType), Mockito.anyLong());
            Map<String, String> result = taggedResourceManagerImplSpy.getTagsFromResource(resourceObjectType, 0l);
            Assert.assertEquals(expectedResult, result);
        });
    }

    @Test
    public void validateGetTagsFromResourceMustReturnNull(){
        Map<String, String> expectedResult = null;

        listResourceObjectTypes.forEach(resourceObjectType -> {
            List<ResourceTag> resourceTags = null;

            Mockito.doReturn(resourceTags).when(taggedResourceManagerImplSpy).listByResourceTypeAndId(Mockito.eq(resourceObjectType), Mockito.anyLong());
            Map<String, String> result = taggedResourceManagerImplSpy.getTagsFromResource(resourceObjectType, 0l);
            Assert.assertEquals(expectedResult, result);
        });
    }

    @Test
    public void validateGetTagsFromResourceMustReturnEmpty(){
        Map<String, String> expectedResult = new HashMap<>();

        listResourceObjectTypes.forEach(resourceObjectType -> {
            List<ResourceTag> resourceTags = new ArrayList<>();

            Mockito.doReturn(resourceTags).when(taggedResourceManagerImplSpy).listByResourceTypeAndId(Mockito.eq(resourceObjectType), Mockito.anyLong());
            Map<String, String> result = taggedResourceManagerImplSpy.getTagsFromResource(resourceObjectType, 0l);
            Assert.assertEquals(expectedResult, result);
        });
    }

    @Test
    public void testCheckTagsDeletePermission() {
        long accountId = 1L;
        Account caller = Mockito.mock(Account.class);
        Mockito.when(caller.getAccountId()).thenReturn(accountId);
        ResourceTag resourceTag = Mockito.mock(ResourceTag.class);
        Mockito.when(resourceTag.getAccountId()).thenReturn(accountId);
        taggedResourceManagerImplSpy.checkTagsDeletePermission(List.of(resourceTag), caller);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testCheckTagsDeletePermissionFail() {
        long callerAccountId = 1L;
        long ownerAccountId = 2L;
        Account caller = Mockito.mock(Account.class);
        Mockito.when(caller.getAccountId()).thenReturn(callerAccountId);
        ResourceTag resourceTag1 = Mockito.mock(ResourceTag.class);
        Mockito.when(resourceTag1.getAccountId()).thenReturn(callerAccountId);
        ResourceTag resourceTag2 = Mockito.mock(ResourceTag.class);
        Mockito.when(resourceTag2.getAccountId()).thenReturn(ownerAccountId);
        Account owner = Mockito.mock(Account.class);
        Mockito.when(accountManager.getAccount(ownerAccountId)).thenReturn(owner);
        Mockito.doThrow(PermissionDeniedException.class).when(accountManager).checkAccess(caller, null, false, owner);
        taggedResourceManagerImplSpy.checkTagsDeletePermission(List.of(resourceTag1, resourceTag2), caller);
    }

    @Test
    public void testRetrieveDataStoreNullPoolId() {
        DataStore dataStore = taggedResourceManagerImplSpy.retrieveDatastore(null);
        Assert.assertNull(dataStore);
    }
}
