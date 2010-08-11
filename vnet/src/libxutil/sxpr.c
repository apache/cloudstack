/*
 * Copyright (C) 2001 - 2004 Mike Wray <mike.wray@hp.com>
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

#include <stdarg.h>
#include "sys_string.h"
#include "lexis.h"
#include "sys_net.h"
#include "hash_table.h"
#include "sxpr.h"

#ifdef __KERNEL__
#include <linux/errno.h>
#else
#include <errno.h>
#endif

#ifdef __KERNEL__
#include <linux/random.h>

int rand(void){
    int v;
    get_random_bytes(&v, sizeof(v));
    return v;
}

#else
#include <stdlib.h>
#endif

#undef free

/** @file
 * General representation of sxprs.
 * Includes print, equal, and free functions for the sxpr types.
 *
 * Zero memory containing an Sxpr will have the value ONONE - this is intentional.
 * When a function returning an sxpr cannot allocate memory we return ONOMEM.
 *
 */

static int atom_print(IOStream *io, Sxpr obj, unsigned flags);
static int atom_equal(Sxpr x, Sxpr y);
static void atom_free(Sxpr obj);
static Sxpr atom_copy(Sxpr obj);

static int string_print(IOStream *io, Sxpr obj, unsigned flags);
static int string_equal(Sxpr x, Sxpr y);
static void string_free(Sxpr obj);
static Sxpr string_copy(Sxpr obj);

static int cons_print(IOStream *io, Sxpr obj, unsigned flags);
static int cons_equal(Sxpr x, Sxpr y);
static void cons_free(Sxpr obj);
static Sxpr cons_copy(Sxpr obj);

static int null_print(IOStream *io, Sxpr obj, unsigned flags);
static int none_print(IOStream *io, Sxpr obj, unsigned flags);
static int int_print(IOStream *io, Sxpr obj, unsigned flags);
static int bool_print(IOStream *io, Sxpr obj, unsigned flags);
static int err_print(IOStream *io, Sxpr obj, unsigned flags);
static int nomem_print(IOStream *io, Sxpr obj, unsigned flags);

/** Type definitions. */
static SxprType types[1024] = {
    [T_NONE]     { .type=    T_NONE,     .name= "none",       .print= none_print      },
    [T_NULL]     { .type=    T_NULL,     .name= "null",       .print= null_print      },
    [T_UINT]     { .type=    T_UINT,     .name= "int",        .print= int_print,      },
    [T_BOOL]     { .type=    T_BOOL,     .name= "bool",       .print= bool_print,     },
    [T_ERR]      { .type=    T_ERR,      .name= "err",        .print= err_print,      },
    [T_NOMEM]    { .type=    T_ERR,      .name= "nomem",      .print= nomem_print,    },
    [T_ATOM]     { .type=    T_ATOM,     .name= "atom",       .print= atom_print,
                   .pointer= TRUE,
                   .free=    atom_free,
                   .equal=   atom_equal,
                   .copy=    atom_copy,
                 },
    [T_STRING]   { .type=    T_STRING,   .name= "string",     .print= string_print,
                   .pointer= TRUE,
                   .free=    string_free,
                   .equal=   string_equal,
                   .copy=    string_copy,
                 },
    [T_CONS]     { .type=    T_CONS,     .name= "cons",       .print= cons_print,
                   .pointer= TRUE,
                   .free=    cons_free,
                   .equal=   cons_equal,
                   .copy=    cons_copy,
                 },
};

/** Number of entries in the types array. */
static int type_sup = sizeof(types)/sizeof(types[0]);

/** Define a type.
 * The tydef must have a non-zero type code.
 * It is an error if the type code is out of range or already defined.
 *
 * @param tydef type definition
 * @return 0 on success, error code otherwise
 */
int def_sxpr_type(SxprType *tydef){
    int err = 0;
    int ty = tydef->type;
    if(ty < 0 || ty >= type_sup){
        err = -EINVAL;
        goto exit;
    }
    if(types[ty].type){
        err = -EEXIST;
        goto exit;
    }
    types[ty] = *tydef;
  exit:
    return err;
    
}

/** Get the type definition for a given type code.
 *
 * @param ty type code
 * @return type definition or null
 */
