/**
 *  Copyright (C) 2010 Cloud.com.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later. 
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later
version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.utils;

import java.util.Random;

/**
 * Generate random passwords
 *
 */
public class PasswordGenerator {
	 public static String generateRandomPassword() {
	        Random r = new Random();
	        StringBuffer password = new StringBuffer();

	        // Generate random 3-character string with a lowercase character,
	        // uppercase character, and a digit

	        // Generate a random lowercase character
	        int lowercase = generateLowercaseChar(r);
	        // Generate a random uppercase character ()
	        int uppercase = generateUppercaseChar(r);
	        // Generate a random digit between 2 and 9
	        int digit = r.nextInt(8) + 2;

	        // Append to the password
	        password.append((char) lowercase);
	        password.append((char) uppercase);
	        password.append(digit);

	        // Generate a random 6-character string with only lowercase
	        // characters
	        for (int i = 0; i < 6; i++) {
	            // Generate a random lowercase character (don't allow lowercase
	            // "l" or lowercase "o")
	            lowercase = generateLowercaseChar(r);
	            // Append to the password
	            password.append((char) lowercase);
	        }

	        return password.toString();
	    }

	    private static char generateLowercaseChar(Random r) {
	        // Don't allow lowercase "l" or lowercase "o"
	        int lowercase = -1;
	        while (lowercase == -1 || lowercase == 108 || lowercase == 111)
	            lowercase = r.nextInt(26) + 26 + 71;
	        return ((char) lowercase);
	    }

	    private static char generateUppercaseChar(Random r) {
	        // Don't allow uppercase "I" or uppercase "O"
	        int uppercase = -1;
	        while (uppercase == -1 || uppercase == 73 || uppercase == 79)
	            uppercase = r.nextInt(26) + 65;
	        return ((char) uppercase);
	    }
}
