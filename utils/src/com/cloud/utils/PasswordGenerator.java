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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
            List<Character> passwordChars = new ArrayList<Character>();
            passwordChars.add(generateLowercaseChar(r));
            passwordChars.add(generateUppercaseChar(r));
            passwordChars.add(generateDigit(r));

            for (int i = passwordChars.size(); i < num; i++) {
                passwordChars.add(generateAlphaNumeric(r));
            }

            Collections.shuffle(passwordChars, new SecureRandom());

            for (char c : passwordChars) {
                password.append(c);
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

}
