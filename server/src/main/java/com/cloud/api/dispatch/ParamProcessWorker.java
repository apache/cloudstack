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

package com.cloud.api.dispatch;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.InfrastructureEntity;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.resource.ArchiveAlertsCmd;
import org.apache.cloudstack.api.command.admin.resource.DeleteAlertsCmd;
import org.apache.cloudstack.api.command.admin.usage.ListUsageRecordsCmd;
import org.apache.cloudstack.api.command.user.event.ArchiveEventsCmd;
import org.apache.cloudstack.api.command.user.event.DeleteEventsCmd;
import org.apache.cloudstack.api.command.user.event.ListEventsCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Strings;

public class ParamProcessWorker implements DispatchWorker {

    private static final Logger s_logger = Logger.getLogger(ParamProcessWorker.class.getName());
    public final DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
    public final DateFormat newInputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Inject
    protected AccountManager _accountMgr;

    @Inject
    protected EntityManager _entityMgr;

    List<SecurityChecker> _secChecker;

    public List<SecurityChecker> getSecChecker() {
        return _secChecker;
    }

    @Inject
    public void setSecChecker(List<SecurityChecker> secChecker) {
        _secChecker = secChecker;
    }

    @Override
    public void handle(final DispatchTask task) {
        processParameters(task.getCmd(), task.getParams());
    }

    private void validateNonEmptyString(final Object param, final String argName) {
        if (param == null || Strings.isNullOrEmpty(param.toString())) {
            throw new InvalidParameterValueException(String.format("Empty or null value provided for API arg: %s", argName));
        }
    }

    private void validateNaturalNumber(final Object param, final String argName) {
        Long value = null;
        if (param != null && param instanceof Long) {
            value = (Long) param;
        } else if (param != null) {
            value = Long.valueOf(param.toString());
        }
        if (value == null || value < 1L) {
            throw new InvalidParameterValueException(String.format("Invalid value provided for API arg: %s", argName));
        }
    }

