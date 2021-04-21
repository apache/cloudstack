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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.cloud.utils.HumanReadableJson;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BadCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.SecStorageFirewallCfgCommand.PortConfig;
import com.cloud.exception.UnsupportedVersionException;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Request is a simple wrapper around command and answer to add sequencing,
 * versioning, and flags. Note that the version here represents the changes
 * in the over the wire protocol. For example, if we decide to not use Gson.
 * It does not version the changes in the actual commands. That's expected
 * to be done by adding new classes to the command and answer list.
 *
 * A request looks as follows:
 * 1. Version - 1 byte;
 * 2. Flags - 3 bytes;
 * 3. Sequence - 8 bytes;
 * 4. Length - 4 bytes;
 * 5. ManagementServerId - 8 bytes;
 * 6. AgentId - 8 bytes;
 * 7. Data Package.
 *
 */
public class Request {
    private static final Logger s_logger = Logger.getLogger(Request.class);

    protected static final Gson s_gson = GsonHelper.getGson();
    protected static final Gson s_gogger = GsonHelper.getGsonLogger();
    protected static final Logger s_gsonLogger = GsonHelper.getLogger();

    public enum Version {
        v1, // using gson to marshall
        v2, // now using gson as marshalled.
        v3; // Adding routing information into the Request data structure.

        public static Version get(final byte ver) throws UnsupportedVersionException {
            for (final Version version : Version.values()) {
                if (ver == version.ordinal()) {
                    return version;
                }
            }
            throw new UnsupportedVersionException("Can't lookup version: " + ver, UnsupportedVersionException.UnknownVersion);
        }
    };

    protected static final short FLAG_RESPONSE = 0x0;
    protected static final short FLAG_REQUEST = 0x1;
    protected static final short FLAG_STOP_ON_ERROR = 0x2;
    protected static final short FLAG_IN_SEQUENCE = 0x4;
    protected static final short FLAG_FROM_SERVER = 0x20;
    protected static final short FLAG_CONTROL = 0x40;
    protected static final short FLAG_COMPRESSED = 0x80;

    protected Version _ver;
    protected long _session;
    protected long _seq;
    protected short _flags;
    protected long _mgmtId;
    protected long _via;
    protected long _agentId;
    protected Command[] _cmds;
    protected String _content;
    protected String _agentName;

    protected Request() {
    }

    protected Request(Version ver, long seq, long agentId, long mgmtId, long via, short flags, final Command[] cmds) {
        _ver = ver;
        _cmds = cmds;
        _flags = flags;
        _seq = seq;
        _via = via;
        _agentId = agentId;
        _mgmtId = mgmtId;
        setInSequence(cmds);
    }

    protected Request(Version ver, long seq, long agentId, long mgmtId, short flags, final Command[] cmds) {
        this(ver, seq, agentId, mgmtId, agentId, flags, cmds);
    }

    protected Request(Version ver, long seq, long agentId, long mgmtId, long via, short flags, final String content) {
        this(ver, seq, agentId, mgmtId, via, flags, (Command[])null);
        _content = content;
    }

    public Request(long agentId, long mgmtId, Command command, boolean fromServer) {
        this(agentId, mgmtId, new Command[] {command}, true, fromServer);
    }

    public Request(long agentId, long mgmtId, Command[] cmds, boolean stopOnError, boolean fromServer) {
        this(Version.v1, -1l, agentId, mgmtId, (short)0, cmds);
        setStopOnError(stopOnError);
        setFromServer(fromServer);
    }

    public Request(long agentId, String agentName, long mgmtId, Command[] cmds, boolean stopOnError, boolean fromServer) {
        this(agentId, mgmtId, cmds, stopOnError, fromServer);
        setAgentName(agentName);
    }

    public void setSequence(long seq) {
        _seq = seq;
    }

    protected void setInSequence(Command[] cmds) {
        if (cmds == null) {
            return;
        }
        for (Command cmd : cmds) {
            if (cmd.executeInSequence()) {
                setInSequence(true);
                break;
            }
        }
    }

