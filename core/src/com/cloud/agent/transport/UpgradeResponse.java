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

import java.nio.ByteBuffer;

import com.cloud.agent.api.Answer;
import com.cloud.utils.NumbersUtil;

/**
 * An UpgradeResponse is sent when there is a protocol version mismatched.
 * It has the same header as the request but contains an url to the
 * updated agent.
 */
public class UpgradeResponse extends Response {
    byte[] _requestBytes;
    
    public UpgradeResponse(Request request, String url) {
        super(request, new Answer[0]);
        _requestBytes = null;
    }
    
    public UpgradeResponse(byte[] request, String url) {
    	super(Version.v2, -1, -1, -1, url, true, true, true, false);
        _requestBytes = request;
    }
    
    protected UpgradeResponse(Version ver, long seq, String url) {
        super(ver, seq, -1, -1, url, true, false, true, false);
        _requestBytes = null;
    }

    @Override
    protected ByteBuffer serializeHeader(int contentSize) {
        if (_requestBytes == null) {
            return super.serializeHeader(contentSize);
        }
        
        byte[] responseHeader = new byte[16];
        ByteBuffer buffer = ByteBuffer.wrap(responseHeader);
        
        buffer.put(_requestBytes[0]);     // version number
        buffer.put((byte)0);
        buffer.putShort(getFlags());
        buffer.put(_requestBytes, 4, 8);  // sequence number
        buffer.putInt(contentSize);
        buffer.flip();
        
        return buffer;
    }
    
    @Override
    public String toString() {
    	if (_requestBytes == null) {
    		return super.toString();
    	}
    	
        final StringBuilder buffer = new StringBuilder();
        buffer.append("{ ").append(getType());
        buffer.append(", Seq: ").append(NumbersUtil.bytesToLong(_requestBytes, 4)).append(", Ver: ").append(_requestBytes[0]).append(", Flags: ").append(Integer.toBinaryString(getFlags()));
        buffer.append(", ").append(_content).append(" }");
        return buffer.toString();
    }
    
    @Override
    public ByteBuffer[] toBytes() {
        ByteBuffer[] buffers = new ByteBuffer[2];
     
        buffers[1] = ByteBuffer.wrap(_content.getBytes());
        buffers[0] = serializeHeader(buffers[1].capacity());
        
        return buffers;
    }
    
    public String getUpgradeUrl() {
        return _content;
    }
    
    @Override
    protected short getFlags() {
        return FLAG_RESPONSE | FLAG_UPDATE;
    }
}
