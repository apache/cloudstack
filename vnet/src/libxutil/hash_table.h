/*
 * Copyright (C) 2001 - 2005 Mike Wray <mike.wray@hp.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#ifndef _XUTIL_HASH_TABLE_H_
#define _XUTIL_HASH_TABLE_H_

#include "iostream.h"
#include "sys_string.h"

typedef unsigned long Hashcode;

/** Type used to pass parameters to table functions. */
typedef union TableArg {
    unsigned long ul;
    void *ptr;
} TableArg;

/** An entry in a bucket list. */
typedef struct HTEntry {
    /** Hashcode of the entry's key. */
    Hashcode hashcode;
    /** The key for this entry. */
    void *key;
    /** The value in this entry. */
    void *value;
    /** The next entry in the list. */
    struct HTEntry *next;
} HTEntry;

/** A bucket in a rule table. */
typedef struct HTBucket {
    /** Number of entries in the bucket. */
    int count;
    /** First entry in the bucket (may be null). */
    HTEntry *head;
} HTBucket;

/** Default number of buckets in a hash table.
 * You want enough buckets so the lists in the buckets will typically be short.
 * If the hash function is good it doesn't matter whether the number of
 * buckets is prime or not.
 */
//#define HT_BUCKETS_N 1
//#define HT_BUCKETS_N 3
//#define HT_BUCKETS_N 7
//#define HT_BUCKETS_N 17
//#define HT_BUCKETS_N 97
//#define HT_BUCKETS_N 211
//#define HT_BUCKETS_N 401
#define HT_BUCKETS_N 1021

typedef struct HashTable HashTable;

/** Type for a function used to select table entries. */
typedef int TableTestFn(TableArg arg, HashTable *table, HTEntry *entry);

/** Type for a function to map over table entries. */
typedef int TableMapFn(TableArg arg, HashTable *table, HTEntry *entry);

/** Type for a function to free table entries. */
typedef void TableFreeFn(HashTable *table, HTEntry *entry);

/** Type for a function to hash table keys. */
typedef Hashcode TableHashFn(void *key);

/** Type for a function to test table keys for equality. */
typedef int TableEqualFn(void *key1, void *key2);

/** Type for a function to order table entries. */
typedef int TableOrderFn(HTEntry *e1, HTEntry *e2);

/** General hash table.
 * A hash table with a list in each bucket.
 * Functions can be supplied for freeing entries, hashing keys, and comparing keys.
 * These all default to 0, when default behaviour treating keys as integers is used.
 */
struct HashTable {
    /** Array of buckets, each with its own list. */
    HTBucket *buckets;
    /** Number of buckets in the bucket array. */
    int buckets_n;
    /** Number of entries in the table. */
    int entry_count;
    unsigned long key_size;
    /** Function to free keys and values in entries. */
    TableFreeFn *entry_free_fn;
    /** Function to hash keys. */
    TableHashFn *key_hash_fn;
    /** Function to compare keys for equality. */
    TableEqualFn *key_equal_fn;
    /** Place for the user of the table to hang extra data. */
    void *user_data;
};

extern HashTable *HashTable_new(int bucket_n);
extern void HashTable_free(HashTable *table);
extern HTEntry * HTEntry_new(Hashcode hashcode, void *key, void *value);
extern void HTEntry_free(HTEntry *entry);
extern int HashTable_set_bucket_n(HashTable *table, int bucket_n);
extern void HashTable_clear(HashTable *table);
extern HTEntry * HashTable_add_entry(HashTable *table, Hashcode hashcode, void *key, void *value);
extern HTEntry * HashTable_get_entry(HashTable *table, void *key);
extern HTEntry * HashTable_add(HashTable *table, void *key, void *value);
extern void * HashTable_get(HashTable *table, void *key);
extern int HashTable_remove(HashTable *table, void *key);
extern HTEntry * HashTable_find_entry(HashTable *table, Hashcode hashcode,
                                      TableTestFn *test_fn, TableArg arg);
extern int HashTable_remove_entry(HashTable *table, Hashcode hashcode,
                                   TableTestFn *test_fn, TableArg arg);
