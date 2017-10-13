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

import org.apache.cloudstack.api.command.admin.annotation.AddAnnotationCmd;
import org.apache.cloudstack.api.command.admin.annotation.ListAnnotationsCmd;
import org.apache.cloudstack.api.command.admin.annotation.RemoveAnnotationCmd;
import org.apache.cloudstack.api.response.AnnotationResponse;
import org.apache.cloudstack.api.response.ListResponse;

public interface AnnotationService {
    ListResponse<AnnotationResponse> searchForAnnotations(ListAnnotationsCmd cmd);

    AnnotationResponse addAnnotation(AddAnnotationCmd addAnnotationCmd);
    AnnotationResponse addAnnotation(String text, EntityType type, String uuid);

    AnnotationResponse removeAnnotation(RemoveAnnotationCmd removeAnnotationCmd);

    enum EntityType {
        HOST("host"), DOMAIN("domain"), VM("vm_instance");
        private String tableName;

        EntityType(String tableName) {
            this.tableName = tableName;
        }
        static public boolean contains(String representation) {
            try {
                /* EntityType tiep = */ valueOf(representation);
                return true;
            } catch (IllegalArgumentException iae) {
                return false;
            }
        }
    }
}
