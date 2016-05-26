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

package org.apache.cloudstack.utils.net.cidr;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.cloud.utils.Nullable;


public interface CIDR extends Comparable<CIDR>, Nullable, Serializable {

    final CIDR NULL = new NullCIDR();
    static final int MAX_CIDR4 = 32;
    static final int MAX_CIDR6 = 128;

    InetAddress getBaseAddress();

    int getMask();

    boolean contains(InetAddress inetAddress);

    boolean contains(CIDR cidr);

    boolean overlaps(CIDR cidr);

    boolean overlaps(CIDR[] cidr);

    boolean isGuestCidr() throws CIDRException;

    Long[] toLong();


    class NullCIDR implements CIDR {

        private static final long serialVersionUID = 1755041938734685988L;

        private NullCIDR() {
            super();
        }

        public boolean isNull() {
            return true;
        }

        @Override
        public String toString() {
            return "Null CIDR";
        }

        @Override
        public InetAddress getBaseAddress() {
            try {
                return InetAddress.getByAddress(null);
            } catch (UnknownHostException e) {
                throw new Error(e);
            }
        }

        @Override
        public int getMask() {
            return 0;
        }

        @Override
        public boolean contains(CIDR cidr) {
            return false;
        }

        @Override
        public boolean overlaps(CIDR cidr) {
            return false;
        }

        @Override
        public boolean contains(InetAddress cidr) {
            return false;
        }

        @Override
        public int compareTo(CIDR o) {
            return 0;
        }

        @Override
        public boolean isGuestCidr() {
            return false;
        }

        @Override
        public boolean overlaps(CIDR[] cidr) {
            return false;
        }

        @Override
        public Long[] toLong() {
            return new Long[0];
        }

    }

}