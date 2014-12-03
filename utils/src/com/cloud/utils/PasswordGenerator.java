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

import java.security.SecureRandom;
import java.util.Random;

/**
 * Generate random passwords
 *
 */
public class PasswordGenerator {
    //Leave out visually confusing  l,L,1,o,O,0
    static private char[] lowerCase = new char[] {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    static private char[] upperCase = new char[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
    static private char[] numeric = new char[] {'2', '3', '4', '5', '6', '7', '8', '9'};

    static private char[] alphaNumeric = new char[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
        'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '2', '3', '4', '5', '6', '7', '8', '9'};

    static private int minLength = 3;

    public static String generateRandomPassword(int num) {
        Random r = new SecureRandom();
        StringBuilder password = new StringBuilder();

        //Guard for num < minLength
        if (num < minLength) {
            //Add alphanumeric chars at random
            for (int i = 0; i < minLength; i++) {
                password.append(generateAlphaNumeric(r));
            }
        } else {
            // Generate random 3-character string with a lowercase character,
            // uppercase character, and a digit
            password.append(generateLowercaseChar(r)).append(generateUppercaseChar(r)).append(generateDigit(r));

            // Generate a random n-character string with only lowercase
            // characters
            for (int i = 0; i < num - 3; i++) {
                password.append(generateLowercaseChar(r));
            }
        }

        return password.toString();
    }

    private static char generateLowercaseChar(Random r) {
        return lowerCase[r.nextInt(lowerCase.length)];
    }

    private static char generateDigit(Random r) {
        return numeric[r.nextInt(numeric.length)];
    }

    private static char generateUppercaseChar(Random r) {
        return upperCase[r.nextInt(upperCase.length)];
    }

    private static char generateAlphaNumeric(Random r) {
        return alphaNumeric[r.nextInt(alphaNumeric.length)];
    }

    public static String generatePresharedKey(int numChars) {
        Random r = new SecureRandom();
        StringBuilder psk = new StringBuilder();
        for (int i = 0; i < numChars; i++) {
            psk.append(generateAlphaNumeric(r));
        }
        return psk.toString();

    }

    public static String rot13(final String password) {
        final StringBuilder newPassword = new StringBuilder(password.length());

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);

            if ((c >= 'a' && c <= 'm') || ((c >= 'A' && c <= 'M'))) {
                c += 13;
            } else if ((c >= 'n' && c <= 'z') || (c >= 'N' && c <= 'Z')) {
                c -= 13;
            }

            newPassword.append(c);
        }

        return newPassword.toString();
    }
}
