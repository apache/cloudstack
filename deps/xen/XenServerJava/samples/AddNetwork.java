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
import java.util.Random;

import com.xensource.xenapi.Network;

public class AddNetwork extends TestBase
{
    /**
     * Adds a new internal network not attached to any NICs.
     */
    protected static void RunTest(ILog logger, TargetServer server) throws Exception
    {
        TestBase.logger = logger;
        connect(server);
        try
        {
            Network.Record networkRecord = new Network.Record();
            networkRecord.nameLabel = "TestNetwork" + new Random().nextInt(10000);
            networkRecord.nameDescription = "Created by AddNetwork.java at " + new Date().toString();

            logln("Adding new network: " + networkRecord.nameLabel);
            Network.create(connection, networkRecord);
        } finally
        {
            disconnect();
        }
    }
}
