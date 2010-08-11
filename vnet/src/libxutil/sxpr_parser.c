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

#ifdef __KERNEL__
#  include <linux/config.h>
#  include <linux/module.h>
#  include <linux/kernel.h>
#  include <linux/string.h>
#  include <linux/errno.h>
#else
#  include <stdlib.h>
#  include <errno.h>
#endif

#include "sys_net.h"

#include "iostream.h"
#include "lexis.h"
#include "sxpr_parser.h"
#include "sys_string.h"
#include "enum.h"

/** @file
 * Sxpr parsing.
 *
 * So that the parser does not leak memory, all sxprs constructed by
 * the parser must be freed on error.  On successful parse the sxpr
 * returned becomes the responsibility of the caller.
 *
 * @author Mike Wray <mike.wray@hpl.hp.com>
 */

#ifdef DEBUG
#define dprintf(fmt, args...) IOStream_print(iostdout, "[DEBUG] %s" fmt, __FUNCTION__, ##args)
#else
#define dprintf(fmt, args...) do{ }while(0)
#endif

#undef printf
#define printf(fmt, args...)   IOStream_print(iostdout, fmt, ##args)

static int state_start(Parser *p, char c);
static int begin_start(Parser *p, char c);

#if 0
/** Print a parse error.
 *
 * @param in parser
 * @param msg format followed by printf arguments
 */
static void eprintf(Parser *in, char *msg, ...){
    va_list args;
    if(in->error_out){
        va_start(args, msg);
        IOStream_vprint(in->error_out, msg, args);
        va_end(args);
    }
}

/** Print a parse warning.
 *
 * @param in parser
 * @param msg format followed by printf arguments
 */
static void wprintf(Parser *in, char *msg, ...){
    va_list args;
    if(in->error_out){
        va_start(args, msg);
        IOStream_vprint(in->error_out, msg, args);
        va_end(args);
    }
}
#endif


/*============================================================================*/

/** Record defining the message for a parse error. */
typedef struct {
    ParseErrorId id;
    char *message;
} ParseError;

/** Format for printing parse error messages. */
#define PARSE_ERR_FMT "parse error> line %3d, column %2d: %s"

/** Message catalog for the parse error codes. */
static ParseError catalog[] = {
    { PARSE_ERR_UNSPECIFIED,            "unspecified error" },
    { PARSE_ERR_NOMEM,                  "out of memory" },
    { PARSE_ERR_UNEXPECTED_EOF,         "unexpected end of input" },
    { PARSE_ERR_TOKEN_TOO_LONG,         "token too long" },
    { PARSE_ERR_INVALID_SYNTAX,         "syntax error" },
    { PARSE_ERR_INVALID_ESCAPE,         "invalid escape" },
    { 0, NULL }
};

/** Number of entries in the message catalog. */
const static int catalog_n = sizeof(catalog)/sizeof(ParseError);

/** Set the parser error stream.
 * Parse errors are reported on the the error stream if it is non-null.
 * 
 * @param z parser
 * @param error_out error stream
 */
void Parser_set_error_stream(Parser *z, IOStream *error_out){
    z->error_out = error_out;
}

/** Get the parser error message for an error code.
 *
 * @param id error code
 * @return error message (empty string if the code is unknown)
 */
static char *get_message(ParseErrorId id){
    int i;
    for(i = 0; i < catalog_n; i++){
        if(id == catalog[i].id){
            return catalog[i].message;
        }
    }
    return "";
}

#if 0
/** Get the line number.
 *
 * @param in parser
 */
static int get_line(Parser *in){
    return in->line_no;
}

/** Get the column number.
 *
 * @param in parser
 */
static int get_column(Parser *in){
    return in->char_no;
}
#endif

/** Get the line number the current token started on.
 *
 * @param in parser
 */
static int get_tok_line(Parser *in){
    return in->tok_begin_line;
}

/** Get the column number the current token started on.
 *
 * @param in parser
 */
static int get_tok_column(Parser *in){
    return in->tok_begin_char;
}

/** Return the current token.
 * The return value points at the internal buffer, so
 * it must not be modified (or freed). Use copy_token() if you need a copy.
 *
 * @param p parser
 * @return token
 */
