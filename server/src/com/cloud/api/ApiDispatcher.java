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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.cloud.agent.manager.AgentManager;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.async.AsyncCommandQueued;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.network.NetworkManager;
import com.cloud.network.security.NetworkGroupManager;
import com.cloud.server.ManagementServer;
import com.cloud.storage.StorageManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
import com.cloud.user.AccountManager;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;

/**
 * A class that dispatches API commands to the appropriate manager for execution.
 */
public class ApiDispatcher {
    private static final Logger s_logger = Logger.getLogger(ApiDispatcher.class.getName());

    private AccountManager _accountMgr;
    private AgentManager _agentMgr;
    private ConfigurationManager _configMgr;
    private ConsoleProxyManager _consoleProxyMgr;
    private ManagementServer _mgmtServer;
    private NetworkGroupManager _networkGroupMgr;
    private NetworkManager _networkMgr;
    private SnapshotManager _snapshotMgr;
    private StorageManager _storageMgr;
    private TemplateManager _templateMgr;
    private UserVmManager _userVmMgr;

    // singleton class
    private static ApiDispatcher s_instance = new ApiDispatcher();

    public static ApiDispatcher getInstance() {
        return s_instance;
    }

    private ApiDispatcher() {
        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
        _mgmtServer = (ManagementServer)ComponentLocator.getComponent(ManagementServer.Name);
        _accountMgr = locator.getManager(AccountManager.class);
        _agentMgr = locator.getManager(AgentManager.class);
        _configMgr = locator.getManager(ConfigurationManager.class);
        _consoleProxyMgr = locator.getManager(ConsoleProxyManager.class);
        _networkGroupMgr = locator.getManager(NetworkGroupManager.class);
        _networkMgr = locator.getManager(NetworkManager.class);
        _snapshotMgr = locator.getManager(SnapshotManager.class);
        _storageMgr = locator.getManager(StorageManager.class);
        _templateMgr = locator.getManager(TemplateManager.class);
        _userVmMgr = locator.getManager(UserVmManager.class);
    }

