/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.maint;

public class Version {
    /**
     * Compares two version strings and see which one is higher version.
     * @param ver1
     * @param ver2
     * @return positive if ver1 is higher.  negative if ver1 is lower; zero if the same.
     */
    public static int compare(String ver1, String ver2) {
        String[] tokens1 = ver1.split("[.]");
        String[] tokens2 = ver2.split("[.]");
//        assert(tokens1.length <= tokens2.length);
        
        int compareLength = Math.min(tokens1.length, tokens2.length);
        for (int i = 0; i < compareLength; i++) {
        	int version1 = Integer.parseInt(tokens1[i]);
        	int version2 = Integer.parseInt(tokens2[i]);
            if (version1 != version2) {
                return version1 < version2 ? -1 : 1;
            }
        }
        
        if (tokens1.length > tokens2.length) {
            return 1;
        } else if (tokens1.length < tokens2.length) {
            return -1;
        }
        
        return 0;
    }
    
    public static void main(String[] args) {
    	System.out.println("Result is " + compare(args[0], args[1]));
    }

}