SxprType *get_sxpr_type(int ty){
    if(0 <= ty && ty < type_sup){
        return types+ty;
    }
    return NULL;
}

/** The default print function.
 *
 * @param io stream to print to
 * @param x sxpr to print
 * @param flags print flags
 * @return number of bytes written on success
 */
int default_print(IOStream *io, Sxpr x, unsigned flags){
    return IOStream_print(io, "#<%u %lu>\n", get_type(x), get_ul(x));
}

/** The default equal function.
 * Uses eq().
 *
 * @param x sxpr to compare
 * @param y sxpr to compare
 * @return 1 if equal, 0 otherwise
 */
int default_equal(Sxpr x, Sxpr y){
    return eq(x, y);
}

/** General sxpr print function.
 * Prints an sxpr on a stream using the print function for the sxpr type.
 * Printing is controlled by flags from the PrintFlags enum.
 * If PRINT_TYPE is in the flags the sxpr type is printed before the sxpr
 * (for debugging).
 *
 * @param io stream to print to
 * @param x sxpr to print
 * @param flags print flags
 * @return number of bytes written
 */
int objprint(IOStream *io, Sxpr x, unsigned flags){
    SxprType *def = get_sxpr_type(get_type(x));
    ObjPrintFn *print_fn = (def && def->print ? def->print : default_print);
    int k = 0;
    if(!io) return k;
    if(flags & PRINT_TYPE){
        k += IOStream_print(io, "%s:", def->name);
    }
    if(def->pointer && (flags & PRINT_ADDR)){
        k += IOStream_print(io, "<%p>", get_ptr(x));
    }
    k += print_fn(io, x, flags);
    return k;
}

Sxpr objcopy(Sxpr x){
    SxprType *def = get_sxpr_type(get_type(x));
    ObjCopyFn *copy_fn = (def ? def->copy : NULL);
    Sxpr v;
    if(copy_fn){
        v = copy_fn(x);
    } else if(def->pointer){
        v = ONOMEM;
    } else {
        v = x;
    }
    return v;
}

/** General sxpr free function.
 * Frees an sxpr using the free function for its type.
 * Free functions must recursively free any subsxprs.
 * If no function is defined then the default is to
 * free sxprs whose type has pointer true.
 * Sxprs must not be used after freeing.
 *
 * @param x sxpr to free
 */
void objfree(Sxpr x){
    SxprType *def = get_sxpr_type(get_type(x));

    if(def){
        if(def->free){
            def->free(x);
        } else if (def->pointer){
            hfree(x);
        }
    }
}

/** General sxpr equality function.
 * Compares x and y using the equal function for x.
 * Uses default_equal() if x has no equal function.
 *
 * @param x sxpr to compare
 * @param y sxpr to compare
 * @return 1 if equal, 0 otherwise
 */
int objequal(Sxpr x, Sxpr y){
    SxprType *def = get_sxpr_type(get_type(x));
    ObjEqualFn *equal_fn = (def && def->equal ? def->equal : default_equal);
    return equal_fn(x, y);
}

/** Search for a key in an alist.
 * An alist is a list of conses, where the cars
 * of the conses are the keys. Compares keys using equality.
 *
 * @param k key
 * @param l alist to search
 * @return first element of l with car k, or ONULL
 */
Sxpr assoc(Sxpr k, Sxpr l){
    for( ; CONSP(l) ; l = CDR(l)){
        Sxpr x = CAR(l);
        if(CONSP(x) && objequal(k, CAR(x))){
            return x;   
        }
    }
    return ONULL;
}

/** Search for a key in an alist.
 * An alist is a list of conses, where the cars
 * of the conses are the keys. Compares keys using eq.
 *
 * @param k key
 * @param l alist to search
 * @return first element of l with car k, or ONULL
 */
Sxpr assocq(Sxpr k, Sxpr l){
    for( ; CONSP(l); l = CDR(l)){
        Sxpr x = CAR(l);
        if(CONSP(x) && eq(k, CAR(x))){
            return x;
        }
    }
    return ONULL;
}

/** Add a new key and value to an alist.
 *
 * @param k key
 * @param l value
 * @param l alist
 * @return l with the new cell added to the front
 */
