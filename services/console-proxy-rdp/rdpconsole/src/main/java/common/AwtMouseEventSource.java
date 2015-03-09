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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import streamer.BaseElement;
import streamer.ByteBuffer;

public class AwtMouseEventSource extends BaseElement implements MouseListener, MouseMotionListener {

    public AwtMouseEventSource(String id) {
        super(id);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Nothing to do
    }

    @Override
    public void mousePressed(MouseEvent e) {
        MouseOrder order = new MouseOrder(e);
        order.pressed = true;
        pushDataToAllOuts(new ByteBuffer(order));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        MouseOrder order = new MouseOrder(e);
        order.released = true;
        pushDataToAllOuts(new ByteBuffer(order));
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Nothing to do
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Nothing to do
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        pushDataToAllOuts(new ByteBuffer(new MouseOrder(e)));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        pushDataToAllOuts(new ByteBuffer(new MouseOrder(e)));
    }

}
