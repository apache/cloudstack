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
import java.util.List;

import javax.inject.Inject;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.command.admin.annotation.AddAnnotationCmd;
import org.apache.cloudstack.api.command.admin.annotation.ListAnnotationsCmd;
import org.apache.cloudstack.api.command.admin.annotation.RemoveAnnotationCmd;
import org.apache.cloudstack.api.response.AnnotationResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

/**
 * @since 4.11
 */
public final class AnnotationManagerImpl extends ManagerBase implements AnnotationService, PluggableService {
    public static final Logger LOGGER = Logger.getLogger(AnnotationManagerImpl.class);

    @Inject
    private AnnotationDao annotationDao;

    @Override
    public ListResponse<AnnotationResponse> searchForAnnotations(ListAnnotationsCmd cmd) {
        List<AnnotationVO> annotations = getAnnotationsForApiCmd(cmd);
        List<AnnotationResponse> annotationResponses = convertAnnotationsToResponses(annotations);
        return createAnnotationsResponseList(annotationResponses);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ANNOTATION_CREATE, eventDescription = "creating an annotation on an entity")
    public AnnotationResponse addAnnotation(AddAnnotationCmd addAnnotationCmd) {
        return addAnnotation(addAnnotationCmd.getAnnotation(), addAnnotationCmd.getEntityType(), addAnnotationCmd.getEntityUuid());
    }

    public AnnotationResponse addAnnotation(String text, EntityType type, String uuid) {
        CallContext ctx = CallContext.current();
        String userUuid = ctx.getCallingUserUuid();

        AnnotationVO annotation = new AnnotationVO(text, type, uuid);
        annotation.setUserUuid(userUuid);
        annotation = annotationDao.persist(annotation);
        return createAnnotationResponse(annotation);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ANNOTATION_REMOVE, eventDescription = "removing an annotation on an entity")
    public AnnotationResponse removeAnnotation(RemoveAnnotationCmd removeAnnotationCmd) {
        String uuid = removeAnnotationCmd.getUuid();
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("marking annotation removed: " + uuid);
        }
        AnnotationVO annotation = annotationDao.findByUuid(uuid);
        annotationDao.remove(annotation.getId());
        return createAnnotationResponse(annotation);
    }

    private List<AnnotationVO> getAnnotationsForApiCmd(ListAnnotationsCmd cmd) {
        List<AnnotationVO> annotations;
        if(cmd.getUuid() != null) {
            annotations = new ArrayList<>();
            String uuid = cmd.getUuid().toString();
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("getting single annotation by uuid: " + uuid);
            }

            annotations.add(annotationDao.findByUuid(uuid));
        } else if( ! (cmd.getEntityType() == null || cmd.getEntityType().isEmpty()) ) {
            String type = cmd.getEntityType();
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("getting annotations for type: " + type);
            }
            if (cmd.getEntityUuid() != null) {
                String uuid = cmd.getEntityUuid().toString();
                if(LOGGER.isDebugEnabled()) {
                    LOGGER.debug("getting annotations for entity: " + uuid);
                }
                annotations = annotationDao.findByEntity(type,cmd.getEntityUuid().toString());
            } else {
                annotations = annotationDao.findByEntityType(type);
            }
        } else {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("getting all annotations");
            }
            annotations = annotationDao.listAll();
        }
        return annotations;
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

    public static AnnotationResponse createAnnotationResponse(AnnotationVO annotation) {
        AnnotationResponse response = new AnnotationResponse();
        response.setUuid(annotation.getUuid());
        response.setEntityType(annotation.getEntityType());
        response.setEntityUuid(annotation.getEntityUuid());
        response.setAnnotation(annotation.getAnnotation());
        response.setUserUuid(annotation.getUserUuid());
        response.setCreated(annotation.getCreated());
        response.setRemoved(annotation.getRemoved());
        response.setObjectName("annotation");

        return response;
    }

    @Override public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(AddAnnotationCmd.class);
        cmdList.add(ListAnnotationsCmd.class);
        cmdList.add(RemoveAnnotationCmd.class);
        return cmdList;
    }
}
