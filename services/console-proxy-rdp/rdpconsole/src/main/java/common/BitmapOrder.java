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
package common;

import java.util.Arrays;

import streamer.Order;

/**
 * Not an order, but derived from Order class for compatibility with orders.
 *
 * @see http://msdn.microsoft.com/en-us/library/dd306368.aspx
 */
public class BitmapOrder extends Order {

    public BitmapOrder() {
        type = OrderType.BITMAP_UPDATE;
    }

    /**
     * Structures, each of which contains a rectangular clipping taken from the
     * server-side screen frame buffer.
     */
    public BitmapRectangle rectangles[];

    @Override
    public String toString() {
        final int maxLen = 10;
        return String.format("BitmapUpdateOrder [rectangles=%s]", rectangles != null ? Arrays.asList(rectangles).subList(0, Math.min(rectangles.length, maxLen)) : null);
    }

}
