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

public abstract class Tag implements Asn1Constants {

    /**
     * Name of this tag, for debugging purposes.
     */
    public String name = "";

    /**
     * Is this tag required or optional, for explicit tags only.
     */
    public boolean optional = false;

    /**
     * Tag primitive (e.g. implicit boolean), or constructed (e.g. sequence, or
     * explicit boolean).
     */
    public boolean constructed = false;

    /**
     * Class of tag, when it is explicit.
     */
    public int tagClass = UNIVERSAL_CLASS;

    /**
     * Tag number (e.g. index in sequence), when tag is explicit.
     */
    public int tagNumber = -1;

    /**
     * Tag type (e.g. INDER), when tag is implicit.
     */
    public int tagType = -1;

    /**
     * If tag is explicit, then it is prefixed with tag number, so it can be
     * optional or used in unordered set.
     */
    public boolean explicit = false;

    public Tag(String name) {
        this.name = name;
    }

    /**
     * Write tag value, with or without prefix.
     */
    public void writeTag(ByteBuffer buf) {

        if (!isMustBeWritten())
            return;

        // Write prefix, when necessary
        if (explicit) {

            // Write tag prefix, always constructed
            BerType berTagPrefix = new BerType(tagClass, true, tagNumber);
            writeBerType(buf, berTagPrefix);

            // Write tag prefix length
            buf.writeBerLength(calculateLength());

            // Write tag value
            writeTagValue(buf);
        } else {
            // If implicit, just write tag value
            writeTagValue(buf);
        }
    }

    /**
     * Must return true when value of this tag is set or tag is required, so it
     * can be written, false otherwise.
     */
    public boolean isMustBeWritten() {
        return !optional || isValueSet();
    }

    /**
     * Must return true when value of this tag is set or tag is required, so it
     * can be written, false otherwise.
     */
    public abstract boolean isValueSet();

    /**
     * Calculate full length of tag, including type (or prefix, when explicit).
     */
    public long calculateFullLength() {
        if (!isMustBeWritten())
            return 0;

        // Length of value, including type
        long length = calculateLength();

        if (!explicit) {
            // Length of tag type and it length
            length += calculateLengthOfTagTypeOrTagNumber(tagType) + calculateLengthOfLength(length);
        } else {
            // Length of tag prefix and it length
            length += calculateLengthOfTagTypeOrTagNumber(tagNumber) + calculateLengthOfLength(length);
        }

        return length;
    }

    /**
     * Calculate length of tag, including type when explicit, but without length
     * of prefix (or type, when implicit).
     */
    public long calculateLength() {
        if (!isMustBeWritten())
            return 0;

        // Length of value
        long length = calculateLengthOfValuePayload();

        if (explicit) {
            // Length of tag type and it length
            length += calculateLengthOfTagTypeOrTagNumber(tagType) + calculateLengthOfLength(length);
        }

        return length;
    }

    /**
     * Calculate length of BER length.
     */
    public int calculateLengthOfLength(long length) {
        if (length < 0)
            throw new RuntimeException("[" + this + "] ERROR: Length of tag cannot be less than zero: " + length + ".");

        if (length <= 0x7f)
            return 1;
        if (length <= 0xff)
            return 2;
        if (length <= 0xffFF)
            return 3;
        if (length <= 0xffFFff)
            return 4;
        if (length <= 0xffFFffFFL)
            return 5;
        if (length <= 0xffFFffFFffL)
            return 6;
        if (length <= 0xffFFffFFffFFL)
            return 7;
        if (length <= 0xffFFffFFffFFffL)
            return 8;

        return 9;
    }

    /**
     * Calculate length of type to tag number. Values less than 31 are encoded
     * using lower 5 bits of first byte of tag. Values larger than 31 are
     * indicated by lower 5 bits set to 1 (0x1F, 31), and next bytes are contain
     * value in network order, where topmost bit of byte (0x80) indicates is value
     * contains more bytes, i.e. last byte of sequence has this bit set to 0.
     */
    public int calculateLengthOfTagTypeOrTagNumber(int tagType) {
        if (tagType >= EXTENDED_TYPE)
            throw new RuntimeException("Multibyte tag types are not supported yet.");

        return 1;
    }

    /**
     * Calculate length of payload only, without tag prefix, tag type, and
     * lengths.
     *
     * @return
     */
    public abstract long calculateLengthOfValuePayload();

