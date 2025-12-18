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
package org.apache.cloudstack.storage.template;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.VNF;
import com.cloud.storage.Storage;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.template.DeleteVnfTemplateCmd;
import org.apache.cloudstack.api.command.user.template.RegisterVnfTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateVnfTemplateCmd;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VnfTemplateUtils {
    private VnfTemplateUtils() {
    }

    public static List<VNF.VnfNic> getVnfNicsList(Map vnfNics) {
        List<VNF.VnfNic> nicsList = new ArrayList<>();
        if (MapUtils.isNotEmpty(vnfNics)) {
            Collection nicsCollection = vnfNics.values();
            Iterator iter = nicsCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> nicDetails = (HashMap<String, String>)iter.next();
                String deviceIdString = nicDetails.get("deviceid");
                String name = nicDetails.get("name");
                String requiredString = nicDetails.get("required");
                String managementString = nicDetails.get("management");
                String description = nicDetails.get("description");
                Integer deviceId = null;
                if (StringUtils.isAnyBlank(name, deviceIdString)) {
                    throw new InvalidParameterValueException("VNF nic name and deviceid cannot be null");
                }
                try {
                    deviceId = Integer.parseInt(deviceIdString);
                } catch (NumberFormatException e) {
                    throw new InvalidParameterValueException("Unable to parse VNF nic deviceId to Integer: " + deviceId);
                }
                boolean required = StringUtils.isBlank(requiredString) || Boolean.parseBoolean(requiredString);
                boolean management = StringUtils.isBlank(managementString) || Boolean.parseBoolean(managementString);
                nicsList.add(new VNF.VnfNic(deviceId, name, required, management, description));
            }
            Collections.sort(nicsList, Comparator.comparing(VNF.VnfNic::getDeviceId));
        }
        return nicsList;
    }

    public static void validateApiCommandParams(Map<String, String> vnfDetails, List<VNF.VnfNic> vnfNics, String templateType) {
        if (templateType != null && !Storage.TemplateType.VNF.name().equals(templateType)) {
            throw new InvalidParameterValueException("The template type must be VNF for VNF templates.");
        }

        if (vnfDetails != null) {
            for (String vnfDetail : vnfDetails.keySet()) {
                if (!EnumUtils.isValidEnumIgnoreCase(VNF.VnfDetail.class, vnfDetail) &&
                        !EnumUtils.isValidEnumIgnoreCase(VNF.AccessDetail.class, vnfDetail)) {
                    throw new InvalidParameterValueException(String.format("Invalid VNF detail found: %s. Valid values are %s and %s", vnfDetail,
                            Arrays.stream(VNF.AccessDetail.values()).map(method -> method.toString()).collect(Collectors.joining(", ")),
                            Arrays.stream(VNF.VnfDetail.values()).map(method -> method.toString()).collect(Collectors.joining(", "))));
                }
                if (vnfDetails.get(vnfDetail) == null) {
                    throw new InvalidParameterValueException("Empty value found for VNF detail: " + vnfDetail);
                }
                if (VNF.AccessDetail.ACCESS_METHODS.name().equalsIgnoreCase(vnfDetail)) {
                    String[] accessMethods = vnfDetails.get(vnfDetail).split(",");
                    for (String accessMethod : accessMethods) {
                        if (VNF.AccessMethod.fromValue(accessMethod.trim()) == null) {
                            throw new InvalidParameterValueException(String.format("Invalid VNF access method found: %s. Valid values are %s", accessMethod,
                                    Arrays.stream(VNF.AccessMethod.values()).map(method -> method.toString()).sorted().collect(Collectors.joining(", "))));
                        }
                    }
                }
            }
        }

        validateVnfNics(vnfNics);
    }

    public static void validateVnfNics(List<VNF.VnfNic> nicsList) {
        long deviceId = 0L;
        boolean required = true;
        for (VNF.VnfNic nic : nicsList) {
            if (nic.getDeviceId() != deviceId) {
                throw new InvalidParameterValueException(String.format("deviceid must be consecutive and start from 0. Nic deviceid should be %s but actual is %s.", deviceId, nic.getDeviceId()));
            }
            if (!required && nic.isRequired()) {
                throw new InvalidParameterValueException(String.format("required cannot be true if a preceding nic is optional. Nic with deviceid %s should be required but actual is optional.", deviceId));
            }
            deviceId ++;
            required = nic.isRequired();
        }
    }

    public static void validateApiCommandParams(BaseCmd cmd, VirtualMachineTemplate template) {
        if (cmd instanceof RegisterVnfTemplateCmd) {
            RegisterVnfTemplateCmd registerCmd = (RegisterVnfTemplateCmd) cmd;
            validateApiCommandParams(registerCmd.getVnfDetails(), registerCmd.getVnfNics(), registerCmd.getTemplateType());
        } else if (cmd instanceof UpdateVnfTemplateCmd) {
            UpdateVnfTemplateCmd updateCmd = (UpdateVnfTemplateCmd) cmd;
            if (!Storage.TemplateType.VNF.equals(template.getTemplateType())) {
                throw new InvalidParameterValueException(String.format("Cannot update as template %s is not a VNF template. The template type is %s.", updateCmd.getId(), template.getTemplateType()));
            }
            validateApiCommandParams(updateCmd.getVnfDetails(), updateCmd.getVnfNics(), updateCmd.getTemplateType());
        } else if (cmd instanceof DeleteVnfTemplateCmd) {
            if (!Storage.TemplateType.VNF.equals(template.getTemplateType())) {
                DeleteVnfTemplateCmd deleteCmd = (DeleteVnfTemplateCmd) cmd;
                throw new InvalidParameterValueException(String.format("Cannot delete as Template %s is not a VNF template. The template type is %s.", deleteCmd.getId(), template.getTemplateType()));
            }
        }
    }

    public static void validateVnfCidrList(List<String> cidrList) {
        if (CollectionUtils.isEmpty(cidrList)) {
            return;
        }
        for (String cidr : cidrList) {
            if (!NetUtils.isValidIp4Cidr(cidr)) {
                throw new InvalidParameterValueException(String.format("Invalid cidr for VNF appliance: %s", cidr));
            }
        }
    }
}