    private void validateField(final Object paramObj, final Parameter annotation) throws ServerApiException {
        if (annotation == null) {
            return;
        }
        final String argName = annotation.name();
        for (final ApiArgValidator validator : annotation.validations()) {
            if (validator == null) {
                continue;
            }
            switch (validator) {
                case NotNullOrEmpty:
                    switch (annotation.type()) {
                        case UUID:
                        case STRING:
                            validateNonEmptyString(paramObj, argName);
                            break;
                    }
                    break;
                case PositiveNumber:
                    switch (annotation.type()) {
                        case SHORT:
                        case INTEGER:
                        case LONG:
                            validateNaturalNumber(paramObj, argName);
                            break;
                    }
                    break;
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void processParameters(final BaseCmd cmd, final Map params) {
        final Map<Object, AccessType> entitiesToAccess = new HashMap<Object, AccessType>();

        final List<Field> cmdFields = cmd.getParamFields();

        for (final Field field : cmdFields) {
            final Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
            final Object paramObj = params.get(parameterAnnotation.name());
            if (paramObj == null) {
                if (parameterAnnotation.required()) {
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to execute API command " +
                            cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8) +
                            " due to missing parameter " + parameterAnnotation.name());
                }
                continue;
            }

            // marshall the parameter into the correct type and set the field value
            try {
                validateField(paramObj, parameterAnnotation);
                setFieldValue(field, cmd, paramObj, parameterAnnotation);
            } catch (final IllegalArgumentException argEx) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to execute API command " + cmd.getCommandName() + " due to invalid value " + paramObj + " for parameter " +
                            parameterAnnotation.name());
                }
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to execute API command " +
                        cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8) + " due to invalid value " + paramObj + " for parameter " +
                        parameterAnnotation.name());
            } catch (final ParseException parseEx) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Invalid date parameter " + paramObj + " passed to command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8));
                }
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to parse date " + paramObj + " for command " +
                        cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8) + ", please pass dates in the format mentioned in the api documentation");
            } catch (final InvalidParameterValueException invEx) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unable to execute API command " +
                        cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8) + " due to invalid value. " + invEx.getMessage());
            } catch (final CloudRuntimeException cloudEx) {
                s_logger.error("CloudRuntimeException", cloudEx);
                // FIXME: Better error message? This only happens if the API command is not executable, which typically
                //means
                // there was
                // and IllegalAccessException setting one of the parameters.
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Internal error executing API command " +
                        cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8));
            }

            //check access on the resource this field points to
            try {
                final ACL checkAccess = field.getAnnotation(ACL.class);
                final CommandType fieldType = parameterAnnotation.type();

                if (checkAccess != null) {
                    // Verify that caller can perform actions in behalf of vm
                    // owner acumulate all Controlled Entities together.
                    // parse the array of resource types and in case of map
                    // check access on key or value or both as specified in @acl
                    // implement external dao for classes that need findByName
                    // for maps, specify access to be checkd on key or value.
                    // Find the controlled entity DBid by uuid

                    if (parameterAnnotation.entityType() != null && parameterAnnotation.entityType().length > 0
                            && parameterAnnotation.entityType()[0].getAnnotation(EntityReference.class) != null) {
                        final Class<?>[] entityList = parameterAnnotation.entityType()[0].getAnnotation(EntityReference.class).value();

                        // Check if the parameter type is a single
                        // Id or list of id's/name's
                        switch (fieldType) {
                        case LIST:
                            final CommandType listType = parameterAnnotation.collectionType();
                            switch (listType) {
                            case LONG:
                            case UUID:
                                final List<Long> listParam = (List<Long>) field.get(cmd);
                                for (final Long entityId : listParam) {
                                    for (final Class entity : entityList) {
                                        final Object entityObj = _entityMgr.findById(entity, entityId);
                                        if(entityObj != null){
                                            entitiesToAccess.put(entityObj, checkAccess.accessType());
                                            break;
                                        }
                                    }
                                }
                                break;
                                /*
                                 * case STRING: List<String> listParam = new
                                 * ArrayList<String>(); listParam =
                                 * (List)field.get(cmd); for(String entityName:
                                 * listParam){ ControlledEntity entityObj =
                                 * (ControlledEntity )daoClassInstance(entityId);
                                 * entitiesToAccess.add(entityObj); } break;
                                 */
                            default:
                                break;
                            }
                            break;
                        case LONG:
                        case UUID:
                            for (final Class entity : entityList) {
                                final Object entityObj = _entityMgr.findById(entity, (Long) field.get(cmd));
                                if(entityObj != null){
                                    entitiesToAccess.put(entityObj, checkAccess.accessType());
                                    break;
                                }
                            }
                            break;
                        default:
                            break;
                        }
                    }
                }

            } catch (final IllegalArgumentException e) {
                throw new CloudRuntimeException("Internal error initializing parameters for command " + cmd.getCommandName() + " [field " + field.getName() +
                        " is not accessible]", e);
            } catch (final IllegalAccessException e) {
                throw new CloudRuntimeException("Internal error initializing parameters for command " + cmd.getCommandName() + " [field " + field.getName() +
                        " is not accessible]", e);
            }

        }

        doAccessChecks(cmd, entitiesToAccess);
    }

    private void doAccessChecks(BaseCmd cmd, Map<Object, AccessType> entitiesToAccess) {
        Account caller = CallContext.current().getCallingAccount();
        List<Long> entityOwners = cmd.getEntityOwnerIds();
        Account[] owners = null;
        if (entityOwners != null) {
            owners = entityOwners.stream().map(id -> _accountMgr.getAccount(id)).toArray(Account[]::new);
        } else {
            owners = new Account[]{_accountMgr.getAccount(cmd.getEntityOwnerId())};
        }

        if (cmd instanceof BaseAsyncCreateCmd) {
            // check that caller can access the owner account.
            _accountMgr.checkAccess(caller, null, false, owners);
        }

        if (!entitiesToAccess.isEmpty()) {
            // check that caller can access the owner account.
            _accountMgr.checkAccess(caller, null, false, owners);
            for (Map.Entry<Object,AccessType>entry : entitiesToAccess.entrySet()) {
                Object entity = entry.getKey();
                if (entity instanceof ControlledEntity) {
                    _accountMgr.checkAccess(caller, entry.getValue(), true, (ControlledEntity) entity);
                } else if (entity instanceof InfrastructureEntity) {
                    // FIXME: Move this code in adapter, remove code from
                    // Account manager
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setFieldValue(final Field field, final BaseCmd cmdObj, final Object paramObj, final Parameter annotation) throws IllegalArgumentException, ParseException {
        try {
            field.setAccessible(true);
            final CommandType fieldType = annotation.type();
            switch (fieldType) {
            case BOOLEAN:
                field.set(cmdObj, Boolean.valueOf(paramObj.toString()));
                break;
            case DATE:
                // This piece of code is for maintaining backward compatibility
                // and support both the date formats(Bug 9724)
                if (cmdObj instanceof ListEventsCmd || cmdObj instanceof DeleteEventsCmd || cmdObj instanceof ArchiveEventsCmd ||
                        cmdObj instanceof ArchiveAlertsCmd || cmdObj instanceof DeleteAlertsCmd || cmdObj instanceof ListUsageRecordsCmd) {
                    final boolean isObjInNewDateFormat = isObjInNewDateFormat(paramObj.toString());
                    if (isObjInNewDateFormat) {
                        final DateFormat newFormat = newInputFormat;
                        synchronized (newFormat) {
                            field.set(cmdObj, newFormat.parse(paramObj.toString()));
                        }
                    } else {
                        final DateFormat format = inputFormat;
                        synchronized (format) {
                            Date date = format.parse(paramObj.toString());
                            if (field.getName().equals("startDate")) {
                                date = messageDate(date, 0, 0, 0);
                            } else if (field.getName().equals("endDate")) {
                                date = messageDate(date, 23, 59, 59);
                            }
                            field.set(cmdObj, date);
                        }
                    }
                } else {
                    final DateFormat format = inputFormat;
                    synchronized (format) {
                        format.setLenient(false);
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
            case DOUBLE:
                // Assuming that the parameters have been checked for required before now,
                // we ignore blank or null values and defer to the command to set a default
                // value for optional parameters ...
                if (paramObj != null && isNotBlank(paramObj.toString())) {
                    field.set(cmdObj, Double.valueOf(paramObj.toString()));
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
                final List listParam = new ArrayList();
                final StringTokenizer st = new StringTokenizer(paramObj.toString(), ",");
                while (st.hasMoreTokens()) {
                    final String token = st.nextToken();
                    final CommandType listType = annotation.collectionType();
                    switch (listType) {
                    case INTEGER:
                        listParam.add(Integer.valueOf(token));
                        break;
                    case UUID:
                        if (token.isEmpty())
                            break;
                        final Long internalId = translateUuidToInternalId(token, annotation);
                        listParam.add(internalId);
                        break;
                    case LONG: {
                        listParam.add(Long.valueOf(token));
                    }
                    break;
                    case SHORT:
                        listParam.add(Short.valueOf(token));
                        break;
                    case STRING:
                        listParam.add(token);
                        break;
                    }
                }
                field.set(cmdObj, listParam);
                break;
            case UUID:
                final Long internalId = translateUuidToInternalId(paramObj.toString(), annotation);
                field.set(cmdObj, internalId);
                break;
            case LONG:
                field.set(cmdObj, Long.valueOf(paramObj.toString()));
                break;
            case SHORT:
                field.set(cmdObj, Short.valueOf(paramObj.toString()));
                break;
            case STRING:
                if ((paramObj != null)) {
                    if (paramObj.toString().length() > annotation.length()) {
                        s_logger.error("Value greater than max allowed length " + annotation.length() + " for param: " + field.getName());
                        throw new InvalidParameterValueException("Value greater than max allowed length " + annotation.length() + " for param: " + field.getName());
                    } else {
                        field.set(cmdObj, paramObj.toString());
                    }
                }
                break;
            case TZDATE:
                field.set(cmdObj, DateUtil.parseTZDateString(paramObj.toString()));
                break;
            case MAP:
            default:
                field.set(cmdObj, paramObj);
                break;
            }
        } catch (final IllegalAccessException ex) {
            s_logger.error("Error initializing command " + cmdObj.getCommandName() + ", field " + field.getName() + " is not accessible.");
            throw new CloudRuntimeException("Internal error initializing parameters for command " + cmdObj.getCommandName() + " [field " + field.getName() +
                    " is not accessible]");
        }
    }

    private boolean isObjInNewDateFormat(final String string) {
        final Matcher matcher = BaseCmd.newInputDateFormat.matcher(string);
        return matcher.matches();
    }

    private Date messageDate(final Date date, final int hourOfDay, final int minute, final int second) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        return cal.getTime();
    }

    private Long translateUuidToInternalId(final String uuid, final Parameter annotation) {
        if (uuid.equals("-1")) {
            // FIXME: This is to handle a lot of hardcoded special cases where -1 is sent
            // APITODO: Find and get rid of all hardcoded params in API Cmds and service layer
            return -1L;
        }
        Long internalId = null;
        // If annotation's empty, the cmd existed before 3.x try conversion to long
        final boolean isPre3x = annotation.since().isEmpty();
        // Match against Java's UUID regex to check if input is uuid string
        final boolean isUuid = uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
        // Enforce that it's uuid for newly added apis from version 3.x
        if (!isPre3x && !isUuid)
            return null;

        // There may be multiple entities defined on the @EntityReference of a Response.class
        // UUID CommandType would expect only one entityType, so use the first entityType
        final Class<?>[] entities = annotation.entityType()[0].getAnnotation(EntityReference.class).value();

        // Allow both uuid and internal id for pre3x apis
        if (isPre3x && !isUuid) {
            try {
                internalId = Long.parseLong(uuid);
            } catch (final NumberFormatException e) {
                internalId = null;
            }
            if (internalId != null){
                // Populate CallContext for each of the entity.
                for (final Class<?> entity : entities) {
                    CallContext.current().putContextParameter(entity, internalId);
                }
                validateNaturalNumber(internalId, annotation.name());
                return internalId;
            }
        }

        // Go through each entity which is an interface to a VO class and get a VO object
        // Try to getId() for the object using reflection, break on first non-null value
        for (final Class<?> entity : entities) {
            // For backward compatibility, we search within removed entities and let service layer deal
            // with removed ones, return empty response or error
            final Object objVO = _entityMgr.findByUuidIncludingRemoved(entity, uuid);
            if (objVO == null) {
                continue;
            }
            // Invoke the getId method, get the internal long ID
            // If that fails hide exceptions as the uuid may not exist                                         s
            try {
                internalId = ((InternalIdentity)objVO).getId();
            } catch (final IllegalArgumentException e) {
            } catch (final NullPointerException e) {
            }
            // Return on first non-null Id for the uuid entity
            if (internalId != null){
                CallContext.current().putContextParameter(entity, uuid);
                break;
            }
        }
        if (internalId == null) {
            if (s_logger.isDebugEnabled())
                s_logger.debug("Object entity uuid = " + uuid + " does not exist in the database.");
            throw new InvalidParameterValueException("Invalid parameter " + annotation.name() + " value=" + uuid +
                    " due to incorrect long value format, or entity does not exist or due to incorrect parameter annotation for the field in api cmd class.");
        }
        validateNaturalNumber(internalId, annotation.name());
        return internalId;
    }
}
