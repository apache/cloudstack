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

import java.util.HashSet;
import java.util.Set;

import com.xensource.xenapi.Event;

/**
 * Listens for events on a connection and prints each event out as it is received.
 */
public class EventMonitor extends TestBase
{
    private static final int MAX_EVENTS = 100;
    private static final int TIMEOUT = 30 * 1000;

    public static void RunTest(ILog logger, TargetServer server) throws Exception
    {
        TestBase.logger = logger;
        try
        {
            connect(server);
            Set<String> everything = new HashSet<String>();
            everything.add("*");
            Event.register(connection, everything);

            int eventsReceived = 0;
            long started = System.currentTimeMillis();

            while (eventsReceived < MAX_EVENTS && System.currentTimeMillis() - started < TIMEOUT)
            {
                Set<Event.Record> events = Event.next(connection);
                announce(events.size() + " event(s) received");

                // print the events out in a nice format
                String format = "%10s %5s %3s %10s %50s";
                logf(format + " date       time%n", "class", "id", "uuid", "operation", "reference");
                for (Event.Record e : events)
                {
                    logf(format, e.clazz, e.id, e.objUuid, e.operation, e.ref);
                    logf(" %te/%<tm/%<tY %<tH.%<tM.%<tS %n", e.timestamp);
                    logln("associated snapshot: " + e.snapshot);
                }
                eventsReceived += events.size();
            }
        } finally
        {
            disconnect();
        }
    }
}
