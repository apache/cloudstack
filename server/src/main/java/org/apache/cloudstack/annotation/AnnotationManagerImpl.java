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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.command.admin.annotation.AddAnnotationCmd;
import org.apache.cloudstack.api.command.admin.annotation.ListAnnotationsCmd;
import org.apache.cloudstack.api.command.admin.annotation.RemoveAnnotationCmd;
import org.apache.cloudstack.api.command.admin.annotation.UpdateAnnotationVisibilityCmd;
import org.apache.cloudstack.api.response.AnnotationResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * @since 4.11
 */
public final class AnnotationManagerImpl extends ManagerBase implements AnnotationService, PluggableService {
    public static final Logger LOGGER = Logger.getLogger(AnnotationManagerImpl.class);

    @Inject
    private AnnotationDao annotationDao;
    @Inject
    private UserDao userDao;
    @Inject
    private AccountDao accountDao;
    @Inject
    private RoleService roleService;
    @Inject
    private AccountService accountService;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private SnapshotDao snapshotDao;
    @Inject
    private VMSnapshotDao vmSnapshotDao;
    @Inject
    private InstanceGroupDao instanceGroupDao;
    @Inject
    private SSHKeyPairDao sshKeyPairDao;
    @Inject
    private NetworkDao networkDao;
    @Inject
    private VpcDao vpcDao;
    @Inject
    private IPAddressDao ipAddressDao;
    @Inject
    private Site2SiteCustomerGatewayDao customerGatewayDao;
    @Inject
    private VMTemplateDao templateDao;

    private static final List<RoleType> adminRoles = Arrays.asList(RoleType.Admin,
            RoleType.DomainAdmin, RoleType.ResourceAdmin);

    @Override
    public ListResponse<AnnotationResponse> searchForAnnotations(ListAnnotationsCmd cmd) {
        List<AnnotationVO> annotations = getAnnotationsForApiCmd(cmd);
        List<AnnotationResponse> annotationResponses = convertAnnotationsToResponses(annotations);
        return createAnnotationsResponseList(annotationResponses);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ANNOTATION_CREATE, eventDescription = "creating an annotation on an entity")
    public AnnotationResponse addAnnotation(AddAnnotationCmd addAnnotationCmd) {
        return addAnnotation(addAnnotationCmd.getAnnotation(), addAnnotationCmd.getEntityType(),
                addAnnotationCmd.getEntityUuid(), addAnnotationCmd.isAdminsOnly());
    }

    public AnnotationResponse addAnnotation(String text, EntityType type, String uuid, boolean adminsOnly) {
        UserVO userVO = getCallingUserFromContext();
        String userUuid = userVO.getUuid();

        AnnotationVO annotation = new AnnotationVO(text, type, uuid, adminsOnly);
        annotation.setUserUuid(userUuid);
        annotation = annotationDao.persist(annotation);
        return createAnnotationResponse(annotation);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ANNOTATION_REMOVE, eventDescription = "removing an annotation on an entity")
    public AnnotationResponse removeAnnotation(RemoveAnnotationCmd removeAnnotationCmd) {
        String uuid = removeAnnotationCmd.getUuid();
        AnnotationVO annotation = annotationDao.findByUuid(uuid);
        if (isCallingUserAllowedToRemoveAnnotation(annotation)) {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("marking annotation removed: " + uuid);
            }
            annotationDao.remove(annotation.getId());
        } else {
            throw new CloudRuntimeException("Only administrators or entity owner users can delete annotations, cannot remove annotation: " + uuid);
        }

