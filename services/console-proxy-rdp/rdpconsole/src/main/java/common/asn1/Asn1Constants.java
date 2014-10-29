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

public interface Asn1Constants {

    /**
     * Universal class type.
     */
    public static final int UNIVERSAL_CLASS = 0x00;

    /**
     * Application class type.
     */
    public static final int APPLICATION_CLASS = 0x40;

    public static final int CONTEXT_CLASS = 0x80;

    public static final int PRIVATE_CLASS = 0xC0;

    /**
     * Constructed type.
     */
    public static final int CONSTRUCTED = 0x20;

    /**
     * Mask to extract class.
     */
    public static final int CLASS_MASK = 0xC0;

    /**
     * Mask to extract type.
     */
    public static final int TYPE_MASK = 0x1F;

    public static final int EOF = 0x00;
    public static final int BOOLEAN = 0x01;
    /**
     * Integer primitive.
     */
    public static final int INTEGER = 0x02;
    public static final int BIT_STRING = 0x03;
    /**
     * Octet string primitive.
     */
    public static final int OCTET_STRING = 0x04;
    public static final int NULL = 0x05;
    public static final int OBJECT_ID = 0x06;
    public static final int REAL = 0x09;
    public static final int ENUMERATED = 0x0A;
    /**
     * Sequence primitive.
     */
    public static final int SEQUENCE = 0x10;
    public static final int SET = 0x11;
    public static final int NUMERIC_STRING = 0x12;
    public static final int PRINTABLE_STRING = 0x13;
    public static final int TELETEX_STRING = 0x14;
    public static final int VIDEOTEXT_STRING = 0x15;
    public static final int IA5_STRING = 0x16;
    public static final int UTCTIME = 0x17;
    public static final int GENERAL_TIME = 0x18;
    public static final int GRAPHIC_STRING = 0x19;
    public static final int VISIBLE_STRING = 0x1A;
    public static final int GENERAL_STRING = 0x1B;

    public static final int EXTENDED_TYPE = 0x1F;

}
