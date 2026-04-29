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
package org.apache.cloudstack.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

class PagedFetcher<R, T> {

    private final Function<String, R> fetchPage;
    private Function<R, String> cursorExtractor;
    private Function<R, List<T>> itemsExtractor;
    private BiConsumer<R, List<T>> itemsSetter;

    static <R, T> PagedFetcher<R, T> withPageFetcher(Function<String, R> pageFetcher) {
        return new PagedFetcher<>(pageFetcher);
    }

    PagedFetcher<R, T> cursorExtractor(Function<R, String> cursorProvider) {
        this.cursorExtractor = cursorProvider;
        return this;
    }

    PagedFetcher<R, T> itemsExtractor(Function<R, List<T>> resultsProvider) {
        this.itemsExtractor = resultsProvider;
        return this;
    }

    PagedFetcher<R, T> itemsSetter(BiConsumer<R, List<T>> resultsSetter) {
        this.itemsSetter = resultsSetter;
        return this;
    }

    private PagedFetcher(Function<String, R> pageFetcher) {
        this.fetchPage = pageFetcher;
    }

    R fetchAll() {
        Objects.requireNonNull(cursorExtractor, "Cursor extractor must be set");
        Objects.requireNonNull(itemsExtractor, "Items extractor must be set");
        Objects.requireNonNull(itemsSetter, "Items setter must be set");

        R firstPage = fetchPage.apply(null);
        String cursor = cursorExtractor.apply(firstPage);
        if (cursor == null || cursor.isEmpty()) {
            return firstPage;
        }

        List<T> firstResults = itemsExtractor.apply(firstPage);
        List<T> allItems = firstResults != null
                ? new ArrayList<>(firstResults)
                : new ArrayList<>();
        while (cursor != null && !cursor.isEmpty()) {
            R nextPage = fetchPage.apply(cursor);
            List<T> nextItems = itemsExtractor.apply(nextPage);
            if (nextItems != null && !nextItems.isEmpty()) {
                allItems.addAll(nextItems);
            }
            cursor = cursorExtractor.apply(nextPage);
        }

        itemsSetter.accept(firstPage, allItems);
        return firstPage;
    }
}
