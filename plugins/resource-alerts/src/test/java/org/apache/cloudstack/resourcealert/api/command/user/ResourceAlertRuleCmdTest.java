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
package org.apache.cloudstack.resourcealert.api.command.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.resourcealert.ResourceAlertService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;

@RunWith(MockitoJUnitRunner.class)
public class ResourceAlertRuleCmdTest {

    @Mock
    ResourceAlertService resourceAlertService;

    @InjectMocks
    CreateResourceAlertRuleCmd createCmd;

    @InjectMocks
    UpdateResourceAlertRuleCmd updateCmd;

    @InjectMocks
    DeleteResourceAlertRuleCmd deleteCmd;

    @Test(expected = InvalidParameterValueException.class)
    public void createKeepsInvalidParameterError() {
        when(resourceAlertService.createResourceAlertRule(any())).thenThrow(new InvalidParameterValueException("bad threshold"));
        createCmd.execute();
    }

    @Test(expected = PermissionDeniedException.class)
    public void updateKeepsPermissionError() {
        when(resourceAlertService.updateResourceAlertRule(any())).thenThrow(new PermissionDeniedException("denied"));
        updateCmd.execute();
    }

    @Test(expected = PermissionDeniedException.class)
    public void deleteKeepsPermissionError() {
        when(resourceAlertService.deleteResourceAlertRule(any())).thenThrow(new PermissionDeniedException("denied"));
        deleteCmd.execute();
    }
}
