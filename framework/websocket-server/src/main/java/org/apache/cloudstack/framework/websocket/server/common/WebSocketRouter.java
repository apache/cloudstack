/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cloudstack.framework.websocket.server.common;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * Dynamic WebSocket router supporting exact, prefix, and regex routes.
 *
 * <p>Design goals:
 * <ul>
 *   <li>Fast, lock-free route resolution on the hot path</li>
 *   <li>Safe concurrent updates (register/unregister at runtime)</li>
 *   <li>Per-route configuration (e.g., idle timeout)</li>
 *   <li>Transport-agnostic (works with Jetty JSR-356 and Netty)</li>
 * </ul>
 *
 * Typical usage:
 * <pre>
 *   WebSocketRouter router = new WebSocketRouter();
 *   router.registerExact("/echo", echoHandler(), RouteConfig.ofSeconds(120));
 *   router.registerPrefix("/logger/", logsHandler(), RouteConfig.ofSeconds(120));
 *   router.registerRegex("^/chat/[^/]+$", chatHandler(), RouteConfig.ofSeconds(300));
 *
 *   ResolvedRoute rr = router.resolve("/logger/token");
 *   if (rr != null) {
 *     rr.handler().onOpen(...); // in your server binding (Jetty/Netty)
 *   }
 * </pre>
 */
public final class WebSocketRouter {
    public static final String WEBSOCKET_PATH_PREFIX = "/ws";

    public static final class RouteConfig {
        private final long idleTimeoutMillis;

        private RouteConfig(long idleTimeoutMillis) {
            this.idleTimeoutMillis = idleTimeoutMillis;
        }

        public long getIdleTimeoutMillis() {
            return idleTimeoutMillis;
        }

        public static RouteConfig ofMillis(long millis) {
            return new RouteConfig(millis);
        }

        public static RouteConfig ofSeconds(long seconds) {
            return new RouteConfig(TimeUnit.SECONDS.toMillis(seconds));
        }

