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

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;

import com.cloud.dao.EntityManager;
import org.apache.cloudstack.api.*;
import org.apache.log4j.Logger;

import com.cloud.acl.ControlledEntity;
import com.cloud.acl.Role;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.command.user.event.ListEventsCmd;
import com.cloud.async.AsyncCommandQueued;
import com.cloud.async.AsyncJobManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.dao.NetworkDao;
import com.cloud.server.ManagementServer;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.exception.CSExceptionErrorCode;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.uuididentity.dao.IdentityDao;

// ApiDispatcher: A class that dispatches API commands to the appropriate manager for execution.
public class ApiDispatcher {
    private static final Logger s_logger = Logger.getLogger(ApiDispatcher.class.getName());

    ComponentLocator _locator;
    Long _createSnapshotQueueSizeLimit;
    @Inject private AsyncJobManager _asyncMgr = null;
    @Inject private AccountManager _accountMgr = null;
    @Inject EntityManager _entityMgr = null;
    @Inject IdentityDao _identityDao = null;

    Map<String, Class<? extends GenericDao>> _daoNameMap = new HashMap<String, Class<? extends GenericDao>>();
    // singleton class
    private static ApiDispatcher s_instance = ApiDispatcher.getInstance();

    public static ApiDispatcher getInstance() {
        if (s_instance == null) {
            s_instance = ComponentLocator.inject(ApiDispatcher.class);
        }
        return s_instance;
    }

    protected ApiDispatcher() {
        super();
        _locator = ComponentLocator.getLocator(ManagementServer.Name);
        ConfigurationDao configDao = _locator.getDao(ConfigurationDao.class);
        Map<String, String> configs = configDao.getConfiguration();
        String strSnapshotLimit = configs.get(Config.ConcurrentSnapshotsThresholdPerHost.key());
        if (strSnapshotLimit != null) {
            Long snapshotLimit = NumbersUtil.parseLong(strSnapshotLimit, 1L);
            if (snapshotLimit <= 0) {
                s_logger.debug("Global config parameter " + Config.ConcurrentSnapshotsThresholdPerHost.toString()
                        + " is less or equal 0; defaulting to unlimited");
            } else {
                _createSnapshotQueueSizeLimit = snapshotLimit;
            }
        }
        _daoNameMap.put("com.cloud.network.Network", NetworkDao.class);
        _daoNameMap.put("com.cloud.template.VirtualMachineTemplate", VMTemplateDao.class);
    }

