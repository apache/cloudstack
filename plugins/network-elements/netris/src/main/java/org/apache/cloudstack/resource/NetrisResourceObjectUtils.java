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

import java.util.Objects;

public class NetrisResourceObjectUtils {

    public enum NetrisObjectType {
        VPC, IPAM_ALLOCATION, IPAM_SUBNET, VNET, SNAT, STATICNAT, DNAT, STATICROUTE, ACL, LB
    }

    public static String retrieveNetrisResourceObjectName(NetrisCommand cmd, NetrisObjectType netrisObjectType, String... suffixes) {
        long zoneId = cmd.getZoneId();
        Long accountId = cmd.getAccountId();
        Long domainId = cmd.getDomainId();
        Long objectId = cmd.getId();
        String objectName = cmd.getName();
        boolean isVpc = cmd.isVpc();
        boolean isZoneLevel = accountId == null && domainId == null;

        StringBuilder stringBuilder = new StringBuilder();
        if (isZoneLevel) {
            stringBuilder.append(String.format("Z%s", zoneId));
        } else {
            stringBuilder.append(String.format("D%s-A%s-Z%s", domainId, accountId, zoneId));
        }
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
                if (!isZoneLevel) {
                    stringBuilder.append(String.format("%s%s", prefix, objectId));
                }
                break;
            case IPAM_SUBNET:
                if (!isZoneLevel) {
                    if (Objects.nonNull(objectId) && Objects.nonNull(objectName) && !isVpc) {
                        stringBuilder.append(String.format("-N%s", objectId));
                    } else {
                        stringBuilder.append(String.format("-V%s-%s", suffixes[0], suffixes[1]));
                        return stringBuilder.toString();
                    }
                }
                break;
            case SNAT:
                stringBuilder.append(String.format("%s%s-%s", prefix, suffixes[0], "SNAT"));
                suffixes = new String[0];
                break;
            case STATICNAT:
                stringBuilder.append(String.format("%s%s-VM%s-%s", prefix, suffixes[0], suffixes[1], "STATICNAT"));
                suffixes = new String[0];
                break;
            case DNAT:
                stringBuilder.append(String.format("%s%s-%s", prefix, suffixes[0], "DNAT"));
                suffixes = ArrayUtils.subarray(suffixes, 1, suffixes.length);
                break;
            case STATICROUTE:
                stringBuilder.append(String.format("%s%s-%s%s", prefix, suffixes[0], "ROUTE", suffixes[1]));
                suffixes = new String[0];
                break;
            case VNET:
            case ACL:
               break;
            case LB:
                stringBuilder.append(String.format("%s%s", prefix, objectId));
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
