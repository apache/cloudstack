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
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

@RunWith(MockitoJUnitRunner.class)
public class QuotaEmailTemplateUpdateCmdTest extends TestCase {
    @Mock
    QuotaResponseBuilder responseBuilder;

    @Test
    public void testQuotaEmailTemplateUpdateCmd () throws NoSuchFieldException, IllegalAccessException {
        QuotaEmailTemplateUpdateCmd cmd = new QuotaEmailTemplateUpdateCmd();

        Field rbField = QuotaEmailTemplateUpdateCmd.class.getDeclaredField("_quotaResponseBuilder");
        rbField.setAccessible(true);
        rbField.set(cmd, responseBuilder);

        // templatename parameter check
        try {
            cmd.execute();
        } catch (ServerApiException e) {
            assertTrue(e.getErrorCode().equals(ApiErrorCode.PARAM_ERROR));
        }

        // invalid template name test
        cmd.setTemplateName("randomTemplate");
        cmd.setTemplateBody("some body");
        cmd.setTemplateSubject("some subject");
        try {
            cmd.execute();
        } catch (ServerApiException e) {
            assertTrue(e.getErrorCode().equals(ApiErrorCode.PARAM_ERROR));
        }

        // valid template test
        cmd.setTemplateName(QuotaConfig.QuotaEmailTemplateTypes.QUOTA_EMPTY.toString());
        Mockito.when(responseBuilder.updateQuotaEmailTemplate(Mockito.eq(cmd))).thenReturn(true);
        cmd.execute();
        Mockito.verify(responseBuilder, Mockito.times(1)).updateQuotaEmailTemplate(Mockito.eq(cmd));
    }
}