    /**
     * Write tag value only, without prefix.
     */
    public void writeTagValue(ByteBuffer buf) {

        // Write type
        BerType valueType = new BerType(UNIVERSAL_CLASS, constructed, tagType);
        writeBerType(buf, valueType);

        // Write length
        long lengthOfPayload = calculateLengthOfValuePayload();
        buf.writeBerLength(lengthOfPayload);

        // Store cursor to check is calculated length matches length of actual bytes
        // written
        int storedCursor = buf.cursor;

        // Write value
        writeTagValuePayload(buf);

        // Check is calculated length matches length of actual bytes written, to catch errors early
        int actualLength = buf.cursor - storedCursor;
        if (actualLength != lengthOfPayload)
            throw new RuntimeException("[" + this + "] ERROR: Unexpected length of data in buffer. Expected " + lengthOfPayload + " of bytes of payload, but "
                    + actualLength + " bytes are written instead. Data: " + buf + ".");
    }

    /**
     * Write tag value only, without prefix, tag type, and length.
     */
    public abstract void writeTagValuePayload(ByteBuffer buf);

    /**
     * Read required tag, i.e. we are 100% sure that byte buffer will contain this
     * tag, or exception will be thrown otherwise.
     *
     * @param buf
     *          buffer with tag data
     */
    public void readTag(ByteBuffer buf) {
        BerType typeAndFlags = readBerType(buf);

        // * DEBUG */System.out.println("Tag, read " + typeAndFlags);

        if (!isTypeValid(typeAndFlags))
            throw new RuntimeException("[" + this + "] Unexpected type: " + typeAndFlags + ".");

        readTag(buf, typeAndFlags);
    }

    /**
     * Read tag when it type is already read.
     */
    public void readTag(ByteBuffer buf, BerType typeAndFlags) {

        if (explicit) {
            long length = buf.readBerLength();

            if (length > buf.length)
                throw new RuntimeException("BER value is too long: " + length + " bytes. Data: " + buf + ".");

            ByteBuffer value = buf.readBytes((int)length);

            readTagValue(value);

            value.unref();
        } else {

            readTagValue(buf, typeAndFlags);
        }
    }

    /**
     * Read tag value only, i.e. it prefix is already read.
     */
    public void readTagValue(ByteBuffer value) {
        BerType typeAndFlags = readBerType(value);

        // * DEBUG */System.out.println("Tag, read value " + typeAndFlags);

        if (!isTypeValid(typeAndFlags, false))
            throw new RuntimeException("[" + this + "] Unexpected type: " + typeAndFlags + ".");

        readTagValue(value, typeAndFlags);
    }

    /**
     * Check are tag type and flags valid for this tag.
     */
    public final boolean isTypeValid(BerType typeAndFlags) {
        return isTypeValid(typeAndFlags, explicit);
    }

    /**
     * Check are tag type and flags valid for this tag with or without tag prefix.
     *
     * @param explicit
     *          if true, then value is wrapped in tag prefix
     */
    public boolean isTypeValid(BerType typeAndFlags, boolean explicit) {
        if (explicit)
            return typeAndFlags.tagClass == tagClass && typeAndFlags.constructed && typeAndFlags.typeOrTagNumber == tagNumber;
        else
            return typeAndFlags.tagClass == UNIVERSAL_CLASS && !typeAndFlags.constructed && typeAndFlags.typeOrTagNumber == tagType;
    }

    @Override
    public String toString() {
        return "  \nTag [name="
                + name

                + ((constructed) ? ", constructed=" + constructed : "")

                + (", tagType=" + tagTypeOrNumberToString(UNIVERSAL_CLASS, tagType))

                + ((explicit) ? ", explicit=" + explicit + ", optional=" + optional + ", tagClass=" + tagClassToString(tagClass) + ", tagNumber="
                        + tagTypeOrNumberToString(tagClass, tagNumber) : "") + "]";
    }