Sxpr acons(Sxpr k, Sxpr v, Sxpr l){
    Sxpr x, y;
    x = cons_new(k, v);
    if(NOMEMP(x)) return x;
    y = cons_new(x, l);
    if(NOMEMP(y)) cons_free_cells(x);
    return y;
}

/** Test if a list contains an element.
 * Uses sxpr equality.
 *
 * @param l list
 * @param x element to look for
 * @return a tail of l with x as car, or ONULL
 */
Sxpr cons_member(Sxpr l, Sxpr x){
    for( ; CONSP(l) && !eq(x, CAR(l)); l = CDR(l)){}
    return l;
}

/** Test if a list contains an element satisfying a test.
 * The test function is called with v and an element of the list.
 *
 * @param l list
 * @param test_fn test function to use
 * @param v value for first argument to the test
 * @return a tail of l with car satisfying the test, or 0
 */
Sxpr cons_member_if(Sxpr l, ObjEqualFn *test_fn, Sxpr v){
    for( ; CONSP(l) && !test_fn(v, CAR(l)); l = CDR(l)){ }
    return l;
}

/** Test if the elements of list 't' are a subset of the elements
 * of list 's'. Element order is not significant.
 *
 * @param s element list to check subset of
 * @param t element list to check if is a subset
 * @return 1 if is a subset, 0 otherwise
 */
int cons_subset(Sxpr s, Sxpr t){
    for( ; CONSP(t); t = CDR(t)){
        if(!CONSP(cons_member(s, CAR(t)))){
            return 0;
        }
    }
    return 1;
}

/** Test if two lists have equal sets of elements.
 * Element order is not significant.
 *
 * @param s list to check
 * @param t list to check
 * @return 1 if equal, 0 otherwise
 */
int cons_set_equal(Sxpr s, Sxpr t){
    return cons_subset(s, t) && cons_subset(t, s);
}

#ifdef USE_GC
/*============================================================================*/
/* The functions inside this ifdef are only safe if GC is used.
 * Otherwise they may leak memory.
 */

/** Remove an element from a list (GC only).
 * Uses sxpr equality and removes all instances, even
 * if there are more than one.
 *
 * @param l list to remove elements from
 * @param x element to remove
 * @return modified input list
 */
Sxpr cons_remove(Sxpr l, Sxpr x){
    return cons_remove_if(l, eq, x);
}

/** Remove elements satisfying a test (GC only).
 * The test function is called with v and an element of the set.
 *
 * @param l list to remove elements from
 * @param test_fn function to use to decide if an element should be removed
 * @return modified input list
 */
Sxpr cons_remove_if(Sxpr l, ObjEqualFn *test_fn, Sxpr v){
    Sxpr prev = ONULL, elt, next;

    for(elt = l; CONSP(elt); elt = next){
        next = CDR(elt);
        if(test_fn(v, CAR(elt))){
            if(NULLP(prev)){
                l = next;
            } else {
                CDR(prev) = next;
            }
        }
    }
    return l;
}

/** Set the value for a key in an alist (GC only).
 * If the key is present, changes the value, otherwise
 * adds a new cell.
 *
 * @param k key
 * @param v value
 * @param l alist
 * @return modified or extended list
 */
Sxpr setf(Sxpr k, Sxpr v, Sxpr l){
    Sxpr e = assoc(k, l);
    if(NULLP(e)){
        l = acons(k, v, l);
    } else {
        CAR(CDR(e)) = v;
    }
    return l;
}
/*============================================================================*/
#endif /* USE_GC */

/** Create a new atom with the given name.
 *
 * @param name the name
 * @return new atom
 */
Sxpr atom_new(char *name){
    Sxpr n, obj = ONOMEM;
    long v;

    // Don't always want to do this.
    if(0 && convert_atol(name, &v) == 0){
        obj = OINT(v);
    } else {
        n = string_new(name);
        if(NOMEMP(n)) goto exit;
        obj = HALLOC(ObjAtom, T_ATOM);
        if(NOMEMP(obj)){
            string_free(n);
            goto exit;
        }
        OBJ_ATOM(obj)->name = n;
    }
  exit:
    return obj;
}

/** Free an atom.
 *
 * @param obj to free
 */
