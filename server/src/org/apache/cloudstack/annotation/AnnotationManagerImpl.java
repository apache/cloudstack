package org.apache.cloudstack.annotation;

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

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 4.11
 */
public final class AnnotationManagerImpl extends ManagerBase implements AnnotationService, PluggableService {
    public static final Logger s_logger = Logger.getLogger(AnnotationManagerImpl.class);

    @Inject
    private AnnotationDao annotationDao;

    @Override
    public ListResponse<AnnotationResponse> searchForAnnotations(ListAnnotationsCmd cmd) {
        List<AnnotationVO> annotations =
                getAnnotationsForApiCmd(cmd);
        List<AnnotationResponse> annotationResponses =
                convertAnnotationsToResponses(annotations);
        return createAnnotationsResponseList(annotationResponses);
    }

    @Override
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

    @Override public AnnotationResponse removeAnnotation(RemoveAnnotationCmd removeAnnotationCmd) {
        String uuid = removeAnnotationCmd.getUuid();
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("marking annotation removed: " + uuid);
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
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("getting single annotation by uuid: " + uuid);
            }

            annotations.add(annotationDao.findByUuid(uuid));
        } else if( ! (cmd.getEntityType() == null || cmd.getEntityType().isEmpty()) ) {
            String type = cmd.getEntityType();
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("getting annotations for type: " + type);
            }
            if (cmd.getEntityUuid() != null) {
                String uuid = cmd.getEntityUuid().toString();
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("getting annotations for entity: " + uuid);
                }
                annotations = annotationDao.findByEntity(type,cmd.getEntityUuid().toString());
            } else {
                annotations = annotationDao.findByEntityType(type);
            }
        } else {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("getting all annotations");
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
