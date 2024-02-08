// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.usage;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.cloud.utils.LogUtils;
import com.cloud.utils.component.ComponentContext;

public class UsageServer implements Daemon {
    protected Logger logger = LogManager.getLogger(getClass());
    public static final String Name = "usage-server";

    private UsageManager mgr;
    private ClassPathXmlApplicationContext appContext;

    /**
     * @param args
     */
    public static void main(String[] args) {
        initLog4j();
        UsageServer usage = new UsageServer();
        usage.start();
    }

    @Override
    public void init(DaemonContext arg0) throws DaemonInitException, Exception {
        initLog4j();
    }

    @Override
    public void start() {

        appContext = new ClassPathXmlApplicationContext("usageApplicationContext.xml");

        ComponentContext.initComponentsLifeCycle();

        mgr = appContext.getBean(UsageManager.class);

        if (mgr != null) {
            if (logger.isInfoEnabled()) {
                logger.info("UsageServer ready...");
            }
        }
    }

    @Override
    public void stop() {
        appContext.close();
    }

    @Override
    public void destroy() {

    }

    static private void initLog4j() {
        LogUtils.initLog4j("log4j-cloud.xml");
    }

}
