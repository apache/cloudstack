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

import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VM;

public class ExtraConfigurationUtility {
    protected static Logger LOGGER = LogManager.getLogger(ExtraConfigurationUtility.class);

    public static void setExtraConfigurationToVm(Connection conn, VM.Record vmr, VM vm, Map<String, String> extraConfig) {
        Map<String, Object> recordMap = vmr.toMap();
        for (String key : extraConfig.keySet()) {
            String cfg = extraConfig.get(key);
            // cfg is either param=value or map-param:key=value
            Pair<String, String> configParam = prepareKeyValuePair(cfg);
            if (configParam == null) {
                LOGGER.warn("Invalid extra config passed: " + cfg);
                continue;
            }

            // paramKey is either param or map-param:key for map parameters
            String paramKey = configParam.first();
            String paramValue = configParam.second();

            //Map params
            LOGGER.debug("Applying [{}] configuration as [{}].", paramKey, paramValue);
            if (paramKey.contains(":")) {
                // Map params - paramKey is map-param:key
                applyConfigWithNestedKeyValue(conn, vm, recordMap, paramKey, paramValue);
            } else {
                // Params - paramKey is param
                applyConfigWithKeyValue(conn, vm, recordMap, paramKey, paramValue);
            }
        }
    }

    private static boolean isValidOperation(Map<String, Object> recordMap, String actualParam) {
        return recordMap.containsKey(actualParam);
    }

    private static Map<String, String> putInMap(Map<String, String> map, String key, String value) {
        map.put(key, value);
        return map;
    }

    /**
     * Nested keys contain ":" between the paramKey and need to split into operation param and key
     * */
    private static void applyConfigWithNestedKeyValue(Connection conn, VM vm, Map<String, Object> recordMap, String paramKey, String paramValue) {
        // paramKey is map-param:key
        int i = paramKey.indexOf(":");
        String actualParam = paramKey.substring(0, i);
        String keyName = paramKey.substring(i + 1);

        if (!isValidOperation(recordMap, actualParam)) {
            LOGGER.error("Unsupported extra configuration has been passed " + actualParam);
            throw new InvalidParameterValueException("Unsupported extra configuration option has been passed: " + actualParam);
        }

        try {
            // map-param param with '_'
            switch (actualParam) {
                case "VCPUs_params":
                    vm.setVCPUsParams(conn, putInMap(vm.getVCPUsParams(conn), keyName, paramValue));
                    break;
                case "platform":
                    vm.addToPlatform(conn, keyName, paramValue);
                    break;
                case "HVM_boot_params":
                    vm.setHVMBootParams(conn, putInMap(vm.getHVMBootParams(conn), keyName, paramValue));
                    break;
                case "other_config":
                    vm.setOtherConfig(conn, putInMap(vm.getOtherConfig(conn), keyName, paramValue));
                    break;
                case "xenstore_data":
                    vm.setXenstoreData(conn, putInMap(vm.getXenstoreData(conn), keyName, paramValue));
                    break;
                default:
                    String msg = String.format("Passed configuration %s is not supported", paramKey);
                    LOGGER.warn(msg);
            }
        } catch (XmlRpcException | Types.XenAPIException e) {
            LOGGER.error("Exception caught while setting VM configuration: [{}]", e.getMessage() == null ? e.toString() : e.getMessage());
            LOGGER.debug("Exception caught while setting VM configuration", e);
            throw new CloudRuntimeException("Exception caught while setting VM configuration", e);
        }
    }

    private static void applyConfigWithKeyValue(Connection conn, VM vm, Map<String, Object> recordMap, String paramKey, String paramValue) {
        if (!isValidOperation(recordMap, paramKey)) {
            LOGGER.error("Unsupported extra configuration has been passed: " + paramKey);
            throw new InvalidParameterValueException("Unsupported extra configuration parameter key has been passed: " + paramKey);
        }

        try {
            // param with '_'
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
                case "is_a_template":
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
                    LOGGER.error(anotherMessage);
            }
        } catch (XmlRpcException | Types.XenAPIException e) {
            LOGGER.error("Exception caught while setting VM configuration, exception: " + e.getMessage());
            throw new CloudRuntimeException("Exception caught while setting VM configuration: ", e);
        }
    }

    protected static Pair<String, String> prepareKeyValuePair(String cfg) {
        // cfg is either param=value or map-param:key=value
        int indexOfEqualSign = cfg.indexOf("=");
        if (indexOfEqualSign <= 0) {
            return null;
        }

        String key;
        // Replace '-' with '_' in param / map-param only
        if (cfg.contains(":")) {
            int indexOfColon = cfg.indexOf(":");
            if (indexOfColon <= 0 || indexOfEqualSign < indexOfColon) {
                return null;
            }
            String mapParam = cfg.substring(0, indexOfColon).replace("-", "_");
            String paramKey = cfg.substring(indexOfColon + 1, indexOfEqualSign);
            key = mapParam + ":" + paramKey;
        } else {
            key = cfg.substring(0, indexOfEqualSign).replace("-", "_");
        }

        String value = cfg.substring(indexOfEqualSign + 1);
        return new Pair<>(key, value);
    }
}
