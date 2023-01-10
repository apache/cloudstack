/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.globo.globodns.cloudstack.resource;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;


import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;
import com.globo.globodns.client.GloboDns;
import com.globo.globodns.client.GloboDnsException;
import com.globo.globodns.client.model.Authentication;
import com.globo.globodns.client.model.Domain;
import com.globo.globodns.client.model.Export;
import com.globo.globodns.client.model.Record;
import com.globo.globodns.cloudstack.commands.CreateOrUpdateDomainCommand;
import com.globo.globodns.cloudstack.commands.CreateOrUpdateRecordAndReverseCommand;
import com.globo.globodns.cloudstack.commands.RemoveDomainCommand;
import com.globo.globodns.cloudstack.commands.RemoveRecordCommand;
import com.globo.globodns.cloudstack.commands.SignInCommand;

public class GloboDnsResource extends ManagerBase implements ServerResource {
    private String _zoneId;

    private String _guid;

    private String _name;

    private String _username;

    private String _url;

    private String _password;

    protected GloboDns _globoDns;

    private static final String IPV4_RECORD_TYPE = "A";
    private static final String REVERSE_RECORD_TYPE = "PTR";
    private static final String REVERSE_DOMAIN_SUFFIX = "in-addr.arpa";
    private static final String DEFAULT_AUTHORITY_TYPE = "M";


    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        _zoneId = (String)params.get("zoneId");
        if (_zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }

