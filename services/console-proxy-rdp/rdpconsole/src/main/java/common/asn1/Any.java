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
 * Any type. Don't forget to set type.
 */
public class Any extends Tag {

    /**
     * Raw bytes of any value.
     */
    public ByteBuffer value;

    public Any(String name) {
        super(name);
    }

    @Override
    public boolean isValueSet() {
        return value != null;
    }

    @Override
    public long calculateLengthOfValuePayload() {
        return value.length;
    }

    @Override
    public void writeTagValuePayload(ByteBuffer buf) {
        buf.writeBytes(value);
    }

    @Override
    public void readTagValue(ByteBuffer buf, BerType typeAndFlags) {
        long length = buf.readBerLength();

        value = buf.readBytes((int)length);
    }

    @Override
    public Tag deepCopy(String suffix) {
        return new Any(name + suffix).copyFrom(this);
    }

    @Override
    public Tag copyFrom(Tag tag) {
        super.copyFrom(tag);
        tagType = tag.tagType;
        value = new ByteBuffer(((Any)tag).value.toByteArray());
        return this;
    }

    @Override
    public boolean isTypeValid(BerType typeAndFlags, boolean explicit) {
        if (explicit)
            return typeAndFlags.tagClass == tagClass && typeAndFlags.constructed && typeAndFlags.typeOrTagNumber == tagNumber;
        else
            return true;
    }

}