char *peek_token(Parser *p){
    return p->tok;
}

int token_len(Parser *p){
    return p->tok_end - p->tok;
}

/** Return a copy of the current token.
 * The returned value should be freed when finished with.
 *
 * @param p parser
 * @return copy of token
 */
char *copy_token(Parser *p){
    int n = token_len(p);
    char *buf = allocate(n + 1);
    if(buf){
        memcpy(buf, peek_token(p), n);
        buf[n] = '\0';
    }
    return buf;
}

void new_token(Parser *p){
    memset(p->buf, 0, p->buf_end - p->buf);
    p->tok = p->buf;
    p->tok_end = p->tok;
    p->tok_begin_line = p->line_no;
    p->tok_begin_char = p->char_no;
}

/** Report a parse error.
 * Does nothing if the error stream is null or there is no error.
 *
 * @param in parser
 */
static void report_error(Parser *in){
    if(in->error_out && in->err){
        char *msg = get_message(in->err);
        char *tok = peek_token(in);
        IOStream_print(in->error_out, PARSE_ERR_FMT,
                       get_tok_line(in), get_tok_column(in), msg);
        if(tok && tok[0]){
            IOStream_print(in->error_out, " '%s'", tok);
        }
        IOStream_print(in->error_out, "\n");
    }
}

/** Get the error message for the current parse error code.
 * Does nothing if there is no error.
 *
 * @param in parser
 * @param buf where to place the message
 * @param n maximum number of characters to place in buf
 * @return current error code (zero for no error)
 */
int Parser_error_message(Parser *in, char *buf, int n){
    if(in->err){
        char *msg = get_message(in->err);
        snprintf(buf, n, PARSE_ERR_FMT, get_tok_line(in),
                 get_tok_column(in), msg);
    }
    return in->err;
}

/** Flag a parse error. All subsequent reads will fail.
 * Does not change the parser error code if it is already set.
 *
 * @param in parser
 * @param id error code
 */
int Parser_error_id(Parser *in, ParseErrorId id){
    if(!in->err){
        in->err = id;
        report_error(in);
    }
    return -EINVAL;
}

/** Flag an unspecified parse error.
 *
 * @param in parser
 */
int Parser_error(Parser *in){
    return Parser_error_id(in, PARSE_ERR_INVALID_SYNTAX);
}

/** Test if the parser's error flag is set.
 *
 * @param in parser
 * @return 1 if set, 0 otherwise
 */
int Parser_has_error(Parser *in){
    return (in->err > 0);
}

/** Test if the parser is at end of input.
 *
 * @param in parser
 * @return 1 if at EOF, 0 otherwise
 */
int Parser_at_eof(Parser *p){
    return p->eof;
}

void ParserState_free(ParserState *z){
    if(!z) return;
    objfree(z->val);
    deallocate(z);
}

int ParserState_new(ParserStateFn *fn, char *name,
                    ParserState *parent, ParserState **val){
    int err = -ENOMEM;
    ParserState *z;
    z = ALLOCATE(ParserState);
    if(!z) goto exit;
    z->name = name;
    z->fn = fn;
    z->parent = parent;
    z->val = ONULL;
    err = 0;
  exit:
    *val = (err ? NULL : z);
    return err;
}

void Parser_pop(Parser *p){
    ParserState *s = p->state;
    if(!s) return;
    dprintf("Parser_pop> %s\n", s->name);
    p->state = s->parent;
    if (p->start_state == s) {
        p->start_state = NULL;
    }
    ParserState_free(s);
}

/** Free a parser.
 * No-op if the parser is null.
 *
 * @param z parser 
 */
void Parser_free(Parser *z){
    if(!z) return;
    // Hmmm. Need to free states, but careful about double free of values.
    while(z->state){
        objfree(z->state->val);
        Parser_pop(z);
    }
    if(z->buf) deallocate(z->buf);
    objfree(z->val);
    z->val = ONONE;
    deallocate(z);
}

int Parser_push(Parser *p, ParserStateFn *fn, char *name){
    dprintf("Parser_push> %s\n", name);
    return ParserState_new(fn, name, p->state, &p->state);
}
        
