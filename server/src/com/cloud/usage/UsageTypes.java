/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.usage;

public class UsageTypes {
    public static final int RUNNING_VM = 1;
    public static final int ALLOCATED_VM = 2; // used for tracking how long storage has been allocated for a VM
    public static final int IP_ADDRESS = 3;
    public static final int NETWORK_BYTES_SENT = 4;
    public static final int NETWORK_BYTES_RECEIVED = 5;
    public static final int VOLUME = 6;
    public static final int TEMPLATE = 7;
    public static final int ISO = 8;
    public static final int SNAPSHOT = 9;
    public static final int SECURITY_GROUP = 10;
    public static final int LOAD_BALANCER_POLICY = 11;
    public static final int PORT_FORWARDING_RULE = 12;
    public static final int NETWORK_OFFERING = 13;
}