        _guid = (String)params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find guid");
        }

        _name = (String)params.get("name");
        if (_name == null) {
            throw new ConfigurationException("Unable to find name");
        }

        _url = (String)params.get("url");
        if (_url == null) {
            throw new ConfigurationException("Unable to find url");
        }

        _username = (String)params.get("username");
        if (_username == null) {
            throw new ConfigurationException("Unable to find username");
        }

        _password = (String)params.get("password");
        if (_password == null) {
            throw new ConfigurationException("Unable to find password");
        }

        _globoDns = GloboDns.buildHttpApi(_url, _username, _password);

        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public Type getType() {
        return Host.Type.L2Networking;
    }

    @Override
    public StartupCommand[] initialize() {
        logger.trace("initialize called");
        StartupCommand cmd = new StartupCommand(getType());
        cmd.setName(_name);
        cmd.setGuid(_guid);
        cmd.setDataCenter(_zoneId);
        cmd.setPod("");
        cmd.setPrivateIpAddress("");
        cmd.setStorageIpAddress("");
        cmd.setVersion(GloboDnsResource.class.getPackage().getImplementationVersion());
        return new StartupCommand[] {cmd};
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        return new PingCommand(getType(), id);
    }

    @Override
    public void disconnected() {
        return;
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        return;
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return new ReadyAnswer((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return new MaintainAnswer((MaintainCommand)cmd);
        } else if (cmd instanceof SignInCommand) {
            return execute((SignInCommand)cmd);
        } else if (cmd instanceof RemoveDomainCommand) {
            return execute((RemoveDomainCommand)cmd);
        } else if (cmd instanceof RemoveRecordCommand) {
            return execute((RemoveRecordCommand)cmd);
        } else if (cmd instanceof CreateOrUpdateDomainCommand) {
            return execute((CreateOrUpdateDomainCommand)cmd);
        } else if (cmd instanceof CreateOrUpdateRecordAndReverseCommand) {
            return execute((CreateOrUpdateRecordAndReverseCommand)cmd);
        }
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    public Answer execute(SignInCommand cmd) {
        try {
            Authentication auth = _globoDns.getAuthAPI().signIn(cmd.getEmail(), cmd.getPassword());
            if (auth != null) {
                return new Answer(cmd, true, "Signed in successfully");
            } else {
                return new Answer(cmd, false, "Unable to sign in on GloboDNS");
            }
        } catch (GloboDnsException e) {
            return new Answer(cmd, false, e.getMessage());
        }
    }

    public Answer execute(RemoveDomainCommand cmd) {
        try {
            Domain domain = searchDomain(cmd.getNetworkDomain(), false);
            if (domain != null) {
                if (!cmd.isOverride()) {
                    for (Record record : _globoDns.getRecordAPI().listAll(domain.getId())) {
                        if (record.getTypeNSRecordAttributes().getId() == null) {
                            logger.warn("There are records in domain " + cmd.getNetworkDomain() + " and override is not enable. I will not delete this domain.");
                            return new Answer(cmd, true, "Domain keeped");
                        }
                    }
                }
                _globoDns.getDomainAPI().removeDomain(domain.getId());
                scheduleExportChangesToBind();
            } else {
                logger.warn("Domain " + cmd.getNetworkDomain() + " already been deleted.");
            }

            return new Answer(cmd, true, "Domain removed");
        } catch (GloboDnsException e) {
            return new Answer(cmd, false, e.getMessage());
        }
    }

    public Answer execute(RemoveRecordCommand cmd) {
        boolean needsExport = false;
        try {
            if (removeRecord(cmd.getRecordName(), cmd.getRecordIp(), cmd.getNetworkDomain(), false, cmd.isOverride())) {
                needsExport = true;
            }

            // remove reverse
            String reverseGloboDnsName = generateReverseDomainNameFromNetworkIp(cmd.getRecordIp());
            String reverseRecordName = generateReverseRecordNameFromNetworkIp(cmd.getRecordIp());
            String reverseRecordContent = cmd.getRecordName() + '.' + cmd.getNetworkDomain();

            if (removeRecord(reverseRecordName, reverseRecordContent, reverseGloboDnsName, true, cmd.isOverride())) {
                needsExport = true;
            }

            return new Answer(cmd, true, "Record removed");
        } catch (GloboDnsException e) {
            return new Answer(cmd, false, e.getMessage());
        } finally {
            if (needsExport) {
                scheduleExportChangesToBind();
            }
        }
    }

    public Answer execute(CreateOrUpdateRecordAndReverseCommand cmd) {
        boolean needsExport = false;
        try {
            Domain domain = searchDomain(cmd.getNetworkDomain(), false);
            if (domain == null) {
                domain = _globoDns.getDomainAPI().createDomain(cmd.getNetworkDomain(), cmd.getReverseTemplateId(), DEFAULT_AUTHORITY_TYPE);
                logger.warn("Domain " + cmd.getNetworkDomain() + " doesn't exist, maybe someone removed it. It was automatically created with template "
                        + cmd.getReverseTemplateId());
            }

            boolean created = createOrUpdateRecord(domain.getId(), cmd.getRecordName(), cmd.getRecordIp(), IPV4_RECORD_TYPE, cmd.isOverride());
            if (!created) {
                String msg = "Unable to create record " + cmd.getRecordName() + " at " + cmd.getNetworkDomain();
                if (!cmd.isOverride()) {
                    msg += ". Override record option is false, maybe record already exist.";
                }
                return new Answer(cmd, false, msg);
            } else {
                needsExport = true;
            }

            String reverseRecordContent = cmd.getRecordName() + '.' + cmd.getNetworkDomain();
            if (createOrUpdateReverse(cmd.getRecordIp(), reverseRecordContent, cmd.getReverseTemplateId(), cmd.isOverride())) {
                needsExport = true;
            } else {
                if (!cmd.isOverride()) {
                    String msg = "Unable to create reverse record " + cmd.getRecordName() + " for ip " + cmd.getRecordIp();
                    msg += ". Override record option is false, maybe record already exist.";
                    return new Answer(cmd, false, msg);
                }
            }

            return new Answer(cmd);
        } catch (GloboDnsException e) {
            return new Answer(cmd, false, e.getMessage());
        } finally {
            if (needsExport) {
                scheduleExportChangesToBind();
            }
        }
    }

    protected boolean createOrUpdateReverse(String networkIp, String reverseRecordContent, Long templateId, boolean override) {
        String reverseDomainName = generateReverseDomainNameFromNetworkIp(networkIp);
        Domain reverseDomain = searchDomain(reverseDomainName, true);
        if (reverseDomain == null) {
            reverseDomain = _globoDns.getDomainAPI().createReverseDomain(reverseDomainName, templateId, DEFAULT_AUTHORITY_TYPE);
            logger.info("Created reverse domain " + reverseDomainName + " with template " + templateId);
        }

        // create reverse
        String reverseRecordName = generateReverseRecordNameFromNetworkIp(networkIp);
        return createOrUpdateRecord(reverseDomain.getId(), reverseRecordName, reverseRecordContent, REVERSE_RECORD_TYPE, override);
    }

    public Answer execute(CreateOrUpdateDomainCommand cmd) {

        boolean needsExport = false;
        try {
            Domain domain = searchDomain(cmd.getDomainName(), false);
            if (domain == null) {
                // create
                domain = _globoDns.getDomainAPI().createDomain(cmd.getDomainName(), cmd.getTemplateId(), DEFAULT_AUTHORITY_TYPE);
                logger.info("Created domain " + cmd.getDomainName() + " with template " + cmd.getTemplateId());
                if (domain == null) {
                    return new Answer(cmd, false, "Unable to create domain " + cmd.getDomainName());
                } else {
                    needsExport = true;
                }
            } else {
                logger.warn("Domain " + cmd.getDomainName() + " already exist.");
            }
            return new Answer(cmd);
        } catch (GloboDnsException e) {
            return new Answer(cmd, false, e.getMessage());
        } finally {
            if (needsExport) {
                scheduleExportChangesToBind();
            }
        }
    }

    /**
     * Try to remove a record from bindZoneName. If record was removed returns true.
     * @param recordName
     * @param bindZoneName
     * @return true if record exists and was removed.
     */
    protected boolean removeRecord(String recordName, String recordValue, String bindZoneName, boolean reverse, boolean override) {
        Domain domain = searchDomain(bindZoneName, reverse);
        if (domain == null) {
            logger.warn("Domain " + bindZoneName + " doesn't exists in GloboDNS. Record " + recordName + " has already been removed.");
            return false;
        }
        Record record = searchRecord(recordName, domain.getId());
        if (record == null) {
            logger.warn("Record " + recordName + " in domain " + bindZoneName + " has already been removed.");
            return false;
        } else {
            if (!override && !record.getContent().equals(recordValue)) {
                logger.warn("Record " + recordName + " in domain " + bindZoneName + " have different value from " + recordValue
                        + " and override is not enable. I will not delete it.");
                return false;
            }
            _globoDns.getRecordAPI().removeRecord(record.getId());
        }

        return true;
    }

    /**
     * Create a new record in Zone, or update it if record has been exists.
     * @param domainId
     * @param name
     * @param ip
     * @param type
     * @return if record was created or updated.
     */
    private boolean createOrUpdateRecord(Long domainId, String name, String ip, String type, boolean override) {
        Record record = this.searchRecord(name, domainId);
        if (record == null) {
            // Create new record
            record = _globoDns.getRecordAPI().createRecord(domainId, name, ip, type);
            logger.info("Created record " + record.getName() + " in domain " + domainId);
        } else {
            if (!ip.equals(record.getContent())) {
                if (Boolean.TRUE.equals(override)) {
                    // ip is incorrect. Fix.
                    _globoDns.getRecordAPI().updateRecord(record.getId(), domainId, name, ip);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * GloboDns export all changes to Bind server.
     */
    public void scheduleExportChangesToBind() {
        try {
            Export export = _globoDns.getExportAPI().scheduleExport();
            if (export != null) {
                logger.info("GloboDns Export: " + export.getResult());
            }
        } catch (GloboDnsException e) {
            logger.warn("Error on scheduling export. Although everything was persist, someone need to manually force export in GloboDns", e);
        }
    }

    /**
     * Try to find bindZoneName in GloboDns.
     * @param name
     * @return Domain object or null if domain not exists.
     */
    private Domain searchDomain(String name, boolean reverse) {
        if (name == null) {
            return null;
        }
        List<Domain> candidates;
        if (reverse) {
            candidates = _globoDns.getDomainAPI().listReverseByQuery(name);
        } else {
            candidates = _globoDns.getDomainAPI().listByQuery(name);
        }
        for (Domain candidate : candidates) {
            if (name.equals(candidate.getName())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Find recordName in domain.
     * @param recordName
     * @param domainId Id of BindZoneName. Maybe you need use searchDomain before to use BindZoneName.
     * @return Record or null if not exists.
     */
    private Record searchRecord(String recordName, Long domainId) {
        if (recordName == null || domainId == null) {
            return null;
        }
        List<Record> candidates = _globoDns.getRecordAPI().listByQuery(domainId, recordName);
        // GloboDns search name in name and content. We need to iterate to check if recordName exists only in name
        for (Record candidate : candidates) {
            if (recordName.equalsIgnoreCase(candidate.getName())) {
                logger.debug("Record " + recordName + " in domain id " + domainId + " found in GloboDNS");
                return candidate;
            }
        }
        logger.debug("Record " + recordName + " in domain id " + domainId + " not found in GloboDNS");
        return null;
    }

    /**
     * Generate reverseBindZoneName of network. We ALWAYS use /24.
     * @param networkIp
     * @return Bind Zone Name reverse of network specified by networkIp
     */
    private String generateReverseDomainNameFromNetworkIp(String networkIp) {
        String[] octets = networkIp.split("\\.");
        String reverseDomainName = octets[2] + '.' + octets[1] + '.' + octets[0] + '.' + REVERSE_DOMAIN_SUFFIX;
        return reverseDomainName;
    }

    private String generateReverseRecordNameFromNetworkIp(String networkIp) {
        String[] octets = networkIp.split("\\.");
        String reverseRecordName = octets[3];
        return reverseRecordName;
    }

}
