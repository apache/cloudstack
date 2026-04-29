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

package org.apache.cloudstack.network.contrail.management;

import java.lang.reflect.Method;
import java.util.HashMap;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageDispatcher;
import org.apache.cloudstack.framework.messagebus.MessageHandler;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;

/*
 * When an Object is created/deleted in cloudstack DB, it has to be reflected in VNC.
 * This class handles create, delete and update events of cloudstack db objects.
 *
 * - subscribe for interested events
 * - create events will have db id of the object and hence db object and its parameters can be retrieved
 * - delete events will have db id but the object no longer exists in db and hence complete class needs to be synchronized
 *
 */
@Component
public class ServerEventHandlerImpl implements ServerEventHandler {
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    ProjectDao _projectDao;
    @Inject
    private MessageBus _messageBus;
    @Inject
    ServerDBSync _dbSync;
    @Inject
    ContrailManager _manager;
    private HashMap<String, Method> _methodMap;
    private HashMap<String, Class<?>> _classMap;

    protected Logger logger = LogManager.getLogger(getClass());

    ServerEventHandlerImpl() {
        setMethodMap();
        setClassMap();
    }

    private void setMethodMap() {
        _methodMap = new HashMap<String, Method>();
        Method methods[] = this.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            _methodMap.put(methods[i].getName(), methods[i]);
        }
    }

    private void setClassMap() {
        _classMap = new HashMap<String, Class<?>>();
        _classMap.put("Domain", net.juniper.contrail.api.types.Domain.class);
        _classMap.put("Project", net.juniper.contrail.api.types.Project.class);
    }

    @MessageHandler(topic = ".*")
    public void defaultMessageHandler(String subject, String topic, Object args) {
        logger.info("DB Event Received - topic: " + topic + "; subject: " + subject);

        org.apache.cloudstack.framework.events.Event event = (org.apache.cloudstack.framework.events.Event)args;

        /* Method name should be on<ClassName><Operation> for example: onDomainCreate */
        Method method = null;

        try {
            /* Only create event needs special implementation */
            if (event.getEventType().contains("CREATE")) {
                String methodName = "on" + event.getResourceType() + "Create";
                method = _methodMap.get(methodName);
                if (method == null) {
                    defaultCreateHandler(subject, topic, event);
                } else {
                    method.invoke(this, subject, topic, event);
                }
            } else if (event.getEventType().contains("DELETE")) {
                defaultDeleteHandler(subject, topic, event);
            } else {
                defaultHandler(subject, topic, event);
            }
        } catch (Exception e) {
            logger.debug(e);
        }
    }

    /* Default create handler */
    void defaultCreateHandler(String subject, String topic, org.apache.cloudstack.framework.events.Event event) {

        logger.debug("Default handler is invoked for subject: " + subject + "; topic: " + topic);
        logger.debug("description: " + event.getDescription());
        logger.debug("category: " + event.getEventCategory());
        logger.debug("type: " + event.getResourceType());
        logger.debug("event-type: " + event.getEventType());

        Class<?> cls = _classMap.get(event.getResourceType());

        if (cls != null) {
            _dbSync.syncClass(cls);
        }

        return;
    }

    /* Default handler */
    void defaultDeleteHandler(String subject, String topic, org.apache.cloudstack.framework.events.Event event) {

        logger.debug("Default handler is invoked for subject: " + subject + "; topic: " + topic);

        logger.debug("description: " + event.getDescription());
        logger.debug("category: " + event.getEventCategory());
        logger.debug("type: " + event.getResourceType());
        logger.debug("event-type: " + event.getEventType());
        Class<?> cls = _classMap.get(event.getResourceType());
        if (cls != null) {
            _dbSync.syncClass(cls);
        }
        return;
    }

    /* Default handler */
    void defaultHandler(String subject, String topic, org.apache.cloudstack.framework.events.Event event) {

        logger.debug("Default handler is invoked for subject: " + subject + "; topic: " + topic);

        logger.debug("description: " + event.getDescription());
        logger.debug("category: " + event.getEventCategory());
        logger.debug("type: " + event.getResourceType());
        logger.debug("event-type: " + event.getEventType());
        Class<?> cls = _classMap.get(event.getResourceType());
        if (cls != null) {
            _dbSync.syncClass(cls);
        }
        return;
    }

    /* Description string contains substring of format "resourceType Id: <int>" for example: "Project id: 35"
     *
     * example:
     *  description: {"details":"Successfully completed deleting project. Project Id: 39","status":"Completed","event":"PROJECT.DELETE","account":"3afca502-d83c-11e2-b748-52540076b7ca","user":"3b111406-d83c-11e2-b748-52540076b7ca"}
     *
     * If the description string format is changed, this code has to be modified
     */
    private long parseForId(String resourceType, String description) {
        String typeStr = resourceType + " Id:";
        int idIdx = description.indexOf(typeStr) + typeStr.length();
        String idStr = description.substring(idIdx, description.indexOf('"', idIdx));
        long id = 0;
        try {
            id = Long.parseLong(idStr.trim());
        } catch (Exception e) {
            logger.debug("Unable to parse id string<" + idStr.trim() + "> for long value, ignored");
        }
        return id;
    }

    public void onDomainCreate(String subject, String topic, org.apache.cloudstack.framework.events.Event event) {
        logger.info("onDomainCreate; topic: " + topic + "; subject: " + subject);
        try {
            long id = parseForId(event.getResourceType(), event.getDescription());
            if (id != 0) {
                DomainVO domain = _domainDao.findById(id);
                if (domain != null) {
                    logger.info("createDomain for name: " + domain.getName() + "; uuid: " + domain.getUuid());
                    StringBuffer logMesg = new StringBuffer();
                    _dbSync.createDomain(domain, logMesg);
                } else {
                    /* could not find db record, resync complete class */
                    _dbSync.syncClass(net.juniper.contrail.api.types.Domain.class);
                }
            } else {
                /* Unknown id, resync complete class */
                _dbSync.syncClass(net.juniper.contrail.api.types.Domain.class);
            }
        } catch (Exception e) {
            logger.debug(e);
        }
    }

    public void onProjectCreate(String subject, String topic, org.apache.cloudstack.framework.events.Event event) {
        logger.info("onProjectCreate; topic: " + topic + "; subject: " + subject);
        try {
            long id = parseForId(event.getResourceType(), event.getDescription());
            if (id != 0) {
                ProjectVO project = _projectDao.findById(id);
                if (project != null) {
                    logger.info("createProject for name: " + project.getName() + "; uuid: " + project.getUuid());
                    StringBuffer logMesg = new StringBuffer();
                    _dbSync.createProject(project, logMesg);
                } else {
                    /* could not find db record, resync complete class */
                    _dbSync.syncClass(net.juniper.contrail.api.types.Project.class);
                }
            } else {
                /* Unknown id, resync complete class */
                _dbSync.syncClass(net.juniper.contrail.api.types.Project.class);
            }
        } catch (Exception e) {
            logger.info(e);
        }

    }

    @Override
    public void subscribe() {
        /* subscribe to DB events */
        _messageBus.subscribe(EventTypes.EVENT_PROJECT_CREATE, MessageDispatcher.getDispatcher(this));
        _messageBus.subscribe(EventTypes.EVENT_PROJECT_DELETE, MessageDispatcher.getDispatcher(this));
        _messageBus.subscribe(EventTypes.EVENT_DOMAIN_CREATE, MessageDispatcher.getDispatcher(this));
        _messageBus.subscribe(EventTypes.EVENT_DOMAIN_DELETE, MessageDispatcher.getDispatcher(this));
    }
}