void atom_free(Sxpr obj){
    // Interned atoms are shared, so do not free.
    if(OBJ_ATOM(obj)->interned) return;
    objfree(OBJ_ATOM(obj)->name);
    hfree(obj);
}

/** Copy an atom.
 *
 * @param obj to copy
 */
Sxpr atom_copy(Sxpr obj){
    Sxpr v;
    if(OBJ_ATOM(obj)->interned){
        v = obj;
    } else {
        v = atom_new(atom_name(obj));
    }
    return v;
}

/** Print an atom. Prints the atom name.
 *
 * @param io stream to print to
 * @param obj to print
 * @param flags print flags
 * @return number of bytes printed
 */
int atom_print(IOStream *io, Sxpr obj, unsigned flags){
    return objprint(io, OBJ_ATOM(obj)->name, flags);
}

/** Atom equality.
 *
 * @param x to compare
 * @param y to compare
 * @return 1 if equal, 0 otherwise
 */
int atom_equal(Sxpr x, Sxpr y){
    int ok;
    ok = eq(x, y);
    if(ok) goto exit;
    ok = ATOMP(y) && string_equal(OBJ_ATOM(x)->name, OBJ_ATOM(y)->name);
    if(ok) goto exit;
    ok = STRINGP(y) && string_equal(OBJ_ATOM(x)->name, y);
  exit:
    return ok;
}

/** Get the name of an atom.
 *
 * @param obj atom
 * @return name
 */
char * atom_name(Sxpr obj){
    return string_string(OBJ_ATOM(obj)->name);
}

int atom_length(Sxpr obj){
    return string_length(OBJ_ATOM(obj)->name);
}

/** Get the C string from a string sxpr.
 *
 * @param obj string sxpr
 * @return string
 */
char * string_string(Sxpr obj){
    return OBJ_STRING(obj)->data;
}

/** Get the length of a string.
 *
 * @param obj string
 * @return length
 */
int string_length(Sxpr obj){
    return OBJ_STRING(obj)->len;
}

/** Create a new string. The input string is copied,
 * and must be null-terminated.
 *
 * @param s characters to put in the string
 * @return new sxpr
 */
Sxpr string_new(char *s){
    int n = (s ? strlen(s) : 0);
    return string_new_n(s, n);
}

/** Create a new string. The input string is copied,
 * and need not be null-terminated.
 *
 * @param s characters to put in the string (may be null)
 * @param n string length
 * @return new sxpr
 */
Sxpr string_new_n(char *s, int n){
    Sxpr obj;
    obj = halloc(sizeof(ObjString) + n + 1, T_STRING);
    if(!NOMEMP(obj)){
        char *str = OBJ_STRING(obj)->data;
        OBJ_STRING(obj)->len = n;
        if(s){
            memcpy(str, s, n);
            str[n] = '\0';
        } else {
            memset(str, 0, n + 1);
        }
    }
    return obj;
}

/** Free a string.
 *
 * @param obj to free
 */
void string_free(Sxpr obj){
    hfree(obj);
}

/** Copy a string.
 *
 * @param obj to copy
 */
Sxpr string_copy(Sxpr obj){
    return string_new_n(string_string(obj), string_length(obj));
}

/** Determine if a string needs escapes when printed
 * using the given flags.
 *
 * @param str string to check
 * @param n string length
 * @param flags print flags
 * @return 1 if needs escapes, 0 otherwise
 */
int needs_escapes(char *str, int n, unsigned flags){
    char *c;
    int i;
    int val = 0;

    if(str){
        for(i=0, c=str; i<n; i++, c++){
            if(in_alpha_class(*c)) continue;
            if(in_decimal_digit_class(*c)) continue;
            if(in_class(*c, "/._+:@~-")) continue;
            val = 1;
            break;
        }
    }
    return val;
}

char randchar(void){
    int r;
    char c;
    for( ; ; ){
        r = rand();
        c = (r >> 16) & 0xff;
        if('a' <= c && c <= 'z') break;
    }
    return c;
}

int string_contains(char *s, int s_n, char *k, int k_n){
    int i, n = s_n - k_n;
    for(i=0; i < n; i++){
        if(!memcmp(s+i, k, k_n)) return 1;
    }
    return 0;
}

