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

import com.xensource.xenapi.HostPatch;

/**
 * The HostPatch.apply() method is deprecated. We'll try to use it. The test is that the 
 * program will compile, but the compiler will protest.
 */
public class DeprecatedMethod extends TestBase
{
    public static void RunTest(ILog logger, TargetServer server) throws Exception
    {
        TestBase.logger = logger;
        try
        {
            connect(server);

            Map<HostPatch, HostPatch.Record> all_recs = HostPatch.getAllRecords(connection);
            if (all_recs.size() > 0)
            {
                logln("Found HostPatches. Applying the first one...");
                Map.Entry<HostPatch, HostPatch.Record> first = null;
                for (Map.Entry<HostPatch, HostPatch.Record> entry : all_recs.entrySet())
                {
                    first = entry;
                    break;
                }
                logln(first.getValue().toString());
                first.getKey().apply(connection);
            } else
            {
                logln("There aren't any HostPatches to be applied...");
            }
        } finally
        {
            disconnect();
        }
    }
}
