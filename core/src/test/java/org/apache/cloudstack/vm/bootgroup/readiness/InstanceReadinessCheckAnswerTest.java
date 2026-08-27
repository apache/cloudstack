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
package org.apache.cloudstack.vm.bootgroup.readiness;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;

public class InstanceReadinessCheckAnswerTest {

    private InstanceReadinessCheckCommand cmd() {
        return new InstanceReadinessCheckCommand("10.1.1.5", false);
    }

    @Test
    public void wellFormedSuccessfulDetailsAreParsed() {
        InstanceReadinessCheckAnswer answer = new InstanceReadinessCheckAnswer(cmd(), true, " out \n && err && 0 ");

        Map<String, String> details = answer.getExecutionDetails();

        Assert.assertEquals("out", details.get(InstanceReadinessCheckAnswer.STDOUT));
        Assert.assertEquals("err", details.get(InstanceReadinessCheckAnswer.STDERR));
        Assert.assertEquals("0", details.get(InstanceReadinessCheckAnswer.EXITCODE));
    }

    @Test
    public void extraDelimitedSegmentsAreIgnoredPastTheThird() {
        InstanceReadinessCheckAnswer answer = new InstanceReadinessCheckAnswer(cmd(), true, "out&&err&&1&&extra");

        Map<String, String> details = answer.getExecutionDetails();

        Assert.assertEquals("out", details.get(InstanceReadinessCheckAnswer.STDOUT));
        Assert.assertEquals("err", details.get(InstanceReadinessCheckAnswer.STDERR));
        Assert.assertEquals("1", details.get(InstanceReadinessCheckAnswer.EXITCODE));
    }

    @Test(expected = CloudRuntimeException.class)
    public void malformedSuccessfulDetailsThrow() {
        new InstanceReadinessCheckAnswer(cmd(), true, "out&&err").getExecutionDetails();
    }

    @Test
    public void failedResultIsNotParsedEvenIfWellFormed() {
        InstanceReadinessCheckAnswer answer = new InstanceReadinessCheckAnswer(cmd(), false, "out&&err&&0");

        Map<String, String> details = answer.getExecutionDetails();

        Assert.assertEquals("", details.get(InstanceReadinessCheckAnswer.STDOUT));
        Assert.assertEquals("out&&err&&0", details.get(InstanceReadinessCheckAnswer.STDERR));
        Assert.assertEquals("-1", details.get(InstanceReadinessCheckAnswer.EXITCODE));
    }

    @Test
    public void blankDetailsWithSuccessfulResultFallsBackToDefaults() {
        InstanceReadinessCheckAnswer answer = new InstanceReadinessCheckAnswer(cmd(), true, "");

        Map<String, String> details = answer.getExecutionDetails();

        Assert.assertEquals("", details.get(InstanceReadinessCheckAnswer.STDOUT));
        Assert.assertEquals("", details.get(InstanceReadinessCheckAnswer.STDERR));
        Assert.assertEquals("-1", details.get(InstanceReadinessCheckAnswer.EXITCODE));
    }

    @Test
    public void nullDetailsWithSuccessfulResultFallsBackToDefaults() {
        InstanceReadinessCheckAnswer answer = new InstanceReadinessCheckAnswer(cmd(), true, null);

        Map<String, String> details = answer.getExecutionDetails();

        Assert.assertEquals("", details.get(InstanceReadinessCheckAnswer.STDOUT));
        Assert.assertNull(details.get(InstanceReadinessCheckAnswer.STDERR));
        Assert.assertEquals("-1", details.get(InstanceReadinessCheckAnswer.EXITCODE));
    }
}