int string_delim(char *s, int s_n, char *d, int d_n){
    int i;
    if(d_n < 4) return -1;
    memset(d, 0, d_n+1);
    for(i=0; i<3; i++){
        d[i] = randchar();
    }
    for( ; i < d_n; i++){
        if(!string_contains(s, s_n, d, i)){
            return i;
        }
        d[i] = randchar();
    }
    return -1;
}

/** Print the bytes in a string as-is.
 *
 * @param io stream
 * @param str string
 * @param n length
 * @return bytes written or error code
 */
int _string_print_raw(IOStream *io, char *str, int n){
    int k = 0;
    k = IOStream_write(io, str, n);
    return k;
}

/** Print a string in counted data format.
 *
 * @param io stream
 * @param str string
 * @param n length
 * @return bytes written or error code
 */
int _string_print_counted(IOStream *io, char *str, int n){
    int k = 0;
    k += IOStream_print(io, "%c%c%d%c",
                        c_data_open, c_data_count, n, c_data_count);
    k += IOStream_write(io, str, n);
    return k;
}
  
/** Print a string in quoted data format.
 *
 * @param io stream
 * @param str string
 * @param n length
 * @return bytes written or error code
 */
int _string_print_quoted(IOStream *io, char *str, int n){
    int k = 0;
    char d[10];
    int d_n;
    d_n = string_delim(str, n, d, sizeof(d) - 1);
    k += IOStream_print(io, "%c%c%s%c",
                        c_data_open, c_data_quote, d, c_data_quote);
    k += IOStream_write(io, str, n);
    k += IOStream_print(io, "%c%s%c", c_data_quote, d, c_data_quote);
    return k;
}

/** Print a string as a quoted string.
 *
 * @param io stream
 * @param str string
 * @param n length
 * @return bytes written or error code
 */
int _string_print_string(IOStream *io, char *str, int n){
    int k = 0;
    
    k += IOStream_print(io, "\"");
    if(str){
        char *s, *t;
        for(s = str, t = str + n; s < t; s++){
            if(*s < ' ' || *s >= 127 ){
                switch(*s){
                case '\a': k += IOStream_print(io, "\\a");  break;
                case '\b': k += IOStream_print(io, "\\b");  break;
                case '\f': k += IOStream_print(io, "\\f");  break;
                case '\n': k += IOStream_print(io, "\\n");  break;
                case '\r': k += IOStream_print(io, "\\r");  break;
                case '\t': k += IOStream_print(io, "\\t");  break;
                case '\v': k += IOStream_print(io, "\\v");  break;
                default:
                    // Octal escape;
                    k += IOStream_print(io, "\\%o", *s);
                    break;
                }
            } else if(*s == c_double_quote ||
                      *s == c_single_quote ||
                      *s == c_escape){
                k += IOStream_print(io, "\\%c", *s);
            } else {
                k+= IOStream_print(io, "%c", *s);
            }
        }
    }
    k += IOStream_print(io, "\"");
    return k;
}

/** Print a string to a stream, with escapes if necessary.
 *
 * @param io stream to print to
 * @param str string
 * @param n string length
 * @param flags print flags
 * @return number of bytes written
 */
int _string_print(IOStream *io, char *str, int n, unsigned flags){
    int k = 0;
    if((flags & PRINT_COUNTED)){
        k = _string_print_counted(io, str, n);
    } else if((flags & PRINT_RAW) || !needs_escapes(str, n, flags)){
        k = _string_print_raw(io, str, n);
    } else if(n > 50){
        k = _string_print_quoted(io, str, n);
    } else {
        k = _string_print_string(io, str, n);
    }
    return k;
}

/** Print a string to a stream, with escapes if necessary.
 *
 * @param io stream to print to
 * @param obj string
 * @param flags print flags
 * @return number of bytes written
 */
int string_print(IOStream *io, Sxpr obj, unsigned flags){
    return _string_print(io,
                         OBJ_STRING(obj)->data,
                         OBJ_STRING(obj)->len,
                         flags);
}

int string_eq(char *s, int s_n, char *t, int t_n){
    return (s_n == t_n) && (memcmp(s, t, s_n) == 0);
}

/** Compare an sxpr with a string for equality.
 *
 * @param x string to compare with
 * @param y sxpr to compare
 * @return 1 if equal, 0 otherwise
 */