        return createAnnotationResponse(annotation);
    }

    @Override
    public AnnotationResponse updateAnnotationVisibility(UpdateAnnotationVisibilityCmd cmd) {
        String uuid = cmd.getUuid();
        Boolean adminsOnly = cmd.getAdminsOnly();
        AnnotationVO annotation = annotationDao.findByUuid(uuid);
        if (annotation != null && isCallingUserAdmin()) {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("updating annotation visibility: " + uuid);
            }
            annotation.setAdminsOnly(adminsOnly);
            annotationDao.update(annotation.getId(), annotation);
        } else {
            throw new CloudRuntimeException("Cannot update visibility for annotation: " + uuid);
        }
        return createAnnotationResponse(annotation);
    }

    private boolean isCallingUserAllowedToRemoveAnnotation(AnnotationVO annotation) {
        if (annotation == null) {
            return false;
        }
        if (isCallingUserAdmin()) {
            return true;
        }
        UserVO callingUser = getCallingUserFromContext();
        String annotationOwnerUuid = annotation.getUserUuid();
        return annotationOwnerUuid != null && annotationOwnerUuid.equals(callingUser.getUuid());
    }

    private UserVO getCallingUserFromContext() {
        CallContext ctx = CallContext.current();
        long userId = ctx.getCallingUserId();
        UserVO userVO = userDao.findById(userId);
        if (userVO == null) {
            throw new CloudRuntimeException("Cannot find a user with ID " + userId);
        }
        return userVO;
    }

    private boolean isCallingUserAdmin() {
        UserVO userVO = getCallingUserFromContext();
        long accountId = userVO.getAccountId();
        AccountVO accountVO = accountDao.findById(accountId);
        if (accountVO == null) {
            throw new CloudRuntimeException("Cannot find account with ID + " + accountId);
        }
        Long roleId = accountVO.getRoleId();
        Role role = roleService.findRole(roleId);
        if (role == null) {
            throw new CloudRuntimeException("Cannot find role with ID " + roleId);
        }
        return adminRoles.contains(role.getRoleType());
    }

    private List<AnnotationVO> getAnnotationsForApiCmd(ListAnnotationsCmd cmd) {
        List<AnnotationVO> annotations;
        String userUuid = cmd.getUserUuid();
        String entityUuid = cmd.getEntityUuid();
        String entityType = cmd.getEntityType();
        String annotationFilter = isNotBlank(cmd.getAnnotationFilter()) ? cmd.getAnnotationFilter() : "all";
        boolean isCallerAdmin = isCallingUserAdmin();
        if ((isBlank(entityUuid) || isBlank(entityType)) && !isCallerAdmin && annotationFilter.equalsIgnoreCase("all")) {
            throw new CloudRuntimeException("Only admins can filter all the annotations");
        }
        UserVO callingUser = getCallingUserFromContext();
        String callingUserUuid = callingUser.getUuid();
        String keyword = cmd.getKeyword();

        if (cmd.getUuid() != null) {
            annotations = new ArrayList<>();
            String uuid = cmd.getUuid();
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("getting single annotation by uuid: " + uuid);
            }

            AnnotationVO annotationVO = annotationDao.findByUuid(uuid);
            if (annotationVO != null && annotationVO.getUserUuid().equals(userUuid) &&
                    (annotationFilter.equalsIgnoreCase("all") ||
                    (annotationFilter.equalsIgnoreCase("self") && annotationVO.getUserUuid().equals(callingUserUuid))) &&
                annotationVO.isAdminsOnly() == isCallerAdmin) {
                annotations.add(annotationVO);
            }
        } else if (isNotBlank(entityType)) {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("getting annotations for type: " + entityType);
            }
            if (isNotBlank(entityUuid)) {
                if (!isCallerAdmin) {
                    ensureEntityIsOwnedByTheUser(entityType, entityUuid, callingUser);
                }
                if(LOGGER.isDebugEnabled()) {
                    LOGGER.debug("getting annotations for entity: " + entityUuid);
                }
                annotations = annotationDao.listByEntity(entityType, entityUuid, userUuid, isCallerAdmin,
                        annotationFilter, callingUserUuid, keyword);
            } else {
                annotations = annotationDao.listByEntityType(entityType, userUuid, isCallerAdmin,
                        annotationFilter, callingUserUuid, keyword);
            }
        } else {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("getting all annotations");
            }
            if ("self".equalsIgnoreCase(annotationFilter) && isBlank(userUuid)) {
                userUuid = callingUserUuid;
            }
            annotations = annotationDao.listAllAnnotations(userUuid, isCallerAdmin, annotationFilter, keyword);
        }
        return annotations;
    }

    private void ensureEntityIsOwnedByTheUser(String entityType, String entityUuid, UserVO callingUser) {
        try {
            EntityType type = EntityType.valueOf(entityType);
            ControlledEntity entity = getEntityFromUuidAndType(entityUuid, type);
            accountService.checkAccess(callingUser, entity);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Could not parse entity type " + entityType, e);
        }
    }

    private ControlledEntity getEntityFromUuidAndType(String entityUuid, EntityType type) {
        switch (type) {
            case VM:
                return vmInstanceDao.findByUuid(entityUuid);
            case VOLUME:
                return volumeDao.findByUuid(entityUuid);
            case SNAPSHOT:
                return snapshotDao.findByUuid(entityUuid);
            case VM_SNAPSHOT:
                return vmSnapshotDao.findByUuid(entityUuid);
            case INSTANCE_GROUP:
                return instanceGroupDao.findByUuid(entityUuid);
            case SSH_KEYPAIR:
                return sshKeyPairDao.findByUuid(entityUuid);
            case NETWORK:
                return networkDao.findByUuid(entityUuid);
            case VPC:
                return vpcDao.findByUuid(entityUuid);
            case PUBLIC_IP_ADDRESS:
                return ipAddressDao.findByUuid(entityUuid);
            case VPN_CUSTOMER_GATEWAY:
                return customerGatewayDao.findByUuid(entityUuid);
            case TEMPLATE:
            case ISO:
                return templateDao.findByUuid(entityUuid);
            default:
                throw new CloudRuntimeException("Invalid entity type " + type);
        }
    }

    private List<AnnotationResponse> convertAnnotationsToResponses(List<AnnotationVO> annotations) {
        List<AnnotationResponse> annotationResponses = new ArrayList<>();
        for (AnnotationVO annotation : annotations) {
            annotationResponses.add(createAnnotationResponse(annotation));
        }
        return annotationResponses;
    }

    private ListResponse<AnnotationResponse> createAnnotationsResponseList(List<AnnotationResponse> annotationResponses) {
        ListResponse<AnnotationResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(annotationResponses);
        return listResponse;
    }

    public AnnotationResponse createAnnotationResponse(AnnotationVO annotation) {
        AnnotationResponse response = new AnnotationResponse();
        response.setUuid(annotation.getUuid());
        response.setEntityType(annotation.getEntityType());
        response.setEntityUuid(annotation.getEntityUuid());
        response.setAnnotation(annotation.getAnnotation());
        response.setUserUuid(annotation.getUserUuid());
        response.setCreated(annotation.getCreated());
        response.setRemoved(annotation.getRemoved());
        UserVO user = userDao.findByUuid(annotation.getUserUuid());
        if (user != null && StringUtils.isNotBlank(user.getUsername())) {
            response.setUsername(user.getUsername());
        }
        response.setAdminsOnly(annotation.isAdminsOnly());
        response.setObjectName("annotation");

        return response;
    }

    @Override public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(AddAnnotationCmd.class);
        cmdList.add(ListAnnotationsCmd.class);
        cmdList.add(RemoveAnnotationCmd.class);
        cmdList.add(UpdateAnnotationVisibilityCmd.class);
        return cmdList;
    }
}
