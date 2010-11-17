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

import java.security.SecureRandom;
import java.util.Random;

/**
 * Generate random passwords
 *
 */
public class PasswordGenerator {
	//Leave out visually confusing  l,L,1,o,O,0
	static private char[] lowerCase = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
	static private char[] upperCase = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
	static private char[] numeric = new char[]{'2', '3', '4', '5', '6', '7', '8', '9'};

	static private char[] alphaNumeric = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',  'J', 'K',  'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',  'j', 'k',  'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
         '2', '3', '4', '5', '6', '7', '8', '9'};

	 public static String generateRandomPassword(int num) {
	        Random r = new SecureRandom();
	        StringBuilder password = new StringBuilder();

	        // Generate random 3-character string with a lowercase character,
	        // uppercase character, and a digit
	        password.append(generateLowercaseChar(r))
	                .append(generateUppercaseChar(r))
	                .append(generateDigit(r));

	        // Generate a random n-character string with only lowercase
	        // characters
	        for (int i = 0; i < num; i++) {
	            password.append(generateLowercaseChar(r));
	        }

	        return password.toString();
	    }

	    private static char generateLowercaseChar(Random r) {
	    	return lowerCase[r.nextInt(lowerCase.length)];
	    }

	    private static char generateDigit(Random r ) {
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
	    	return  psk.toString();
	    	
	    }
	    
	    public static void main(String [] args) {
	    	for (int i=0; i < 100; i++) {
	    		System.out.println("PSK: " + generatePresharedKey(24));
	    	}
	    	for (int i=0; i < 100; i++) {
	    		System.out.println("Password: " + generateRandomPassword(6));
	    	}
	    }
}
