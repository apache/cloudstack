/**
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
package com.cloud.hypervisor.xen.resource;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.SecurityIngressRuleAnswer;
import com.cloud.agent.api.SecurityIngressRulesCmd;
import com.cloud.hypervisor.xen.resource.CitrixResourceBase;
import com.xensource.xenapi.VM;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types.XenAPIException;


@Local(value=ServerResource.class)
public class XenServerResource extends CitrixResourceBase {
    private static final Logger s_logger = Logger.getLogger(XenServerResource.class);
    
    public XenServerResource() {
        super();
    }
    
    @Override
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
    	return CitrixHelper.getXenServerGuestOsType(stdType);
    }

    @Override
    protected void setMemory(Connection conn, VM vm, long memsize) throws XmlRpcException, XenAPIException {
        vm.setMemoryLimits(conn, memsize, memsize, memsize, memsize);
    }   
    
    @Override
    protected List<File> getPatchFiles() {      
        List<File> files = new ArrayList<File>();
        String patch = "scripts/vm/hypervisor/xenserver/xenserver56/patch";      
        String patchfilePath = Script.findScript("" , patch);
        if ( patchfilePath == null ) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        File file = new File(patchfilePath);
        files.add(file);
        return files;
    }


}
