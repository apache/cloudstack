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

import java.util.Set;

/**
 * Element is for processing of data. It has one or more contact pads, which can
 * be wired with other elements using links.
 */
public interface Element {

    /**
     * Name of pad for standard input. Should be set in all elements except pure
     * sinks.
     */
    public static final String STDIN = "stdin";

    /**
     * Name of pad for standard output. Should be set in all elements except pure
     * sources.
     */
    public static final String STDOUT = "stdout";

    /**
     * Get link connected to given pad.
     *
     * @param padName
     *          Standard pads are "stdin" and "stdout".
     */
    Link getLink(String padName);

    /**
     * Get pads of this element.
     */
    Set<String> getPads(Direction direction);

    /**
     * Connect link to given pad.
     *
     * @param padName
     *          a pad name. Standard pads are "stdin" and "stdout".
     */
    void setLink(String padName, Link link, Direction direction);

    /**
     * Disconnect link from given pad.
     *
     * @param padName
     *          Standard pads are "stdin" and "stdout".
     */
    void dropLink(String padName);

    /**
     * Pull data from element and handle it. Element should ask one of it input
     * pads for data, handle data and push result to it sink(s), if any.
     *
     * @param block
     *          block until data will be available, or do a slight delay at least,
     *          when data is not available
     */
    void poll(boolean block);

    /**
     * Handle incoming data.
     *
     * @param buf
     *          a data
     * @param link
     *          TODO
     */
    void handleData(ByteBuffer buf, Link link);

    /**
     * Handle event.
     *
     * @param event
     *          an event
     * @param direction
     *          if IN, then send event to input pads, when OUT, then send to
     *          output pads
     */
    void handleEvent(Event event, Direction direction);

    /**
     * Get element ID.
     */
    String getId();

    /**
     * Validate element: check is all required pads are connected.
     */
    void validate();

    /**
     * Drop link.
     *
     * @param link a link to drop
     */
    void dropLink(Link link);

    /**
     * Drop existing link and replace it by new link.
     */
    void replaceLink(Link existingLink, Link newLink);
}
