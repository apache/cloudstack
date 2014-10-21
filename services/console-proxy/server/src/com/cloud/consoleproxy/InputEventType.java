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
package com.cloud.consoleproxy;

public enum InputEventType {
    MOUSE_MOVE(1), MOUSE_DOWN(2), MOUSE_UP(3), KEY_PRESS(4), KEY_DOWN(5), KEY_UP(6), MOUSE_DBLCLICK(8);

    int eventCode;

    private InputEventType(int eventCode) {
        this.eventCode = eventCode;
    }

    public int getEventCode() {
        return eventCode;
    }

    public static InputEventType fromEventCode(int eventCode) {
        switch (eventCode) {
            case 1:
                return MOUSE_MOVE;
            case 2:
                return MOUSE_DOWN;
            case 3:
                return MOUSE_UP;
            case 4:
                return KEY_PRESS;
            case 5:
                return KEY_DOWN;
            case 6:
                return KEY_UP;
            case 8:
                return MOUSE_DBLCLICK;
            default:
                break;
        }
        throw new IllegalArgumentException("Unsupport event code: " + eventCode);
    }
}
