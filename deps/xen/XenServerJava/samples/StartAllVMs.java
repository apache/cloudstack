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

import java.util.Map;

import com.xensource.xenapi.Types;
import com.xensource.xenapi.VM;

/**
 * Connects to a host and tries to start each VM on it.
 */
public class StartAllVMs extends TestBase
{
    public static void RunTest(ILog logger, TargetServer server) throws Exception
    {
        TestBase.logger = logger;
        try
        {
            connect(server);
            logln("We'll try to start all the available VMs. Since most of them will be templates");
            logln("This should cause a fair number of exceptions");
            startAllVMs();
        } finally
        {
            disconnect();
        }
    }

    private static void startAllVMs() throws Types.XenAPIException, org.apache.xmlrpc.XmlRpcException
    {
        announce("Getting all VM records");
        Map<VM, VM.Record> vms = VM.getAllRecords(connection);
        logln("got: " + vms.size() + " records");

        announce("Start all the available VMs");
        for (VM.Record record : vms.values())
        {
            log("Trying to start: " + record.nameLabel);
            if (record.isATemplate)
                log("(template) ");
            try
            {
                VM vm = VM.getByUuid(connection, record.uuid);
                vm.start(connection, false, false);
                logln(" -- success!");
            } catch (Types.VmIsTemplate ex)
            {
                if (record.isATemplate)
                    logln(" -- expected failure: can't start a template.");
                else
                    throw ex;
            } catch (Types.NoHostsAvailable ex)
            {
                logln(" -- predictable failure: insufficient host capacity to start the VM");
            } catch (Types.OperationNotAllowed ex)
            {
                if (record.isControlDomain)
                    logln(" -- expected failure: can't start the control domain");
                else
                    throw ex;
            } catch (Types.VmBadPowerState ex)
            {
                if (record.powerState != Types.VmPowerState.HALTED)
                    logln(" -- expected failure: bad power state (actual: " + ex.actual + " expected: " + ex.expected
                            + ")");
                else
                    throw ex;
            } catch (Types.LicenceRestriction ex)
            {
                logln(" -- predictable failure: licence restriction");
            } catch (Types.BootloaderFailed ex)
            {
                logln(" -- predictable failure: the vm would not boot (" + ex + ")");
            }
        }

        hRule();
    }
}
