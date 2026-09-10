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
package com.cloud.configuration;

import org.junit.Test;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.vm.VmDetailConstants;

public class ConfigurationManagerOverCommitDetailTest {

    private final ConfigurationManagerImpl configurationManager = new ConfigurationManagerImpl();

    private void validate(String value) {
        configurationManager.validateOverCommitRatioInServiceOfferingDetail(
                VmDetailConstants.CPU_OVER_COMMIT_RATIO, value);
    }

    @Test
    public void testRatiosAreAccepted() {
        validate("1");
        validate("1.0");
        validate("4");
        validate("10.5");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUndercommitIsRejected() {
        validate("0.1");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testJustBelowOneIsRejected() {
        validate("0.99");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testZeroIsRejected() {
        validate("0");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testNegativeIsRejected() {
        validate("-1");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testNonNumericIsRejected() {
        validate("none");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testNullIsRejected() {
        validate(null);
    }
}
