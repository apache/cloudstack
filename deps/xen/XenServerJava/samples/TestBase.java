/*
 * Copyright (c) 2008-2009 Citrix Systems, Inc.
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

import java.net.URL;
import java.util.Map;
import java.util.Set;

import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VM;

public abstract class TestBase
{
    protected static ILog logger;
    protected static Connection connection;
    private static String connectionName;

    protected static void connect(TargetServer target) throws Exception
    {
        /*
         * Old style: Connection constructor performs login_with_password for you. Deprecated.
         *
         * connection = new Connection(target.Hostname, target.Username, target.Password);
         */

        /*
         * New style: we are responsible for Session login/logout.
         */
        connection = new Connection(new URL("http://" + target.Hostname));
        logln(String.format("logging in to '%s' as '%s' with password '%s'...", target.Hostname, target.Username,
                target.Password));
        logln("Success");
        Session.loginWithPassword(connection, target.Username, target.Password, APIVersion.latest().toString());
        logln(String.format("Session API version is %s", connection.getAPIVersion().toString()));

        connectionName = target.Hostname;
    }

    protected static void disconnect() throws Exception
    {
        logln("disposing connection for " + connectionName);
        Session.logout(connection);
    }

    protected static void hRule()
    {
        logln("----------------------------------------------------------------------");
    }

    protected static void announce(String s)
    {
        hRule();
        log(s);
        hRule();
    }

    protected static void log(String s)
    {
        logger.log(s);
    }

    protected static void logf(String s, Object... args)
    {
        logger.log(String.format(s, args));
    }

    protected static void logln(String s)
    {
        logger.logln(s);
    }

    protected static void logln(Object o)
    {
        logln(o.toString());
    }

    protected static TargetServer ParseTarget(String[] args)
    {
        return new TargetServer(args[0], args[1], args[2]);
    }

    /**
     * Given a task in progress, sleeps until it completes, waking to print status reports periodically.
     */
    protected static void waitForTask(Connection c, Task task, int delay) throws Exception
    {
        while (task.getStatus(c) == Types.TaskStatusType.PENDING)
        {
            logf("%.2f;", task.getProgress(c));
            Thread.sleep(delay);
        }
        logln("");
    }

    protected static SR getDefaultSR() throws Exception
    {
        Set<Pool> pools = Pool.getAll(connection);
        Pool pool = (pools.toArray(new Pool[0]))[0];
        return pool.getDefaultSR(connection);
    }

    protected static VM getFirstWindowsTemplate() throws Exception
    {
        Map<VM, VM.Record> all_recs = VM.getAllRecords(connection);
        for (Map.Entry<VM, VM.Record> e : all_recs.entrySet())
        {
            if (e.getValue().isATemplate == true && e.getValue().nameLabel.contains("Windows"))
            {
                return e.getKey();
            }
        }

        throw new Exception("No Windows templates found!");
    }

    /**
     * Finds the first network (probably the one created by AddNetwork.java).
     */
    protected static Network getFirstNetwork() throws Exception
    {
        Set<Network> networks = Network.getAll(connection);
        for (Network i : networks)
        {
            return i;
        }

        throw new Exception("No networks found!");
    }
}
