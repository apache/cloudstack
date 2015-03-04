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

import java.util.ArrayList;

import streamer.ByteBuffer;

/**
 * Zero or more elements of same type (array).
 */
public class SequenceOf extends Sequence {

    /**
     * Type of this array.
     */
    public Tag type;

    /* Values are stored in tags[] variable inherited from Sequence. */

    public SequenceOf(String name) {
        super(name);
    }

    @Override
    protected void parseContent(ByteBuffer buf) {
        ArrayList<Tag> tagList = new ArrayList<Tag>();

        for (int index = 0; buf.remainderLength() > 0; index++) {
            // End of array is marked with two zero bytes (0x00 0x00)
            if (buf.peekUnsignedByte(0) == 0x00 && buf.peekUnsignedByte(1) == 0x00) {
                break;
            }

            Tag tag = type.deepCopy(index);

            tag.readTag(buf);
            tagList.add(tag);
        }

        tags = tagList.toArray(new Tag[tagList.size()]);
    }

    @Override
    public Tag deepCopy(String suffix) {
        return new SequenceOf(name + suffix).copyFrom(this);
    }

    @Override
    public Tag copyFrom(Tag tag) {
        super.copyFrom(tag);
        // We can create shallow copy of type, because it will not be modified
        type = ((SequenceOf)tag).type;

        tags = new Tag[((Sequence)tag).tags.length];
        for (int i = 0; i < tags.length; i++) {
            tags[i] = ((Sequence)tag).tags[i].deepCopy("");
        }

        return this;
    }

    @Override
    public String toString() {
        return super.toString() + ": " + type;
    }

}
