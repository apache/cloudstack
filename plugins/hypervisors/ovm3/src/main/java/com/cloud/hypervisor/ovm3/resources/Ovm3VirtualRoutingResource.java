package com.cloud.hypervisor.ovm3.resources;

import org.apache.log4j.Logger;

import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.resource.virtualnetwork.VirtualRouterDeployer;
import com.cloud.hypervisor.ovm3.objects.CloudStackPlugin;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Xen;
import com.cloud.utils.ExecutionResult;

public class Ovm3VirtualRoutingResource implements VirtualRouterDeployer {
    private static final Logger LOGGER = Logger
            .getLogger(Ovm3VirtualRoutingResource.class);
    private String domRCloudPath = "/opt/cloud/bin/";
    private static final int VRTIMEOUT = 600;
    private Connection c;
    private String agentName;
    public Ovm3VirtualRoutingResource(Connection conn) {
        c = conn;
        agentName=c.getIp();
    }

    @Override
    public ExecutionResult executeInVR(String routerIp, String script,
            String args) {
        return executeInVR(routerIp, script, args, VRTIMEOUT);
    }

    @Override
    public ExecutionResult executeInVR(String routerIp, String script,
            String args, int timeout) {
        if (!script.contains(domRCloudPath)) {
            script = domRCloudPath + "/" + script;
        }
        String cmd = script + " " + args;
        LOGGER.debug("executeInVR via " + agentName + " on " + routerIp + ": "
                + cmd);
        try {
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            CloudStackPlugin.ReturnCode result;
            result = cSp.domrExec(routerIp, cmd);
            return new ExecutionResult(result.getRc(), result.getStdOut());
        } catch (Exception e) {
            LOGGER.error("executeInVR FAILED via " + agentName + " on "
                    + routerIp + ":" + cmd + ", " + e.getMessage(), e);
        }
        return new ExecutionResult(false, "");
    }

    @Override
    public ExecutionResult createFileInVR(String routerIp, String path,
            String filename, String content) {
        String error = null;
        LOGGER.debug("createFileInVR via " + agentName + " on " + routerIp
                + ": " + path + "/" + filename + ", content: " + content);
        try {
            CloudStackPlugin cSp = new CloudStackPlugin(c);
            boolean result = cSp.ovsDomrUploadFile(routerIp, path, filename,
                    content);
            return new ExecutionResult(result, "");
        } catch (Exception e) {
            error = e.getMessage();
            LOGGER.warn(
                    "createFileInVR failed for " + path + "/" + filename
                            + " in VR " + routerIp + " via " + agentName + ": "
                            + error, e);
        }
        return new ExecutionResult(error == null, error);
    }

    @Override
    public ExecutionResult prepareCommand(NetworkElementCommand cmd) {
        // Update IP used to access router
        cmd.setRouterAccessIp(cmd
                .getAccessDetail(NetworkElementCommand.ROUTER_IP));
        assert cmd.getRouterAccessIp() != null;

        if (cmd instanceof IpAssocVpcCommand) {
            return prepareNetworkElementCommand((IpAssocVpcCommand) cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return prepareNetworkElementCommand((IpAssocCommand) cmd);
        } else if (cmd instanceof SetupGuestNetworkCommand) {
            return prepareNetworkElementCommand((SetupGuestNetworkCommand) cmd);
        } else if (cmd instanceof SetSourceNatCommand) {
            return prepareNetworkElementCommand((SetSourceNatCommand) cmd);
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult cleanupCommand(NetworkElementCommand cmd) {
        if (cmd instanceof IpAssocCommand
                && !(cmd instanceof IpAssocVpcCommand)) {
            return cleanupNetworkElementCommand((IpAssocCommand) cmd);
        }
        return new ExecutionResult(true, null);
    }

    private ExecutionResult cleanupNetworkElementCommand(IpAssocCommand cmd) {
        return new ExecutionResult(true, null);
    }

    /* TODO: fill in these so we can get rid of the VR/SVM/etc specifics */
    private ExecutionResult prepareNetworkElementCommand(
            SetupGuestNetworkCommand cmd) {
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocVpcCommand cmd) {
        String routerName = cmd
                .getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        Xen xen = new Xen(c);
        try {
            Xen.Vm vm = xen.getVmConfig(routerName);
            IpAddressTO[] ips = cmd.getIpAddresses();
            for (IpAddressTO ip : ips) {
                Integer devId = vm.getVifIdByMac(ip.getVifMacAddress());
                if (devId < 0) {
                    LOGGER.error("No valid devId found for " + vm.getVmName()
                            + " with " + ip.getVifMacAddress());
                    return new ExecutionResult(false, null);
                }
                ip.setNicDevId(devId);
            }
        } catch (Exception e) {
            LOGGER.error(
                    "Ip Assoc failure on applying one ip due to exception:  ",
                    e);
            return new ExecutionResult(false, null);
        }
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(SetSourceNatCommand cmd) {
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocCommand cmd) {
        return new ExecutionResult(true, null);
    }

}