    public Long dispatchCreateCmd(BaseAsyncCreateCmd cmd, Map<String, String> params) {
        setupParameters(cmd, params);

        Implementation impl = cmd.getClass().getAnnotation(Implementation.class);
        if (impl == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to execute create command " + cmd.getClass().getName() + ", no implementation specified.");
        }

        String methodName = impl.createMethod();
        Object mgr = _mgmtServer;
        switch (impl.manager()) {
        case AccountManager:
            mgr = _accountMgr;
            break;
        case AgentManager:
            mgr = _agentMgr;
            break;
        case ConfigManager:
            mgr = _configMgr;
            break;
        case ConsoleProxyManager:
            mgr = _consoleProxyMgr;
            break;
        case NetworkGroupManager:
            mgr = _networkGroupMgr;
            break;
        case NetworkManager:
            mgr = _networkMgr;
            break;
        case SnapshotManager:
            mgr = _snapshotMgr;
            break;
        case StorageManager:
            mgr = _storageMgr;
            break;
        case TemplateManager:
            mgr = _templateMgr;
            break;
        case UserVmManager:
            mgr = _userVmMgr;
            break;
        }

        try {
            Method method = mgr.getClass().getMethod(methodName, cmd.getClass());
            Object dbObject = method.invoke(mgr, cmd);

            Method getIdMethod = dbObject.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(dbObject);
            
            return (Long)id;
        } catch (NoSuchMethodException nsme) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), nsme);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", unable to find implementation.");
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof InvalidParameterValueException) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, cause.getMessage());
            } else if (cause instanceof PermissionDeniedException) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, cause.getMessage());
            }
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), ite);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        } catch (IllegalAccessException iae) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), iae);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        } catch (IllegalArgumentException iArgEx) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), iArgEx);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        }
    }

    public void dispatch(BaseCmd cmd, Map<String, String> params) {
        setupParameters(cmd, params);

        Implementation impl = cmd.getClass().getAnnotation(Implementation.class);
        if (impl == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to execute command " + cmd.getClass().getName() + ", no implementation specified.");
        }

        String methodName = impl.method();
        Object mgr = _mgmtServer;
        switch (impl.manager()) {
        case AccountManager:
            mgr = _accountMgr;
            break;
        case AgentManager:
            mgr = _agentMgr;
            break;
        case ConfigManager:
            mgr = _configMgr;
            break;
        case ConsoleProxyManager:
            mgr = _consoleProxyMgr;
            break;
        case NetworkGroupManager:
            mgr = _networkGroupMgr;
            break;
        case NetworkManager:
            mgr = _networkMgr;
            break;
        case SnapshotManager:
            mgr = _snapshotMgr;
            break;
        case StorageManager:
            mgr = _storageMgr;
            break;
        case TemplateManager:
            mgr = _templateMgr;
            break;
        case UserVmManager:
            mgr = _userVmMgr;
            break;
        }

        try {
            Method method = mgr.getClass().getMethod(methodName, cmd.getClass());
            Object result = method.invoke(mgr, cmd);
            cmd.setResponseObject(result);
        } catch (NoSuchMethodException nsme) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), nsme);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", unable to find implementation.");
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof AsyncCommandQueued) {
                throw (AsyncCommandQueued)cause;
            }
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), ite);
            if (cause instanceof InvalidParameterValueException) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, cause.getMessage());
            } else if (cause instanceof PermissionDeniedException) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, cause.getMessage());
            }
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        } catch (IllegalAccessException iae) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), iae);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        } catch (IllegalArgumentException iArgEx) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), iArgEx);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        }
    }

    private void setupParameters(BaseCmd cmd, Map<String, String> params) {
        Map<String, Object> unpackedParams = cmd.unpackParams(params);
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

            Object paramObj = unpackedParams.get(parameterAnnotation.name());
            if (paramObj == null) {
                if (parameterAnnotation.required()) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to execute API command " + cmd.getName() + " due to missing parameter " + parameterAnnotation.name());
                }
                continue;
            }

            // marshall the parameter into the correct type and set the field value
            try {
                setFieldValue(field, cmd, paramObj, parameterAnnotation);
            } catch (IllegalArgumentException argEx) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to execute API command " + cmd.getName() + " due to invalid value " + paramObj + " for parameter " + parameterAnnotation.name());
                }
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to execute API command " + cmd.getName() + " due to invalid value " + paramObj + " for parameter " + parameterAnnotation.name());
            } catch (ParseException parseEx) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Invalid date parameter " + paramObj + " passed to command " + cmd.getName());
                }
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to parse date " + paramObj + " for command " + cmd.getName() + ", please pass dates in the format yyyy-MM-dd");
            } catch (CloudRuntimeException cloudEx) {
                // FIXME:  Better error message?  This only happens if the API command is not executable, which typically means there was
                //         and IllegalAccessException setting one of the parameters.
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error executing API command " + cmd.getName());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setFieldValue(Field field, BaseCmd cmdObj, Object paramObj, Parameter annotation) throws IllegalArgumentException, ParseException {
        try {
            field.setAccessible(true);
            CommandType fieldType = annotation.type();
            switch (fieldType) {
            case BOOLEAN:
                field.set(cmdObj, Boolean.valueOf(paramObj.toString()));
                break;
            case DATE:
                DateFormat format = BaseCmd.INPUT_FORMAT;
                synchronized (format) {
                    field.set(cmdObj, format.parse(paramObj.toString()));
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
                    case LONG:
                        listParam.add(Long.valueOf(token));
                        break;
                    }
                }
                field.set(cmdObj, listParam);
                break;
            case LONG:
                field.set(cmdObj, Long.valueOf(paramObj.toString()));
                break;
            case STRING:
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
            s_logger.error("Error initializing command " + cmdObj.getName() + ", field " + field.getName() + " is not accessible.");
            throw new CloudRuntimeException("Internal error initializing parameters for command " + cmdObj.getName() + " [field " + field.getName() + " is not accessible]");
        }
    }
}