extern void HashTable_print(HashTable *table, IOStream *out);
extern int HashTable_set_buckets_n(HashTable *table, int buckets_n);
extern int HashTable_adjust(HashTable *table, int buckets_min);

extern int HashTable_order_bucket(HashTable *table, Hashcode hashcode, TableOrderFn *order);

typedef unsigned long ub4;
typedef unsigned char ub1;

extern ub4 hash(const ub1 *k, ub4 length, ub4 initval);

/** Hash some bytes starting with a given hashcode.
 *
 * @param h initial hashcode - use 0, a previous hash, or an arbitrary value
 * @param b bytes to hash
 * @param b_n number of bytes to hash
 * @return hashcode
 */
static inline Hashcode hash_hvoid(Hashcode h, const void *b, unsigned b_n){
    return hash(b, b_n, h);
}

/** Hash a string (null-terminated).
 *
 * @param s input to hash
 * @return hashcode
 */
static inline Hashcode hash_string(char *s){
    return (s ? hash_hvoid(0, s, strlen(s)) : 0);
}

/** Macro to declare variables for HashTable_for_each() to use.
 *
 * @param entry variable that is set to entries in the table
 */
#define HashTable_for_decl(entry) \
  HashTable *_var_table; \
  HTBucket *_var_bucket; \
  HTBucket *_var_end; \
  HTEntry *_var_next; \
  HTEntry *entry

/** Macro to iterate over the entries in a hashtable.
 * Must be in a scope where HashTable_for_decl() has been used to declare
 * variables for it to use.
 * The variable 'entry' is iterated over entries in the table.
 * The code produced is syntactically a loop, so it must be followed by
 * a loop body, typically some statements in braces:
 * HashTable_for_each(entry, table){ ...loop body... }
 *
 * HashTable_for_each() and HashTable_for_decl() cannot be used for nested
 * loops as variables will clash.
 *
 * @note The simplest way to code a direct loop over the entries in a hashtable
 * is to use a loop over the buckets, with a nested loop over the entries
 * in a bucket. Using this approach in a macro means the macro contains
 * an opening brace, and calls to it must be followed by 2 braces!
 * To avoid this the code has been restructured so that it is a for loop.
 * So that statements could be used in the test expression of the for loop,
 * we have used the gcc statement expression extension ({ ... }).
 *
 * @param entry variable to iterate over the entries
 * @param table to iterate over (non-null)
 */
#define HashTable_for_each(entry, table) \
  _var_table = table; \
  _var_bucket = _var_table->buckets; \
  _var_end = _var_bucket + _var_table->buckets_n; \
  for(entry=0, _var_next=0; \
      ({ if(_var_next){ \
             entry = _var_next; \
             _var_next = entry->next; \
          } else { \
             while(_var_bucket < _var_end){ \
                 entry = _var_bucket->head; \
                 _var_bucket++; \
                 if(entry){ \
                      _var_next = entry->next; \
                      break; \
                 } \
             } \
          }; \
         entry; }); \
      entry = _var_next )

/** Map a function over the entries in a table.
 * Mapping stops when the function returns a non-zero value.
 * Uses the gcc statement expression extension ({ ... }).
 *
 * @param table to map over
 * @param fn function to apply to entries
 * @param arg first argument to call the function with
 * @return 0 if fn always returned 0, first non-zero value otherwise
 */
#define HashTable_map(table, fn, arg) \
  ({ HashTable_for_decl(_var_entry); \
    TableArg _var_arg = arg; \
    int _var_value = 0; \
    HashTable_for_each(_var_entry, table){ \
        if((_var_value = fn(_var_arg, _var_table, _var_entry))) break; \
    } \
    _var_value; })

/** Cast x to the type for a key or value in a hash table.
 * This avoids compiler warnings when using short integers
 * as keys or values (especially on 64-bit platforms).
 */
#define HKEY(x) ((void*)(unsigned long)(x))

/** Cast x from the type for a key or value in a hash table.
 * to an unsigned long. This avoids compiler warnings when using
 * short integers as keys or values (especially on 64-bit platforms).
 */
#define HVAL(x) ((unsigned long)(x))

#endif /* !_XUTIL_HASH_TABLE_H_ */
