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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parser for an oVirt-like 'search' parameter.
 *
 * Examples:
 *   name=myvm
 *   status=down and cluster=Default
 *   name="My VM" or name="Other VM"
 *   (status=up and host=hv1) or (status=down and host=hv2)
 *
 * Values can be IDENT (unquoted) or STRING (quoted with " ... ").
 */
public final class VmSearchParser {

    public static final class VmSearchParseException extends RuntimeException {
        public VmSearchParseException(final String message) { super(message); }
    }

    private final Set<String> allowedFields;

    public VmSearchParser(final Set<String> allowedFields) {
        this.allowedFields = allowedFields;
    }

    /**
     * @return AST or null if input is null/blank
     */
    public VmSearchExpr parse(final String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        final Lexer lexer = new Lexer(input);
        final List<Token> tokens = lexer.lex();
        final Parser p = new Parser(tokens, allowedFields);
        final VmSearchExpr expr = p.parseExpression();
        p.expect(TokenType.EOF);
        return expr;
    }

    // -------------------- lexer --------------------

    enum TokenType {
        IDENT, STRING, EQ, AND, OR, LPAREN, RPAREN, EOF
    }

    static final class Token {
        private final TokenType type;
        private final String text;
        private final int pos;

        Token(final TokenType type, final String text, final int pos) {
            this.type = type;
            this.text = text;
            this.pos = pos;
        }

        TokenType type() { return type; }
        String text() { return text; }
        int pos() { return pos; }
    }

    static final class Lexer {
        private final String s;
        private final int n;
        private int i = 0;

        Lexer(final String s) {
            this.s = s;
            this.n = s.length();
        }

        List<Token> lex() {
            final List<Token> out = new ArrayList<>();
            while (true) {
                skipWs();
                if (i >= n) {
                    out.add(new Token(TokenType.EOF, "", i));
                    return out;
                }
                final char c = s.charAt(i);

                if (c == '(') {
                    out.add(new Token(TokenType.LPAREN, "(", i++));
                } else if (c == ')') {
                    out.add(new Token(TokenType.RPAREN, ")", i++));
                } else if (c == '=') {
                    out.add(new Token(TokenType.EQ, "=", i++));
                } else if (c == '"') {
                    out.add(readQuoted());
                } else if (isIdentStart(c)) {
                    out.add(readIdentOrKeyword());
                } else {
                    throw new VmSearchParseException("Unexpected character '" + c + "' at position " + i);
                }
            }
        }

        private void skipWs() {
            while (i < n) {
                final char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++;
                else break;
            }
        }

        private Token readQuoted() {
            final int start = i;
            i++; // skip opening "
            final StringBuilder b = new StringBuilder();
            while (i < n) {
                final char c = s.charAt(i);
                if (c == '"') {
                    i++; // closing "
                    return new Token(TokenType.STRING, b.toString(), start);
                }
                if (c == '\\') {
                    if (i + 1 >= n) {
                        throw new VmSearchParseException("Unterminated escape at position " + i);
                    }
                    final char nxt = s.charAt(i + 1);
                    switch (nxt) {
                        case '"': b.append('"'); i += 2; break;
                        case '\\': b.append('\\'); i += 2; break;
                        case 'n': b.append('\n'); i += 2; break;
                        case 't': b.append('\t'); i += 2; break;
                        default:
                            throw new VmSearchParseException("Unsupported escape \\" + nxt + " at position " + i);
                    }
                    continue;
                }
                b.append(c);
                i++;
            }
            throw new VmSearchParseException("Unterminated string starting at position " + start);
        }

        private Token readIdentOrKeyword() {
            final int start = i;
            i++;
            while (i < n && isIdentPart(s.charAt(i))) i++;

            final String text = s.substring(start, i);
            final String lower = text.toLowerCase(Locale.ROOT);

            if ("and".equals(lower)) return new Token(TokenType.AND, text, start);
            if ("or".equals(lower)) return new Token(TokenType.OR, text, start);

            return new Token(TokenType.IDENT, text, start);
        }

        private static boolean isIdentStart(final char c) {
            return Character.isLetter(c) || c == '_' || c == '.';
        }

        private static boolean isIdentPart(final char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '-';
        }
    }

    // -------------------- parser --------------------

    static final class Parser {
        private final List<Token> tokens;
        private final Set<String> allowedFields;
        private int k = 0;

        Parser(final List<Token> tokens, final Set<String> allowedFields) {
            this.tokens = tokens;
            this.allowedFields = allowedFields;
        }

        VmSearchExpr parseExpression() {
            return parseOr();
        }

        private VmSearchExpr parseOr() {
            VmSearchExpr left = parseAnd();
            while (peek(TokenType.OR)) {
                consume(TokenType.OR);
                final VmSearchExpr right = parseAnd();
                left = new VmSearchExpr.Or(left, right);
            }
            return left;
        }

        private VmSearchExpr parseAnd() {
            VmSearchExpr left = parsePrimary();
            while (peek(TokenType.AND)) {
                consume(TokenType.AND);
                final VmSearchExpr right = parsePrimary();
                left = new VmSearchExpr.And(left, right);
            }
            return left;
        }

        private VmSearchExpr parsePrimary() {
            if (peek(TokenType.LPAREN)) {
                consume(TokenType.LPAREN);
                final VmSearchExpr e = parseExpression();
                expect(TokenType.RPAREN);
                return e;
            }
            return parseTerm();
        }

        private VmSearchExpr parseTerm() {
            final Token fieldTok = expect(TokenType.IDENT);
            final String field = fieldTok.text();

            if (allowedFields != null && !allowedFields.contains(field)) {
                throw new VmSearchParseException("Unsupported search field '" + field + "' at position " + fieldTok.pos());
            }

            expect(TokenType.EQ);

            final Token v = next();
            final String value;
            if (v.type() == TokenType.IDENT || v.type() == TokenType.STRING) {
                value = v.text();
            } else {
                throw new VmSearchParseException("Expected value after '=' at position " + v.pos());
            }

            if (value == null || value.isEmpty()) {
                throw new VmSearchParseException("Empty value for field '" + field + "' at position " + v.pos());
            }

            return new VmSearchExpr.Term(field, value);
        }

        boolean peek(final TokenType t) {
            return tokens.get(k).type() == t;
        }

        Token next() {
            return tokens.get(k++);
        }

        Token expect(final TokenType t) {
            final Token tok = next();
            if (tok.type() != t) {
                throw new VmSearchParseException("Expected " + t + " at position " + tok.pos() + " but found " + tok.type());
            }
            return tok;
        }

        Token consume(final TokenType t) {
            return expect(t);
        }
    }
}
