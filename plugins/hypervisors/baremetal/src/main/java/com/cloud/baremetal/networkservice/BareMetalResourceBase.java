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
//
// Automatically generated by addcopyright.py at 01/29/2013
// Apache License, Version 2.0 (the "License"); you may not use this
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//
// Automatically generated by addcopyright.py at 04/03/2012
package com.cloud.baremetal.networkservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.baremetal.IpmISetBootDevCommand;
import com.cloud.agent.api.baremetal.IpmISetBootDevCommand.BootDev;
import com.cloud.agent.api.baremetal.IpmiBootorResetCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.baremetal.manager.BaremetalManager;
import com.cloud.configuration.Config;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ServerResource;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.cloud.utils.script.Script2;
import com.cloud.utils.script.Script2.ParamType;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.PowerState;
import com.cloud.vm.dao.VMInstanceDao;

public class BareMetalResourceBase extends ManagerBase implements ServerResource {
    protected String _uuid;
    protected String _zone;
    protected String _pod;
    protected Long hostId;
    protected String _cluster;
    protected long _memCapacity;
    protected long _cpuCapacity;
    protected long _cpuNum;
    protected String _mac;
    protected String _username;
    protected String _password;
    protected String _ip;
    protected boolean _isEchoScAgent;
    protected IAgentControl _agentControl;
    protected Script2 _pingCommand;
    protected Script2 _setPxeBootCommand;
    protected Script2 _setDiskBootCommand;
    protected Script2 _rebootCommand;
    protected Script2 _getStatusCommand;
    protected Script2 _powerOnCommand;
    protected Script2 _powerOffCommand;
    protected Script2 _forcePowerOffCommand;
    protected Script2 _bootOrRebootCommand;
    protected String _vmName;
    protected int ipmiRetryTimes = 5;
    protected boolean provisionDoneNotificationOn = false;
    protected int isProvisionDoneNotificationTimeout = 1800;

    protected ConfigurationDao configDao;
    protected VMInstanceDao vmDao;


    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        setName(name);
        _uuid = (String) params.get("guid");
        try {
            _memCapacity = Long.parseLong((String) params.get(ApiConstants.MEMORY)) * 1024L * 1024L;
            _cpuCapacity = Long.parseLong((String) params.get(ApiConstants.CPU_SPEED));
            _cpuNum = Long.parseLong((String) params.get(ApiConstants.CPU_NUMBER));
        } catch (NumberFormatException e) {
            throw new ConfigurationException(String.format("Unable to parse number of CPU or memory capacity "
                    + "or cpu capacity(cpu number = %1$s memCapacity=%2$s, cpuCapacity=%3$s", params.get(ApiConstants.CPU_NUMBER),
                    params.get(ApiConstants.MEMORY), params.get(ApiConstants.CPU_SPEED)));
        }

        _zone = (String) params.get("zone");
        _pod = (String) params.get("pod");
        _cluster = (String) params.get("cluster");
        hostId = (Long) params.get("hostId");
        _ip = (String) params.get(ApiConstants.PRIVATE_IP);
        _mac = (String) params.get(ApiConstants.HOST_MAC);
        _username = (String) params.get(ApiConstants.USERNAME);
        _password = (String) params.get(ApiConstants.PASSWORD);
        _vmName = (String) params.get("vmName");
        String echoScAgent = (String) params.get(BaremetalManager.EchoSecurityGroupAgent);
        vmDao = (VMInstanceDao) params.get("vmDao");
        configDao = (ConfigurationDao) params.get("configDao");

        if (_pod == null) {
            throw new ConfigurationException("Unable to get the pod");
        }

        if (_cluster == null) {
            throw new ConfigurationException("Unable to get the pod");
        }

        if (_ip == null) {
            throw new ConfigurationException("Unable to get the host address");
        }

        if (_mac.equalsIgnoreCase("unknown")) {
            throw new ConfigurationException("Unable to get the host mac address");
        }

        if (_mac.split(":").length != 6) {
            throw new ConfigurationException("Wrong MAC format(" + _mac
                    + "). It must be in format of for example 00:11:ba:33:aa:dd which is not case sensitive");
        }

        if (_uuid == null) {
            throw new ConfigurationException("Unable to get the uuid");
        }

        if (echoScAgent != null) {
            _isEchoScAgent = Boolean.valueOf(echoScAgent);
        }

