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

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.exception.UnsupportedVersionException;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Request is a simple wrapper around command and answer to add sequencing,
 * versioning, and flags. Note that the version here represents the changes
 * in the over the wire protocol.  For example, if we decide to not use Gson.
 * It does not version the changes in the actual commands.  That's expected
 * to be done by adding new classes to the command and answer list.
 *
 * A request looks as follows:
 *   1. Version - 1 byte;
 *   2. Flags - 3 bytes;
 *   3. Sequence - 8 bytes;
 *   4. Length - 4 bytes;
 *   5. ManagementServerId - 8 bytes;
 *   6. AgentId - 8 bytes;
 *   7. Data Package.
 *
 */
public class Request {
    private static final Logger s_logger = Logger.getLogger(Request.class);

    public enum Version {
        v1, // using gson to marshall
        v2, // now using gson as marshalled.
        v3; // Adding routing information into the Request data structure.

        public static Version get(final byte ver) throws UnsupportedVersionException {
            for (final Version version : Version.values()) {
                if (ver == version.ordinal())
                    return version;
            }
            throw new UnsupportedVersionException("Can't lookup version: " + ver, UnsupportedVersionException.UnknownVersion);
        }
    };

    protected static final short FLAG_RESPONSE = 0x0;
    protected static final short FLAG_REQUEST = 0x1;
    protected static final short FLAG_STOP_ON_ERROR = 0x2;
    protected static final short FLAG_IN_SEQUENCE = 0x4;
    protected static final short FLAG_REVERT_ON_ERROR = 0x8;
    protected static final short FLAG_FROM_SERVER = 0x20;
    protected static final short FLAG_CONTROL = 0x40;

    protected static final GsonBuilder s_gBuilder;
    static {
        s_gBuilder = new GsonBuilder();
        s_gBuilder.registerTypeAdapter(Command[].class, new ArrayTypeAdaptor<Command>());
        s_gBuilder.registerTypeAdapter(Answer[].class, new ArrayTypeAdaptor<Answer>());
        final Type listType = new TypeToken<List<VolumeVO>>() {}.getType();
        s_gBuilder.registerTypeAdapter(listType, new VolListTypeAdaptor());
        s_logger.info("Builder inited.");
    }

    public static GsonBuilder initBuilder() {
        return s_gBuilder;
    }

    protected Version                 _ver;
    protected long                    _seq;
    protected short                   _flags;
    protected long                    _mgmtId;
    protected long                    _agentId;
    protected Command[]               _cmds;
    protected String                  _content;

    protected Request() {
    }

    protected Request(Version ver, long seq, long agentId, long mgmtId, short flags, final Command[] cmds) {
        _ver = ver;
        _cmds = cmds;
        _flags = flags;
        _seq = seq;
        _agentId = agentId;
        _mgmtId = mgmtId;
    }
    
    protected Request(Version ver, long seq, long agentId, long mgmtId, short flags, final String content) {
        this(ver, seq, agentId, mgmtId, flags, (Command[])null);
        _content = content;
    }
    
    public Request(long seq, long agentId, long mgmtId, final Command command, boolean fromServer) {
        this(seq, agentId, mgmtId, new Command[] {command}, true, fromServer, true);
    }
    
    public Request(long seq, long agentId, long mgmtId, Command[] cmds, boolean stopOnError, boolean fromServer, boolean revert) {
        this(Version.v3, seq, agentId, mgmtId, (short)0, cmds);
        setStopOnError(stopOnError);
        setFromServer(fromServer);
        setRevertOnError(revert);
    }
    
    protected Request(final Request that, final Command[] cmds) {
        this._ver = that._ver;
        this._seq = that._seq;
        setInSequence(that.executeInSequence());
        setStopOnError(that.stopOnError());
        this._cmds = cmds;
        this._mgmtId = that._mgmtId;
        this._agentId = that._agentId;
        setFromServer(!that.isFromServer());
    }
    
    private final void setStopOnError(boolean stopOnError) {
        _flags |= (stopOnError ? 1 : 0) << FLAG_STOP_ON_ERROR;
    }
    
    private final void setInSequence(boolean inSequence) {
        _flags |= (inSequence ? 1 : 0) << FLAG_IN_SEQUENCE;
    }
    
    
    public boolean isControl() {
        return (_flags & FLAG_CONTROL) > 0; 
    }
    
    public void setControl(boolean control) {
        _flags |= (control ? 1 : 0) << FLAG_CONTROL;
    }
    
    public boolean revertOnError() {
        return (_flags & FLAG_CONTROL) > 0;
    }
    
    private final void setRevertOnError(boolean revertOnError) {
        _flags |= (revertOnError ? 1 : 0) << FLAG_REVERT_ON_ERROR;
    }
    
    private final void setFromServer(boolean fromServer) {
        _flags |= (fromServer ? 1 : 0) << FLAG_FROM_SERVER;
    }
    
    public long getManagementServerId() {
        return _mgmtId;
    }

    public boolean isFromServer() {
        return (_flags & FLAG_FROM_SERVER) > 0;
    }
    
    public Version getVersion() {
        return _ver;
    }
    
    public void setAgentId(long agentId) {
        _agentId = agentId;
    }

    public boolean executeInSequence() {
        return (_flags & FLAG_IN_SEQUENCE) > 0;
    }

    public long getSequence() {
        return _seq;
    }

    public boolean stopOnError() {
        return (_flags & FLAG_STOP_ON_ERROR) > 0;
    }

