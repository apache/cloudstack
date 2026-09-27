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

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class PagedFetcherTest {

    private static class Page {
        private String cursor;
        private List<String> items;

        Page(String cursor, List<String> items) {
            this.cursor = cursor;
            this.items = items;
        }

        String getCursor() {
            return cursor;
        }

        List<String> getItems() {
            return items;
        }

        void setItems(List<String> items) {
            this.items = items;
        }
    }

    @Test
    public void testFetchAllWhenThereIsNoPagination() {
        // given
        Page firstPage = new Page(null, new ArrayList<>(List.of("a", "b")));
        AtomicBoolean itemsSetterCalled = new AtomicBoolean(false);
        PagedFetcher<Page, String> fetcher = PagedFetcher.<Page, String>withPageFetcher(
                        cursor -> {
                            assertNull(cursor);
                            return firstPage;
                        })
                .cursorExtractor(Page::getCursor)
                .itemsExtractor(Page::getItems)
                .itemsSetter((page, items) -> itemsSetterCalled.set(true));

        // when
        Page result = fetcher.fetchAll();

        // then
        assertSame(firstPage, result);
        assertEquals(List.of("a", "b"), result.getItems());
        assertFalse("itemsSetter must not be called when there is no next page", itemsSetterCalled.get());
    }

    @Test
    public void testFetchAllWhenThereIsNoPaginationAndEmptyCursor() {
        // given
        Page firstPage = new Page("", new ArrayList<>(List.of("x")));

        AtomicBoolean itemsSetterCalled = new AtomicBoolean(false);

        PagedFetcher<Page, String> fetcher = PagedFetcher
                .<Page, String>withPageFetcher(cursor -> {
                    assertNull(cursor);
                    return firstPage;
                })
                .cursorExtractor(Page::getCursor)
                .itemsExtractor(Page::getItems)
                .itemsSetter((page, items) -> itemsSetterCalled.set(true));

        // when
        Page result = fetcher.fetchAll();

        // then
        assertSame(firstPage, result);
        assertEquals(List.of("x"), result.getItems());
        assertFalse("itemsSetter must not be called when there is no next page", itemsSetterCalled.get());
    }

    @Test
    public void testFetchAllWhenMultiPages() {
        // given
        Page page1 = new Page("c1", new ArrayList<>(List.of("p1a", "p1b")));
        Page page2 = new Page("c2", new ArrayList<>(List.of("p2a")));
        Page page3 = new Page(null, new ArrayList<>(List.of("p3a", "p3b")));

        Map<String, Page> pagesByCursor = new HashMap<>();
        pagesByCursor.put(null, page1);
        pagesByCursor.put("c1", page2);
        pagesByCursor.put("c2", page3);

        PagedFetcher<Page, String> fetcher = PagedFetcher
                .<Page, String>withPageFetcher(pagesByCursor::get)
                .cursorExtractor(Page::getCursor)
                .itemsExtractor(Page::getItems)
                .itemsSetter((page, items) -> {
                    assertSame(page1, page);
                    page.setItems(items);
                });

        // when
        Page result = fetcher.fetchAll();

        // then
        assertSame("Result must be the first page object", page1, result);
        assertEquals(List.of("p1a", "p1b", "p2a", "p3a", "p3b"), result.getItems());
    }

    @Test
    public void testFetchAllFirstPageItemsNullSecondWithItems() {
        // given
        Page page1 = new Page("next", null);
        Page page2 = new Page(null, new ArrayList<>(List.of("x", "y")));

        Map<String, Page> pages = new HashMap<>();
        pages.put(null, page1);
        pages.put("next", page2);

        PagedFetcher<Page, String> fetcher = PagedFetcher
                .<Page, String>withPageFetcher(pages::get)
                .cursorExtractor(Page::getCursor)
                .itemsExtractor(Page::getItems)
                .itemsSetter(Page::setItems);

        // when
        Page result = fetcher.fetchAll();

        // then
        assertSame(page1, result);
        assertEquals(List.of("x", "y"), result.getItems());
    }
}
