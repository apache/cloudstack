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

public interface DataSource {

    /**
     * Get data from source.
     *
     * @param block
     *          if false, then return immediately when no data is available,
     *          otherwise wait for data
     * @return new data or null, when no data is available
     */
    ByteBuffer pull(boolean block);

    /**
     * Hold data temporary to use at next pull or push.
     *
     * @param buf
     *          a data
     */
    void pushBack(ByteBuffer buf);

    /**
     * Hold data temporary to use at next pull. Don't return abything until given
     * amount of data will be read from source, because data will be pushed back
     * anyway.
     *
     * @param buf
     *          a data
     * @param lengthOfFullPacket
     *          length of full block of data to read from source
     */
    void pushBack(ByteBuffer buf, int lengthOfFullPacket);

    /**
     * Send event to pads.
     *
     * @param event
     *          a event
     * @param direction
     *          pad direction
     */
    void sendEvent(Event event, Direction direction);
}