        String ipmiIface = "default";
        try {
            ipmiIface = configDao.getValue(Config.BaremetalIpmiLanInterface.key());
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
        }

        try {
            ipmiRetryTimes = Integer.parseInt(configDao.getValue(Config.BaremetalIpmiRetryTimes.key()));
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
        }

        try {
            provisionDoneNotificationOn = Boolean.valueOf(configDao.getValue(Config.BaremetalProvisionDoneNotificationEnabled.key()));
            isProvisionDoneNotificationTimeout = Integer.parseInt(configDao.getValue(Config.BaremetalProvisionDoneNotificationTimeout.key()));
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
        }

        String injectScript = "scripts/util/ipmi.py";
        String scriptPath = Script.findScript("", injectScript);
        if (scriptPath == null) {
            throw new ConfigurationException("Cannot find ping script " + scriptPath);
        }
        String pythonPath = "/usr/bin/python";
        _pingCommand = new Script2(pythonPath, logger);
        _pingCommand.add(scriptPath);
        _pingCommand.add("ping");
        _pingCommand.add("interface=" + ipmiIface);
        _pingCommand.add("hostname=" + _ip);
        _pingCommand.add("usrname=" + _username);
        _pingCommand.add("password=" + _password, ParamType.PASSWORD);

        _setPxeBootCommand = new Script2(pythonPath, logger);
        _setPxeBootCommand.add(scriptPath);
        _setPxeBootCommand.add("boot_dev");
        _setPxeBootCommand.add("interface=" + ipmiIface);
        _setPxeBootCommand.add("hostname=" + _ip);
        _setPxeBootCommand.add("usrname=" + _username);
        _setPxeBootCommand.add("password=" + _password, ParamType.PASSWORD);
        _setPxeBootCommand.add("dev=pxe");

        _setDiskBootCommand = new Script2(pythonPath, logger);
        _setDiskBootCommand.add(scriptPath);
        _setDiskBootCommand.add("boot_dev");
        _setDiskBootCommand.add("interface=" + ipmiIface);
        _setDiskBootCommand.add("hostname=" + _ip);
        _setDiskBootCommand.add("usrname=" + _username);
        _setDiskBootCommand.add("password=" + _password, ParamType.PASSWORD);
        _setDiskBootCommand.add("dev=disk");

        _rebootCommand = new Script2(pythonPath, logger);
        _rebootCommand.add(scriptPath);
        _rebootCommand.add("reboot");
        _rebootCommand.add("interface=" + ipmiIface);
        _rebootCommand.add("hostname=" + _ip);
        _rebootCommand.add("usrname=" + _username);
        _rebootCommand.add("password=" + _password, ParamType.PASSWORD);

        _getStatusCommand = new Script2(pythonPath, logger);
        _getStatusCommand.add(scriptPath);
        _getStatusCommand.add("ping");
        _getStatusCommand.add("interface=" + ipmiIface);
        _getStatusCommand.add("hostname=" + _ip);
        _getStatusCommand.add("usrname=" + _username);
        _getStatusCommand.add("password=" + _password, ParamType.PASSWORD);

        _powerOnCommand = new Script2(pythonPath, logger);
        _powerOnCommand.add(scriptPath);
        _powerOnCommand.add("power");
        _powerOnCommand.add("interface=" + ipmiIface);
        _powerOnCommand.add("hostname=" + _ip);
        _powerOnCommand.add("usrname=" + _username);
        _powerOnCommand.add("password=" + _password, ParamType.PASSWORD);
        _powerOnCommand.add("action=on");

        _powerOffCommand = new Script2(pythonPath, logger);
        _powerOffCommand.add(scriptPath);
        _powerOffCommand.add("power");
        _powerOffCommand.add("interface=" + ipmiIface);
        _powerOffCommand.add("hostname=" + _ip);
        _powerOffCommand.add("usrname=" + _username);
        _powerOffCommand.add("password=" + _password, ParamType.PASSWORD);
        _powerOffCommand.add("action=soft");

        _forcePowerOffCommand = new Script2(pythonPath, logger);
        _forcePowerOffCommand.add(scriptPath);
        _forcePowerOffCommand.add("power");
        _forcePowerOffCommand.add("interface=" + ipmiIface);
        _forcePowerOffCommand.add("hostname=" + _ip);
        _forcePowerOffCommand.add("usrname=" + _username);
        _forcePowerOffCommand.add("password=" + _password, ParamType.PASSWORD);
        _forcePowerOffCommand.add("action=off");

