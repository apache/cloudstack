/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.engine;

import java.util.ArrayList;
import java.util.List;

import com.cloud.utils.StringUtils;

/**
 * Rules specifies all rules about developing and using CloudStack Orchestration
 * Platforms APIs.  This class is not actually used in CloudStack Orchestration
 * Platform but must be read by all who wants to use and develop against
 * CloudStack Orchestration Platform.
 * 
 * Make sure to make changes here when there are changes to how the APIs should 
 * be used and developed. 
 * 
 * Changes to this class must be approved by the maintainer of this project.
 *
 */
public class Rules {
    public static List<String> whenUsing() {
        List<String> rules = new ArrayList<String>();
        rules.add("Always be prepared to handle RuntimeExceptions.");
        return rules;
    }

    public static List<String> whenWritingNewApis() {
        List<String> rules = new ArrayList<String>();
        rules.add("You may think you're the greatest developer in the " +
                "world but every change to the API must be reviewed and approved. ");
        rules.add("Every API must have unit tests written against it.  And not it's unit tests");
        rules.add("");


        return rules;
    }

    private static void printRule(String rule) {
        System.out.print("API Rule: ");
        String skip = "";
        int brk = 0;
        while (true) {
            int stop = StringUtils.formatForOutput(rule, brk, 75 - skip.length(), ' ');
            if (stop < 0) {
                break;
            }
            System.out.print(skip);
            skip = "          ";
            System.out.println(rule.substring(brk, stop).trim());
            brk = stop;
        } 
    }

    public static void main(String[] args) {
        System.out.println("When developing against the CloudStack Orchestration Platform, you must following the following rules:");
        for (String rule : whenUsing()) {
            printRule(rule);
        }
        System.out.println("");
        System.out.println("When writing APIs, you must follow these rules:");
        for (String rule : whenWritingNewApis()) {
            printRule(rule);
        }
    }

}