        @Override public String toString() {
            return "RouteConfig{idileTimeoutMillis=" + idleTimeoutMillis + "}";
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RouteConfig)) return false;
            RouteConfig that = (RouteConfig) o;
            return idleTimeoutMillis == that.idleTimeoutMillis;
        }

        @Override public int hashCode() {
            return Long.hashCode(idleTimeoutMillis);
        }
    }

    /** Result of resolving a path. */
    public static final class ResolvedRoute {
        private final WebSocketHandler handler;
        private final RouteConfig config;
        private final String matchedKind;
        private final String matchedKey;

        public ResolvedRoute(WebSocketHandler handler, RouteConfig config, String matchedKind, String matchedKey) {
            this.handler = Objects.requireNonNull(handler, "handler");
            this.config = config; // may be null
            this.matchedKind = matchedKind;
            this.matchedKey = matchedKey;
        }

        public WebSocketHandler getHandler() { return handler; }
        public RouteConfig getConfig() { return config; }
        public String getMatchedKind() { return matchedKind; }
        public String getMatchedKey() { return matchedKey; }
    }

    // ---- Internal state -----------------------------------------------------

    private static final class Entry {
        final String kind;                 // "exact" | "prefix" | "regex"
        final String key;                  // for exact/prefix
        final Pattern pattern;             // for regex
        final WebSocketHandler handler;
        final RouteConfig config;

        Entry(String kind, String key, WebSocketHandler h, RouteConfig c) {
            this.kind = kind;
            this.key = key;
            this.pattern = null;
            this.handler = Objects.requireNonNull(h, "handler");
            this.config = (c == null) ? RouteConfig.ofSeconds(120) : c;
        }

        Entry(Pattern p, WebSocketHandler h, RouteConfig c) {
            this.kind = "regex";
            this.key = null;
            this.pattern = Objects.requireNonNull(p, "pattern");
            this.handler = Objects.requireNonNull(h, "handler");
            this.config = (c == null) ? RouteConfig.ofSeconds(120) : c;
        }
    }

    /** Exact path routes: O(1) lookup. */
    private final ConcurrentHashMap<String, Entry> exact = new ConcurrentHashMap<>();

    /** Prefix routes: checked longest-first for specificity. */
    private final CopyOnWriteArrayList<Entry> prefixes = new CopyOnWriteArrayList<>();

    /** Regex routes: checked in registration order (keep few for perf). */
    private final CopyOnWriteArrayList<Entry> regexes = new CopyOnWriteArrayList<>();

    // --- Registration API ----------------------------------------------------

    /** Register an exact route, e.g. "/echo". */
    public void registerExact(String path, WebSocketHandler handler, RouteConfig config) {
        String norm = normalizeExact(path);
        exact.put(norm, new Entry("exact", norm, handler, config));
    }

    /** Overload with seconds. */
    public void registerExact(String path, WebSocketHandler handler, long idleTimeoutSeconds) {
        registerExact(path, handler, RouteConfig.ofSeconds(idleTimeoutSeconds));
    }

    /**
     * Register a prefix route, e.g. "/logger/" which matches "/logger/**".
     * The router keeps prefixes sorted by descending length so the most specific wins.
     */
    public void registerPrefix(String basePath, WebSocketHandler handler, RouteConfig config) {
        String norm = normalizePrefix(basePath);
        prefixes.add(new Entry("prefix", norm, handler, config));
        // Keep most specific first (longest path first). COW list sorting is safe for concurrent readers.
        prefixes.sort((a, b) -> Integer.compare(b.key.length(), a.key.length()));
    }

    /** Overload with seconds. */
    public void registerPrefix(String basePath, WebSocketHandler handler, long idleTimeoutSeconds) {
        registerPrefix(basePath, handler, RouteConfig.ofSeconds(idleTimeoutSeconds));
    }

    /** Register a regex route; use sparingly for performance. */
    public void registerRegex(String regex, WebSocketHandler handler, RouteConfig config) {
        Pattern p = Pattern.compile(Objects.requireNonNull(regex, "regex"));
        regexes.add(new Entry(p, handler, config));
    }

    /** Overload with seconds. */
    public void registerRegex(String regex, WebSocketHandler handler, long idleTimeoutSeconds) {
        registerRegex(regex, handler, RouteConfig.ofSeconds(idleTimeoutSeconds));
    }

    /**
     * Unregister a route by its key:
     * <ul>
     *   <li>Exact: pass the exact path you used (e.g., "/echo")</li>
     *   <li>Prefix: pass the normalized base (e.g., "/logger/")</li>
     *   <li>Regex: pass the original pattern string</li>
     * </ul>
     */
    public void unregister(String keyOrPattern) {
        if (keyOrPattern == null) return;
        String exactKey = normalizeExactOrNull(keyOrPattern);
        if (exactKey != null) exact.remove(exactKey);

        String prefixKey = normalizePrefixOrNull(keyOrPattern);
        if (prefixKey != null) prefixes.removeIf(e -> prefixKey.equals(e.key));

        regexes.removeIf(e -> e.pattern != null && keyOrPattern.equals(e.pattern.pattern()));
    }

    /** Clear all routes (useful in tests or module resets). */
    public void clear() {
        exact.clear();
        prefixes.clear();
        regexes.clear();
    }

    // --- Resolution ----------------------------------------------------------

    /**
     * Resolve a request path to a handler.
     * Order: exact → longest prefix → first regex match.
     * Returns {@code null} if no match.
     */
    public ResolvedRoute resolve(String requestPath) {
        if (requestPath == null || requestPath.isEmpty()) return null;
        String p = ensureLeadingSlash(requestPath);
        // Exact
        Entry e = exact.get(p);
        if (e != null) return new ResolvedRoute(e.handler, e.config, e.kind, e.key);

        // Prefix (already sorted longest-first)
        for (Entry pe : prefixes) {
            if (p.startsWith(pe.key)) {
                return new ResolvedRoute(pe.handler, pe.config, pe.kind, pe.key);
            }
        }

        // Regex (keep few for perf)
        for (Entry re : regexes) {
            if (re.pattern.matcher(p).matches()) {
                return new ResolvedRoute(re.handler, re.config, re.kind, re.pattern.pattern());
            }
        }

        return null;
    }

    // --- Introspection (optional helpers) -----------------------------------

    /** Returns a snapshot of currently registered exact routes. */
    public List<String> listExactRoutes() { return List.copyOf(exact.keySet()); }

    /** Returns a snapshot of currently registered prefix routes (normalized). */
    public List<String> listPrefixRoutes() {
        return prefixes.stream().map(e -> e.key).collect(Collectors.toList());
    }

    /** Returns a snapshot of currently registered regex patterns. */
    public List<String> listRegexRoutes() {
        return regexes.stream().map(e -> Objects.requireNonNull(e.pattern).pattern()).collect(Collectors.toList());
    }

    // --- Normalization helpers ----------------------------------------------

    private static String normalizeExact(String path) {
        String p = ensureLeadingSlash(Objects.requireNonNull(path, "path"));
        // never allow trailing slash normalization for exact; treat "/a" and "/a/" as different on purpose
        return p;
    }

    private static String normalizePrefix(String base) {
        String p = ensureLeadingSlash(Objects.requireNonNull(base, "basePath"));
        // guarantee trailing slash for prefix semantics
        return p.endsWith("/") ? p : p + "/";
    }

    private static String normalizeExactOrNull(String maybe) {
        if (maybe == null || maybe.isEmpty()) return null;
        if (maybe.endsWith("/*")) return null; // looks like a path spec, not exact
        return ensureLeadingSlash(maybe);
    }

    private static String normalizePrefixOrNull(String maybe) {
        if (maybe == null || maybe.isEmpty()) return null;
        String p = ensureLeadingSlash(maybe);
        // treat strings ending with "/" as prefix keys
        return p.endsWith("/") ? p : null;
    }

    public static String ensureLeadingSlash(String p) {
        return (p.charAt(0) == '/') ? p : ("/" + p);
    }

    public static String stripWebSocketPathPrefix(String path) {
        if (StringUtils.isNotBlank(path) && path.startsWith(WebSocketRouter.WEBSOCKET_PATH_PREFIX)) {
            return path.replaceFirst(WebSocketRouter.WEBSOCKET_PATH_PREFIX, "/");
        }
        return path;
    }
}