int string_equal(Sxpr x, Sxpr y){
    int ok = 0;
    ok = eq(x,y);
    if(ok) goto exit;
    ok = has_type(y, T_STRING) &&
        string_eq(OBJ_STRING(x)->data, OBJ_STRING(x)->len,
                  OBJ_STRING(y)->data, OBJ_STRING(y)->len);
    if(ok) goto exit;
    ok = has_type(y, T_ATOM) &&
        string_eq(OBJ_STRING(x)->data, OBJ_STRING(x)->len,
                  atom_name(y), atom_length(y));
  exit:
    return ok;
}

/** Create a new cons cell.
 * The cell is ONOMEM if either argument is.
 *
 * @param car sxpr for the car
 * @param cdr sxpr for the cdr
 * @return new cons
 */
Sxpr cons_new(Sxpr car, Sxpr cdr){
    Sxpr obj;
    if(NOMEMP(car) || NOMEMP(cdr)){
        obj = ONOMEM;
    } else {
        obj = HALLOC(ObjCons, T_CONS);
        if(!NOMEMP(obj)){
            ObjCons *z = OBJ_CONS(obj);
            z->car = car;
            z->cdr = cdr;
        }
    }
    return obj;
}

/** Push a new element onto a list.
 *
 * @param list list to add to
 * @param elt element to add
 * @return 0 if successful, error code otherwise
 */
int cons_push(Sxpr *list, Sxpr elt){
    Sxpr l;
    l = cons_new(elt, *list);
    if(NOMEMP(l)) return -ENOMEM;
    *list = l;
    return 0;
}

/** Free a cons. Recursively frees the car and cdr.
 *
 * @param obj to free
 */
void cons_free(Sxpr obj){
    Sxpr next;
    for(; CONSP(obj); obj = next){
        next = CDR(obj);
        objfree(CAR(obj));
        hfree(obj);
    }
    if(!NULLP(obj)){
        objfree(obj);
    }
}

/** Copy a cons. Recursively copies the car and cdr.
 *
 * @param obj to copy
 */
Sxpr cons_copy(Sxpr obj){
    Sxpr v = ONULL;
    Sxpr l = ONULL, x = ONONE;
    for(l = obj; CONSP(l); l = CDR(l)){
        x = objcopy(CAR(l));
        if(NOMEMP(x)) goto exit;
        x = cons_new(x, v);
        if(NOMEMP(x)) goto exit;
        v = x;
    }
    v = nrev(v);
  exit:
    if(NOMEMP(x)){
        objfree(v);
        v = ONOMEM;
    }
    return v;
}

/** Free a cons and its cdr cells, but not the car sxprs.
 * Does nothing if called on something that is not a cons.
 *
 * @param obj to free
 */
void cons_free_cells(Sxpr obj){
    Sxpr next;
    for(; CONSP(obj); obj = next){
        next = CDR(obj);
        hfree(obj);
    }
}

/** Print a cons.
 * Prints the cons in list format if the cdrs are conses.
 * uses pair (dot) format if the last cdr is not a cons (or null).
 *
 * @param io stream to print to
 * @param obj to print
 * @param flags print flags
 * @return number of bytes written
 */
int cons_print(IOStream *io, Sxpr obj, unsigned flags){
    int first = 1;
    int k = 0;
    k += IOStream_print(io, "(");
    for( ; CONSP(obj) ; obj = CDR(obj)){
        if(first){ 
            first = 0;
        } else {
            k += IOStream_print(io, " ");
        }
        k += objprint(io, CAR(obj), flags);
    }
    if(!NULLP(obj)){
        k += IOStream_print(io, " . ");
        k += objprint(io, obj, flags);
    }
    k += IOStream_print(io, ")");
    return (IOStream_error(io) ? -1 : k);
}

/** Compare a cons with another sxpr for equality.
 * If y is a cons, compares the cars and cdrs recursively.
 *
 * @param x cons to compare
 * @param y sxpr to compare
 * @return 1 if equal, 0 otherwise
 */
int cons_equal(Sxpr x, Sxpr y){
    return CONSP(y) &&
        objequal(CAR(x), CAR(y)) &&
        objequal(CDR(x), CDR(y));
}

