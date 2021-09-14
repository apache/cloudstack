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
package org.apache.cloudstack.annotation.dao;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.annotation.AnnotationVO;
import com.cloud.utils.db.GenericDao;

import java.util.List;

/**
 * @since 4.11
 */
public interface AnnotationDao extends GenericDao<AnnotationVO, Long> {
    List<AnnotationVO> listByEntityType(String entityType, String userUuid, boolean isCallerAdmin, String annotationFilter, String callingUserUuid, String keyword);
    List<AnnotationVO> listByEntity(String entityType, String entityUuid, String userUuid, boolean isCallerAdmin, String annotationFilter, String callingUserUuid, String keyword);
    List<AnnotationVO> listAllAnnotations(String userUuid, RoleType roleType, String annotationFilter, String keyword);
    boolean hasAnnotations(String entityUuid, String entityType, boolean isCallerAdmin);
    boolean removeByEntityType(String entityType, String entityUuid);
    AnnotationVO findOneByEntityId(String entityUuid);
}
