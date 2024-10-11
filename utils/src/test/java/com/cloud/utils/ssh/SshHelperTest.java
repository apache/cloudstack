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

package com.cloud.utils.ssh;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.Session;

@RunWith(MockitoJUnitRunner.class)
public class SshHelperTest {

    @Test
    public void canEndTheSshConnectionTest() throws Exception {
        MockedStatic<SshHelper> sshHelperMocked = Mockito.mockStatic(SshHelper.class, Mockito.CALLS_REAL_METHODS);
        Session mockedSession = Mockito.mock(Session.class);

        Mockito.when(SshHelper.isChannelConditionEof(Mockito.anyInt())).thenReturn(true);
        Mockito.when(mockedSession.waitForCondition(ChannelCondition.EXIT_STATUS, 1L)).thenReturn(0);

        SshHelper.canEndTheSshConnection(1, mockedSession, 0);
        sshHelperMocked.verify(() -> SshHelper.isChannelConditionEof(Mockito.anyInt()), Mockito.times(1));
        sshHelperMocked.verify(() -> SshHelper.throwSshExceptionIfConditionsTimeout(Mockito.anyInt()));

        Mockito.verify(mockedSession).waitForCondition(ChannelCondition.EXIT_STATUS, 1l);
        sshHelperMocked.close();
    }

    @Test(expected = SshException.class)
    public void throwSshExceptionIfConditionsTimeout() throws SshException {
        SshHelper.throwSshExceptionIfConditionsTimeout(ChannelCondition.TIMEOUT);
    }

    @Test
    public void doNotThrowSshExceptionIfConditionsClosed() throws SshException {
        SshHelper.throwSshExceptionIfConditionsTimeout(ChannelCondition.CLOSED);
    }

    @Test
    public void doNotThrowSshExceptionIfConditionsStdout() throws SshException {
        SshHelper.throwSshExceptionIfConditionsTimeout(ChannelCondition.STDOUT_DATA);
    }

    @Test
    public void doNotThrowSshExceptionIfConditionsStderr() throws SshException {
        SshHelper.throwSshExceptionIfConditionsTimeout(ChannelCondition.STDERR_DATA);
    }

    @Test
    public void doNotThrowSshExceptionIfConditionsEof() throws SshException {
        SshHelper.throwSshExceptionIfConditionsTimeout(ChannelCondition.EOF);
    }

    @Test
    public void doNotThrowSshExceptionIfConditionsExitStatus() throws SshException {
        SshHelper.throwSshExceptionIfConditionsTimeout(ChannelCondition.EXIT_STATUS);
    }

    @Test
    public void doNotThrowSshExceptionIfConditionsExitSignal() throws SshException {
        SshHelper.throwSshExceptionIfConditionsTimeout(ChannelCondition.EXIT_SIGNAL);
    }

    @Test
    public void isChannelConditionEofTestTimeout() {
        Assert.assertFalse(SshHelper.isChannelConditionEof(ChannelCondition.TIMEOUT));
    }

    @Test
    public void isChannelConditionEofTestClosed() {
        Assert.assertFalse(SshHelper.isChannelConditionEof(ChannelCondition.CLOSED));
    }

    @Test
    public void isChannelConditionEofTestStdout() {
        Assert.assertFalse(SshHelper.isChannelConditionEof(ChannelCondition.STDOUT_DATA));
    }

    @Test
    public void isChannelConditionEofTestStderr() {
        Assert.assertFalse(SshHelper.isChannelConditionEof(ChannelCondition.STDERR_DATA));
    }

    @Test
    public void isChannelConditionEofTestEof() {
        Assert.assertTrue(SshHelper.isChannelConditionEof(ChannelCondition.EOF));
    }

    @Test
    public void isChannelConditionEofTestExitStatus() {
        Assert.assertFalse(SshHelper.isChannelConditionEof(ChannelCondition.EXIT_STATUS));
    }

    @Test
    public void isChannelConditionEofTestExitSignal() {
        Assert.assertFalse(SshHelper.isChannelConditionEof(ChannelCondition.EXIT_SIGNAL));
    }

    @Test(expected = SshException.class)
    public void throwSshExceptionIfStdoutOrStdeerIsNullTestNull() throws SshException {
        SshHelper.throwSshExceptionIfStdoutOrStdeerIsNull(null, null);
    }

    @Test
    public void throwSshExceptionIfStdoutOrStdeerIsNullTestNotNull() throws SshException {
        InputStream inputStream = Mockito.mock(InputStream.class);
        SshHelper.throwSshExceptionIfStdoutOrStdeerIsNull(inputStream, inputStream);
    }

    @Test
    public void openConnectionSessionTest() throws IOException, InterruptedException {
        Connection conn = Mockito.mock(Connection.class);
        SshHelper.openConnectionSession(conn);

        Mockito.verify(conn).openSession();
    }
}
