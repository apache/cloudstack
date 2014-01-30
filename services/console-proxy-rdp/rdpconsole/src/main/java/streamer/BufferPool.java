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
package streamer;

public class BufferPool {
    public static byte[] allocateNewBuffer(int minSize) {
        // TODO: search for free buffer in pool
        if (minSize >= 0)
            return new byte[minSize];
        else
            // Return large buffer by default, too minimize number of round trips
            // between to read full packet when packet is large, but it is important
            // to return buffer to pool to reuse it (or null-ify links to it for
            // faster GC)
            // TODO: get free buffer from pool
            return new byte[128 * 1024];
    }

    public static void recycleBuffer(byte[] buf) {
        // TODO: return buffer to pool
    }
}
