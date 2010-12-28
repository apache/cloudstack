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
import java.util.Set;

import com.xensource.xenapi.Host;
import com.xensource.xenapi.PBD;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types.XenAPIException;

/**
 * Creates a shared NFS SR.
 * 
 * java equivalent to the cli command: xe sr-create type=nfs name-label=dummy device-config-server=<nfsServer>
 * device-config-serverpath=<serverPath> shared=true
 */
public class SharedStorage extends TestBase
{
    public static void RunTest(ILog logger, TargetServer server, String nfsServer, String serverPath) throws Exception
    {
        TestBase.logger = logger;
        try
        {
            connect(server);

            logln("getting list of hosts...");

            // find first host
            Host host = (Host) Host.getAll(connection).toArray()[0];

            log("choosing the first one...");

            // get its name
            logln(host.getNameLabel(connection));

            // create config parameter for shared storage on nfs server
            Map<String, String> deviceConfig = new HashMap<String, String>();
            deviceConfig.put("server", nfsServer);
            deviceConfig.put("serverpath", serverPath);

            logln("creating a shared storage SR ...");
            // make a shared disk there
            long size = 100000L;
            String name = "NFS SR created by SharedStorage.java";
            String desc = "[" + nfsServer + ":" + serverPath + "] Created at " + new Date().toString();
            String type = "nfs";
            String contentType = "unused";
            boolean shared = true;
            SR newSr = SR.create(connection, host, deviceConfig, size, name, desc, type, contentType, shared,
                    new HashMap<String, String>());

            // try a couple of dodgy calls to generate exceptions
            logln("now trying to create one with bad device_config");
            logln("should throw exception");
            try
            {
                // fail for bad device_config
                SR.create(connection, host, new HashMap<String, String>(), 100000L, "name", "description", "nfs",
                        "contenttype", true, new HashMap<String, String>());
            } catch (XenAPIException ex)
            {
                logln("Received exception as exected:");
                logln(ex);
            }

            logln("now trying to create one with a bad \"type\" field as well");
            logln("should throw a different exception");
            try
            {
                // fail for bad type
                SR.create(connection, host, new HashMap<String, String>(), 100000L, "name", "description", "made_up",
                        "contenttype", true, new HashMap<String, String>());
            } catch (XenAPIException ex)
            {
                logln("Received exception as exected:");
                logln(ex);
            }

            logln("Now unplugging any PBDs");
            // First unplug any PBDs associated with the SR
            Set<PBD> pbds = PBD.getAll(connection);
            for (PBD pbd : pbds)
            {
                if (pbd.getSR(connection).equals(newSr))
                {
                    pbd.unplug(connection);
                }
            }

            logln("Now destroying the newly-created SR");
            newSr.destroy(connection);
        } finally
        {
            disconnect();
        }
    }
}
