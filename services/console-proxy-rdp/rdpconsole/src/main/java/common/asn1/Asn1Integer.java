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
package common.asn1;

import streamer.ByteBuffer;

/**
 * Variable length integer.
 */
public class Asn1Integer extends Tag {

    public Long value = null;

    public Asn1Integer(String name) {
        super(name);
        tagType = INTEGER;
    }

    @Override
    public void readTagValue(ByteBuffer buf, BerType typeAndFlags) {
        // Type is already read by parent parser

        long length = buf.readBerLength();
        if (length > 8)
            throw new RuntimeException("[" + this + "] ERROR: Integer value is too long: " + length + " bytes. Cannot handle integers more than 8 bytes long. Data: "
                    + buf + ".");

        value = buf.readSignedVarInt((int)length);
    }

    @Override
    public Tag deepCopy(String suffix) {
        return new Asn1Integer(name + suffix).copyFrom(this);
    }

    @Override
    public Tag copyFrom(Tag tag) {
        super.copyFrom(tag);
        value = ((Asn1Integer)tag).value;
        return this;
    }

    @Override
    public String toString() {
        return super.toString() + "= " + value;
    }

    @Override
    public long calculateLengthOfValuePayload() {
        if (value <= 0xff)
            return 1;
        if (value <= 0xffFF)
            return 2;
        if (value <= 0xffFFff)
            return 3;
        if (value <= 0xffFFffFFL)
            return 4;
        if (value <= 0xffFFffFFffL)
            return 5;
        if (value <= 0xffFFffFFffFFL)
            return 6;
        if (value <= 0xffFFffFFffFFffL)
            return 7;

        return 8;
    }

    @Override
    public void writeTagValuePayload(ByteBuffer buf) {
        long value = this.value.longValue();

        if (value < 0xff) {
            buf.writeByte((int)value);
        } else if (value <= 0xffFF) {
            buf.writeShort((int)value);
        } else if (value <= 0xffFFff) {
            buf.writeByte((int)(value >> 16));
            buf.writeShort((int)value);
        } else if (value <= 0xffFFffFFL) {
            buf.writeInt((int)value);
        } else if (value <= 0xffFFffFFffL) {
            buf.writeByte((int)(value >> 32));
            buf.writeInt((int)value);
        } else if (value <= 0xffFFffFFffFFL) {
            buf.writeShort((int)(value >> 32));
            buf.writeInt((int)value);
        } else if (value <= 0xffFFffFFffFFffL) {
            buf.writeByte((int)(value >> (32 + 16)));
            buf.writeShort((int)(value >> 32));
            buf.writeInt((int)value);
        } else {
            buf.writeInt((int)(value >> 32));
            buf.writeInt((int)value);
        }
    }

    @Override
    public boolean isValueSet() {
        return value != null;
    }

}
