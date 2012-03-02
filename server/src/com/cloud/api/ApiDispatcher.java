/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.commands.ListEventsCmd;
import com.cloud.async.AsyncCommandQueued;
import com.cloud.async.AsyncJobManager;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.IdentityProxy;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CSExceptionErrorCode;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.uuididentity.dao.IdentityDao;

/**
 * A class that dispatches API commands to the appropriate manager for execution.
 */
public class ApiDispatcher {
    private static final Logger s_logger = Logger.getLogger(ApiDispatcher.class.getName());

    ComponentLocator _locator;
    AsyncJobManager _asyncMgr;
    IdentityDao _identityDao;

    // singleton class
    private static ApiDispatcher s_instance = new ApiDispatcher();

    public static ApiDispatcher getInstance() {
        return s_instance;
    }

    private ApiDispatcher() {
        _locator = ComponentLocator.getLocator(ManagementServer.Name);
        _asyncMgr = _locator.getManager(AsyncJobManager.class);
        _identityDao = _locator.getDao(IdentityDao.class);
    }

    public void dispatchCreateCmd(BaseAsyncCreateCmd cmd, Map<String, String> params) {

        setupParameters(cmd, params);
        plugService(cmd);

        try {
            UserContext ctx = UserContext.current();
            ctx.setAccountId(cmd.getEntityOwnerId());
            cmd.create();
        } catch (Throwable t) {
            if (t instanceof InvalidParameterValueException || t instanceof IllegalArgumentException) {
                s_logger.info(t.getMessage());
                throw new ServerApiException(BaseCmd.PARAM_ERROR, t.getMessage());
            } else if (t instanceof PermissionDeniedException) {
                s_logger.info("PermissionDenied: " + t.getMessage());
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, t.getMessage());
            } else if (t instanceof AccountLimitException) {
                s_logger.info(t.getMessage());
                throw new ServerApiException(BaseCmd.ACCOUNT_RESOURCE_LIMIT_ERROR, t.getMessage());
            } else if (t instanceof InsufficientCapacityException) {
                s_logger.info(t.getMessage());
                throw new ServerApiException(BaseCmd.INSUFFICIENT_CAPACITY_ERROR, t.getMessage());
            } else if (t instanceof ResourceAllocationException) {
                s_logger.info(t.getMessage());
                throw new ServerApiException(BaseCmd.RESOURCE_ALLOCATION_ERROR, t.getMessage());
            } else if (t instanceof ResourceUnavailableException) {
                s_logger.warn("Exception: ", t);
                throw new ServerApiException(BaseCmd.RESOURCE_UNAVAILABLE_ERROR, t.getMessage());
            } else if (t instanceof AsyncCommandQueued) {
                throw (AsyncCommandQueued) t;
            } else if (t instanceof ServerApiException) {
                s_logger.warn(t.getClass() + " : " + ((ServerApiException) t).getDescription());
                throw (ServerApiException) t;
            } else {
                s_logger.error("Exception while executing " + cmd.getClass().getSimpleName() + ":", t);
                if (UserContext.current().getCaller().getType() == Account.ACCOUNT_TYPE_ADMIN) {
                    throw new ServerApiException(BaseCmd.INTERNAL_ERROR, t.getMessage());
                } else {
                    throw new ServerApiException(BaseCmd.INTERNAL_ERROR, BaseCmd.USER_ERROR_MESSAGE);
                }
            }
        }
    }

    public void dispatch(BaseCmd cmd, Map<String, String> params) {
        setupParameters(cmd, params);
        ApiDispatcher.plugService(cmd);
        try {
            UserContext ctx = UserContext.current();
            ctx.setAccountId(cmd.getEntityOwnerId());
            if (cmd instanceof BaseAsyncCmd) {

                BaseAsyncCmd asyncCmd = (BaseAsyncCmd) cmd;
                String startEventId = params.get("ctxStartEventId");
                ctx.setStartEventId(Long.valueOf(startEventId));

                // Synchronise job on the object if needed
                if (asyncCmd.getJob() != null && asyncCmd.getSyncObjId() != null && asyncCmd.getSyncObjType() != null) {
                    _asyncMgr.syncAsyncJobExecution(asyncCmd.getJob(), asyncCmd.getSyncObjType(), asyncCmd.getSyncObjId().longValue());
                }
            }

            cmd.execute();

        } catch (Throwable t) {
            if (t instanceof InvalidParameterValueException) {
            	// earlier, we'd log the db id as part of the log message, but now since we've pushed
            	// the id into a IdentityProxy object, we would need to dump that object alongwith the
            	// message.
            	InvalidParameterValueException ref = (InvalidParameterValueException) t;
            	ServerApiException ex = new ServerApiException(BaseCmd.PARAM_ERROR, t.getMessage());
                // copy over the IdentityProxy information as well and throw the serverapiexception.
                ArrayList<IdentityProxy> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                		IdentityProxy id = idList.get(i);
                		ex.addProxyObject(id.getTableName(), id.getValue(), id.getidFieldName());
                		s_logger.info(t.getMessage() + " db_id: " + id.getValue());
                	}
                } else {
                	s_logger.info(t.getMessage());
                }
                // Also copy over the cserror code.
    			ex.setCSErrorCode(ref.getCSErrorCode());
                throw ex;
            } else if(t instanceof IllegalArgumentException) {            	
            	throw new ServerApiException(BaseCmd.PARAM_ERROR, t.getMessage());
            } else if (t instanceof PermissionDeniedException) {            	
            	PermissionDeniedException ref = (PermissionDeniedException)t;
            	ServerApiException ex = new ServerApiException(BaseCmd.ACCOUNT_ERROR, t.getMessage());
                // copy over the IdentityProxy information as well and throw the serverapiexception.
            	ArrayList<IdentityProxy> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                 		IdentityProxy id = idList.get(i);
                 		ex.addProxyObject(id.getTableName(), id.getValue(), id.getidFieldName());
                 		s_logger.info("PermissionDenied: " + t.getMessage() + "db_id: " + id.getValue());
                 	}
                 } else {
                	 s_logger.info("PermissionDenied: " + t.getMessage());
                 }
                // Also copy over the cserror code.
    			ex.setCSErrorCode(ref.getCSErrorCode());
    			throw ex;
            } else if (t instanceof AccountLimitException) {            	
            	AccountLimitException ref = (AccountLimitException)t;
            	ServerApiException ex = new ServerApiException(BaseCmd.ACCOUNT_RESOURCE_LIMIT_ERROR, t.getMessage());
                // copy over the IdentityProxy information as well and throw the serverapiexception.
            	ArrayList<IdentityProxy> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                 		IdentityProxy id = idList.get(i);
                 		ex.addProxyObject(id.getTableName(), id.getValue(), id.getidFieldName());
                 		s_logger.info(t.getMessage() + "db_id: " + id.getValue());
                	}
                } else {
                	s_logger.info(t.getMessage());
                }
                // Also copy over the cserror code.
    			ex.setCSErrorCode(ref.getCSErrorCode());
                throw ex;
            } else if (t instanceof InsufficientCapacityException) {            	
            	InsufficientCapacityException ref = (InsufficientCapacityException)t;
            	ServerApiException ex = new ServerApiException(BaseCmd.INSUFFICIENT_CAPACITY_ERROR, t.getMessage());
                // copy over the IdentityProxy information as well and throw the serverapiexception.
            	ArrayList<IdentityProxy> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                 		IdentityProxy id = idList.get(i);
                 		ex.addProxyObject(id.getTableName(), id.getValue(), id.getidFieldName());
                 		s_logger.info(t.getMessage() + "db_id: " + id.getValue());
                	}
                } else {
                	s_logger.info(t.getMessage());
                }
                // Also copy over the cserror code
    			ex.setCSErrorCode(ref.getCSErrorCode());
                throw ex;
            } else if (t instanceof ResourceAllocationException) {
            	ResourceAllocationException ref = (ResourceAllocationException)t;
                ServerApiException ex = new ServerApiException(BaseCmd.RESOURCE_ALLOCATION_ERROR, t.getMessage());
                // copy over the IdentityProxy information as well and throw the serverapiexception.
                ArrayList<IdentityProxy> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                 		IdentityProxy id = idList.get(i);
                 		ex.addProxyObject(id.getTableName(), id.getValue(), id.getidFieldName());
                 		s_logger.warn("Exception: " + t.getMessage() + "db_id: " + id.getValue());
                	}
                } else {
                	s_logger.warn("Exception: ", t);
                }
                // Also copy over the cserror code.
    			ex.setCSErrorCode(ref.getCSErrorCode());
                throw ex;
            } else if (t instanceof ResourceUnavailableException) {
            	ResourceUnavailableException ref = (ResourceUnavailableException)t;
                ServerApiException ex = new ServerApiException(BaseCmd.RESOURCE_UNAVAILABLE_ERROR, t.getMessage());
                // copy over the IdentityProxy information as well and throw the serverapiexception.
                ArrayList<IdentityProxy> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                 		IdentityProxy id = idList.get(i);
                 		ex.addProxyObject(id.getTableName(), id.getValue(), id.getidFieldName());
                 		s_logger.warn("Exception: " + t.getMessage() + "db_id: " + id.getValue());
                	}
                } else {
                	s_logger.warn("Exception: ", t);
                }
                // Also copy over the cserror code.
    			ex.setCSErrorCode(ref.getCSErrorCode());
                throw ex;
            } else if (t instanceof AsyncCommandQueued) {            	
                throw (AsyncCommandQueued) t;
            } else if (t instanceof ServerApiException) {
                s_logger.warn(t.getClass() + " : " + ((ServerApiException) t).getDescription());
                throw (ServerApiException) t;
            } else {
                s_logger.error("Exception while executing " + cmd.getClass().getSimpleName() + ":", t);
                ServerApiException ex;
                if (UserContext.current().getCaller().getType() == Account.ACCOUNT_TYPE_ADMIN) {
                	ex = new ServerApiException(BaseCmd.INTERNAL_ERROR, t.getMessage());
                } else {
                    ex = new ServerApiException(BaseCmd.INTERNAL_ERROR, BaseCmd.USER_ERROR_MESSAGE);
                }                
                ex.setCSErrorCode(CSExceptionErrorCode.getCSErrCode(ex.getClass().getName()));
            	throw ex;
            }
        }
    }

    public static void setupParameters(BaseCmd cmd, Map<String, String> params) {
        Map<String, Object> unpackedParams = cmd.unpackParams(params);

        if (cmd instanceof BaseListCmd) {
            Object pageSizeObj = unpackedParams.get(ApiConstants.PAGE_SIZE);
            Long pageSize = null;
            if (pageSizeObj != null) {
                pageSize = Long.valueOf((String) pageSizeObj);
            }

            if ((unpackedParams.get(ApiConstants.PAGE) == null) && (pageSize != null && pageSize != BaseListCmd.PAGESIZE_UNLIMITED)) {
                ServerApiException ex = new ServerApiException(BaseCmd.PARAM_ERROR, "\"page\" parameter is required when \"pagesize\" is specified");                
                ex.setCSErrorCode(CSExceptionErrorCode.getCSErrCode(ex.getClass().getName()));
            	throw ex;
            } else if (pageSize == null && (unpackedParams.get(ApiConstants.PAGE) != null)) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "\"pagesize\" parameter is required when \"page\" is specified");
            }
        }

        Field[] fields = cmd.getClass().getDeclaredFields();
        Class<?> superClass = cmd.getClass().getSuperclass();
        while (BaseCmd.class.isAssignableFrom(superClass)) {
            Field[] superClassFields = superClass.getDeclaredFields();
            if (superClassFields != null) {
                Field[] tmpFields = new Field[fields.length + superClassFields.length];
                System.arraycopy(fields, 0, tmpFields, 0, fields.length);
                System.arraycopy(superClassFields, 0, tmpFields, fields.length, superClassFields.length);
                fields = tmpFields;
            }
            superClass = superClass.getSuperclass();
        }

        for (Field field : fields) {
            Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
            if ((parameterAnnotation == null) || !parameterAnnotation.expose()) {
                continue;
            }

            IdentityMapper identityMapper = field.getAnnotation(IdentityMapper.class);

            Object paramObj = unpackedParams.get(parameterAnnotation.name());
            if (paramObj == null) {
                if (parameterAnnotation.required()) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to execute API command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8) + " due to missing parameter "
                            + parameterAnnotation.name());
                }
                continue;
            }

            // marshall the parameter into the correct type and set the field value
            try {
                setFieldValue(field, cmd, paramObj, parameterAnnotation, identityMapper);
            } catch (IllegalArgumentException argEx) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to execute API command " + cmd.getCommandName() + " due to invalid value " + paramObj + " for parameter " + parameterAnnotation.name());
                }
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to execute API command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8) + " due to invalid value " + paramObj
                        + " for parameter "
                        + parameterAnnotation.name());
            } catch (ParseException parseEx) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Invalid date parameter " + paramObj + " passed to command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8));
                }
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to parse date " + paramObj + " for command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8)
                        + ", please pass dates in the format mentioned in the api documentation");
            } catch (InvalidParameterValueException invEx) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to execute API command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8) + " due to invalid value. " + invEx.getMessage());
            } catch (CloudRuntimeException cloudEx) {
                // FIXME: Better error message? This only happens if the API command is not executable, which typically
// means
                // there was
                // and IllegalAccessException setting one of the parameters.
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error executing API command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8));
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void setFieldValue(Field field, BaseCmd cmdObj, Object paramObj, Parameter annotation, IdentityMapper identityMapper) throws IllegalArgumentException, ParseException {
        try {
            field.setAccessible(true);
            CommandType fieldType = annotation.type();
            switch (fieldType) {
            case BOOLEAN:
                field.set(cmdObj, Boolean.valueOf(paramObj.toString()));
                break;
            case DATE:
                // This piece of code is for maintaining backward compatibility and support both the date formats(Bug
// 9724)
                // Do the date massaging for ListEventsCmd only
                if (cmdObj instanceof ListEventsCmd) {
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
                                date = massageDate(date, 0, 0, 0);
                            } else if (field.getName().equals("endDate")) {
                                date = massageDate(date, 23, 59, 59);
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
                field.set(cmdObj, Float.valueOf(paramObj.toString()));
                break;
            case INTEGER:
                field.set(cmdObj, Integer.valueOf(paramObj.toString()));
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
                    case LONG: {
                        Long val = null;
                        if (identityMapper != null)
                            val = s_instance._identityDao.getIdentityId(identityMapper, token);
                        else
                            val = Long.valueOf(token);

                        listParam.add(val);
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
            case LONG:
                if (identityMapper != null)
                    field.set(cmdObj, s_instance._identityDao.getIdentityId(identityMapper, paramObj.toString()));
                else
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

    private static Date massageDate(Date date, int hourOfDay, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        return cal.getTime();
    }

    public static void plugService(BaseCmd cmd) {

        if (!ApiServer.isPluggableServiceCommand(cmd.getClass().getName())) {
            return;
        }
        Class<?> clazz = cmd.getClass();
        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
        do {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                PlugService plugService = field.getAnnotation(PlugService.class);
                if (plugService == null) {
                    continue;
                }
                Class<?> fc = field.getType();
                Object instance = null;
                if (PluggableService.class.isAssignableFrom(fc)) {
                    instance = locator.getPluggableService(fc);
                }

                if (instance == null) {
                    throw new CloudRuntimeException("Unable to plug service " + fc.getSimpleName() + " in command " + clazz.getSimpleName());
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
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class && clazz != null);

    }
}
