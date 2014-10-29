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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import streamer.Element;
import streamer.SocketWrapper;

@RunWith(PowerMockRunner.class)
public class ClientTest {

    @Test(expected = NullPointerException.class)
    public void testAssemblePipelineWhenMainElementIsNull() throws Exception {
        SocketWrapper socketMock = mock(SocketWrapper.class);
        when(socketMock.getId()).thenReturn("socket");
        Whitebox.setInternalState(Client.class, "socket", socketMock);
        Element main = null;

        Client.assemblePipeline(main);
    }

}
