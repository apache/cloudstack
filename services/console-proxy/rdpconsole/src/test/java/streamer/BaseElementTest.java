//
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
//
package streamer;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class BaseElementTest {

    @Test
    public void testPoll() {
        BaseElement element = new BaseElement("test") {

            @Override
            public void handleData(ByteBuffer buf, Link link) {
                inputPads.remove("testpad1");
                inputPads.remove("testpad2");
            }

        };
        Link testLink1 = mock(Link.class);
        when(testLink1.pull(anyBoolean())).thenReturn(new ByteBuffer("hello".getBytes()));
        Link testLink2 = mock(Link.class);
        when(testLink2.pull(anyBoolean())).thenReturn(new ByteBuffer("hello".getBytes()));

        element.setLink("testpad1", testLink1, streamer.Direction.IN);
        element.setLink("testpad2", testLink2, streamer.Direction.IN);
        element.poll(false);
    }

}
