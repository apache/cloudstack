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
package com.cloud.consoleproxy.util;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

public class Region {
    private Rectangle bound;
    private List<Rectangle> rectList;

    public Region() {
        bound = new Rectangle(0, 0, 0, 0);
        rectList = new ArrayList<Rectangle>();
    }

    public Region(Rectangle rect) {
        bound = new Rectangle(rect.x, rect.y, rect.width, rect.height);
        rectList = new ArrayList<Rectangle>();
        rectList.add(rect);
    }

    public Rectangle getBound() {
        return bound;
    }

    public void clearBound() {
        assert (rectList.size() == 0);
        bound.x = bound.y = bound.width = bound.height = 0;
    }

    public List<Rectangle> getRectangles() {
        return rectList;
    }

    public boolean add(Rectangle rect) {
        if (bound.isEmpty()) {
            assert (rectList.size() == 0);
            bound.x = rect.x;
            bound.y = rect.y;
            bound.width = rect.width;
            bound.height = rect.height;

            rectList.add(rect);
            return true;
        }

        Rectangle rcInflated = new Rectangle(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2);
        if (!bound.intersects(rcInflated))
            return false;

        for (Rectangle r : rectList) {
            if (r.intersects(rcInflated)) {
                if (!r.contains(rect)) {
                    enlargeBound(rect);
                    rectList.add(rect);
                    return true;
                }
            }
        }
        return false;
    }

    private void enlargeBound(Rectangle rect) {
        int boundLeft = Math.min(bound.x, rect.x);
        int boundTop = Math.min(bound.y, rect.y);
        int boundRight = Math.max(bound.x + bound.width, rect.x + rect.width);
        int boundBottom = Math.max(bound.y + bound.height, rect.y + rect.height);

        bound.x = boundLeft;
        bound.y = boundTop;
        bound.width = boundRight - boundLeft;
        bound.height = boundBottom - boundTop;
    }
}
