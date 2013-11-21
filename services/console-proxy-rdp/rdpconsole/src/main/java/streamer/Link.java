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
 * Link is wire between two elements. It always must contain source and sink
 * elements.
 */
public interface Link extends DataSource, DataSink, Runnable {

    /**
     * Wire this link with given sink.
     *
     * @param sink
     *          an Element
     * @return same sink element, for chaining
     */
    Element setSink(Element sink);

    /**
     * Wire this link with given source.
     *
     * @param source
     *          an Element
     * @return same source element, for chaining
     */
    Element setSource(Element source);

    Element getSource();

    Element getSink();

    /**
     * Hold all data in cache, don't pass data to sink until resumed.
     */
    void pause();

    /**
     * Resume transfer.
     */
    void resume();

    /**
     * Change mode of operation of this link from push mode to pull mode.
     */
    void setPullMode();

    /**
     * Drop this link.
     */
    void drop();
}
