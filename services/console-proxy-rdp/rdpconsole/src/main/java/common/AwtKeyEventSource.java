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
package common;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import streamer.BaseElement;
import streamer.ByteBuffer;

public class AwtKeyEventSource extends BaseElement implements KeyListener {

    public AwtKeyEventSource(String id) {
        super(id);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Nothing to do

    }

    @Override
    public void keyPressed(KeyEvent e) {
        sendEvent(e, true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        sendEvent(e, false);
    }

    private void sendEvent(KeyEvent e, boolean pressed) {
        ByteBuffer buf = new ByteBuffer(new KeyOrder(e, pressed));

        pushDataToAllOuts(buf);
    }

}
