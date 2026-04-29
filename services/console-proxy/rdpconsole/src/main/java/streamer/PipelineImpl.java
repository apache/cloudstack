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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import streamer.debug.FakeSink;
import streamer.debug.FakeSource;

public class PipelineImpl implements Pipeline {

    protected Logger logger = LogManager.getLogger(getClass());

    protected String id;
    protected boolean verbose = System.getProperty("streamer.Pipeline.debug", "false").equals("true");

    public PipelineImpl(String id) {
        this.id = id;
        elements = initElementMap(id);
    }

    protected Map<String, Element> elements;

    protected HashMap<String, Element> initElementMap(String id) {
        HashMap<String, Element> map = new HashMap<String, Element>();

        map.put(IN, new BaseElement(id + "." + IN));
        map.put(OUT, new BaseElement(id + "." + OUT));
        return map;
    }

    @Override
    public Link getLink(String padName) {
        Link link = elements.get(IN).getLink(padName);
        if (link == null)
            link = elements.get(OUT).getLink(padName);
        return link;
    }

    @Override
    public Set<String> getPads(Direction direction) {
        switch (direction) {
        case IN:
            return elements.get(IN).getPads(direction);

        case OUT:
            return elements.get(OUT).getPads(direction);
        }
        return null;
    }

    @Override
    public void validate() {
        for (Element element : elements.values())
            element.validate();

        // Check IN element
        {
            Element element = get(IN);
            int outPadsNumber = element.getPads(Direction.OUT).size();
            int inPadsNumber = element.getPads(Direction.IN).size();
            if ((outPadsNumber | inPadsNumber) > 0 && (outPadsNumber == 0 || inPadsNumber == 0))
                throw new RuntimeException("[ " + this + "] Pads of input element of pipeline are not balanced. Element: " + element + ", output pads: "
                        + element.getPads(Direction.OUT).toString() + ", input pads: " + element.getPads(Direction.IN).toString() + ".");
        }

        // Check OUT element
        {
            Element element = get(OUT);
            int outPadsNumber = element.getPads(Direction.OUT).size();
            int inPadsNumber = element.getPads(Direction.IN).size();
            if ((outPadsNumber | inPadsNumber) > 0 && (outPadsNumber == 0 || inPadsNumber == 0))
                throw new RuntimeException("[ " + this + "] Pads of output element of pipeline are not balanced. Element: " + element + ", output pads: "
                        + element.getPads(Direction.OUT).toString() + ", input pads: " + element.getPads(Direction.IN).toString() + ".");
        }

    }

    @Override
    public void dropLink(String padName) {
        if (elements.get(IN).getLink(padName) != null)
            elements.get(IN).dropLink(padName);

        if (elements.get(OUT).getLink(padName) != null)
            elements.get(OUT).dropLink(padName);
    }

    @Override
    public void dropLink(Link link) {
        elements.get(IN).dropLink(link);
        elements.get(OUT).dropLink(link);
    }

    @Override
    public void replaceLink(Link existingLink, Link newLink) {
        elements.get(IN).replaceLink(existingLink, newLink);
        elements.get(OUT).replaceLink(existingLink, newLink);
    }

    @Override
    public void setLink(String padName, Link link, Direction direction) {
        // Wire links to internal elements instead
        elements.get(direction.toString()).setLink(padName, link, direction);
    }

    @Override
    public void poll(boolean block) {
        throw new RuntimeException("Not implemented.");
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        get(IN).handleData(buf, link);
    }

    @Override
    public void handleEvent(Event event, Direction direction) {
        switch (direction) {
        case IN:
            get(IN).handleEvent(event, direction);
            break;
        case OUT:
            get(OUT).handleEvent(event, direction);
            break;
        }
    }

    @Override
    public void add(Element... elements) {
        for (Element element : elements) {
            String id = element.getId();

            if (this.elements.containsKey(id))
                throw new RuntimeException("This pipeline already contains element with same ID. New element: " + element + ", existing element: "
                        + this.elements.get(id) + ".");

            this.elements.put(id, element);
        }
    }

