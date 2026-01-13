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

package org.apache.cloudstack.veeam.api.request;

import java.util.LinkedHashMap;
import java.util.Map;

public final class VmSearchFilters {

    private final Map<String, String> equals = new LinkedHashMap<>();

    public Map<String, String> equals() {
        return equals;
    }

    public VmSearchFilters put(final String field, final String value) {
        equals.put(field, value);
        return this;
    }

    public static VmSearchFilters fromAndOnly(final VmSearchExpr expr) {
        final VmSearchFilters f = new VmSearchFilters();
        if (expr == null) {
            return f;
        }
        collect(expr, f);
        return f;
    }

    private static void collect(final VmSearchExpr expr, final VmSearchFilters f) {
        if (expr instanceof VmSearchExpr.Term) {
            final VmSearchExpr.Term t = (VmSearchExpr.Term) expr;
            f.put(t.getField(), t.getValue());
            return;
        }
        if (expr instanceof VmSearchExpr.And) {
            final VmSearchExpr.And a = (VmSearchExpr.And) expr;
            collect(a.getLeft(), f);
            collect(a.getRight(), f);
            return;
        }
        if (expr instanceof VmSearchExpr.Or) {
            throw new VmSearchParser.VmSearchParseException("Only AND expressions are supported currently");
        }
        throw new VmSearchParser.VmSearchParseException("Unsupported search expression: " + expr.getClass().getName());
    }
}
