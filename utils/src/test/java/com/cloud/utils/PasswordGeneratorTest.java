//
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
//

package com.cloud.utils;

import org.junit.Assert;
import org.junit.Test;

public class PasswordGeneratorTest {
    @Test
    public void generateRandomPassword() {
        // actual length is requested length, minimum length is 3
        Assert.assertTrue(PasswordGenerator.generateRandomPassword(0).length() == 3);
        Assert.assertTrue(PasswordGenerator.generateRandomPassword(1).length() == 3);
        Assert.assertTrue(PasswordGenerator.generateRandomPassword(5).length() == 5);
        String password = PasswordGenerator.generateRandomPassword(8);

        Assert.assertTrue(containsDigit(password));
        Assert.assertTrue(containsLowercase(password));
        Assert.assertTrue(containsUppercase(password));
    }

    private boolean containsUppercase(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLowercase(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDigit(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }
}
