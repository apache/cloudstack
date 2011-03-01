package com.cloud.hypervisor.xen.resource;

import java.util.Random;

import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.VM;

public class XenServer56FP1SDKResource extends XenServer56FP1Resource {



    @Override
    protected StartAnswer execute(StartCommand cmd) {
        // TODO Auto-generated method stub
        return super.execute(cmd);
    }

    @Override
    protected GetVmStatsAnswer execute(GetVmStatsCommand cmd) {
        // TODO Auto-generated method stub
        return super.execute(cmd);
    }

    @Override
    protected Answer execute(RebootCommand cmd) {
        // TODO Auto-generated method stub
        return super.execute(cmd);
    }

    @Override
    protected StopAnswer execute(StopCommand cmd) {
        // TODO Auto-generated method stub
        return super.execute(cmd);
    }

    @Override
    protected String callHostPlugin(Connection conn, String plugin, String cmd,
            String... params) {
        // TODO Auto-generated method stub
        return super.callHostPlugin(conn, plugin, cmd, params);
    }

    @Override
    protected boolean can_bridge_firewall(Connection conn) {
        return true;
    }

}