/** Return the length of a cons list.
 *
 * @param obj list
 * @return length
 */
int cons_length(Sxpr obj){
    int count = 0;
    for( ; CONSP(obj); obj = CDR(obj)){
        count++;
    }
    return count;
}

/** Destructively reverse a cons list in-place.
 * If the argument is not a cons it is returned unchanged.
 * 
 * @param l to reverse
 * @return reversed list
 */
Sxpr nrev(Sxpr l){
    if(CONSP(l)){
        // Iterate down the cells in the list making the cdr of
        // each cell point to the previous cell. The last cell 
        // is the head of the reversed list.
        Sxpr prev = ONULL;
        Sxpr cell = l;
        Sxpr next;

        while(1){
            next = CDR(cell);
            CDR(cell) = prev;
            if(!CONSP(next)) break;
            prev = cell;
            cell = next;
        }
        l = cell;
    }
    return l;
}

/** Print the null sxpr.        
 *
 * @param io stream to print to
 * @param obj to print
 * @param flags print flags
 * @return number of bytes written
 */
static int null_print(IOStream *io, Sxpr obj, unsigned flags){
    return IOStream_print(io, "()");
}

/** Print the `unspecified' sxpr none.
 *
 * @param io stream to print to
 * @param obj to print
 * @param flags print flags
 * @return number of bytes written
 */
static int none_print(IOStream *io, Sxpr obj, unsigned flags){
    return IOStream_print(io, "<none>");
}

/** Print an integer.
 *
 * @param io stream to print to
 * @param obj to print
 * @param flags print flags
 * @return number of bytes written
 */
static int int_print(IOStream *io, Sxpr obj, unsigned flags){
    return IOStream_print(io, "%d", OBJ_INT(obj));
}

/** Print a boolean.
 *
 * @param io stream to print to
 * @param obj to print
 * @param flags print flags
 * @return number of bytes written
 */
static int bool_print(IOStream *io, Sxpr obj, unsigned flags){
    return IOStream_print(io, (OBJ_UINT(obj) ? k_true : k_false));
}

/** Print an error.
 *
 * @param io stream to print to
 * @param obj to print
 * @param flags print flags
 * @return number of bytes written
 */
static int err_print(IOStream *io, Sxpr obj, unsigned flags){
    int err = OBJ_INT(obj);
    if(err < 0) err = -err;
    return IOStream_print(io, "[error:%d:%s]", err, strerror(err));
}

/** Print the 'nomem' sxpr.
 *
 * @param io stream to print to
 * @param obj to print
 * @param flags print flags
 * @return number of bytes written
 */
static int nomem_print(IOStream *io, Sxpr obj, unsigned flags){
    return IOStream_print(io, "[ENOMEM]");
}

int sxprp(Sxpr obj, Sxpr name){
    return CONSP(obj) && objequal(CAR(obj), name);
}

/** Get the name of an element.
 * 
 * @param obj element
 * @return name
 */
Sxpr sxpr_name(Sxpr obj){
    Sxpr val = ONONE;
    if(CONSP(obj)){
        val = CAR(obj);
    } else if(STRINGP(obj) || ATOMP(obj)){
        val = obj;
    }
    return val;
}

int sxpr_is(Sxpr obj, char *s){
    if(ATOMP(obj)) return string_eq(atom_name(obj), atom_length(obj), s, strlen(s));
    if(STRINGP(obj)) return string_eq(string_string(obj), string_length(obj), s, strlen(s));
    return 0;
}

int sxpr_elementp(Sxpr obj, Sxpr name){
    int ok = 0;
    ok = CONSP(obj) && objequal(CAR(obj), name);
    return ok;
}

/** Get the attributes of an sxpr.
 * 
 * @param obj sxpr
 * @return attributes
 */
Sxpr sxpr_attributes(Sxpr obj){
    Sxpr val = ONULL;
    if(CONSP(obj)){
        obj = CDR(obj);
        if(CONSP(obj)){
            obj = CAR(obj);
            if(sxprp(obj, intern("@"))){
                val = CDR(obj);
            }
        }
    }
    return val;
}

