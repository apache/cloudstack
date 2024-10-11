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

public class ConfigTest {
    @Test
    public void configTestIntegerRange() {
        for (Config configuration : Config.values()) {
            if (configuration.getType().equals(Integer.class) && configuration.getRange() != null) {
                try {
                    final String[] options = configuration.getRange().split("-");
                    final int min = Integer.parseInt(options[0]);
                    final int max = Integer.parseInt(options[1]);
                    if (options.length != 2) {
                        throw new AssertionError(String.format("Invalid range for configuration [%s], a valid value for the range should be two integers separated by [-].", configuration.toString()));
                    }
                    if (min > max) {
                        throw new AssertionError(String.format("Invalid range for configuration [%s], the second value should be greater than the first.", configuration.toString()));
                    }
                } catch (java.lang.NumberFormatException e) {
                    throw new AssertionError(String.format("Invalid range for configuration [%s], a valid value for the range should be two integers separated by [-].", configuration.toString()));
                }
            }
        }
    }
}
