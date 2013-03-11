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
package com.cloud.api;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.InfrastructureEntity;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.Validate;
import org.apache.cloudstack.api.command.user.event.ArchiveEventsCmd;
import org.apache.cloudstack.api.command.user.event.DeleteEventsCmd;
import org.apache.cloudstack.api.command.user.event.ListEventsCmd;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.async.AsyncCommandQueued;
import com.cloud.async.AsyncJobManager;
import com.cloud.dao.EntityManager;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.utils.DateUtil;
import com.cloud.utils.ReflectUtil;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CSExceptionErrorCode;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class ApiDispatcher {
    private static final Logger s_logger = Logger.getLogger(ApiDispatcher.class.getName());

    Long _createSnapshotQueueSizeLimit;
    @Inject AsyncJobManager _asyncMgr = null;
    @Inject AccountManager _accountMgr = null;
    @Inject EntityManager _entityMgr = null;

    private static ApiDispatcher s_instance;

    public static ApiDispatcher getInstance() {
        return s_instance;
    }

    public ApiDispatcher() {
    }

    @PostConstruct
    void init() {
    	s_instance = this;
    }

    public void setCreateSnapshotQueueSizeLimit(Long snapshotLimit) {
        _createSnapshotQueueSizeLimit = snapshotLimit;
    }

    public void dispatchCreateCmd(BaseAsyncCreateCmd cmd, Map<String, String> params) throws Exception {
        processParameters(cmd, params);

            UserContext ctx = UserContext.current();
            ctx.setAccountId(cmd.getEntityOwnerId());
            cmd.create();

    }

    private void doAccessChecks(BaseCmd cmd, Map<Object, AccessType> entitiesToAccess) {
        Account caller = UserContext.current().getCaller();
        Account owner = _accountMgr.getActiveAccountById(cmd.getEntityOwnerId());

        if(cmd instanceof BaseAsyncCreateCmd) {
            //check that caller can access the owner account.
            _accountMgr.checkAccess(caller, null, true, owner);
        }

        if(!entitiesToAccess.isEmpty()){
            //check that caller can access the owner account.
            _accountMgr.checkAccess(caller, null, true, owner);
            for (Object entity : entitiesToAccess.keySet()) {
                if (entity instanceof ControlledEntity) {
                    _accountMgr.checkAccess(caller, entitiesToAccess.get(entity), true, (ControlledEntity) entity);
                }
                else if (entity instanceof InfrastructureEntity) {
                    //FIXME: Move this code in adapter, remove code from Account manager
                }
            }
        }
    }

    public void dispatch(BaseCmd cmd, Map<String, String> params) throws Exception {
            processParameters(cmd, params);
            UserContext ctx = UserContext.current();
            ctx.setAccountId(cmd.getEntityOwnerId());
            
            BaseCmd realCmdObj = ComponentContext.getTargetObject(cmd);
            if (realCmdObj instanceof BaseAsyncCmd) {

                BaseAsyncCmd asyncCmd = (BaseAsyncCmd) cmd;
                String startEventId = params.get("ctxStartEventId");
                ctx.setStartEventId(Long.valueOf(startEventId));

                // Synchronise job on the object if needed
                if (asyncCmd.getJob() != null && asyncCmd.getSyncObjId() != null && asyncCmd.getSyncObjType() != null) {
                    Long queueSizeLimit = null;
                    if (asyncCmd.getSyncObjType() != null && asyncCmd.getSyncObjType().equalsIgnoreCase(BaseAsyncCmd.snapshotHostSyncObject)) {
                        queueSizeLimit = _createSnapshotQueueSizeLimit;
                    } else {
                        queueSizeLimit = 1L;
                    }

                    if (queueSizeLimit != null) {
                    _asyncMgr.syncAsyncJobExecution(asyncCmd.getJob(), asyncCmd.getSyncObjType(), asyncCmd.getSyncObjId().longValue(), queueSizeLimit);
                    } else {
                        s_logger.trace("The queue size is unlimited, skipping the synchronizing");
                    }
                }
            }

            cmd.execute();

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void processParameters(BaseCmd cmd, Map<String, String> params) {
        Map<Object, AccessType> entitiesToAccess = new HashMap<Object, AccessType>();
        Map<String, Object> unpackedParams = cmd.unpackParams(params);

        cmd = ComponentContext.getTargetObject(cmd);

        if (cmd instanceof BaseListCmd) {
            Object pageSizeObj = unpackedParams.get(ApiConstants.PAGE_SIZE);
            Long pageSize = null;
            if (pageSizeObj != null) {
                pageSize = Long.valueOf((String) pageSizeObj);
            }

            if ((unpackedParams.get(ApiConstants.PAGE) == null) && (pageSize != null && pageSize != BaseListCmd.PAGESIZE_UNLIMITED)) {
                ServerApiException ex = new ServerApiException(ApiErrorCode.PARAM_ERROR, "\"page\" parameter is required when \"pagesize\" is specified");
                ex.setCSErrorCode(CSExceptionErrorCode.getCSErrCode(ex.getClass().getName()));
                throw ex;
            } else if (pageSize == null && (unpackedParams.get(ApiConstants.PAGE) != null)) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "\"pagesize\" parameter is required when \"page\" is specified");
            }
        }

        List<Field> fields = ReflectUtil.getAllFieldsForClass(cmd.getClass(), BaseCmd.class);

        for (Field field : fields) {
            Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
            if ((parameterAnnotation == null) || !parameterAnnotation.expose()) {
                continue;
            }

            //TODO: Annotate @Validate on API Cmd classes, FIXME how to process Validate
            Validate validateAnnotation = field.getAnnotation(Validate.class);
            Object paramObj = unpackedParams.get(parameterAnnotation.name());
            if (paramObj == null) {
                if (parameterAnnotation.required()) {
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to execute API command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8) + " due to missing parameter "
                            + parameterAnnotation.name());
                }
                continue;
            }

            // marshall the parameter into the correct type and set the field value
            try {
                setFieldValue(field, cmd, paramObj, parameterAnnotation);
            } catch (IllegalArgumentException argEx) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to execute API command " + cmd.getCommandName() + " due to invalid value " + paramObj + " for parameter " + parameterAnnotation.name());
                }
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to execute API command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8) + " due to invalid value " + paramObj
                        + " for parameter "
                        + parameterAnnotation.name());
            } catch (ParseException parseEx) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Invalid date parameter " + paramObj + " passed to command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8));
                }
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to parse date " + paramObj + " for command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8)
                        + ", please pass dates in the format mentioned in the api documentation");
            } catch (InvalidParameterValueException invEx) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to execute API command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8) + " due to invalid value. " + invEx.getMessage());
            } catch (CloudRuntimeException cloudEx) {
            	s_logger.error("CloudRuntimeException", cloudEx);
                // FIXME: Better error message? This only happens if the API command is not executable, which typically
                //means
                // there was
                // and IllegalAccessException setting one of the parameters.
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Internal error executing API command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8));
            }

            //check access on the resource this field points to
            try {
                ACL checkAccess = field.getAnnotation(ACL.class);
                CommandType fieldType = parameterAnnotation.type();

                if (checkAccess != null) {
                    // Verify that caller can perform actions in behalf of vm owner
                    //acumulate all Controlled Entities together.

                    //parse the array of resource types and in case of map check access on key or value or both as specified in @acl
                    //implement external dao for classes that need findByName
                    //for maps, specify access to be checkd on key or value.

                    // find the controlled entity DBid by uuid
                    if (parameterAnnotation.entityType() != null) {
                        Class<?>[] entityList = parameterAnnotation.entityType()[0].getAnnotation(EntityReference.class).value();

                        for (Class entity : entityList) {
                            // Check if the parameter type is a single
                            // Id or list of id's/name's
                            switch (fieldType) {
                            case LIST:
                                CommandType listType = parameterAnnotation.collectionType();
                                switch (listType) {
                                case LONG:
                                case UUID:
                                    List<Long> listParam = (List<Long>) field.get(cmd);
                                    for (Long entityId : listParam) {
                                        Object entityObj = s_instance._entityMgr.findById(entity, entityId);
                                        entitiesToAccess.put(entityObj, checkAccess.accessType());
                                    }
                                    break;
                                    /*
                                     * case STRING: List<String> listParam =
                                     * new ArrayList<String>(); listParam =
                                     * (List)field.get(cmd); for(String
                                     * entityName: listParam){
                                     * ControlledEntity entityObj =
                                     * (ControlledEntity
                                     * )daoClassInstance(entityId);
                                     * entitiesToAccess.add(entityObj); }
                                     * break;
                                     */
                                default:
                                    break;
                                }
                                break;
                            case LONG:
                            case UUID:
                                Object entityObj = s_instance._entityMgr.findById(entity, (Long) field.get(cmd));
                                entitiesToAccess.put(entityObj, checkAccess.accessType());
                                break;
                            default:
                                break;
                            }

                            if (ControlledEntity.class.isAssignableFrom(entity)) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("ControlledEntity name is:" + entity.getName());
                                }
                            }

                            if (InfrastructureEntity.class.isAssignableFrom(entity)) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("InfrastructureEntity name is:" + entity.getName());
                                }
                            }
                        }

                    }

                }

            } catch (IllegalArgumentException e) {
                s_logger.error("Error initializing command " + cmd.getCommandName() + ", field " + field.getName() + " is not accessible.");
                throw new CloudRuntimeException("Internal error initializing parameters for command " + cmd.getCommandName() + " [field " + field.getName() + " is not accessible]");
            } catch (IllegalAccessException e) {
                s_logger.error("Error initializing command " + cmd.getCommandName() + ", field " + field.getName() + " is not accessible.");
                throw new CloudRuntimeException("Internal error initializing parameters for command " + cmd.getCommandName() + " [field " + field.getName() + " is not accessible]");
            }

        }

        //check access on the entities.
        getInstance().doAccessChecks(cmd, entitiesToAccess);

    }

    private static Long translateUuidToInternalId(String uuid, Parameter annotation)
    {
        if (uuid.equals("-1")) {
            // FIXME: This is to handle a lot of hardcoded special cases where -1 is sent
            // APITODO: Find and get rid of all hardcoded params in API Cmds and service layer
            return -1L;
        }
        Long internalId = null;
        // If annotation's empty, the cmd existed before 3.x try conversion to long
        boolean isPre3x = annotation.since().isEmpty();
        // Match against Java's UUID regex to check if input is uuid string
        boolean isUuid = uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        // Enforce that it's uuid for newly added apis from version 3.x
        if (!isPre3x && !isUuid)
            return null;
        // Allow both uuid and internal id for pre3x apis
        if (isPre3x && !isUuid) {
            try {
                internalId = Long.parseLong(uuid);
            } catch(NumberFormatException e) {
                internalId = null;
            }
            if (internalId != null)
                return internalId;
        }
        // There may be multiple entities defined on the @EntityReference of a Response.class
        // UUID CommandType would expect only one entityType, so use the first entityType
        Class<?>[] entities = annotation.entityType()[0].getAnnotation(EntityReference.class).value();
        // Go through each entity which is an interface to a VO class and get a VO object
        // Try to getId() for the object using reflection, break on first non-null value
        for (Class<?> entity: entities) {
            // For backward compatibility, we search within removed entities and let service layer deal
            // with removed ones, return empty response or error
            Object objVO = s_instance._entityMgr.findByUuidIncludingRemoved(entity, uuid);
            if (objVO == null) {
                continue;
            }
            // Invoke the getId method, get the internal long ID
            // If that fails hide exceptions as the uuid may not exist
            try {
                internalId = ((InternalIdentity)objVO).getId();
            } catch (IllegalArgumentException e) {
            } catch (NullPointerException e) {
            }
            // Return on first non-null Id for the uuid entity
            if (internalId != null)
                break;
        }
        if (internalId == null) {
            if (s_logger.isDebugEnabled())
                s_logger.debug("Object entity uuid = " + uuid + " does not exist in the database.");
            throw new InvalidParameterValueException("Invalid parameter " + annotation.name() + " value=" + uuid
                + " due to incorrect long value format, or entity does not exist or due to incorrect parameter annotation for the field in api cmd class.");
        }
        return internalId;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void setFieldValue(Field field, BaseCmd cmdObj, Object paramObj, Parameter annotation) throws IllegalArgumentException, ParseException {
        try {
            field.setAccessible(true);
            CommandType fieldType = annotation.type();
            switch (fieldType) {
            case BOOLEAN:
                field.set(cmdObj, Boolean.valueOf(paramObj.toString()));
                break;
            case DATE:
                // This piece of code is for maintaining backward compatibility
                // and support both the date formats(Bug 9724)
                // Do the date messaging for ListEventsCmd only
                if (cmdObj instanceof ListEventsCmd || cmdObj instanceof DeleteEventsCmd || cmdObj instanceof ArchiveEventsCmd) {
                    boolean isObjInNewDateFormat = isObjInNewDateFormat(paramObj.toString());
                    if (isObjInNewDateFormat) {
                        DateFormat newFormat = BaseCmd.NEW_INPUT_FORMAT;
                        synchronized (newFormat) {
                            field.set(cmdObj, newFormat.parse(paramObj.toString()));
                        }
                    } else {
                        DateFormat format = BaseCmd.INPUT_FORMAT;
                        synchronized (format) {
                            Date date = format.parse(paramObj.toString());
                            if (field.getName().equals("startDate")) {
                                date = messageDate(date, 0, 0, 0);
                            } else if (field.getName().equals("endDate")) {
                                date = messageDate(date, 23, 59, 59);
                            } else if (field.getName().equals("olderThan")) {
                                date = messageDate(date, 0, 0, 0);
                            }
                            field.set(cmdObj, date);
                        }
                    }
                } else {
                    DateFormat format = BaseCmd.INPUT_FORMAT;
                    format.setLenient(false);
                    synchronized (format) {
                        field.set(cmdObj, format.parse(paramObj.toString()));
                    }
                }
                break;
            case FLOAT:
                // Assuming that the parameters have been checked for required before now,
                // we ignore blank or null values and defer to the command to set a default
                // value for optional parameters ...
                if (paramObj != null && isNotBlank(paramObj.toString())) {
                    field.set(cmdObj, Float.valueOf(paramObj.toString()));
                }
                break;
            case INTEGER:
                // Assuming that the parameters have been checked for required before now,
                // we ignore blank or null values and defer to the command to set a default
                // value for optional parameters ...
                if (paramObj != null && isNotBlank(paramObj.toString())) {
                    field.set(cmdObj, Integer.valueOf(paramObj.toString()));
                }
                break;
            case LIST:
                List listParam = new ArrayList();
                StringTokenizer st = new StringTokenizer(paramObj.toString(), ",");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    CommandType listType = annotation.collectionType();
                    switch (listType) {
                    case INTEGER:
                        listParam.add(Integer.valueOf(token));
                        break;
                    case UUID:
                        if (token.isEmpty())
                            break;
                        Long internalId = translateUuidToInternalId(token, annotation);
                        listParam.add(internalId);
                        break;
                    case LONG: {
                        listParam.add(Long.valueOf(token));
                    }
                    break;
                    case SHORT:
                        listParam.add(Short.valueOf(token));
                    case STRING:
                        listParam.add(token);
                        break;
                    }
                }
                field.set(cmdObj, listParam);
                break;
            case UUID:
                if (paramObj.toString().isEmpty())
                    break;
                Long internalId = translateUuidToInternalId(paramObj.toString(), annotation);
                field.set(cmdObj, internalId);
                break;
            case LONG:
                field.set(cmdObj, Long.valueOf(paramObj.toString()));
                break;
            case SHORT:
                field.set(cmdObj, Short.valueOf(paramObj.toString()));
                break;
            case STRING:
                if ((paramObj != null) && paramObj.toString().length() > annotation.length()) {
                    s_logger.error("Value greater than max allowed length " + annotation.length() + " for param: " + field.getName());
                    throw new InvalidParameterValueException("Value greater than max allowed length " + annotation.length() + " for param: " + field.getName());
                }
                field.set(cmdObj, paramObj.toString());
                break;
            case TZDATE:
                field.set(cmdObj, DateUtil.parseTZDateString(paramObj.toString()));
                break;
            case MAP:
            default:
                field.set(cmdObj, paramObj);
                break;
            }
        } catch (IllegalAccessException ex) {
            s_logger.error("Error initializing command " + cmdObj.getCommandName() + ", field " + field.getName() + " is not accessible.");
            throw new CloudRuntimeException("Internal error initializing parameters for command " + cmdObj.getCommandName() + " [field " + field.getName() + " is not accessible]");
        }
    }

    private static boolean isObjInNewDateFormat(String string) {
        Matcher matcher = BaseCmd.newInputDateFormat.matcher(string);
        return matcher.matches();
    }

    private static Date messageDate(Date date, int hourOfDay, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        return cal.getTime();
    }

    public static void plugService(Field field, BaseCmd cmd) {

        Class<?> fc = field.getType();
        Object instance = null;

        if (instance == null) {
            throw new CloudRuntimeException("Unable to plug service " + fc.getSimpleName() + " in command " + cmd.getClass().getSimpleName());
        }

        try {
            field.setAccessible(true);
            field.set(cmd, instance);
        } catch (IllegalArgumentException e) {
            s_logger.error("IllegalArgumentException at plugService for command " + cmd.getCommandName() + ", field " + field.getName());
            throw new CloudRuntimeException("Internal error at plugService for command " + cmd.getCommandName() + " [Illegal argumet at field " + field.getName() + "]");
        } catch (IllegalAccessException e) {
            s_logger.error("Error at plugService for command " + cmd.getCommandName() + ", field " + field.getName() + " is not accessible.");
            throw new CloudRuntimeException("Internal error at plugService for command " + cmd.getCommandName() + " [field " + field.getName() + " is not accessible]");
        }
    }
}
