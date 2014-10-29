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

package com.cloud.agent.transport;

import com.cloud.agent.api.Answer;
import com.cloud.exception.UnsupportedVersionException;

/**
 *
 */
public class Response extends Request {
    protected Response() {
    }

    public Response(Request request, Answer answer) {
        this(request, new Answer[] {answer});
    }

    public Response(Request request, Answer answer, long mgmtId, long agentId) {
        this(request, new Answer[] {answer}, mgmtId, agentId);
    }

    public Response(Request request, Answer[] answers) {
        super(request, answers);
    }

    public Response(Request request, Answer[] answers, long mgmtId, long agentId) {
        super(request, answers);
        _mgmtId = mgmtId;
        _via = agentId;
    }

    protected Response(Version ver, long seq, long agentId, long mgmtId, long via, short flags, String ans) {
        super(ver, seq, agentId, mgmtId, via, flags, ans);
    }

    public Answer getAnswer() {
        Answer[] answers = getAnswers();
        return answers[0];
    }

    public Answer[] getAnswers() {
        if (_cmds == null) {
            _cmds = s_gson.fromJson(_content, Answer[].class);
        }
        return (Answer[])_cmds;
    }

    @Override
    protected String getType() {
        return "Ans: ";
    }

    public static Response parse(byte[] bytes) throws ClassNotFoundException, UnsupportedVersionException {
        return (Response)Request.parse(bytes);
    }
}