    @Override
    public void link(String... elementNames) {

        elementNames = filterOutEmptyStrings(elementNames);

        if (elementNames.length < 2)
            throw new RuntimeException("At least two elements are necessary to create link between them.");

        // Parse array of element and pad names

        Element elements[] = new Element[elementNames.length];
        String inputPads[] = new String[elementNames.length];
        String outputPads[] = new String[elementNames.length];

        int i = 0;
        for (String elementName : elementNames) {
            if (elementName.contains("< ")) {
                inputPads[i] = elementName.substring(0, elementName.indexOf("< "));
                elementName = elementName.substring(elementName.indexOf("< ") + 2);
            } else {
                inputPads[i] = STDIN;
            }

            if (elementName.contains(" >")) {
                outputPads[i] = elementName.substring(elementName.indexOf(" >") + 2);
                elementName = elementName.substring(0, elementName.indexOf(" >"));
            } else {
                outputPads[i] = STDOUT;
            }

            elements[i] = get(elementName);

            if (elements[i] == null)
                throw new RuntimeException("Cannot find element by name in this pipeline. Element name: \"" + elementName + "\" (" + elementNames[i] + "), pipeline: "
                        + this + ".");

            i++;
        }

        // Link elements
        for (i = 0; i < elements.length - 1; i++) {
            Element leftElement = elements[i];
            Element rightElement = elements[i + 1];
            String leftPad = outputPads[i];
            String rightPad = inputPads[i + 1];

            String linkId = leftElement.getId() + " >" + leftPad + " | " + rightPad + "< " + rightElement.getId();

            if (verbose)
                System.out.println("[" + this + "] INFO: Linking: " + linkId + ".");

            Link link = new SyncLink(linkId);
            leftElement.setLink(leftPad, link, Direction.OUT);
            rightElement.setLink(rightPad, link, Direction.IN);
        }
    }

    /**
     * Filter out empty strings from array and return new array with non-empty
     * elements only. If array contains no empty string, returns same array.
     */
    private String[] filterOutEmptyStrings(String[] strings) {

        boolean found = false;
        for (String string : strings) {
            if (string == null || string.isEmpty()) {
                found = true;
                break;
            }
        }

        if (!found)
            return strings;

        List<String> filteredStrings = new ArrayList<String>(strings.length);
        for (String string : strings)
            if (string != null && !string.isEmpty())
                filteredStrings.add(string);
        return filteredStrings.toArray(new String[filteredStrings.size()]);
    }

    @Override
    public void addAndLink(Element... elements) {
        add(elements);
        link(elements);
    }

    private void link(Element... elements) {
        String elementNames[] = new String[elements.length];

        int i = 0;
        for (Element element : elements) {
            elementNames[i++] = element.getId();
        }

        link(elementNames);
    }

    @Override
    public Element get(String elementName) {
        return elements.get(elementName);
    }

    @Override
    public Link getLink(String elementName, String padName) {
        return elements.get(elementName).getLink(padName);

    }

    @Override
    public void setLink(String elementName, String padName, Link link, Direction direction) {
        elements.get(elementName).setLink(padName, link, direction);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void runMainLoop(String elementName, String padName, boolean separateThread, boolean waitForStartEvent) {
        validate();

        Link link = getLink(elementName, padName);

        if (link == null)
            throw new NullPointerException("Cannot find link. Element name: " + elementName + ", element: " + get(elementName) + ", pad: " + padName + ".");

        if (!waitForStartEvent)
            link.sendEvent(Event.STREAM_START, Direction.OUT);

        if (separateThread) {
            Thread thread = new Thread(link);
            thread.setDaemon(true);
            thread.start();
        } else {
            link.run();
        }
    }

    @Override
    public String toString() {
        return "Pipeline(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        // System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        Pipeline pipeline = new PipelineImpl("main");

        // Create elements
        pipeline.add(new FakeSource("source") {
            {
                incomingBufLength = 3;
                numBuffers = 10;
                delay = 100;
            }
        });
        pipeline.add(new BaseElement("tee"));
        pipeline.add(new FakeSink("sink") {
            {
                verbose = true;
            }
        });
        pipeline.add(new FakeSink("sink2") {
            {
                verbose = true;
            }
        });

        // Link elements
        pipeline.link("source", "tee", "sink");
        pipeline.link("tee >out2", "sink2");

        // Run main loop
        pipeline.runMainLoop("source", STDOUT, false, false);
    }

}
