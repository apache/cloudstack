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

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.annotation.AnnotationService.EntityType;
import org.apache.cloudstack.annotation.AnnotationVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @since 4.1
 */
@Component
public class AnnotationDaoImpl extends GenericDaoBase<AnnotationVO, Long> implements AnnotationDao {
    private final SearchBuilder<AnnotationVO> AnnotationSearchBuilder;

    public AnnotationDaoImpl() {
        super();
        AnnotationSearchBuilder = createSearchBuilder();
        AnnotationSearchBuilder.and("entityType", AnnotationSearchBuilder.entity().getEntityType(), SearchCriteria.Op.EQ);
        AnnotationSearchBuilder.and("entityUuid", AnnotationSearchBuilder.entity().getEntityUuid(), SearchCriteria.Op.EQ);
        AnnotationSearchBuilder.and("userUuid", AnnotationSearchBuilder.entity().getUserUuid(), SearchCriteria.Op.EQ);
        AnnotationSearchBuilder.and("adminsOnly", AnnotationSearchBuilder.entity().getUserUuid(), SearchCriteria.Op.EQ);
        AnnotationSearchBuilder.and("annotation", AnnotationSearchBuilder.entity().getAnnotation(), SearchCriteria.Op.LIKE);
        AnnotationSearchBuilder.and("entityTypeNotIn", AnnotationSearchBuilder.entity().getEntityType(), SearchCriteria.Op.NOTIN);
        AnnotationSearchBuilder.done();
    }

    private List<AnnotationVO> listAnnotationsOrderedByCreatedDate(SearchCriteria<AnnotationVO> sc) {
        Filter filter = new Filter(AnnotationVO.class, "created", false, null, null);
        return listBy(sc, filter);
    }

    @Override
    public List<AnnotationVO> listByEntityType(String entityType, String userUuid, boolean isCallerAdmin,
                                                         String annotationFilter, String callingUserUuid, String keyword) {
        SearchCriteria<AnnotationVO> sc = AnnotationSearchBuilder.create();
        sc.addAnd("entityType", SearchCriteria.Op.EQ, entityType);
        if (StringUtils.isNotBlank(userUuid)) {
            sc.addAnd("userUuid", SearchCriteria.Op.EQ, userUuid);
        }
        if (!isCallerAdmin) {
            List<EntityType> adminOnlyTypes = Arrays.asList(EntityType.SERVICE_OFFERING, EntityType.DISK_OFFERING,
                    EntityType.NETWORK_OFFERING, EntityType.ZONE, EntityType.POD, EntityType.CLUSTER, EntityType.HOST,
                    EntityType.DOMAIN, EntityType.PRIMARY_STORAGE, EntityType.SECONDARY_STORAGE,
                    EntityType.VR, EntityType.SYSTEM_VM);
            if (StringUtils.isBlank(entityType)) {
                sc.setParameters("entityTypeNotIn", EntityType.SERVICE_OFFERING, EntityType.DISK_OFFERING,
                        EntityType.NETWORK_OFFERING, EntityType.ZONE, EntityType.POD, EntityType.CLUSTER, EntityType.HOST,
                        EntityType.DOMAIN, EntityType.PRIMARY_STORAGE, EntityType.SECONDARY_STORAGE,
                        EntityType.VR, EntityType.SYSTEM_VM);
            } else if (adminOnlyTypes.contains(EntityType.valueOf(entityType))) {
                return new ArrayList<>();
            }
            sc.addAnd("adminsOnly", SearchCriteria.Op.EQ, false);
        }
        if (StringUtils.isNotBlank(keyword)) {
            sc.setParameters("annotation", "%" + keyword + "%");
        }
        return listAnnotationsOrderedByCreatedDate(sc);
    }

    @Override
    public List<AnnotationVO> listByEntity(String entityType, String entityUuid, String userUuid, boolean isCallerAdmin,
                                           String annotationFilter, String callingUserUuid, String keyword) {
        SearchCriteria<AnnotationVO> sc = AnnotationSearchBuilder.create();
        sc.addAnd("entityType", SearchCriteria.Op.EQ, entityType);
        sc.addAnd("entityUuid", SearchCriteria.Op.EQ, entityUuid);
        if (StringUtils.isNotBlank(userUuid)) {
            sc.addAnd("userUuid", SearchCriteria.Op.EQ, userUuid);
        }
        if (StringUtils.isNoneBlank(callingUserUuid, annotationFilter) && annotationFilter.equalsIgnoreCase("self")) {
            sc.addAnd("userUuid", SearchCriteria.Op.EQ, callingUserUuid);
        }
        if (!isCallerAdmin) {
            sc.addAnd("adminsOnly", SearchCriteria.Op.EQ, false);
        }
        if (StringUtils.isNotBlank(keyword)) {
            sc.setParameters("annotation", "%" + keyword + "%");
        }
        return listAnnotationsOrderedByCreatedDate(sc);
    }

    @Override
    public List<AnnotationVO> listAllAnnotations(String userUuid, RoleType roleType, String annotationFilter, String keyword) {
        SearchCriteria<AnnotationVO> sc = AnnotationSearchBuilder.create();
        if (StringUtils.isNotBlank(keyword)) {
            sc.setParameters("annotation", "%" + keyword + "%");
        }
        if (StringUtils.isNotBlank(userUuid)) {
            sc.addAnd("userUuid", SearchCriteria.Op.EQ, userUuid);
        }
        if (roleType != RoleType.Admin) {
            sc.addAnd("adminsOnly", SearchCriteria.Op.EQ, false);
            List<EntityType> notAllowedTypes = EntityType.getNotAllowedTypesForNonAdmins(roleType);
            sc.setParameters("entityTypeNotIn", notAllowedTypes.toArray());
        }
        return listAnnotationsOrderedByCreatedDate(sc);
    }

    @Override
    public boolean hasAnnotations(String entityUuid, String entityType, boolean isCallerAdmin) {
        List<AnnotationVO> annotations = listByEntity(entityType, entityUuid, null,
                isCallerAdmin, "all", null, null);
        return CollectionUtils.isNotEmpty(annotations);
    }

    @Override
    public boolean removeByEntityType(String entityType, String entityUuid) {
        SearchCriteria<AnnotationVO> sc = AnnotationSearchBuilder.create();
        sc.addAnd("entityType", SearchCriteria.Op.EQ, entityType);
        sc.addAnd("entityUuid", SearchCriteria.Op.EQ, entityUuid);
        return remove(sc) > 0;
    }

    @Override
    public AnnotationVO findOneByEntityId(String entityUuid) {
        SearchCriteria<AnnotationVO> sc = AnnotationSearchBuilder.create();
        sc.addAnd("entityUuid", SearchCriteria.Op.EQ, entityUuid);
        return findOneBy(sc);
    }
}
