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

package org.apache.cloudstack.vm.bootgroup.readiness;

import java.util.Date;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.vm.bootgroup.InstanceBootGroupMember;

/**
 * A readiness rule always belongs to exactly one boot group and references one "item" within it:
 * a direct {@code VirtualMachine}-type member, a direct {@code InstanceGroup}-type member, or a VM
 * sitting inside one of the boot group's {@code InstanceGroup}-type members (no member row of its
 * own, referenced directly by VM id).
 */
public interface InstanceBootGroupReadinessRule extends Identity, InternalIdentity {

    long getBootGroupId();

    InstanceBootGroupMember.MemberType getItemType();

    long getItemId();

    RuleType getRuleType();

    String getName();

    boolean isEnabled();

    Date getCreated();

    /**
     * Which rule types are valid depends on {@link InstanceBootGroupMember.MemberType},
     * hypervisor-agnostic at the DB/API layer (no qemu/KVM naming stored). Ping/PortCheck/
     * CustomScript/GuestAgentLiveness apply to VirtualMachine items; MemberQuorum and
     * (group-scope) CustomScript apply to InstanceGroup items.
     */
    enum RuleType {
        GuestAgentLiveness(true),
        Ping(true),
        PortCheck(true),
        CustomScript(false),
        MemberQuorum(false);

        private final boolean memberTargeted;

        RuleType(boolean memberTargeted) {
            this.memberTargeted = memberTargeted;
        }

        /**
         * True for a rule type that, when attached to an InstanceGroup, is evaluated against every
         * current member individually and inherited by each member's own readiness — unlike
         * MemberQuorum/(group-scope) CustomScript, which only ever operate at group scope.
         */
        public boolean isMemberTargeted() {
            return memberTargeted;
        }
    }

    enum Status {
        Ready, NotReady, Error, Unknown
    }
}
