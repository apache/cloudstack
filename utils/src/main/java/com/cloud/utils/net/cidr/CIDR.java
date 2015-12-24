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

import java.net.InetAddress;
import com.cloud.utils.Nullable;

public interface CIDR extends Comparable<CIDR>, Nullable {

    final static CIDR NULL = new NullCIDR();
    final static int MAX_CIDR = 32;

    public InetAddress getBaseAddress();

    public int getMask();

    public boolean contains(InetAddress inetAddress);

    public boolean contains(CIDR cidr);

    public boolean overlaps(CIDR cidr);

    public String toString();

    public boolean equals(Object o);

    public int hashCode();

    class NullCIDR implements CIDR {

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
        public boolean contains(InetAddress cidr) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public int compareTo(CIDR o) {
            // TODO Auto-generated method stub
            return 0;
        }
    }

}