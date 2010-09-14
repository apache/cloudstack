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

import com.cloud.api.BaseCmd.CommandType;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.network.NetworkManager;
import com.cloud.network.security.NetworkGroupManager;
import com.cloud.server.ManagementServer;
import com.cloud.storage.StorageManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;

/**
 * A class that dispatches API commands to the appropriate manager for execution.
 */
public class ApiDispatcher {
    private static final Logger s_logger = Logger.getLogger(ApiDispatcher.class.getName());

    private ConfigurationManager _configMgr;
    private ManagementServer _mgmtServer;
    private NetworkGroupManager _networkGroupMgr;
    private NetworkManager _networkMgr;
    private SnapshotManager _snapshotMgr;
    private StorageManager _storageMgr;
    private UserVmManager _userVmMgr;

    // singleton class
    private static ApiDispatcher s_instance = new ApiDispatcher();

    public static ApiDispatcher getInstance() {
        return s_instance;
    }

    private ApiDispatcher() {
        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);
        _mgmtServer = (ManagementServer)ComponentLocator.getComponent(ManagementServer.Name);
        _configMgr = locator.getManager(ConfigurationManager.class);
        _networkGroupMgr = locator.getManager(NetworkGroupManager.class);
        _networkMgr = locator.getManager(NetworkManager.class);
        _snapshotMgr = locator.getManager(SnapshotManager.class);
        _storageMgr = locator.getManager(StorageManager.class);
        _userVmMgr = locator.getManager(UserVmManager.class);
    }

    public Long dispatchCreateCmd(BaseAsyncCreateCmd cmd, Map<String, String> params) {
        setupParameters(cmd, params);

        Implementation impl = cmd.getClass().getAnnotation(Implementation.class);
        String methodName = impl.createMethod();
        Object mgr = _mgmtServer;
        switch (impl.manager()) {
        case ConfigManager:
            mgr = _configMgr;
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
        case UserVmManager:
            mgr = _userVmMgr;
            break;
        }

        try {
            Method method = mgr.getClass().getMethod(methodName, cmd.getClass());
            method.invoke(mgr, cmd);
            return cmd.getId();
        } catch (NoSuchMethodException nsme) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), nsme);
            throw new CloudRuntimeException("Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", unable to find implementation.");
        } catch (InvocationTargetException ite) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), ite);
            throw new CloudRuntimeException("Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        } catch (IllegalAccessException iae) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), iae);
            throw new CloudRuntimeException("Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        } catch (IllegalArgumentException iArgEx) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), iArgEx);
            throw new CloudRuntimeException("Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        }
    }

    public void dispatch(BaseCmd cmd, Map<String, String> params) {
        setupParameters(cmd, params);

        Implementation impl = cmd.getClass().getAnnotation(Implementation.class);
        String methodName = impl.method();
        Object mgr = _mgmtServer;
        switch (impl.manager()) {
        case ConfigManager:
            mgr = _configMgr;
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
        case UserVmManager:
            mgr = _userVmMgr;
            break;
        }

        try {
            Method method = mgr.getClass().getMethod(methodName, cmd.getClass());
            method.invoke(mgr, cmd);
        } catch (NoSuchMethodException nsme) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), nsme);
            throw new CloudRuntimeException("Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", unable to find implementation.");
        } catch (InvocationTargetException ite) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), ite);
            throw new CloudRuntimeException("Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        } catch (IllegalAccessException iae) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), iae);
            throw new CloudRuntimeException("Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        } catch (IllegalArgumentException iArgEx) {
            s_logger.warn("Exception executing method " + methodName + " for command " + cmd.getClass().getSimpleName(), iArgEx);
            throw new CloudRuntimeException("Unable to execute method " + methodName + " for command " + cmd.getClass().getSimpleName() + ", internal error in the implementation.");
        }
    }

    private void setupParameters(BaseCmd cmd, Map<String, String> params) {
        Map<String, Object> unpackedParams = cmd.unpackParams(params);
        Field[] fields = cmd.getClass().getDeclaredFields();
        for (Field field : fields) {
            Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
            if (parameterAnnotation == null) {
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
