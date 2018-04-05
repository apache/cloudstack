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

import java.awt.Image;
import java.util.List;

/**
 * ConsoleProxyClient defines an standard interface that a console client should implement,
 *
 * ConsoleProxyClient maintains a session towards the target host, it glues the session
 * to a AJAX front-end viewer
 */
public interface ConsoleProxyClient {
    int getClientId();

    //
    // Quick status
    //
    boolean isHostConnected();

    boolean isFrontEndAlive();

    //
    // AJAX viewer
    //
    long getAjaxSessionId();

    AjaxFIFOImageCache getAjaxImageCache();

    Image getClientScaledImage(int width, int height);                  // client thumbnail support

    String onAjaxClientStart(String title, List<String> languages, String guest);

    String onAjaxClientUpdate();

    String onAjaxClientKickoff();

    //
    // Input handling
    //
    void sendClientRawKeyboardEvent(InputEventType event, int code, int modifiers);

    void sendClientMouseEvent(InputEventType event, int x, int y, int code, int modifiers);

    //
    // Info/Stats
    //
    long getClientCreateTime();

    long getClientLastFrontEndActivityTime();

    String getClientHostAddress();

    int getClientHostPort();

    String getClientHostPassword();

    String getClientTag();

    //
    // Setup/house-keeping
    //
    void initClient(ConsoleProxyClientParam param);

    void closeClient();
}