int Parser_return(Parser *p){
    int err = 0;
    Sxpr val = ONONE;
    if(!p->state){
        err = -EINVAL;
        goto exit;
    }
    val = p->state->val;
    p->state->val = ONONE;
    Parser_pop(p);
    if(p->state){
        err = cons_push(&p->state->val, val);
    } else {
        val = nrev(val);
        p->val = val;
    }
  exit:
    if(err){
        objfree(val);
    }
    return err;
}

/** Reset the fields of a parser to initial values.
 *
 * @param z parser
 */
static void reset(Parser *z){
    // leave flags
    // leave error_out
    while(z->state){
        Parser_pop(z);
    }
    z->val = ONONE;
    z->eof = 0;
    z->err = 0;
    z->line_no = 1;
    z->char_no = 0;
    memset(z->buf, 0, z->buf_end - z->buf);
    z->tok = z->buf;
    z->tok_end = z->tok;
    z->tok_begin_line = 0;
    z->tok_begin_char = 0;
    z->start_state = NULL;
}

/** Create a new parser. The error stream defaults to null.
 */
Parser * Parser_new(void){
    Parser *z = ALLOCATE(Parser);
    int n = PARSER_BUF_SIZE;
    int err = -ENOMEM;
  
    if(!z) goto exit;
    z->buf = allocate(n);
    if(!z->buf) goto exit;
    err = 0;
    z->buf_end = z->buf + n;
    z->begin = begin_start;
    reset(z);
  exit:
    if(err){
        Parser_free(z);
        z = NULL;
    }
    return z;
}

/** Get the next character.
 * Records the character read in the parser,
 * and sets the line and character counts.
 *
 * @param p parser
 * @return error flag: 0 on success, non-zero on error
 */
static int input_char(Parser *p, char c){
    int err = 0;
    if(c=='\n'){
        p->line_no++;
        p->char_no = 0;
    } else {
        p->char_no++;
    }
    return err;
}

int save_char(Parser *p, char c){
    int err = 0;
    if(p->tok_end >= p->buf_end){
        int buf_n = (p->buf_end - p->buf) + PARSER_BUF_INCREMENT;
        char *buf = allocate(buf_n);
        if(!buf){
            err = -ENOMEM;
            goto exit;
        }
        memcpy(buf, p->buf, p->tok_end - p->buf);
        p->buf_end = buf + buf_n;
        p->tok     = buf + (p->tok     - p->buf);
        p->tok_end = buf + (p->tok_end - p->buf);
        deallocate(p->buf);
        p->buf = buf;
    }
    *p->tok_end++ = c;
  exit:
    return err;
}

/** Determine if a character is a separator.
 *
 * @param p parser
 * @param c character to test
 * @return 1 if a separator, 0 otherwise
 */
static int is_separator(Parser *p, char c){
    return in_sep_class(c);
}

int Parser_set_value(Parser *p, Sxpr obj){
    int err = 0;
    if(NOMEMP(obj)){
        err = -ENOMEM;
    } else {
        p->state->val = obj;
    }
    return err;
}
    
int Parser_intern(Parser *p){
    Sxpr obj = intern(peek_token(p));
    return Parser_set_value(p, obj);
}

int Parser_atom(Parser *p){
    Sxpr obj;
    long v;
    if(Parser_flags(p, PARSE_INT) &&
       convert_atol(peek_token(p), &v) == 0){
        obj = OINT(v);
    } else {
        obj = atom_new(peek_token(p));
    }
    return Parser_set_value(p, obj);
}

int Parser_string(Parser *p){
    Sxpr obj = string_new_n(peek_token(p), token_len(p));
    return Parser_set_value(p, obj);
}

int Parser_data(Parser *p){
    Sxpr obj = string_new_n(peek_token(p), token_len(p));
    return Parser_set_value(p, obj);
}

int Parser_uint(Parser *p){
    unsigned int x = htonl(*(unsigned int *)peek_token(p));
    return Parser_set_value(p, OINT(x));
}

