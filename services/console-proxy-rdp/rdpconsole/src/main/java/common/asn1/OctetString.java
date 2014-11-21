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

public class OctetString extends Tag {

    public ByteBuffer value = null;

    public OctetString(String name) {
        super(name);
        tagType = OCTET_STRING;
    }

    @Override
    public void readTagValue(ByteBuffer buf, BerType typeAndFlags) {
        // Type is already read by parent parser

        long length = buf.readBerLength();

        if (length > buf.length)
            throw new RuntimeException("BER octet string is too long: " + length + " bytes. Data: " + buf + ".");

        value = buf.readBytes((int)length);
    }

    @Override
    public Tag deepCopy(String suffix) {
        return new OctetString(name + suffix).copyFrom(this);
    }

    @Override
    public Tag copyFrom(Tag tag) {
        super.copyFrom(tag);
        value = ((OctetString)tag).value;
        return this;
    }

    @Override
    public String toString() {
        return super.toString() + "= " + value;
    }

    @Override
    public long calculateLengthOfValuePayload() {
        if (value != null)
            return value.length;
        else
            return 0;
    }

    @Override
    public void writeTagValuePayload(ByteBuffer buf) {
        if (value != null)
            buf.writeBytes(value);
        else
            return;
    }

    @Override
    public boolean isValueSet() {
        return value != null;
    }

}
