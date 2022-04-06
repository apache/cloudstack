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
package com.cloud.user;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public interface Account extends ControlledEntity, InternalIdentity, Identity {

    /**
     *  Account states.
     * */
    enum State {
        DISABLED, ENABLED, LOCKED;

        /**
         * The toString method was overridden to maintain consistency in the DB, as the GenericDaoBase uses toString in the enum value to make the sql statements
         * and previously the enum was in lowercase.
         * */
        @Override
        public String toString(){
            return super.toString().toLowerCase();
        }

        /**
         * This method was created to maintain backwards compatibility to the DB schema. Unfortunately we can't override the valueOf method.
         * */
        public static State getValueOf(String name){
            return State.valueOf(name.toUpperCase());
        }

    }

    /**
     * Account types.
     * */
    enum Type {
        NORMAL, ADMIN, DOMAIN_ADMIN, RESOURCE_DOMAIN_ADMIN, READ_ONLY_ADMIN, PROJECT, UNKNOWN;

        private static Map<Integer,Type> ACCOUNT_TYPE_MAP = new HashMap<>();

        static {
            for (Type t: Type.values()) {
                ACCOUNT_TYPE_MAP.put(t.ordinal(),t);
            }
            ACCOUNT_TYPE_MAP.remove(6);
        }

        public static Type getFromValue(Integer type){
            return ACCOUNT_TYPE_MAP.get(type);
        }
    }

    public static final long ACCOUNT_ID_SYSTEM = 1;

    public String getAccountName();

    public Type getType();

    public Long getRoleId();

    public State getState();

    public Date getCreated();

    public Date getRemoved();

    public String getNetworkDomain();

    public Long getDefaultZoneId();

    @Override
    public String getUuid();

    boolean isDefault();

}
