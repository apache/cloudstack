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
package org.apache.cloudstack.storage.command;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class QuerySnapshotZoneCopyAnswerTest {

    @Test
    public void testQuerySnapshotZoneCopyAnswerSuccess() {
        QuerySnapshotZoneCopyCommand cmd = Mockito.mock(QuerySnapshotZoneCopyCommand.class);
        List<String> files = List.of("File1", "File2");
        QuerySnapshotZoneCopyAnswer answer = new QuerySnapshotZoneCopyAnswer(cmd, files);
        Assert.assertTrue(answer.getResult());
        Assert.assertEquals(files.size(), answer.getFiles().size());
        Assert.assertEquals(files.get(0), answer.getFiles().get(0));
        Assert.assertEquals(files.get(1), answer.getFiles().get(1));
    }

    @Test
    public void testQuerySnapshotZoneCopyAnswerFailure() {
        QuerySnapshotZoneCopyCommand cmd = Mockito.mock(QuerySnapshotZoneCopyCommand.class);
        String err = "SOMEERROR";
        QuerySnapshotZoneCopyAnswer answer = new QuerySnapshotZoneCopyAnswer(cmd, err);
        Assert.assertFalse(answer.getResult());
        Assert.assertEquals(err, answer.getDetails());
    }
}