static int get_escape(char c, char *d){
    int err = 0;
    switch(c){
    case 'a':            *d = '\a'; break;
    case 'b':            *d = '\b'; break;
    case 'f':            *d = '\f'; break;
    case 'n':            *d = '\n'; break;
    case 'r':            *d = '\r'; break;
    case 't':            *d = '\t'; break;
    case 'v':            *d = '\v'; break;
    case c_escape:       *d = c_escape; break;
    case c_single_quote: *d = c_single_quote; break;
    case c_double_quote: *d = c_double_quote; break;
    default:
        err = -EINVAL;
    }
    return err;
}

int Parser_ready(Parser *p){
    return CONSP(p->val) || (p->start_state && CONSP(p->start_state->val));
}

Sxpr Parser_get_val(Parser *p){
    Sxpr v = ONONE, w = ONONE;
    if(CONSP(p->val)){
    } else if (p->start_state && CONSP(p->start_state->val)){
        p->val = p->start_state->val;
        p->val = nrev(p->val);
        p->start_state->val = ONULL;
    }  else {
        goto exit;
    }
    w = p->val;
    v = CAR(w);
    p->val = CDR(w);
    hfree(w);
  exit:
    return v;
}

Sxpr Parser_get_all(Parser *p){
    Sxpr v = ONULL;
    if(CONSP(p->val)){
        v = p->val;
        p->val = ONONE;
    } else if(p->start_state && CONSP(p->start_state->val)){
        v = p->start_state->val;
        p->start_state->val = ONULL;
        v = nrev(v);
    }
    return v;
}

static int state_comment(Parser *p, char c){
    int err = 0;
    if(c == '\n' || Parser_at_eof(p)){
        Parser_pop(p);
    } else {
        err = input_char(p, c);
    }
    return err;
}

static int begin_comment(Parser *p, char c){
    int err = 0;
    err = Parser_push(p, state_comment, "comment");
    if(err) goto exit;
    err = input_char(p, c);
  exit:
    return err;
}

static int end_string(Parser *p){
    int err = 0;
    err = Parser_string(p);
    if(err) goto exit;
    err = Parser_return(p);
  exit:
    return err;
}

static int octaldone(Parser *p){
    int err = 0;
    char d = (char)(p->state->ival & 0xff);
    Parser_pop(p);
    err = Parser_input_char(p, d);
    return err;
}

static int octaldigit(Parser *p, int d){
    int err = 0;
    p->state->ival *= 8;
    p->state->ival += d; 
    p->state->count++;
    if(err) goto exit;
    if(p->state->ival < 0 || p->state->ival > 0xff){
        err = Parser_error(p);
        goto exit;
    }
    if(p->state->count == 3){
        err = octaldone(p);
    }
  exit:
    return err;
}

static int state_octal(Parser *p, char c){
    int err = 0;
    if(Parser_at_eof(p)){
        err = Parser_error_id(p, PARSE_ERR_UNEXPECTED_EOF);
        goto exit;
    } else if('0' <= c && c <= '7'){
        err = octaldigit(p, c - '0');
    } else {
        err = octaldone(p);
        if(err) goto exit;
        Parser_input_char(p, c);
    }
  exit:
    return err;
}

static int hexdone(Parser *p){
    int err = 0;
    char d = (char)(p->state->ival & 0xff);
    Parser_pop(p);
    err = Parser_input_char(p, d);
    return err;
}
    
static int hexdigit(Parser *p, int d){
    int err = 0;
    p->state->ival *= 16;
    p->state->ival += d; 
    p->state->count++;
    if(err) goto exit;
    if(p->state->ival < 0 || p->state->ival > 0xff){
        err = Parser_error(p);
        goto exit;
    }
    if(p->state->count == 2){
        err = hexdone(p);
    }
  exit:
    return err;
}
    
static int state_hex(Parser *p, char c){
    int err = 0;
    if(Parser_at_eof(p)){
        err = Parser_error_id(p, PARSE_ERR_UNEXPECTED_EOF);
        goto exit;
    } else if('0' <= c && c <= '9'){
        err = hexdigit(p, c - '0');
    } else if('A' <= c && c <= 'F'){
        err = hexdigit(p, c - 'A' + 10);
    } else if('a' <= c && c <= 'f'){
        err = hexdigit(p, c - 'a' + 10);
    } else if(p->state->count){
        err = hexdone(p);
        if(err) goto exit;
        Parser_input_char(p, c);
    }
  exit:
    return err;
}

