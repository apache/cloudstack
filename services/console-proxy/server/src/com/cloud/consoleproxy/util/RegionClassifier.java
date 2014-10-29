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

public class RegionClassifier {
    private List<Region> regionList;

    public RegionClassifier() {
        regionList = new ArrayList<Region>();
    }

    public void add(Rectangle rect) {
        boolean newRegion = true;
        Rectangle rcInflated = new Rectangle(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2);
        for (Region region : regionList) {
            if (region.getBound().intersects(rcInflated)) {
                newRegion = false;
                break;
            }
        }

        if (newRegion) {
            regionList.add(new Region(rect));
        } else {
            for (Region region : regionList) {
                if (region.add(rect))
                    return;
            }
            regionList.add(new Region(rect));
        }
    }

    public List<Region> getRegionList() {
        return regionList;
    }

    public void clear() {
        regionList.clear();
    }
}