    public void dispatchCreateCmd(BaseAsyncCreateCmd cmd, Map<String, String> params) {
    	List<ControlledEntity> entitiesToAccess = new ArrayList<ControlledEntity>();
    	setupParameters(cmd, params, entitiesToAccess);

        doAccessChecks(cmd, entitiesToAccess);

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

    private void doAccessChecks(BaseAsyncCreateCmd cmd, List<ControlledEntity> entitiesToAccess) {
		//owner
		Account caller = UserContext.current().getCaller();
		Account owner = _accountMgr.getActiveAccountById(cmd.getEntityOwnerId());

		List<Role> callerRoles = determineRole(caller);
		List<Role> ownerRoles = determineRole(owner);

		//check permission to call this command for the caller
		//this needs checking of static roles of the caller
		checkACLOnCommand(cmd);

		//check that caller can access the owner account.
		_accountMgr.checkAccess(caller, null, true, owner);

		checkACLOnEntities(caller, entitiesToAccess);
	}


    private void checkACLOnCommand(BaseAsyncCreateCmd cmd) {
		// TODO Auto-generated method stub
		//need to write an commandACLChecker adapter framework to check ACL on commands - default one will use the static roles by referring to commands.properties.
    	//one can write another commandACLChecker to check access via custom roles.
	}

	private List<Role> determineRole(Account caller) {
		// TODO Auto-generated method stub
		List<Role> effectiveRoles = new ArrayList<Role>();
		return effectiveRoles;

	}

	private void checkACLOnEntities(Account caller, List<ControlledEntity> entitiesToAccess){
		//checkACLOnEntities
    	if(!entitiesToAccess.isEmpty()){
			for(ControlledEntity entity : entitiesToAccess)
			    _accountMgr.checkAccess(caller, null, true, entity);
       }
    }

	public void dispatch(BaseCmd cmd, Map<String, String> params) {
    	List<ControlledEntity> entitiesToAccess = new ArrayList<ControlledEntity>();
    	setupParameters(cmd, params, entitiesToAccess);

        if(!entitiesToAccess.isEmpty()){
			 //owner
			Account caller = UserContext.current().getCaller();
			Account owner = s_instance._accountMgr.getActiveAccountById(cmd.getEntityOwnerId());
			s_instance._accountMgr.checkAccess(caller, null, true, owner);
			for(ControlledEntity entity : entitiesToAccess)
			s_instance._accountMgr.checkAccess(caller, null, true, entity);
        }

        try {
            UserContext ctx = UserContext.current();
            ctx.setAccountId(cmd.getEntityOwnerId());
            if (cmd instanceof BaseAsyncCmd) {

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
                        _asyncMgr.syncAsyncJobExecution(asyncCmd.getJob(), asyncCmd.getSyncObjType(),
                                asyncCmd.getSyncObjId().longValue(), queueSizeLimit);
                    } else {
                        s_logger.trace("The queue size is unlimited, skipping the synchronizing");
                    }
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
                ArrayList<String> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                		ex.addProxyObject(idList.get(i));
                		s_logger.info(t.getMessage() + " uuid: " + idList.get(i));
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
            	ArrayList<String> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                 		ex.addProxyObject(idList.get(i));
                 		s_logger.info("PermissionDenied: " + t.getMessage() + "uuid: " + idList.get(i));
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
            	ArrayList<String> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                 		ex.addProxyObject(idList.get(i));
                 		s_logger.info(t.getMessage() + "uuid: " + idList.get(i));
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
            	ArrayList<String> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                 		ex.addProxyObject(idList.get(i));
                 		s_logger.info(t.getMessage() + "uuid: " + idList.get(i));
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
                ArrayList<String> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                 		String id = idList.get(i);
                 		ex.addProxyObject(id);
                 		s_logger.warn("Exception: " + t.getMessage() + "uuid: " + id);
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
                ArrayList<String> idList = ref.getIdProxyList();
                if (idList != null) {
                	// Iterate through entire arraylist and copy over each proxy id.
                	for (int i = 0 ; i < idList.size(); i++) {
                 		String id = idList.get(i);
                 		ex.addProxyObject(id);
                 		s_logger.warn("Exception: " + t.getMessage() + "uuid: " + id);
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public static void setupParameters(BaseCmd cmd, Map<String, String> params, List<ControlledEntity> entitiesToAccess) {
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

        // Process all the fields of the cmd object using reflection to recursively process super class
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
            //plug Services
            PlugService plugServiceAnnotation = field.getAnnotation(PlugService.class);
            if(plugServiceAnnotation != null){
                plugService(field, cmd);
            }
            //APITODO: change the checking here
            Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
            if ((parameterAnnotation == null) || !parameterAnnotation.expose()) {
                continue;
            }
            // APITODO Will remove this
            IdentityMapper identityMapper = field.getAnnotation(IdentityMapper.class);

            //ACL checkAccess = field.getAnnotation(ACL.class);

            Validator validators = field.getAnnotation(Validator.class);
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
            	//means
                // there was
                // and IllegalAccessException setting one of the parameters.
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error executing API command " + cmd.getCommandName().substring(0, cmd.getCommandName().length() - 8));
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
                        Class<?>[] entityList = parameterAnnotation.entityType();
                        for (Class entity : entityList){
                            if (ControlledEntity.class.isAssignableFrom(entity)) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("entity name is:" + entity.getName());
                                }

                                if (s_instance._daoNameMap.containsKey(entity.getName())) {
                                    Class<? extends GenericDao> daoClass = s_instance._daoNameMap.get(entity.getName());
                                    GenericDao daoClassInstance = s_instance._locator.getDao(daoClass);

                                    // Check if the parameter type is a single
                                    // Id or list of id's/name's
                                    switch (fieldType) {
                                    case LIST:
                                        CommandType listType = parameterAnnotation.collectionType();
                                        switch (listType) {
                                        case LONG:
                                            List<Long> listParam = new ArrayList<Long>();
                                            listParam = (List) field.get(cmd);

                                            for (Long entityId : listParam) {
                                                ControlledEntity entityObj = (ControlledEntity) daoClassInstance.findById(entityId);
                                                entitiesToAccess.add(entityObj);
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
                                        Long entityId = (Long) field.get(cmd);
                                        ControlledEntity entityObj = (ControlledEntity) daoClassInstance.findById(entityId);
                                        entitiesToAccess.add(entityObj);
                                        break;
                                    default:
                                        break;
                                    }

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
                            case UUID:
                                //APITODO: FIXME if there is any APICmd that has List<UUID>
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
            case UUID:
                if (paramObj.toString().isEmpty())
                    break;
                if (paramObj.toString().equals("-1")) {
                    // FIXME: This is to handle a lot of hardcoded special cases where -1 is sent
                    // APITODO: Find and get rid of all hardcoded params in API Cmds and service layer
                    field.set(cmdObj, -1L);
                    break;
                }
                // There may be multiple entities defined on the @Entity of a Response.class
                // UUID CommandType would expect only one entityType, so use the first entityType
                Class<?>[] entities = annotation.entityType()[0].getAnnotation(Entity.class).value();
                Long id = null;
                // Go through each entity which is an interface to a VO class and get a VO object
                // Try to getId() for the object using reflection, break on first non-null value
                for (Class<?> entity: entities) {
                    // findByUuid returns one VO object using uuid, use reflect to get the Id
                    Object objVO = s_instance._entityMgr.findByUuid(entity, paramObj.toString());
                    if (objVO == null) {
                        continue;
                    }
                    // Invoke the getId method, get the internal long ID
                    // If that fails hide exceptions as the uuid may not exist
                    try {
                        id = (Long) ((Identity)objVO).getId();
                    } catch (IllegalArgumentException e) {
                    } catch (NullPointerException e) {
                    }
                    // Return on first non-null Id for the uuid entity
                    if (id != null)
                        break;
                }
                // If id is null, entity with the uuid was not found, throw exception
                if (id == null) {
                    throw new InvalidParameterValueException("No entity with " + field.getName() + "(uuid)="
                            + paramObj.toString() + " was found in the database.");
                }
                field.set(cmdObj, id);
                break;
            case LONG:
                // APITODO: Remove identityMapper, simply convert the over the wire param to Long
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

    public static void plugService(Field field, BaseCmd cmd) {
        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);

        Class<?> fc = field.getType();
        Object instance = null;
        if (PluggableService.class.isAssignableFrom(fc)) {
            instance = locator.getPluggableService(fc);
        }

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

    public static Long getIdentiyId(String tableName, String token) {
        return s_instance._identityDao.getIdentityId(tableName, token);
    }
}