    protected Request(final Request that, final Command[] cmds) {
        _ver = that._ver;
        _seq = that._seq;
        setInSequence(that.executeInSequence());
        setStopOnError(that.stopOnError());
        _cmds = cmds;
        _mgmtId = that._mgmtId;
        _via = that._via;
        _agentId = that._agentId;
        _agentName = that._agentName;
        setFromServer(!that.isFromServer());
    }

    private final void setStopOnError(boolean stopOnError) {
        _flags |= (stopOnError ? FLAG_STOP_ON_ERROR : 0);
    }

    private final void setAgentName(String agentName) {
        _agentName = agentName;
    }

    private final void setInSequence(boolean inSequence) {
        _flags |= (inSequence ? FLAG_IN_SEQUENCE : 0);
    }

    public boolean isControl() {
        return (_flags & FLAG_CONTROL) > 0;
    }

    public void setControl(boolean control) {
        _flags |= (control ? FLAG_CONTROL : 0);
    }

    private final void setFromServer(boolean fromServer) {
        _flags |= (fromServer ? FLAG_FROM_SERVER : 0);
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

    public void setVia(long viaId) {
        _via = viaId;
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
            try {
                StringReader reader = new StringReader(_content);
                JsonReader jsonReader = new JsonReader(reader);
                jsonReader.setLenient(true);
                _cmds = s_gson.fromJson(jsonReader, (Type)Command[].class);
            } catch (JsonParseException e) {
                _cmds = new Command[] { new BadCommand() };
            } catch (RuntimeException e) {
                s_logger.error("Caught problem with " + _content, e);
                throw e;
            }
        }
        return _cmds;
    }

    protected String getType() {
        return "Cmd ";
    }

    protected ByteBuffer serializeHeader(final int contentSize) {
        final ByteBuffer buffer = ByteBuffer.allocate(40);
        buffer.put(getVersionInByte());
        buffer.put((byte)0);
        buffer.putShort(getFlags());
        buffer.putLong(_seq);
        // The size here is uncompressed size, if the data is compressed.
        buffer.putInt(contentSize);
        buffer.putLong(_mgmtId);
        buffer.putLong(_agentId);
        buffer.putLong(_via);
        buffer.flip();

        return buffer;
    }

    public static ByteBuffer doDecompress(ByteBuffer buffer, int length) {
        byte[] byteArrayIn = new byte[1024];
        ByteArrayInputStream byteIn;
        if (buffer.hasArray()) {
            byteIn = new ByteArrayInputStream(buffer.array(), buffer.position() + buffer.arrayOffset(), buffer.remaining());
        } else {
            byte[] array = new byte[buffer.limit() - buffer.position()];
            buffer.get(array);
            byteIn = new ByteArrayInputStream(array);
        }
        ByteBuffer retBuff = ByteBuffer.allocate(length);
        int len = 0;
        try {
            GZIPInputStream in = new GZIPInputStream(byteIn);
            while ((len = in.read(byteArrayIn)) > 0) {
                retBuff.put(byteArrayIn, 0, len);
            }
            in.close();
        } catch (IOException e) {
            s_logger.error("Fail to decompress the request!", e);
        }
        retBuff.flip();
        return retBuff;
    }

