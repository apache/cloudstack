/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.agent.transport;

import com.cloud.agent.api.Answer;
import com.cloud.exception.UnsupportedVersionException;
import com.google.gson.Gson;

/**
 *
 */
public class Response extends Request {
    protected Response() {
    }
    
    public Response(Request request, Answer answer) {
        this(request, new Answer[] { answer });
    }
    
    public Response(Request request, Answer answer, long mgmtId, long agentId) {
        this(request, new Answer[] { answer }, mgmtId, agentId);
    }
    
    public Response(Request request, Answer[] answers) {
        super(request, answers);
    }
    
    public Response(Request request, Answer[] answers, long mgmtId, long agentId) {
    	super(request, answers);
    	_mgmtId = mgmtId;
        _agentId = agentId;
    }
    
    protected Response(Version ver, long seq, long agentId, long mgmtId, String ans, boolean inSequence, boolean stopOnError, boolean fromServer, boolean control) {
        super(ver, seq, agentId, mgmtId, ans, inSequence, stopOnError, fromServer, control);
    }
    
    public Answer getAnswer() {
    	Answer[] answers = getAnswers();
    	return answers[0];
    }
    
    public Answer[] getAnswers() {
    	if (_cmds == null) {
            final Gson json = s_gBuilder.create();
    		_cmds = json.fromJson(_content, Answer[].class);
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
