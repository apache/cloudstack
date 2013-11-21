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
package streamer;

/**
 * One time switch for handshake and initialization stages.
 *
 * At beginning, element handles data internally, sending output to "otout" pad.
 * After switchOff() method is called, element drops its links, so packets from
 * "stdin" pad are forwarded directly to "stdout" pad, without processing.
 *
 * Event STREAM_START is captured by this element and not propagated further.
 * When switchOff() method is called, event STREAM_START is generated and sent
 * to "stdout".
 */
public abstract class OneTimeSwitch extends BaseElement {

    /**
     * One-time out - name of output pad for one time logic. By default, output
     * directly to socket.
     */
    public static final String OTOUT = "otout";

    private boolean switched = false;

    public OneTimeSwitch(String id) {
        super(id);
        declarePads();
    }

    protected void declarePads() {
        inputPads.put(STDIN, null);
        outputPads.put(OTOUT, null);
        outputPads.put(STDOUT, null);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (switched)
            throw new RuntimeException(this + " element is switched off and must not receive any data or events anymore.");

        if (buf == null)
            return;

        handleOneTimeData(buf, link);
    }

    public void pushDataToOTOut(ByteBuffer buf) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Sending data:  " + buf + ".");

        outputPads.get(OTOUT).sendData(buf);
    }

    /**
     * Switch this element off. Pass data directly to main output(s).
     */
    public void switchOff() {
        if (verbose)
            System.out.println("[" + this + "] INFO: Switching OFF.");

        switched = true;
        verbose = false;

        // Rewire links: drop otout link, replace stdout link by stdin to send data
        // directly to stdout
        Link stdout = (Link)outputPads.get(STDOUT);
        Link stdin = (Link)inputPads.get(STDIN);
        Link otout = (Link)outputPads.get(OTOUT);

        otout.drop();

        // Wake up next peer(s)
        sendEventToAllPads(Event.STREAM_START, Direction.OUT);

        stdin.setSink(null);
        inputPads.remove(STDIN);

        Element nextPeer = stdout.getSink();
        nextPeer.replaceLink(stdout, stdin);
        stdout.drop();

        for (Object link : inputPads.values().toArray())
            ((Link)link).drop();
        for (Object link : outputPads.values().toArray())
            ((Link)link).drop();

    }

    public void switchOn() {
        if (verbose)
            System.out.println("[" + this + "] INFO: Switching ON.");

        switched = false;
    }

    /**
     * Override this method to handle one-time packet(s) at handshake or
     * initialization stages. Execute method @see switchRoute() when this method
     * is no longer necessary.
     */
    protected abstract void handleOneTimeData(ByteBuffer buf, Link link);

    @Override
    public void handleEvent(Event event, Direction direction) {
        if (event == Event.STREAM_START) {
            if (verbose)
                System.out.println("[" + this + "] INFO: Event " + event + " is received.");

            switchOn();

            // Execute this element onStart(), but do not propagate event further,
            // to not wake up next elements too early
            onStart();
        } else
            super.handleEvent(event, direction);
    }

}
