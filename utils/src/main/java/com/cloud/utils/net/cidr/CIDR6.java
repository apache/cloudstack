//
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
//

package com.cloud.utils.net.cidr;

import java.net.Inet6Address;
import java.net.InetAddress;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;

public class CIDR6 implements CIDR {
    protected final static Logger s_logger = Logger.getLogger(CIDR6.class);

    protected InetAddress baseAddress;
    protected int cidrMask;

    protected CIDR6(Inet6Address newaddress, int newmask) {
    }

    @Override
    public boolean isNull() {
        return false;
    }

    public int compareTo(CIDR arg) {
        throw new NotImplementedException();
    }

    @Override
    public boolean contains(InetAddress inetAddress) {
        throw new NotImplementedException();
        // TODO Auto-generated method stub
        //return false;
    }

    @Override
    public InetAddress getBaseAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getMask() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean contains(CIDR cidr) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean overlaps(CIDR cidr) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String toString() {
        return baseAddress.getHostAddress() + '/' + cidrMask;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CIDR)) {
            return false;
        }
        return compareTo((CIDR)o) == 0;
    }

    @Override
    public int hashCode() {
        return baseAddress.hashCode();
    }

}