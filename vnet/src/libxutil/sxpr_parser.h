/*
 * Copyright (C) 2001 - 2005 Mike Wray <mike.wray@hp.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or  (at your option) any later version. This library is 
 * distributed in the  hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

#ifndef _XUTIL_SXPR_PARSER_H_
#define _XUTIL_SXPR_PARSER_H_

#include "sxpr.h"
#include "iostream.h"

/** @file
 * Sxpr parsing definitions.
 */

/** Initial size of a parser input buffer.
 */
#define PARSER_BUF_SIZE 512

/** Input buffer size increment (when it's full).
 */
#define PARSER_BUF_INCREMENT 512

struct Parser;
typedef int ParserStateFn(struct Parser *, char c);

typedef struct ParserState {
    struct ParserState *parent;
    Sxpr val;
    int ival;
    int count;
    char delim;
    ParserStateFn *fn;
    char *name;
} ParserState;

typedef struct Parser {
    /** Initial state function. */
    ParserStateFn *begin;
    /** Parse value. */
    Sxpr val;
    /** Error reporting stream (null for no reports). */
    IOStream *error_out;
    /** End-of-file flag, */
    int eof;
    /** Error flag. Non-zero if there has been a read error. */
    int err;
    /** Line number on input (from 1). */
    int line_no;
    /** Column number of input (reset on new line). */
    int char_no;
    /** Buffer for reading tokens. */
    char *buf;
    char *buf_end;
    char *tok;
    char *tok_end;
    /** Line the last token started on. */
    int tok_begin_line;
    /** Character number the last token started on. */
    int tok_begin_char;
    /** Parsing flags. */
    int flags;
    ParserState *state;
    ParserState *start_state;
} Parser;

/** Parser error codes. */
typedef enum {
    PARSE_ERR_NONE=0,
    PARSE_ERR_UNSPECIFIED,
    PARSE_ERR_NOMEM,
    PARSE_ERR_UNEXPECTED_EOF,
    PARSE_ERR_TOKEN_TOO_LONG,
    PARSE_ERR_INVALID_SYNTAX,
    PARSE_ERR_INVALID_ESCAPE,
} ParseErrorId;


/** Parser flags. */
enum {
    /** Convert integer atoms to ints. */
    PARSE_INT=1,
};

/** Raise some parser flags.
 *
 * @param in parser
 * @param flags flags mask
 */
static inline void Parser_flags_raise(Parser *in, int flags){
    in->flags |= flags;
}

/** Lower some parser flags.
 *
 * @param in parser
 * @param flags flags mask
 */
static inline void Parser_flags_lower(Parser *in, int flags){
    in->flags &= ~flags;
}

/** Clear all parser flags.
 *
 * @param in parser
 */
static inline void Parser_flags_clear(Parser *in){
    in->flags = 0;
}

static inline int Parser_flags(Parser *in, int flags){
    return in->flags & flags;
}

extern void Parser_free(Parser *z);
extern Parser * Parser_new(void);
extern int Parser_input(Parser *p, char *buf, int buf_n);
extern int Parser_input_eof(Parser *p);
extern int Parser_input_char(Parser *p, char c);
extern void Parser_set_error_stream(Parser *z, IOStream *error_out);

extern int Parser_error_message(Parser *in, char *buf, int n);
extern int Parser_has_error(Parser *in);
extern int Parser_at_eof(Parser *in);

extern int Parser_ready(Parser *p);
extern Sxpr Parser_get_val(Parser *p);
extern Sxpr Parser_get_all(Parser *p);

/* Internal parser api. */
void Parser_pop(Parser *p);
int Parser_push(Parser *p, ParserStateFn *fn, char *name);
int Parser_return(Parser *p);
int Parser_at_eof(Parser *p);
int Parser_error(Parser *in);
int Parser_set_value(Parser *p, Sxpr val);
int Parser_intern(Parser *p);
int Parser_string(Parser *p);
int Parser_data(Parser *p);
int Parser_uint(Parser *p);

char *peek_token(Parser *p);
char *copy_token(Parser *p);
void new_token(Parser *p);
int save_char(Parser *p, char c);
int token_len(Parser *p);

#endif /* ! _XUTIL_SXPR_PARSER_H_ */