static int state_escape(Parser *p, char c){
    int err = 0;
    char d;
    if(Parser_at_eof(p)){
        err = Parser_error_id(p, PARSE_ERR_UNEXPECTED_EOF);
        goto exit;
    }
    if(get_escape(c, &d) == 0){
        err = save_char(p, d);
        if(err) goto exit;
        Parser_pop(p);
    } else if(c == 'x'){
        p->state->fn = state_hex;
        p->state->ival = 0;
        p->state->count = 0;
    } else {
        p->state->fn = state_octal;
        p->state->ival = 0;
        p->state->count = 0;
        err = Parser_input_char(p, c);
    }
  exit:
    return err;
}

static int state_string(Parser *p, char c){
    int err = 0;
    if(Parser_at_eof(p)){
        err = Parser_error_id(p, PARSE_ERR_UNEXPECTED_EOF);
    } else if(c == p->state->delim){
        err = end_string(p);
    } else if(c == '\\'){
        err = Parser_push(p, state_escape, "escape");
    } else {
        err = save_char(p, c);
    }
    return err;
}

static int begin_string(Parser *p, char c){
    int err = 0;
    err = Parser_push(p, state_string, "string");
    if(err) goto exit;
    new_token(p);
    p->state->delim = c;
  exit:
    return err;
}

static int end_atom(Parser *p){
    int err = 0;
    err = Parser_atom(p);
    if(err) goto exit;
    err = Parser_return(p);
  exit:
    return err;
}

static int state_atom(Parser *p, char c){
    int err = 0;
    if(Parser_at_eof(p)){
        err = end_atom(p);
    } else if(is_separator(p, c) ||
              in_space_class(c) ||
              in_comment_class(c)){
        err = end_atom(p);
        if(err) goto exit;
        err = Parser_input_char(p, c);
    } else {
        err = save_char(p, c);
    }
  exit:
    return err;
}

static int begin_atom(Parser *p, char c){
    int err = 0;
    err = Parser_push(p, state_atom, "atom");
    if(err) goto exit;
    new_token(p);
    err = save_char(p, c);
  exit:
    return err;
}

static int end_data(Parser *p){
    int err = 0;
    err = Parser_data(p);
    if(err) goto exit;
    err = Parser_return(p);
  exit:
    return err;
}

static int counted_data(Parser *p, char c){
    int err = 0;
    err = save_char(p, c);
    if(err) goto exit;
    if(token_len(p) == p->state->count){
        err = end_data(p);
    }
  exit:
    return err;
}

static int counted_data_count(Parser *p, char c){
    int err = 0;
    if(c == p->state->delim){
        new_token(p);
        p->state->count = p->state->ival;
        p->state->fn = counted_data;
    } else if('0' <= c && c <= '9'){
        p->state->ival *= 10;
        p->state->ival += c - '0';
    } else {
        err = -EINVAL;
    }
    return err;
}

static int quoted_data(Parser *p, char c){
    int err = 0;
    int count = p->state->count;
    err = save_char(p, c);
    if(err) goto exit;
    // Check that buf is longer than delim and
    // ends with delim. If so, trim delim off and return.
    if((token_len(p) >= count) &&
       !memcmp(p->tok_end - count, p->buf, count)){
        p->tok_end -= count;
        end_data(p);
    }
  exit:
    return err;
}

static int quoted_data_delim(Parser *p, char c){
    // Saves the delim in the token buffer.
    int err = 0;
    err = save_char(p, c);
    if(err) goto exit;
    if(c == p->state->delim){
        p->state->fn = quoted_data;
        p->state->count = token_len(p);
        // Advance the token pointer past the delim.
        p->tok = p->tok_end;
    }
  exit:
    return err;
}

static int state_data(Parser *p, char c){
    // Quoted data:
    // <<delim< anything not containing delimiter<delim<
    // Where 'delim' is anything not containing '<'.
    // Counted data:
    // <*nnn..* N bytes
    // Where nnn... is N in decimal (
    int err = 0;
    switch(c){
    case c_data_count:
        p->state->delim = c;
        p->state->fn = counted_data_count;
        p->state->ival = 0;
        new_token(p);
        break;
    case c_data_quote:
        p->state->delim = c;
        p->state->fn = quoted_data_delim;
        new_token(p);
        err = save_char(p, c);
        break;
    default:
        err = Parser_error(p);
        break;
    }
    return err;
}

