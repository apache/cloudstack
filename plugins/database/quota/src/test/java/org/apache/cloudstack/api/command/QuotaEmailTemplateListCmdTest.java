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
package org.apache.cloudstack.api.command;

import junit.framework.TestCase;
import org.apache.cloudstack.api.response.QuotaEmailTemplateResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class QuotaEmailTemplateListCmdTest extends TestCase {

    @Mock
    QuotaResponseBuilder responseBuilder;

    @Test
    public void testQuotaEmailTemplateListCmd() throws NoSuchFieldException, IllegalAccessException {
        QuotaEmailTemplateListCmd cmd = new QuotaEmailTemplateListCmd();
        Field rbField = QuotaEmailTemplateListCmd.class.getDeclaredField("_quotaResponseBuilder");
        rbField.setAccessible(true);
        rbField.set(cmd, responseBuilder);

        List<QuotaEmailTemplateResponse> responses = new ArrayList<QuotaEmailTemplateResponse>();
        Mockito.when(responseBuilder.listQuotaEmailTemplates(Mockito.eq(cmd))).thenReturn(responses);
        cmd.execute();
        Mockito.verify(responseBuilder, Mockito.times(1)).listQuotaEmailTemplates(cmd);
    }
}
