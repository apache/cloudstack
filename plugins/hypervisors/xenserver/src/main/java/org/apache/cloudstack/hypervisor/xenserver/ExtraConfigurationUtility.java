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
package org.apache.cloudstack.hypervisor.xenserver;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VM;

public class ExtraConfigurationUtility {
    private static final Logger LOG = Logger.getLogger(ExtraConfigurationUtility.class);

    public static void setExtraConfigurationToVm(Connection conn, VM.Record vmr, VM vm, Map<String, String> extraConfig) {
        Map<String, Object> recordMap = vmr.toMap();
        for (String key : extraConfig.keySet()) {
            String cfg = extraConfig.get(key);
            Map<String, String> configParams = prepareKeyValuePair(cfg);

            // paramKey is either param or param:key for map parameters
            String paramKey = configParams.keySet().toString().replaceAll("[\\[\\]]", "");
            String paramValue = configParams.get(paramKey);

            //Map params
            if (paramKey.contains(":")) {
                applyConfigWithNestedKeyValue(conn, vm, recordMap, paramKey, paramValue);
            } else {
                applyConfigWithKeyValue(conn, vm, recordMap, paramKey, paramValue);
            }
        }
    }

    private static boolean isValidOperation(Map<String, Object> recordMap, String actualParam) {
        return recordMap.containsKey(actualParam);
    }

    /**
     * Nested keys contain ":" between the paramKey and need to split into operation param and key
     * */
    private static void applyConfigWithNestedKeyValue(Connection conn, VM vm, Map<String, Object> recordMap, String paramKey, String paramValue) {
        int i = paramKey.indexOf(":");
        String actualParam = paramKey.substring(0, i);
        String keyName = paramKey.substring(i + 1);

        if (!isValidOperation(recordMap, actualParam)) {
            LOG.error("Unsupported extra configuration has been passed " + actualParam);
            throw new InvalidParameterValueException("Unsupported extra configuration option has been passed: " + actualParam);
        }

        try {
            switch (actualParam) {
                case "VCPUs_params":
                    vm.addToVCPUsParams(conn, keyName, paramValue);
                    break;
                case "platform":
                    vm.addToOtherConfig(conn, keyName, paramValue);
                    break;
                case "HVM_boot_params":
                    vm.addToHVMBootParams(conn, keyName, paramValue);
                    break;
                case "other_config":
                    vm.addToOtherConfig(conn, keyName, paramValue);
                    break;
                case "xenstore_data":
                    vm.addToXenstoreData(conn, keyName, paramValue);
                    break;
                default:
                    String msg = String.format("Passed configuration %s is not supported", paramKey);
                    LOG.warn(msg);
            }
        } catch (XmlRpcException | Types.XenAPIException e) {
            LOG.error("Exception caught while setting VM configuration. exception: " + e.getMessage());
            throw new CloudRuntimeException("Exception caught while setting VM configuration", e);
        }
    }

    private static void applyConfigWithKeyValue(Connection conn, VM vm, Map<String, Object> recordMap, String paramKey, String paramValue) {
        if (!isValidOperation(recordMap, paramKey)) {
            LOG.error("Unsupported extra configuration has been passed: " + paramKey);
            throw new InvalidParameterValueException("Unsupported extra configuration parameter key has been passed: " + paramKey);
        }

        try {
            switch (paramKey) {
                case "HVM_boot_policy":
                    vm.setHVMBootPolicy(conn, paramValue);
                    break;
                case "HVM_shadow_multiplier":
                    vm.setHVMShadowMultiplier(conn, Double.valueOf(paramValue));
                    break;
                case "PV_kernel":
                    vm.setPVKernel(conn, paramValue);
                    break;
                case "PV_ramdisk":
                    vm.setPVRamdisk(conn, paramValue);
                    break;
                case "PV_args":
                    vm.setPVArgs(conn, paramValue);
                    break;
                case "PV_legacy_args":
                    vm.setPVLegacyArgs(conn, paramValue);
                    break;
                case "PV_bootloader":
                    vm.setPVBootloader(conn, paramValue);
                    break;
                case "PV_bootloader_args":
                    vm.setPVBootloaderArgs(conn, paramValue);
                    break;
                case "ha_restart_priority":
                    vm.setHaRestartPriority(conn, paramValue);
                    break;
                case "start_delay":
                    vm.setStartDelay(conn, Long.valueOf(paramValue));
                    break;
                case "shutdown_delay":
                    vm.setShutdownDelay(conn, Long.valueOf(paramValue));
                    break;
                case "order":
                    vm.setOrder(conn, Long.valueOf(paramValue));
                    break;
                case "VCPUs_max":
                    vm.setVCPUsMax(conn, Long.valueOf(paramValue));
                    break;
                case "VCPUs_at_startup":
                    vm.setVCPUsAtStartup(conn, Long.valueOf(paramValue));
                    break;
                case "is-a-template":
                    vm.setIsATemplate(conn, Boolean.valueOf(paramValue));
                    break;
                case "memory_static_max":
                    vm.setMemoryStaticMax(conn, Long.valueOf(paramValue));
                    break;
                case "memory_static_min":
                    vm.setMemoryStaticMin(conn, Long.valueOf(paramValue));
                    break;
                case "memory_dynamic_max":
                    vm.setMemoryDynamicMax(conn, Long.valueOf(paramValue));
                    break;
                case "memory_dynamic_min":
                    vm.setMemoryDynamicMin(conn, Long.valueOf(paramValue));
                    break;
                default:
                    String anotherMessage = String.format("Passed configuration %s is not supported", paramKey);
                    LOG.error(anotherMessage);
            }
        } catch (XmlRpcException | Types.XenAPIException e) {
            LOG.error("Exception caught while setting VM configuration, exception: " + e.getMessage());
            throw new CloudRuntimeException("Exception caught while setting VM configuration: ", e);
        }
    }

    private static Map<String, String> prepareKeyValuePair(String cfg) {
        Map<String, String> configKeyPair = new HashMap<>();
        int indexOfEqualSign = cfg.indexOf("=");
        String key = cfg.substring(0, indexOfEqualSign).replace("-", "_");
        String value = cfg.substring(indexOfEqualSign + 1);
        configKeyPair.put(key, value);
        return configKeyPair;
    }
}