    public static final String tagTypeOrNumberToString(int tagClass, int tagTypeOrNumber) {
        switch (tagClass) {
        case UNIVERSAL_CLASS:
            switch (tagTypeOrNumber) {
            case EOF:
                return "EOF";
            case BOOLEAN:
                return "BOOLEAN";
            case INTEGER:
                return "INTEGER";
            case BIT_STRING:
                return "BIT_STRING";
            case OCTET_STRING:
                return "OCTET_STRING";
            case NULL:
                return "NULL";
            case OBJECT_ID:
                return "OBJECT_ID";
            case REAL:
                return "REAL";
            case ENUMERATED:
                return "ENUMERATED";
            case SEQUENCE:
                return "SEQUENCE";
            case SET:
                return "SET";
            case NUMERIC_STRING:
                return "NUMERIC_STRING";
            case PRINTABLE_STRING:
                return "PRINTABLE_STRING";
            case TELETEX_STRING:
                return "TELETEX_STRING";
            case VIDEOTEXT_STRING:
                return "VIDEOTEXT_STRING";
            case IA5_STRING:
                return "IA5_STRING";
            case UTCTIME:
                return "UTCTIME";
            case GENERAL_TIME:
                return "GENERAL_TIME";
            case GRAPHIC_STRING:
                return "GRAPHIC_STRING";
            case VISIBLE_STRING:
                return "VISIBLE_STRING";
            case GENERAL_STRING:
                return "GENERAL_STRING";
            case EXTENDED_TYPE:
                return "EXTENDED_TYPE (multibyte)";
            default:
                return "UNKNOWN(" + tagTypeOrNumber + ")";

            }

        default:
            return "[" + tagTypeOrNumber + "]";
        }
    }

    public static final String tagClassToString(int tagClass) {
        switch (tagClass) {
        case UNIVERSAL_CLASS:
            return "UNIVERSAL";
        case CONTEXT_CLASS:
            return "CONTEXT";
        case APPLICATION_CLASS:
            return "APPLICATION";
        case PRIVATE_CLASS:
            return "PRIVATE";
        default:
            return "UNKNOWN";
        }
    }

    /**
     * Read BER tag type.
     */
    public BerType readBerType(ByteBuffer buf) {
        int typeAndFlags = buf.readUnsignedByte();

        int tagClass = typeAndFlags & CLASS_MASK;

        boolean constructed = (typeAndFlags & CONSTRUCTED) != 0;

        int type = typeAndFlags & TYPE_MASK;
        if (type == EXTENDED_TYPE)
            throw new RuntimeException("Extended tag types/numbers (31+) are not supported yet.");

        return new BerType(tagClass, constructed, type);
    }

    /**
     * Write BER tag type.
     */
    public void writeBerType(ByteBuffer buf, BerType berType) {

        if (berType.typeOrTagNumber >= EXTENDED_TYPE || berType.typeOrTagNumber < 0)
            throw new RuntimeException("Extended tag types/numbers (31+) are not supported yet: " + berType + ".");

        if ((berType.tagClass & CLASS_MASK) != berType.tagClass)
            throw new RuntimeException("Value of BER tag class is out of range: " + berType.tagClass + ". Expected values: " + UNIVERSAL_CLASS + ", " + CONTEXT_CLASS
                    + ", " + APPLICATION_CLASS + ", " + PRIVATE_CLASS + ".");

        int typeAndFlags = berType.tagClass | ((berType.constructed) ? CONSTRUCTED : 0) | berType.typeOrTagNumber;

        buf.writeByte(typeAndFlags);
    }

    /**
     * Read tag value only, i.e. it prefix is already read, when value type is
     * already read.
     *
     * @param buf
     *          buffer with tag data
     */
    public abstract void readTagValue(ByteBuffer buf, BerType typeAndFlags);

    /**
     * Create deep copy of this tag with given suffix appended to name.
     *
     * @param suffix
     *          suffix to add to tag name, or empty string
     * @return deep copy of this tag
     */
    public abstract Tag deepCopy(String suffix);

    /**
     * Create deep copy of this tag for array or set.
     *
     * @param index
     *          index of element in array or set
     * @return deep copy of this tag
     */
    public Tag deepCopy(int index) {
        return deepCopy("[" + index + "]");
    }

    /**
     * Copy tag values from an other tag, except name.
     *
     * @return this
     */
    public Tag copyFrom(Tag tag) {
        constructed = tag.constructed;
        explicit = tag.explicit;
        optional = tag.optional;
        tagClass = tag.tagClass;
        tagNumber = tag.tagNumber;
        return this;
    }

}
