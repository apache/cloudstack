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
package org.apache.cloudstack.spring.lifecycle;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.naming.ConfigurationException;


import com.cloud.utils.component.ComponentLifecycle;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.mgmt.JmxUtil;
import com.cloud.utils.mgmt.ManagementBean;

public class CloudStackExtendedLifeCycle extends AbstractBeanCollector {


    Map<Integer, Set<ComponentLifecycle>> sorted = new TreeMap<Integer, Set<ComponentLifecycle>>();

    public CloudStackExtendedLifeCycle() {
        super();
        setTypeClasses(new Class<?>[] {ComponentLifecycle.class, SystemIntegrityChecker.class});
    }

    @Override
    public void start() {
        sortBeans();
        checkIntegrity();
        configure();

        super.start();
    }

    protected void checkIntegrity() {
        for (SystemIntegrityChecker checker : getBeans(SystemIntegrityChecker.class)) {
            logger.info("Running system integrity checker " + checker);

            checker.check();
        }
    }

    public void startBeans() {
        logger.info("Starting CloudStack Components");

        with(new WithComponentLifeCycle() {
            @Override
            public void with(ComponentLifecycle lifecycle) {
                lifecycle.start();

                if (lifecycle instanceof ManagementBean) {
                    ManagementBean mbean = (ManagementBean)lifecycle;
                    try {
                        JmxUtil.registerMBean(mbean);
                    } catch (MalformedObjectNameException e) {
                        logger.warn("Unable to register MBean: " + mbean.getName(), e);
                    } catch (InstanceAlreadyExistsException e) {
                        logger.warn("Unable to register MBean: " + mbean.getName(), e);
                    } catch (MBeanRegistrationException e) {
                        logger.warn("Unable to register MBean: " + mbean.getName(), e);
                    } catch (NotCompliantMBeanException e) {
                        logger.warn("Unable to register MBean: " + mbean.getName(), e);
                    }
                    logger.info("Registered MBean: " + mbean.getName());
                }
            }
        });

        logger.info("Done Starting CloudStack Components");
    }

    public void stopBeans() {
        with(new WithComponentLifeCycle() {
            @Override
            public void with(ComponentLifecycle lifecycle) {
                logger.info("stopping bean " + lifecycle.getName());
                lifecycle.stop();
            }
        });
    }

    private void configure() {
        logger.info("Configuring CloudStack Components");

        with(new WithComponentLifeCycle() {
            @Override
            public void with(ComponentLifecycle lifecycle) {
                try {
                    lifecycle.configure(lifecycle.getName(), lifecycle.getConfigParams());
                } catch (ConfigurationException e) {
                    logger.error("Failed to configure " +  lifecycle.getName(), e);
                    throw new CloudRuntimeException(e);
                }
            }
        });

        logger.info("Done Configuring CloudStack Components");
    }

    private void sortBeans() {
        for (ComponentLifecycle lifecycle : getBeans(ComponentLifecycle.class)) {
            Set<ComponentLifecycle> set = sorted.get(lifecycle.getRunLevel());

            if (set == null) {
                set = new HashSet<ComponentLifecycle>();
                sorted.put(lifecycle.getRunLevel(), set);
            }

            set.add(lifecycle);
        }
    }

    @Override
    public void stop() {
        with(new WithComponentLifeCycle() {
            @Override
            public void with(ComponentLifecycle lifecycle) {
                lifecycle.stop();
            }
        });

        super.stop();
    }

    protected void with(WithComponentLifeCycle with) {
        for (Set<ComponentLifecycle> lifecycles : sorted.values()) {
            for (ComponentLifecycle lifecycle : lifecycles) {
                with.with(lifecycle);
            }
        }
    }

    @Override
    public int getPhase() {
        return 2000;
    }

    private static interface WithComponentLifeCycle {
        public void with(ComponentLifecycle lifecycle);
    }
}
