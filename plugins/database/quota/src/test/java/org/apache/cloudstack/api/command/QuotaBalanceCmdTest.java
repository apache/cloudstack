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

import org.apache.cloudstack.api.response.QuotaBalanceResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class QuotaBalanceCmdTest extends TestCase {

    @Mock
    QuotaResponseBuilder quotaResponseBuilderMock;

    @InjectMocks
    @Spy
    QuotaBalanceCmd quotaBalanceCmdSpy;

    @Test
    public void executeTestSetResponseObject() {
        QuotaBalanceResponse expected = new QuotaBalanceResponse();
        Mockito.doReturn(expected).when(quotaResponseBuilderMock).createQuotaBalanceResponse(Mockito.eq(quotaBalanceCmdSpy));

        quotaBalanceCmdSpy.execute();

        Assert.assertEquals(expected, quotaBalanceCmdSpy.getResponseObject());
    }

}
