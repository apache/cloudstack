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
import java.util.HashMap;
import java.util.Map;

import com.xensource.xenapi.Network;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;

/**
 * Makes a new VM from a built-in template, starts and stops it.
 */
public class AsyncVMCreate extends TestBase
{
    public static void RunTest(ILog logger, TargetServer target) throws Exception
    {
        TestBase.logger = logger;
        connect(target);
        try
        {
            createVM(new Date().toString() + " (made by AsyncVMCreate.java)");
        } finally
        {
            disconnect();
        }
    }

    private static void createVM(String newvmname) throws Exception
    {
        VM template = getFirstWindowsTemplate();
        logln("Template found: " + template.getNameLabel(connection));

        /* Clone the template */
        logln("Cloning the template...");
        Task cloning = template.createCloneAsync(connection, newvmname);
        waitForTask(connection, cloning, 500);
        checkForSuccess(cloning);
        VM newVm = Types.toVM(cloning, connection);
        logln("New VM clone: " + newVm.getNameLabel(connection));

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

        /* Now provision the disks */
        logln("provisioning... ");
        Task provisioning = newVm.provisionAsync(connection);
        waitForTask(connection, provisioning, 5000);
        checkForSuccess(provisioning);
        logln("provisioned");

        /* Should have done the trick. Let's see if it starts. */
        logln("Starting new VM.....");
        Task t = newVm.startAsync(connection, false, false);
        waitForTask(connection, t, 250);
        checkForSuccess(t);
        logln("started");

        /* and shut it down */
        logln("Shutting it down.....");
        t = newVm.cleanShutdownAsync(connection);
        waitForTask(connection, t, 500);
        logln("Shut down.");
    }

    /* Assert that a task has succeeded. Throw an exception if not */
    private static void checkForSuccess(Task task) throws Exception
    {
        if (task.getStatus(connection) == Types.TaskStatusType.SUCCESS)
        {
            logln("task succeeded");
        } else
        {
            throw new Exception("Task failed! Task record:\n" + task.getRecord(connection));
        }
    }

    /*
     * Create a VIF by making a VIF.record and then filling in the necessary
     * fields
     */
    private static VIF makeVIF(VM newVm, Network defaultNetwork, String device) throws Exception
    {
        VIF.Record newVifRecord = new VIF.Record();

        // These three parameters are used in the command line VIF creation
        newVifRecord.VM = newVm;
        newVifRecord.network = defaultNetwork;
        newVifRecord.device = device;

        // These appear to be necessary
        newVifRecord.MTU = 1500L;
        newVifRecord.qosAlgorithmType = "";
        newVifRecord.qosAlgorithmParams = new HashMap<String, String>();
        newVifRecord.otherConfig = new HashMap<String, String>();

        /* Create the VIF by asynchronous means */
        logln("Creating a VIF");
        Task task1 = VIF.createAsync(connection, newVifRecord);

        /*
         * Now deliberately screw things up by creating a second VIF with the
         * same parameters.
         */
        Task task2;
        log("Deliberately causing an error by trying to create the same VIF twice: ");
        task2 = VIF.createAsync(connection, newVifRecord);
        waitForTask(connection, task2, 0);
        /* This should all go through, but the task shouldn't have succeeded */
        try
        {
            checkForSuccess(task2);
        } catch (Exception e)
        {
            logln("Exception duly thrown");
        }

        /*
         * However, the first call should have worked, so we can get its result
         * and use that
         */
        waitForTask(connection, task1, 0);
        checkForSuccess(task1);
        return Types.toVIF(task1, connection);
    }
}
