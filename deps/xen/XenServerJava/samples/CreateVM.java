/*
 * Copyright (c) 2008 Citrix Systems, Inc.
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

import java.util.Date;
import java.util.Map;

import com.xensource.xenapi.Network;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;

/**
 * Creates a VM on the default SR with a network and DVD drive.
 */
public class CreateVM extends TestBase
{
    public static void RunTest(ILog logger, TargetServer server) throws Exception
    {
        TestBase.logger = logger;
        try
        {
            connect(server);
            createVM(new Date().toString() + " (made by CreateVM.java)");
        } finally
        {
            disconnect();
        }
    }

    private static void createVM(String newVmName) throws Exception
    {
        VM template = getFirstWindowsTemplate();
        logln("Template found: " + template.getNameLabel(connection));

        /* Clone the template */
        VM newVm = template.createClone(connection, newVmName);
        logln("New clone: " + newVm.getNameLabel(connection));

        /* Find a storage repository */
        SR defaultSR = getDefaultSR();
        logln("Default SR: " + defaultSR.getNameLabel(connection));

        /* Find a network */
        Network network = getFirstNetwork();
        logln("Network chosen: " + network.getNameLabel(connection));

        /*
         * We have our clone and our network, attach them to each other with a
         * VIF
         */
        makeVIF(newVm, network, "0");

        /* Put the SR uuid into the provision XML */
        Map<String, String> otherConfig = newVm.getOtherConfig(connection);
        String disks = otherConfig.get("disks");
        disks = disks.replace("sr=\"\"", "sr=\"" + defaultSR.getUuid(connection) + "\"");
        otherConfig.put("disks", disks);
        newVm.setOtherConfig(connection, otherConfig);

        makeCDDrive(newVm);

        /* Now provision the disks */
        log("provisioning... ");
        newVm.provision(connection);
        logln("provisioned");

        /* Should have done the trick. Let's see if it starts. */
        logln("Starting new VM.....");
        newVm.start(connection, false, false);

        logln("Shutting it down (hard).....");
        newVm.hardShutdown(connection);
    }

    /*
     * Create a VIF by making a VIF.record and then filling in the necessary
     * fields
     */
    private static VIF makeVIF(VM newVm, Network network, String device) throws Exception
    {
        VIF.Record newvifrecord = new VIF.Record();

        // These three parameters are used in the command line VIF creation
        newvifrecord.VM = newVm;
        newvifrecord.network = network;
        newvifrecord.device = device;
        newvifrecord.MTU = 1500L;

        return VIF.create(connection, newvifrecord);
    }

    private static VBD makeCDDrive(VM vm) throws Exception
    {
        VBD.Record vbdrecord = new VBD.Record();

        vbdrecord.VM = vm;
        vbdrecord.VDI = null;
        vbdrecord.userdevice = "3";
        vbdrecord.mode = Types.VbdMode.RO;
        vbdrecord.type = Types.VbdType.CD;
        vbdrecord.empty = true;

        return VBD.create(connection, vbdrecord);
    }
}