static int begin_data(Parser *p, char c){
    int err = 0;
    err = Parser_push(p, state_data, "data");
    if(err) goto exit;
    new_token(p);
  exit:
    return err;
}

static int state_list(Parser *p, char c){
    int err = 0;
    dprintf(">\n");
    if(Parser_at_eof(p)){
        err = Parser_error_id(p, PARSE_ERR_UNEXPECTED_EOF);
    } else if(c == c_list_close){
        p->state->val = nrev(p->state->val);
        err = Parser_return(p);
    } else {
        err = state_start(p, c);
    }
    dprintf("< err=%d\n", err);
    return err;
    
}

static int begin_list(Parser *p, char c){
    return Parser_push(p, state_list, "list");
}

static int state_start(Parser *p, char c){
    int err = 0;
    dprintf(">\n");
    if(Parser_at_eof(p)){
        err = Parser_return(p);
    } else if(in_space_class(c)){
        //skip
    } else if(in_comment_class(c)){
        begin_comment(p, c);
    } else if(c == c_list_open){
        begin_list(p, c);
    } else if(c == c_list_close){
        err = Parser_error(p);
    } else if(in_string_quote_class(c)){
        begin_string(p, c);
    } else if(c == c_data_open){
        begin_data(p, c);
    } else if(in_printable_class(c)){
        begin_atom(p, c);
    } else if(c == 0x04){
        //ctrl-D, EOT: end-of-text.
        Parser_input_eof(p);
    } else {
        err = Parser_error(p);
    }
    dprintf("< err=%d\n", err);
    return err;
}

int begin_start(Parser *p, char c){
    int err = 0;
    dprintf(">\n");
    err = Parser_push(p, state_start, "start");
    if(err) goto exit;
    p->start_state = p->state;
  exit:
    dprintf("< err=%d\n", err);
    return err;
}

int Parser_input_char(Parser *p, char c){
    int err = 0;
    if(Parser_at_eof(p)){
        //skip;
    } else {
        input_char(p, c);
    }
    if(!p->state){
        err = p->begin(p, c);
        if(err) goto exit;
    }
    err = p->state->fn(p, c);
  exit:
    return err;
}

int Parser_input_eof(Parser *p){
    int err = 0;
    p->eof = 1;
    err = Parser_input_char(p, IOSTREAM_EOF);
    return err;
}

int Parser_input(Parser *p, char *buf, int buf_n){
    int err = 0;
    int i = 0;
    dprintf("> buf_n=%d\n", buf_n);
    if(buf_n <= 0){
        buf_n = 0;
        err = Parser_input_eof(p);
        goto exit;
    }
    dprintf("> buf=|%*s|\n", buf_n, buf);
    for(i = 0; i < buf_n; i++){
        err = Parser_input_char(p, buf[i]);
        if(err) goto exit;
    }
  exit:
    err = (err < 0 ? err : buf_n);
    dprintf("< err=%d\n", err);
    return err;
}

#ifdef SXPR_PARSER_MAIN
/* Stuff for standalone testing. */

#include "file_stream.h"
//#include "string_stream.h"

/** Main program for testing.
 * Parses input and prints it.
 *
 * @param argc number of arguments
 * @param argv arguments
 * @return error code
 */
int main(int argc, char *argv[]){
    Parser *pin;
    int err = 0;
    char buf[1024];
    int k;
    Sxpr obj;
    int i = 0;

    pin = Parser_new();
    Parser_set_error_stream(pin, iostdout);
    dprintf("> parse...\n");
    while(1){
        k = fread(buf, 1, 100, stdin);
        if(k>=0){
            buf[k+1] = '\0';
        }
        err = Parser_input(pin, buf, k);
        while(Parser_ready(pin)){
            obj = Parser_get_val(pin);
            printf("obj %d\n", i++);
            objprint(iostdout, obj, 0); printf("\n");
        }
        if(k <= 0) break;
    }
    dprintf("> err=%d\n", err);
    return 0;
}
#endif