        _bootOrRebootCommand = new Script2(pythonPath, logger);
        _bootOrRebootCommand.add(scriptPath);
        _bootOrRebootCommand.add("boot_or_reboot");
        _bootOrRebootCommand.add("interface=" + ipmiIface);
        _bootOrRebootCommand.add("hostname=" + _ip);
        _bootOrRebootCommand.add("usrname=" + _username);
        _bootOrRebootCommand.add("password=" + _password, ParamType.PASSWORD);

        return true;
    }

    protected boolean doScript(Script cmd) {
        return doScript(cmd, null);
    }

    protected boolean doScript(Script cmd, int retry) {
        return doScript(cmd, null, retry);
    }

    protected boolean doScript(Script cmd, OutputInterpreter interpreter) {
        return doScript(cmd, interpreter, ipmiRetryTimes);
    }

    protected boolean doScript(Script cmd, OutputInterpreter interpreter, int retry) {
        String res = null;
        while (retry-- > 0) {
            if (interpreter == null) {
                res = cmd.execute();
            } else {
                res = cmd.execute(interpreter);
            }
            if (res != null && res.startsWith("Error: Unable to establish LAN")) {
                logger.warn("IPMI script timeout(" + cmd.toString() + "), will retry " + retry + " times");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    logger.debug("[ignored] interrupted while waiting to retry running script.");
                }
                continue;
            } else if (res == null) {
                return true;
            } else {
                break;
            }
        }

        logger.warn("IPMI Scirpt failed due to " + res + "(" + cmd.toString() + ")");
        return false;
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
        return com.cloud.host.Host.Type.Routing;
    }

    protected Map<String, HostVmStateReportEntry> getHostVmStateReport() {
        Map<String, HostVmStateReportEntry> states = new HashMap<String, HostVmStateReportEntry>();
        if (hostId != null) {
            final List<? extends VMInstanceVO> vms = vmDao.listByHostId(hostId);
            for (VMInstanceVO vm : vms) {
                states.put(
                    vm.getInstanceName(),
                    new HostVmStateReportEntry(
                        vm.getPowerState(), "host-" + hostId
                    )
                );
            }
        }
        return states;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupRoutingCommand cmd = new StartupRoutingCommand(0, 0, 0, 0, null, Hypervisor.HypervisorType.BareMetal,
            new HashMap<String, String>());

        cmd.setDataCenter(_zone);
        cmd.setPod(_pod);
        cmd.setCluster(_cluster);
        cmd.setGuid(_uuid);
        cmd.setName(_ip);
        cmd.setPrivateIpAddress(_ip);
        cmd.setStorageIpAddress(_ip);
        cmd.setVersion(BareMetalResourceBase.class.getPackage().getImplementationVersion());
        cmd.setCpus((int) _cpuNum);
        cmd.setSpeed(_cpuCapacity);
        cmd.setMemory(_memCapacity);
        cmd.setPrivateMacAddress(_mac);
        cmd.setPublicMacAddress(_mac);
        return new StartupCommand[] { cmd };
    }

    private boolean ipmiPing() {
        return doScript(_pingCommand);
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        try {
            if (!ipmiPing()) {
                Thread.sleep(1000);
                if (!ipmiPing()) {
                    logger.warn("Cannot ping ipmi nic " + _ip);
                    return null;
                }
            }
        } catch (Exception e) {
            logger.debug("Cannot ping ipmi nic " + _ip, e);
            return null;
        }

        return new PingRoutingCommand(getType(), id, null);

            /*
        if (hostId != null) {
            final List<? extends VMInstanceVO> vms = vmDao.listByHostId(hostId);
            if (vms.isEmpty()) {
                return new PingRoutingCommand(getType(), id, null);
            } else {
                VMInstanceVO vm = vms.get(0);
                SecurityGroupHttpClient client = new SecurityGroupHttpClient();
                HashMap<String, Pair<Long, Long>> nwGrpStates = client.sync(vm.getInstanceName(), vm.getId(), vm.getPrivateIpAddress());
                return new PingRoutingWithNwGroupsCommand(getType(), id, null, nwGrpStates);
            }
        } else {
            return new PingRoutingCommand(getType(), id, null);
        }
            */
    }

    protected Answer execute(IpmISetBootDevCommand cmd) {
        Script bootCmd = null;
        if (cmd.getBootDev() == BootDev.disk) {
            bootCmd = _setDiskBootCommand;
        } else if (cmd.getBootDev() == BootDev.pxe) {
            bootCmd = _setPxeBootCommand;
        } else {
            throw new CloudRuntimeException("Unknown boot dev " + cmd.getBootDev());
        }

        String bootDev = cmd.getBootDev().name();
        if (!doScript(bootCmd)) {
            logger.warn("Set " + _ip + " boot dev to " + bootDev + "failed");
            return new Answer(cmd, false, "Set " + _ip + " boot dev to " + bootDev + "failed");
        }

        logger.warn("Set " + _ip + " boot dev to " + bootDev + "Success");
        return new Answer(cmd, true, "Set " + _ip + " boot dev to " + bootDev + "Success");
    }

    protected MaintainAnswer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd, false);
    }

    protected PrepareForMigrationAnswer execute(PrepareForMigrationCommand cmd) {
        return new PrepareForMigrationAnswer(cmd);
    }

    protected MigrateAnswer execute(MigrateCommand cmd) {
        if (!doScript(_powerOffCommand)) {
            return new MigrateAnswer(cmd, false, "IPMI power off failed", null);
        }
        return new MigrateAnswer(cmd, true, "success", null);
    }

    protected CheckVirtualMachineAnswer execute(final CheckVirtualMachineCommand cmd) {
        return new CheckVirtualMachineAnswer(cmd, PowerState.PowerOff, null);
    }

    protected Answer execute(IpmiBootorResetCommand cmd) {
        if (!doScript(_bootOrRebootCommand)) {
            return new Answer(cmd, false, "IPMI boot or reboot failed");
        }
        return new Answer(cmd, true, "Success");

    }

    protected CheckNetworkAnswer execute(CheckNetworkCommand cmd) {
        return new CheckNetworkAnswer(cmd, true, "Success");
    }

    protected Answer execute(SecurityGroupRulesCmd cmd) {
        SecurityGroupHttpClient hc = new SecurityGroupHttpClient();
        return hc.call(cmd.getGuestIp(), cmd);
    }

    @Override
    public Answer executeRequest(Command cmd) {
        try {
            if (cmd instanceof ReadyCommand) {
                return execute((ReadyCommand) cmd);
            } else if (cmd instanceof StartCommand) {
                return execute((StartCommand) cmd);
            } else if (cmd instanceof StopCommand) {
                return execute((StopCommand) cmd);
            } else if (cmd instanceof RebootCommand) {
                return execute((RebootCommand) cmd);
            } else if (cmd instanceof IpmISetBootDevCommand) {
                return execute((IpmISetBootDevCommand) cmd);
            } else if (cmd instanceof MaintainCommand) {
                return execute((MaintainCommand) cmd);
            } else if (cmd instanceof PrepareForMigrationCommand) {
                return execute((PrepareForMigrationCommand) cmd);
            } else if (cmd instanceof MigrateCommand) {
                return execute((MigrateCommand) cmd);
            } else if (cmd instanceof CheckVirtualMachineCommand) {
                return execute((CheckVirtualMachineCommand) cmd);
            } else if (cmd instanceof IpmiBootorResetCommand) {
                return execute((IpmiBootorResetCommand) cmd);
            } else if (cmd instanceof SecurityGroupRulesCmd) {
                return execute((SecurityGroupRulesCmd) cmd);
            } else if (cmd instanceof CheckNetworkCommand) {
                return execute((CheckNetworkCommand) cmd);
            } else {
                return Answer.createUnsupportedCommandAnswer(cmd);
            }
        } catch (Throwable t) {
            logger.debug(t.getMessage(), t);
            return new Answer(cmd, false, t.getMessage());
        }
    }

    protected boolean isPowerOn(String str) {
        if (str.startsWith("Chassis Power is on")) {
            return true;
        } else if (str.startsWith("Chassis Power is off")) {
            return false;
        } else {
            throw new CloudRuntimeException("Cannot parse IPMI power status " + str);
        }
    }

    protected RebootAnswer execute(final RebootCommand cmd) {
        String infoStr = "Command not supported in present state";
        OutputInterpreter.AllLinesParser interpreter = new OutputInterpreter.AllLinesParser();
        if (!doScript(_rebootCommand, interpreter, 10)) {
            if (interpreter.getLines().contains(infoStr)) {
                // try again, this error should be temporary
                if (!doScript(_rebootCommand, interpreter, 10)) {
                    return new RebootAnswer(cmd, "IPMI reboot failed", false);
                }
            } else {
                return new RebootAnswer(cmd, "IPMI reboot failed", false);
            }
        }

        return new RebootAnswer(cmd, "reboot succeeded", true);
    }

    protected StopAnswer execute(final StopCommand cmd) {
        boolean success = false;
        int count = 0;
        Script powerOff = _powerOffCommand;

        while (count < 10) {
            if (!doScript(powerOff)) {
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }

            OutputInterpreter.AllLinesParser interpreter = new OutputInterpreter.AllLinesParser();
            if (!doScript(_getStatusCommand, interpreter)) {
                success = true;
                logger.warn("Cannot get power status of " + getName() + ", assume VM state changed successfully");
                break;
            }

            if (!isPowerOn(interpreter.getLines())) {
                success = true;
                break;
            } else {
                powerOff = _forcePowerOffCommand;
            }

            count++;
        }

        return success ? new StopAnswer(cmd, "Success", true) : new StopAnswer(cmd, "IPMI power off failed", false);
    }

    protected StartAnswer execute(StartCommand cmd) {
        VirtualMachineTO vm = cmd.getVirtualMachine();

        OutputInterpreter.AllLinesParser interpreter = new OutputInterpreter.AllLinesParser();
        if (!doScript(_getStatusCommand, interpreter)) {
            return new StartAnswer(cmd, "Cannot get current power status of " + getName());
        }

        if (isPowerOn(interpreter.getLines())) {
            if (!doScript(_rebootCommand)) {
                return new StartAnswer(cmd, "IPMI reboot failed");
            }
        } else {
            if (!doScript(_powerOnCommand)) {
                return new StartAnswer(cmd, "IPMI power on failed");
            }
        }

        if (_isEchoScAgent) {
            SecurityGroupHttpClient hc = new SecurityGroupHttpClient();
            boolean echoRet = hc.echo(vm.getNics()[0].getIp(), TimeUnit.MINUTES.toMillis(30), TimeUnit.MINUTES.toMillis(1));
            if (!echoRet) {
                return new StartAnswer(cmd, String.format("Call security group agent on vm[%s] timeout", vm.getNics()[0].getIp()));
            }
        }

        if (provisionDoneNotificationOn) {
            QueryBuilder<VMInstanceVO> q = QueryBuilder.create(VMInstanceVO.class);
            q.and(q.entity().getInstanceName(), SearchCriteria.Op.EQ, vm.getName());
            VMInstanceVO vmvo = q.find();

            if (vmvo.getLastHostId() == null) {
                // this is new created vm
                long timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(isProvisionDoneNotificationTimeout);
                while (timeout > System.currentTimeMillis()) {
                    try {
                        TimeUnit.SECONDS.sleep(5);
                    } catch (InterruptedException e) {
                        logger.warn(e.getMessage(), e);
                    }

                    q = QueryBuilder.create(VMInstanceVO.class);
                    q.and(q.entity().getInstanceName(), SearchCriteria.Op.EQ, vm.getName());
                    vmvo = q.find();
                    if (vmvo == null) {
                        return new StartAnswer(cmd, String.format("cannot find vm[name:%s] while waiting for baremtal provision done notification", vm.getName()));
                    }

                    if (VirtualMachine.State.Running == vmvo.getState()) {
                        return new StartAnswer(cmd);
                    }

                    logger.debug(String.format("still wait for baremetal provision done notification for vm[name:%s], current vm state is %s", vmvo.getInstanceName(), vmvo.getState()));
                }

                return new StartAnswer(cmd, String.format("timeout after %s seconds, no baremetal provision done notification received. vm[name:%s] failed to start", isProvisionDoneNotificationTimeout, vm.getName()));
            }
        }

        logger.debug("Start bare metal vm " + vm.getName() + "successfully");
        _vmName = vm.getName();
        return new StartAnswer(cmd);
    }

    protected ReadyAnswer execute(ReadyCommand cmd) {
        // derived resource should check if the PXE server is ready
        logger.debug("Bare metal resource " + getName() + " is ready");
        return new ReadyAnswer(cmd);
    }

    @Override
    public void disconnected() {

    }

    @Override
    public IAgentControl getAgentControl() {
        return _agentControl;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        _agentControl = agentControl;
    }

}