Sxpr sxpr_attribute(Sxpr obj, Sxpr key, Sxpr def){
    Sxpr val = ONONE;
    val = assoc(sxpr_attributes(obj), key);
    if(CONSP(val) && CONSP(CDR(val))){
        val = CADR(def);
    } else {
        val = def;
    }
    return val;
}

/** Get the children of an sxpr.
 * 
 * @param obj sxpr
 * @return children
 */
Sxpr sxpr_children(Sxpr obj){
    Sxpr val = ONULL;
    if(CONSP(obj)){
        val = CDR(obj);
        if(CONSP(val) && sxprp(CAR(val), intern("@"))){
            val = CDR(val);
        }
    }
    return val;
}

Sxpr sxpr_child(Sxpr obj, Sxpr name, Sxpr def){
    Sxpr val = ONONE;
    Sxpr l;
    for(l = sxpr_children(obj); CONSP(l); l = CDR(l)){
        if(sxprp(CAR(l), name)){
            val = CAR(l);
            break;
        }
    }
    if(NONEP(val)) val = def;
    return val;
}

Sxpr sxpr_child0(Sxpr obj, Sxpr def){
    Sxpr val = ONONE;
    Sxpr l = sxpr_children(obj);
    if(CONSP(l)){
        val = CAR(l);
    } else {
        val = def;
    }
    return val;
}

Sxpr sxpr_childN(Sxpr obj, int n, Sxpr def){
    Sxpr val = def;
    Sxpr l;
    int i;
    for (i = 0, l = sxpr_children(obj); CONSP(l); i++, l = CDR(l)){
        if(i == n){
            val = CAR(l);
            break;
        }
    }
    return val;
}
    
Sxpr sxpr_child_value(Sxpr obj, Sxpr name, Sxpr def){
    Sxpr val = ONONE;
    val = sxpr_child(obj, name, ONONE);
    if(NONEP(val)){
        val = def;
    } else {
        val = sxpr_child0(val, def);
    }
    return val;
}

/** Table of interned symbols. Indexed by symbol name. */
static HashTable *symbols = NULL;

/** Hash function for entries in the symbol table.
 *
 * @param key to hash
 * @return hashcode
 */
static Hashcode sym_hash_fn(void *key){
    return hash_string((char*)key);
}

/** Key equality function for the symbol table.
 *
 * @param x to compare
 * @param y to compare
 * @return 1 if equal, 0 otherwise
 */
static int sym_equal_fn(void *x, void *y){
    return !strcmp((char*)x, (char*)y);
}

/** Entry free function for the symbol table.
 *
 * @param table the entry is in
 * @param entry being freed
 */
static void sym_free_fn(HashTable *table, HTEntry *entry){
    if(entry){
        objfree(((ObjAtom*)entry->value)->name);
        HTEntry_free(entry);
    }
}
        
/** Initialize the symbol table.
 *
 * @return 0 on sucess, error code otherwise
 */
static int init_symbols(void){
    symbols = HashTable_new(100);
    if(symbols){
        symbols->key_hash_fn = sym_hash_fn;
        symbols->key_equal_fn = sym_equal_fn;
        symbols->entry_free_fn = sym_free_fn;
        return 0;
    }
    return -1;
}

/** Cleanup the symbol table. Frees the table and all its symbols.
 */
void cleanup_symbols(void){
    HashTable_free(symbols);
    symbols = NULL;
}

/** Get the interned symbol with the given name.
 * No new symbol is created.
 *
 * @return symbol or null
 */
Sxpr get_symbol(char *sym){
    HTEntry *entry;
    if(!symbols){
        if(init_symbols()) return ONOMEM;
        return ONULL;
    }
    entry = HashTable_get_entry(symbols, sym);
    if(entry){
        return OBJP(T_ATOM, entry->value);
    } else {
        return ONULL;
    }
}

/** Get the interned symbol with the given name.
 * Creates a new symbol if necessary.
 *
 * @return symbol
 */
Sxpr intern(char *sym){
    Sxpr symbol = get_symbol(sym);
    if(NULLP(symbol)){
        if(!symbols) return ONOMEM;
        symbol = atom_new(sym);
        if(!NOMEMP(symbol)){
            OBJ_ATOM(symbol)->interned = TRUE;
            HashTable_add(symbols, atom_name(symbol), get_ptr(symbol));
        }
    }
    return symbol;
}