    public Command getCommand() {
    	getCommands();
        return _cmds[0];
    }

    public Command[] getCommands() {
    	if (_cmds == null) {
            final Gson json = s_gBuilder.create();
    		_cmds = json.fromJson(_content, Command[].class);
    	}
		return _cmds;
    }

    /**
     * Use this only surrounded by debug.
     */
    @Override
    public String toString() {
        String content = _content;
        if (content == null) {
            final Gson gson = s_gBuilder.create();
            content = gson.toJson(_cmds);
        }
        final StringBuilder buffer = new StringBuilder();
        buffer.append("{ ").append(getType());
        buffer.append(", Seq: ").append(_seq).append(", Ver: ").append(_ver.toString()).append(", MgmtId: ").append(_mgmtId).append(", AgentId: ").append(_agentId).append(", Flags: ").append(Integer.toBinaryString(getFlags()));
        buffer.append(", ").append(content).append(" }");
        return buffer.toString();
    }

    protected String getType() {
        return "Cmd ";
    }

    protected ByteBuffer serializeHeader(final int contentSize) {
        final ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.put(getVersionInByte());
        buffer.put((byte)0);
        buffer.putShort(getFlags());
        buffer.putLong(_seq);
        buffer.putInt(contentSize);
        buffer.putLong(_mgmtId);
        buffer.putLong(_agentId);
        buffer.flip();

        return buffer;
    }

    public ByteBuffer[] toBytes() {
        final Gson gson = s_gBuilder.create();
        final ByteBuffer[] buffers = new ByteBuffer[2];

        if (_content == null) {
        	_content = gson.toJson(_cmds, _cmds.getClass());
        }
        buffers[1] = ByteBuffer.wrap(_content.getBytes());
        buffers[0] = serializeHeader(buffers[1].capacity());

        return buffers;
    }

    public byte[] getBytes() {
        final ByteBuffer[] buffers = toBytes();
        final int len1 = buffers[0].remaining();
        final int len2 = buffers[1].remaining();
        final byte[] bytes = new byte[len1 + len2];
        buffers[0].get(bytes, 0, len1);
        buffers[1].get(bytes, len1, len2);
        return bytes;
    }

    protected byte getVersionInByte() {
        return (byte)_ver.ordinal();
    }

    protected short getFlags() {
        return (short)(((this instanceof Response) ? FLAG_RESPONSE : FLAG_REQUEST) | _flags);
    }

    /**
     * Factory method for Request and Response.  It expects the bytes to be
     * correctly formed so it's possible that it throws underflow exceptions
     * but you shouldn't be concerned about that since that all bytes sent in
     * should already be formatted correctly.
     *
     * @param bytes bytes to be converted.
     * @return Request or Response depending on the data.
     * @throws ClassNotFoundException if the Command or Answer can not be formed.
     * @throws
     */
    public static Request parse(final byte[] bytes) throws ClassNotFoundException, UnsupportedVersionException {
        final ByteBuffer buff = ByteBuffer.wrap(bytes);
        final byte ver = buff.get();
        final Version version = Version.get(ver);
        if (version.ordinal() < Version.v3.ordinal()) {
            throw new UnsupportedVersionException("This version is no longer supported: " + version.toString(), UnsupportedVersionException.IncompatibleVersion);
        }
        final byte reserved = buff.get(); // tossed away for now.
        final Short flags = buff.getShort();
        final boolean isRequest = (flags & FLAG_REQUEST) > 0;

        final long seq = buff.getLong();
        final int size = buff.getInt();
        final long mgmtId = buff.getLong();
        final long agentId = buff.getLong();

        byte[] command = null;
        int offset = 0;
        if (buff.hasArray()) {
            command = buff.array();
            offset = buff.arrayOffset() + buff.position();
        } else {
            command = new byte[buff.remaining()];
            buff.get(command);
            offset = 0;
        }

        final String content = new String(command, offset, command.length - offset);

        if (isRequest) {
            return new Request(version, seq, agentId, mgmtId, flags, content);
        } else {
            return new Response(Version.get(ver), seq, agentId, mgmtId, flags, content);
        }
    }

    public long getAgentId() {
    	return _agentId;
    }
    
    public static boolean requiresSequentialExecution(final byte[] bytes) {
        return (bytes[3] & FLAG_IN_SEQUENCE) > 0;
    }
    
    public static Version getVersion(final byte[] bytes) throws UnsupportedVersionException {
    	try {
    		return Version.get(bytes[0]);
    	} catch (UnsupportedVersionException e) {
    		throw new CloudRuntimeException("Unsupported version: " + bytes[0]);
    	}
    }
    
    public static long getManagementServerId(final byte[] bytes) {
    	return NumbersUtil.bytesToLong(bytes, 16);
    }
    
    public static long getAgentId(final byte[] bytes) {
    	return NumbersUtil.bytesToLong(bytes, 24);
    }
    
    public static boolean fromServer(final byte[] bytes) {
    	return (bytes[3] & FLAG_FROM_SERVER)  > 0;
    }
    
    public static boolean isRequest(final byte[] bytes) {
    	return (bytes[3] & FLAG_REQUEST) > 0;
    }
    
    public static long getSequence(final byte[] bytes) {
    	return NumbersUtil.bytesToLong(bytes, 4);
    }
    
    public static boolean isControl(final byte[] bytes) {
        return (bytes[3] & FLAG_CONTROL) > 0;
    }
}
