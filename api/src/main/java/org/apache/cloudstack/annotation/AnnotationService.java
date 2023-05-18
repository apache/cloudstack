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
package org.apache.cloudstack.annotation;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.command.admin.annotation.AddAnnotationCmd;
import org.apache.cloudstack.api.command.admin.annotation.ListAnnotationsCmd;
import org.apache.cloudstack.api.command.admin.annotation.RemoveAnnotationCmd;
import org.apache.cloudstack.api.command.admin.annotation.UpdateAnnotationVisibilityCmd;
import org.apache.cloudstack.api.response.AnnotationResponse;
import org.apache.cloudstack.api.response.ListResponse;

import java.util.ArrayList;
import java.util.List;

public interface AnnotationService {
    ListResponse<AnnotationResponse> searchForAnnotations(ListAnnotationsCmd cmd);

    AnnotationResponse addAnnotation(AddAnnotationCmd addAnnotationCmd);
    AnnotationResponse addAnnotation(String text, EntityType type, String uuid, boolean adminsOnly);

    AnnotationResponse removeAnnotation(RemoveAnnotationCmd removeAnnotationCmd);

    AnnotationResponse updateAnnotationVisibility(UpdateAnnotationVisibilityCmd cmd);

    enum EntityType {
        VM(true), VOLUME(true), SNAPSHOT(true),
        VM_SNAPSHOT(true), INSTANCE_GROUP(true), SSH_KEYPAIR(true), USER_DATA(true),
        NETWORK(true), VPC(true), PUBLIC_IP_ADDRESS(true), VPN_CUSTOMER_GATEWAY(true),
        TEMPLATE(true), ISO(true), KUBERNETES_CLUSTER(true),
        SERVICE_OFFERING(false), DISK_OFFERING(false), NETWORK_OFFERING(false),
        ZONE(false), POD(false), CLUSTER(false), HOST(false), DOMAIN(false),
        PRIMARY_STORAGE(false), SECONDARY_STORAGE(false), VR(false), SYSTEM_VM(false),
        AUTOSCALE_VM_GROUP(true), MANAGEMENT_SERVER(false),;

        private final boolean usersAllowed;

        public boolean isUserAllowed() {
            return this.usersAllowed;
        }

        EntityType(boolean usersAllowed) {
            this.usersAllowed = usersAllowed;
        }

        static public boolean contains(String representation) {
            try {
                /* EntityType tiep = */ valueOf(representation);
                return true;
            } catch (IllegalArgumentException iae) {
                return false;
            }
        }

        static public List<EntityType> getNotAllowedTypesForNonAdmins(RoleType roleType) {
            List<EntityType> list = new ArrayList<>();
            list.add(EntityType.NETWORK_OFFERING);
            list.add(EntityType.ZONE);
            list.add(EntityType.POD);
            list.add(EntityType.CLUSTER);
            list.add(EntityType.HOST);
            list.add(EntityType.PRIMARY_STORAGE);
            list.add(EntityType.SECONDARY_STORAGE);
            list.add(EntityType.VR);
            list.add(EntityType.SYSTEM_VM);
            list.add(EntityType.MANAGEMENT_SERVER);
            if (roleType != RoleType.DomainAdmin) {
                list.add(EntityType.DOMAIN);
                list.add(EntityType.SERVICE_OFFERING);
                list.add(EntityType.DISK_OFFERING);
            }
            return list;
        }
    }
}
