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
package org.apache.cloudstack.framework.sampleserver;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SampleManagementServerApp {

    private static void setupLog4j() {
        URL configUrl = System.class.getResource("/resources/log4j-cloud.xml");
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

    public static void main(String args[]) {
        setupLog4j();

        ApplicationContext context = new ClassPathXmlApplicationContext("/resources/SampleManagementServerAppContext.xml");
        SampleManagementServer server = context.getBean(SampleManagementServer.class);
        server.mainLoop();
    }
}
