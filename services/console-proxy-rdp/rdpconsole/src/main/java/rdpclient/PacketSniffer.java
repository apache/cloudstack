// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package rdpclient;

import java.util.regex.Pattern;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;

/**
 * Try to determine packet content by it header fingerprint.
 */
public class PacketSniffer extends BaseElement {

    protected Pair regexps[] = null;

    public PacketSniffer(String id, Pair[] regexps) {
        super(id);
        this.regexps = regexps;
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {

        matchPacket(buf);

        super.handleData(buf, link);
    }

    private void matchPacket(ByteBuffer buf) {
        String header = buf.toPlainHexString(100);
        for (Pair pair : regexps) {
            if (pair.regexp.matcher(header).find()) {
                System.out.println("[" + this + "] INFO: Packet: " + pair.name + ".");
                return;
            }
        }

        System.out.println("[" + this + "] INFO: Unknown packet: " + header + ".");
    }

    protected static class Pair {
        String name;
        Pattern regexp;

        protected Pair(String name, String regexp) {
            this.name = name;
            this.regexp = Pattern.compile("^" + replaceShortcuts(regexp), Pattern.CASE_INSENSITIVE);
        }

        private static String replaceShortcuts(String regexp) {
            String result = regexp;
            result = result.replaceAll("XX\\*", "([0-9a-fA-F]{2} )*?");
            result = result.replaceAll("XX\\?", "([0-9a-fA-F]{2} )?");
            result = result.replaceAll("XX", "[0-9a-fA-F]{2}");
            return result;
        }
    }

}
