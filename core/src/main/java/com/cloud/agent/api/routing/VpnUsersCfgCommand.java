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

package com.cloud.agent.api.routing;

import java.util.List;

import com.cloud.agent.api.LogLevel;
import com.cloud.agent.api.LogLevel.Log4jLevel;
import com.cloud.network.VpnUser;

public class VpnUsersCfgCommand extends NetworkElementCommand {
    public static class UsernamePassword {
        private String username;
        @LogLevel(Log4jLevel.Off)
        private String password;
        boolean add = true;

        public boolean isAdd() {
            return add;
        }

        public void setAdd(boolean add) {
            this.add = add;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public UsernamePassword(String username, String password) {
            super();
            this.username = username;
            this.password = password;
        }

        public UsernamePassword(String username, String password, boolean add) {
            super();
            this.username = username;
            this.password = password;
            this.add = add;
        }

        protected UsernamePassword() {
            //for Gson
        }

        public String getUsernamePassword() {
            return getUsername() + "," + getPassword();
        }
    }

    UsernamePassword[] userpwds;

    protected VpnUsersCfgCommand() {

    }

    public VpnUsersCfgCommand(List<VpnUser> addUsers, List<VpnUser> removeUsers) {
        userpwds = new UsernamePassword[addUsers.size() + removeUsers.size()];
        int i = 0;
        for (VpnUser vpnUser : removeUsers) {
            userpwds[i++] = new UsernamePassword(vpnUser.getUsername(), vpnUser.getPassword(), false);
        }
        for (VpnUser vpnUser : addUsers) {
            userpwds[i++] = new UsernamePassword(vpnUser.getUsername(), vpnUser.getPassword(), true);
        }
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public UsernamePassword[] getUserpwds() {
        return userpwds;
    }

}
