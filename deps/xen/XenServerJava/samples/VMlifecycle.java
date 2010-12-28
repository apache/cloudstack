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
import java.util.Set;

import com.xensource.xenapi.Types;
import com.xensource.xenapi.VM;

/**
 * Takes a VM through the various lifecycle states. Requires a shutdown VM with tools installed.
 */
public class VMlifecycle extends TestBase
{
    protected static void RunTest(ILog logger, TargetServer server) throws Exception
    {
        TestBase.logger = logger;
        try
        {
            connect(server);

            // find a halted real virtual machine.
            Set<VM> refVMs = VM.getAll(connection);
            VM chosen = null;
            for (VM vm : refVMs)
            {
                VM.Record record = vm.getRecord(connection);
                if (!record.isATemplate && !record.isControlDomain && record.powerState == Types.VmPowerState.HALTED)
                {
                    chosen = vm;
                    break;
                }
            }

            if (chosen == null)
            {
                throw new Exception("We need a non-template, halted VM to clone. Can't find one, so aborting.");
            } else
            {
                // clone the vm we found, name it and set its description
                String cloneName = "Cloned by VMlifecycle.java";

                logln("We're cloning: " + chosen.getNameLabel(connection) + " to " + cloneName);

                VM cloneVM = chosen.createClone(connection, cloneName);
                cloneVM.setNameDescription(connection, "Created at " + new Date().toString());

                logf("VM Name: %s Description: %s\n", cloneVM.getNameLabel(connection), cloneVM
                        .getNameDescription(connection));
                printPowerState(cloneVM);

                // power-cycle it
                cloneVM.start(connection, true, false);
                printPowerState(cloneVM);
                cloneVM.unpause(connection);
                printPowerState(cloneVM);
                cloneVM.suspend(connection);
                printPowerState(cloneVM);
                cloneVM.resume(connection, false, false);
                printPowerState(cloneVM);
                cloneVM.cleanReboot(connection);
                printPowerState(cloneVM);
                cloneVM.cleanShutdown(connection);
                printPowerState(cloneVM);
            }
        } finally
        {
            disconnect();
        }
    }

    private static void printPowerState(VM vm) throws Exception
    {
        logln("VM powerstate: " + vm.getPowerState(connection));
    }
}
