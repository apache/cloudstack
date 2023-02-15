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

import java.util.Arrays;

import streamer.ByteBuffer;

/**
 * One or more elements of different types.
 *
 * Only prefixed tags are supported.
 */
public class Sequence extends Tag {

    public Tag[] tags;

    public Sequence(String name) {
        super(name);
        tagType = SEQUENCE;
        // Sequence and SequenceOf are always encoded as constructed
        constructed = true;
    }

    @Override
    public long calculateLengthOfValuePayload() {
        long sum = 0;

        for (Tag tag : tags) {
            long tagLength = tag.calculateFullLength();
            sum += tagLength;
        }

        return sum;
    }

    @Override
    public void writeTagValuePayload(ByteBuffer buf) {
        // Write tags
        for (Tag tag : tags) {
            tag.writeTag(buf);
        }
    }

    @Override
    public void readTagValue(ByteBuffer buf, BerType typeAndFlags) {
        // Type is already read by parent parser

        long length = buf.readBerLength();
        if (length > buf.remainderLength())
            throw new RuntimeException("BER sequence is too long: " + length + " bytes, while buffer remainder length is " + buf.remainderLength() + ". Data: " + buf
                    + ".");

        ByteBuffer value = buf.readBytes((int)length);
        parseContent(value);

        value.unref();
    }

    protected void parseContent(ByteBuffer buf) {
        for (int i = 0; buf.remainderLength() > 0 && i < tags.length; i++) {
            BerType typeAndFlags = readBerType(buf);

            // If current tag does not match data in buffer
            if (!tags[i].isTypeValid(typeAndFlags)) {

                // If tag is required, then throw exception
                if (!tags[i].optional) {
                    throw new RuntimeException("[" + this + "] ERROR: Required tag is missed: " + tags[i] + ". Unexpected tag type: " + typeAndFlags + ". Data: " + buf
                            + ".");
                } else {
                    // One or more tags are omitted, so skip them
                    for (; i < tags.length; i++) {
                        if (tags[i].isTypeValid(typeAndFlags)) {
                            break;
                        }
                    }

                    if (i >= tags.length || !tags[i].isTypeValid(typeAndFlags)) {
                        throw new RuntimeException("[" + this + "] ERROR: No more tags to read or skip, but some data still left in buffer. Unexpected tag type: "
                                + typeAndFlags + ". Data: " + buf + ".");
                    }
                }
            }

            tags[i].readTag(buf, typeAndFlags);
        }

    }

    @Override
    public boolean isTypeValid(BerType typeAndFlags, boolean explicit) {
        if (explicit)
            return typeAndFlags.tagClass == tagClass && typeAndFlags.constructed && typeAndFlags.typeOrTagNumber == tagNumber;
        else
            // Sequences are always encoded as "constructed" in BER.
            return typeAndFlags.tagClass == UNIVERSAL_CLASS && typeAndFlags.constructed && typeAndFlags.typeOrTagNumber == SEQUENCE;
    }

    @Override
    public Tag deepCopy(String suffix) {
        return new Sequence(name + suffix).copyFrom(this);
    }

    @Override
    public Tag copyFrom(Tag tag) {
        super.copyFrom(tag);

        if (tags.length != ((Sequence)tag).tags.length)
            throw new RuntimeException("Incompatible sequences. This: " + this + ", another: " + tag + ".");

        for (int i = 0; i < tags.length; i++) {
            tags[i].copyFrom(((Sequence)tag).tags[i]);
        }

        return this;
    }

    @Override
    public String toString() {
        return super.toString() + "{" + Arrays.toString(tags) + " }";
    }

    @Override
    public boolean isValueSet() {
        return tags != null;
    }

}
