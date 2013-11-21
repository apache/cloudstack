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
 * Pipeline groups multiple elements.
 */
public interface Pipeline extends Element {

    static final String IN = Direction.IN.toString();
    static final String OUT = Direction.OUT.toString();

    /**
     * Add elements to pipeline.
     *
     * @param elements
     */
    void add(Element... elements);

    /**
     * Add elements to pipeline and link them in given order.
     *
     * @param elements
     */
    void addAndLink(Element... elements);

    /**
     * Link elements in given order using SyncLink. Element name can have prefix
     * "PADNAME< " or/and suffix " >PADNAME" to use given named pads instead of
     * "stdin" and "stdout". I.e. <code>link("foo", "bar", "baz");</code> is equal
     * to <code>link("foo >stdin", "stdout< bar >stdin", "stdout< baz");</code> .
     *
     * Special elements "IN" and "OUT" are pointing to pipeline outer interfaces,
     * so when pipeline will be connected with other elements, outside of this
     * pipeline, they will be connected to IN and OUT elements.
     *
     * Example:
     *
     * <pre>
     * pipeline.link(&quot;IN&quot;, &quot;foo&quot;, &quot;bar&quot;, &quot;OUT&quot;);
     * // Make additional branch from foo to baz, and then to OUT
     * pipeline.link(&quot;foo &gt;baz_out&quot;, &quot;baz&quot;, &quot;baz_in&lt; OUT&quot;);
     * </pre>
     *
     * @param elements
     *          elements to link
     */
    void link(String... elements);

    /**
     * Get element by name.
     *
     * @return an element
     */
    Element get(String elementName);

    /**
     * Get link by element name and pad name.
     */
    Link getLink(String elementName, String padName);

    /**
     * Set link by element name and pad name. Allows to link external elements
     * into internal elements of pipeline. Special elements "IN" and "OUT" are
     * pointing to pipeline outer interfaces.
     */
    void setLink(String elementName, String padName, Link link, Direction direction);

    /**
     * Get link connected to given pad in given element and run it main loop.
     * @param separateThread
     *          set to true to start main loop in separate thread.
     * @param waitForStartEvent TODO
     */
    void runMainLoop(String element, String padName, boolean separateThread, boolean waitForStartEvent);

}
