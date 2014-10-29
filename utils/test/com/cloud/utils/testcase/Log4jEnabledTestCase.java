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

package com.cloud.utils.testcase;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.log4j.xml.DOMConfigurator;

public class Log4jEnabledTestCase extends TestCase {
    @Override
    protected void setUp() {
        URL configUrl = System.class.getResource("/conf/log4j-cloud.xml");
        if (configUrl != null) {
            System.out.println("Configure log4j using log4j-cloud.xml");

            try {
                File file = new File(configUrl.toURI());

                System.out.println("Log4j configuration from : " + file.getAbsolutePath());
                DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
            } catch (URISyntaxException e) {
                System.out.println("Unable to convert log4j configuration Url to URI");
            }
        } else {
            System.out.println("Configure log4j with default properties");
        }
    }

    public static int getRandomMilliseconds(int rangeLo, int rangeHi) {
        int i = new Random().nextInt();

        long pos = (long)i - (long)Integer.MIN_VALUE;
        long iRange = (long)Integer.MAX_VALUE - (long)Integer.MIN_VALUE;
        return rangeLo + (int)((rangeHi - rangeLo) * pos / iRange);
    }
}