    public static ByteBuffer doCompress(ByteBuffer buffer, int length) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream(length);
        byte[] array;
        if (buffer.hasArray()) {
            array = buffer.array();
        } else {
            array = new byte[buffer.capacity()];
            buffer.get(array);
        }
        try {
            GZIPOutputStream out = new GZIPOutputStream(byteOut, length);
            out.write(array);
            out.finish();
            out.close();
        } catch (IOException e) {
            s_logger.error("Fail to compress the request!", e);
        }
        return ByteBuffer.wrap(byteOut.toByteArray());
    }

    public ByteBuffer[] toBytes() {
        final ByteBuffer[] buffers = new ByteBuffer[2];
        ByteBuffer tmp;

        if (_content == null) {
            _content = s_gson.toJson(_cmds, _cmds.getClass());
        }
        tmp = ByteBuffer.wrap(_content.getBytes());
        int capacity = tmp.capacity();
        /* Check if we need to compress the data */
        if (capacity >= 8192) {
            tmp = doCompress(tmp, capacity);
            _flags |= FLAG_COMPRESSED;
        }
        buffers[1] = tmp;
        buffers[0] = serializeHeader(capacity);

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

    public void logD(String msg) {
        logD(msg, true);
    }

    public void logD(String msg, boolean logContent) {
        if (s_logger.isDebugEnabled()) {
            String log = log(msg, logContent, Level.DEBUG);
            if (log != null) {
                s_logger.debug(log);
            }
        }
    }

    public void logT(String msg, boolean logD) {
        if (s_logger.isTraceEnabled()) {
            String log = log(msg, true, Level.TRACE);
            if (log != null) {
                s_logger.trace(log);
            }
        } else if (logD && s_logger.isDebugEnabled()) {
            String log = log(msg, false, Level.DEBUG);
            if (log != null) {
                s_logger.debug(log);
            }
        }
    }

    @Override
    public String toString() {
        return log("", true, Level.DEBUG);
    }

    protected String log(String msg, boolean logContent, Level level) {
        StringBuilder content = new StringBuilder();
        if (logContent) {
            if (_cmds == null) {
                try {
                    _cmds = s_gson.fromJson(_content, this instanceof Response ? Answer[].class : Command[].class);
                } catch (RuntimeException e) {
                    s_logger.error("Unable to deserialize from json: " + _content);
                    throw e;
                }
            }
            try {
                s_gogger.toJson(_cmds, content);
            } catch (Throwable e) {
                StringBuilder buff = new StringBuilder();
                for (Command cmd : _cmds) {
                    buff.append(cmd.getClass().getSimpleName()).append("/");
                }
                s_logger.error("Gson serialization error " + buff.toString(), e);
                assert false : "More gson errors on " + buff.toString();
                return "";
            }
            content = new StringBuilder(HumanReadableJson.getHumanReadableBytesJson(content.toString()));
            if (content.length() <= (1 + _cmds.length * 3)) {
                return null;
            }
        } else {
            if (_cmds == null) {
                _cmds = s_gson.fromJson(_content, this instanceof Response ? Answer[].class : Command[].class);
            }
            content.append("{ ");
            for (Command cmd : _cmds) {
                content.append(cmd.getClass().getSimpleName()).append(", ");
            }
            content.replace(content.length() - 2, content.length(), " }");

        }

        StringBuilder buf = new StringBuilder("Seq ");

        buf.append(_agentId).append("-").append(_seq).append(": ");

        buf.append(msg);
        buf.append(" { ").append(getType());
        if (_agentName != null) {
            buf.append(", MgmtId: ").append(_mgmtId).append(", via: ").append(_via).append("(" + _agentName + ")");
        } else {
            buf.append(", MgmtId: ").append(_mgmtId).append(", via: ").append(_via);
        }
        buf.append(", Ver: ").append(_ver.toString());
        buf.append(", Flags: ").append(Integer.toBinaryString(getFlags())).append(", ");
        String cleanContent = content.toString();
        if(cleanContent.contains("password")) {
            buf.append(cleanPassword(cleanContent));
        } else {
            buf.append(content);
        }
        buf.append(" }");
        return buf.toString();
    }

    public static String cleanPassword(String logString) {
        String cleanLogString = null;
        if (logString != null) {
            cleanLogString = logString;
            String[] temp = logString.split(",");
            int i = 0;
            if (temp != null) {
                while (i < temp.length) {
                    temp[i] = StringUtils.cleanString(temp[i]);
                    i++;
                }
                List<String> stringList = new ArrayList<String>();
                Collections.addAll(stringList, temp);
                cleanLogString = StringUtils.join(stringList, ",");
            }
        }
        return cleanLogString;
    }

    /**
     * Factory method for Request and Response. It expects the bytes to be
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
        ByteBuffer buff = ByteBuffer.wrap(bytes);
        final byte ver = buff.get();
        final Version version = Version.get(ver);
        if (version.ordinal() != Version.v1.ordinal() && version.ordinal() != Version.v3.ordinal()) {
            throw new UnsupportedVersionException("This version is no longer supported: " + version.toString(), UnsupportedVersionException.IncompatibleVersion);
        }
        buff.get();
        final short flags = buff.getShort();
        final boolean isRequest = (flags & FLAG_REQUEST) > 0;

        final long seq = buff.getLong();
        // The size here is uncompressed size, if the data is compressed.
        final int size = buff.getInt();
        final long mgmtId = buff.getLong();
        final long agentId = buff.getLong();

        long via;
        if (version.ordinal() == Version.v1.ordinal()) {
            via = buff.getLong();
        } else {
            via = agentId;
        }

        if ((flags & FLAG_COMPRESSED) != 0) {
            buff = doDecompress(buff, size);
        }

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
            return new Request(version, seq, agentId, mgmtId, via, flags, content);
        } else {
            return new Response(Version.get(ver), seq, agentId, mgmtId, via, flags, content);
        }
    }

    public long getAgentId() {
        return _agentId;
    }

    public long getViaAgentId() {
        return _via;
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

    public static long getViaAgentId(final byte[] bytes) {
        return NumbersUtil.bytesToLong(bytes, 32);
    }

    public static boolean fromServer(final byte[] bytes) {
        return (bytes[3] & FLAG_FROM_SERVER) > 0;
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

    public static class NwGroupsCommandTypeAdaptor implements JsonDeserializer<Pair<Long, Long>>, JsonSerializer<Pair<Long, Long>> {

        public NwGroupsCommandTypeAdaptor() {
        }

        @Override
        public JsonElement serialize(Pair<Long, Long> src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            JsonArray array = new JsonArray();
            if (src.first() != null) {
                array.add(s_gson.toJsonTree(src.first()));
            } else {
                array.add(new JsonNull());
            }

            if (src.second() != null) {
                array.add(s_gson.toJsonTree(src.second()));
            } else {
                array.add(new JsonNull());
            }

            return array;
        }

        @Override
        public Pair<Long, Long> deserialize(JsonElement json, java.lang.reflect.Type type, JsonDeserializationContext context) throws JsonParseException {
            Pair<Long, Long> pairs = new Pair<Long, Long>(null, null);
            JsonArray array = json.getAsJsonArray();
            if (array.size() != 2) {
                return pairs;
            }
            JsonElement element = array.get(0);
            if (!element.isJsonNull()) {
                pairs.first(element.getAsLong());
            }

            element = array.get(1);
            if (!element.isJsonNull()) {
                pairs.second(element.getAsLong());
            }

            return pairs;
        }

    }

    public static class PortConfigListTypeAdaptor implements JsonDeserializer<List<PortConfig>>, JsonSerializer<List<PortConfig>> {

        public PortConfigListTypeAdaptor() {
        }

        @Override
        public JsonElement serialize(List<PortConfig> src, Type typeOfSrc, JsonSerializationContext context) {
            if (src.size() == 0) {
                return new JsonNull();
            }
            JsonArray array = new JsonArray();
            for (PortConfig pc : src) {
                array.add(s_gson.toJsonTree(pc));
            }

            return array;
        }

        @Override
        public List<PortConfig> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull()) {
                return new ArrayList<PortConfig>();
            }
            List<PortConfig> pcs = new ArrayList<PortConfig>();
            JsonArray array = json.getAsJsonArray();
            Iterator<JsonElement> it = array.iterator();
            while (it.hasNext()) {
                JsonElement element = it.next();
                pcs.add(s_gson.fromJson(element, PortConfig.class));
            }
            return pcs;
        }
    }
}
