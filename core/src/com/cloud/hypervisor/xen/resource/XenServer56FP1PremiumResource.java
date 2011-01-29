package com.cloud.hypervisor.xen.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;


@Local(value=ServerResource.class)
public class XenServer56FP1PremiumResource extends XenServer56Resource {
    private final static Logger s_logger = Logger.getLogger(XenServer56FP1PremiumResource.class);
    public XenServer56FP1PremiumResource() {
        super();
    }
    
    @Override
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
        return CitrixHelper.getXenServer56FP1GuestOsType(stdType);
    }

    @Override
    protected List<File> getPatchFiles() {      
        List<File> files = new ArrayList<File>();
        String patch = "scripts/vm/hypervisor/xenserver/xenserver56fp1/patch";      
        String patchfilePath = Script.findScript("" , patch);
        if ( patchfilePath == null ) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        File file = new File(patchfilePath);
        files.add(file);
        patch = "premium-scripts/vm/hypervisor/xenserver/xenserver56fp1/patch";      
        patchfilePath = Script.findScript("" , patch);
        if (patchfilePath == null) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        file = new File(patchfilePath);
        files.add(file);
        return files;
    }

}