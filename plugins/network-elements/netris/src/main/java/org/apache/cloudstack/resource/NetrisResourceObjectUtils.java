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
package org.apache.cloudstack.resource;

import org.apache.cloudstack.agent.api.NetrisCommand;
import org.apache.commons.lang3.ArrayUtils;

public class NetrisResourceObjectUtils {

    public enum NetrisObjectType {
        VPC, IPAM_ALLOCATION, IPAM_SUBNET, VNET
    }

    public static String retrieveNetrisResourceObjectName(NetrisCommand cmd, NetrisObjectType netrisObjectType, String... suffixes) {
        long zoneId = cmd.getZoneId();
        long accountId = cmd.getAccountId();
        long domainId = cmd.getDomainId();
        long objectId = cmd.getId();
        String objectName = cmd.getName();
        boolean isVpc = cmd.isVpc();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("D%s-A%s-Z%s", domainId, accountId, zoneId));
        String prefix = isVpc ? "-V" : "-N";
        switch (netrisObjectType) {
            case VPC:
                if (ArrayUtils.isEmpty(suffixes)) {
                    stringBuilder.append(String.format("%s%s-%s", prefix, objectId, objectName));
                } else {
                    stringBuilder.append(String.format("%s%s", prefix, suffixes[0]));
                    suffixes = new String[0];
                }
                break;
            case IPAM_ALLOCATION:
                stringBuilder.append(String.format("%s%s", prefix, objectId));
                break;
            case IPAM_SUBNET:
                stringBuilder.append(String.format("-N%s", objectId));
                break;
            case VNET:
               break;
            default:
                stringBuilder.append(String.format("-%s", objectName));
                break;
        }
        if (ArrayUtils.isNotEmpty(suffixes)) {
            for (String suffix : suffixes) {
                stringBuilder.append(String.format("-%s", suffix));
            }
        }
        return stringBuilder.toString();
    }
}
