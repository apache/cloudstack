/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.alert;

import com.cloud.capacity.CapacityVO;
import com.cloud.utils.component.Manager;

public interface AlertManager extends Manager {
    public static final short ALERT_TYPE_MEMORY = CapacityVO.CAPACITY_TYPE_MEMORY;
    public static final short ALERT_TYPE_CPU = CapacityVO.CAPACITY_TYPE_CPU;
    public static final short ALERT_TYPE_STORAGE = CapacityVO.CAPACITY_TYPE_STORAGE;
    public static final short ALERT_TYPE_STORAGE_ALLOCATED = CapacityVO.CAPACITY_TYPE_STORAGE_ALLOCATED;
    public static final short ALERT_TYPE_VIRTUAL_NETWORK_PUBLIC_IP = CapacityVO.CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP;
    public static final short ALERT_TYPE_PRIVATE_IP = CapacityVO.CAPACITY_TYPE_PRIVATE_IP;
    public static final short ALERT_TYPE_SECONDARY_STORAGE = CapacityVO.CAPACITY_TYPE_SECONDARY_STORAGE;
    public static final short ALERT_TYPE_HOST = 6;
    public static final short ALERT_TYPE_USERVM = 7;
    public static final short ALERT_TYPE_DOMAIN_ROUTER = 8;
    public static final short ALERT_TYPE_CONSOLE_PROXY = 9;
    public static final short ALERT_TYPE_ROUTING = 10; // lost connection to default route (to the gateway)
    public static final short ALERT_TYPE_STORAGE_MISC = 11; // lost connection to default route (to the gateway)
    public static final short ALERT_TYPE_USAGE_SERVER = 12; // lost connection to default route (to the gateway)
    public static final short ALERT_TYPE_MANAGMENT_NODE = 13; // lost connection to default route (to the gateway)
    public static final short ALERT_TYPE_DOMAIN_ROUTER_MIGRATE = 14;
    public static final short ALERT_TYPE_CONSOLE_PROXY_MIGRATE = 15;
    public static final short ALERT_TYPE_USERVM_MIGRATE = 16;
    public static final short ALERT_TYPE_VLAN = 17;
    public static final short ALERT_TYPE_SSVM = 18;
    public static final short ALERT_TYPE_USAGE_SERVER_RESULT = 19; // Usage job result
    public static final short ALERT_TYPE_STORAGE_DELETE = 20;
    public static final short ALERT_TYPE_UPDATE_RESOURCE_COUNT = 21; // Generated when we fail to update the resource count
    public static final short ALERT_TYPE_USAGE_SANITY_RESULT = 22;

    void clearAlert(short alertType, long dataCenterId, long podId);

    void sendAlert(short alertType, long dataCenterId, Long podId, String subject, String body);

    void recalculateCapacity();
}
