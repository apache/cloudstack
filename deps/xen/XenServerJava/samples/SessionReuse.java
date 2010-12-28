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

import java.lang.Thread.State;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.xensource.xenapi.*;
import com.xensource.xenapi.Host.Record;

/**
 * Demonstrates how a Session object can be shared between multiple Connections.
 */
public class SessionReuse extends TestBase
{
    private static boolean threadExit = false;

    protected static void RunTest(ILog logger, TargetServer server) throws Exception
    {
        TestBase.logger = logger;

        URL url = new URL("http://" + server.Hostname);

        // Create a Connection. No login is performed for us.
        final Connection connection1 = new Connection(url);

        try
        {
            // Create a new Session, whose reference is stored in the Connection.
            Session.loginWithPassword(connection1, server.Username, server.Password, "1.3");

            // Re-use the Session in a second Connection object
            Connection connection2 = new Connection(url, connection1.getSessionReference());

            // Listen for events using the first Connection.
            Thread listener = new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        Set<String> everything = new HashSet<String>();
                        everything.add("*");
                        Event.register(connection1, everything);
                        Set<Event.Record> events = Event.next(connection1);

                        if (threadExit)
                        {
                            // We took too long to get the event, and the test will already have failed.
                            // Exit now, rather than spamming the logs.
                            return;
                        }

                        logln("Received " + events.size() + " Event(s). First Event follows.");
                        for (Event.Record record : events)
                        {
                            logln(record.toString());
                            break;
                        }
                    } catch (Exception e)
                    {
                        logln("Event listener thread got an Exception");
                        logln(e.toString());
                    }
                };
            });
            listener.start();

            // Wait a bit for other thread to start listening
            Thread.sleep(15000);

            // Cause an event to be generated on the second thread.
            Map<Host, Record> hosts = Host.getAllRecords(connection2);
            for (Host ref : hosts.keySet())
            {
                ref.setNameDescription(connection2, "Set by SessionReuse.java at " + new Date().toString());
                break;
            }

            listener.join(60 * 1000);

            threadExit = true;

            if (listener.getState() != State.TERMINATED)
            {
                throw new IllegalStateException("Listener thread failed to terminate after 60 seconds");
            }
        } finally
        {
            if (connection1 != null)
            {
                Session.logout(connection1);
            }
        }
    }
}
