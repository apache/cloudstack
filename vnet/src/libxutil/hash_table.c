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

#ifdef __KERNEL__
#  include <linux/config.h>
#  include <linux/module.h>
#  include <linux/kernel.h>
#  include <linux/errno.h>
#else
#  include <errno.h>
#  include <stddef.h>
#endif

#include "allocate.h"
#include "hash_table.h"

/** @file
 * Base support for hashtables.
 *
 * Hash codes are reduced modulo the number of buckets to index tables,
 * so there is no need for hash functions to limit the range of hashcodes.
 * In fact it is assumed that hashcodes do not change when the number of
 * buckets in the table changes.
 */

/*============================================================================*/
/*
--------------------------------------------------------------------
lookup2.c, by Bob Jenkins, December 1996, Public Domain.
You can use this free for any purpose.  It has no warranty.
--------------------------------------------------------------------
*/

#define hashsize(n) ((ub4)1<<(n))
#define hashmask(n) (hashsize(n)-1)

/*
--------------------------------------------------------------------
mix -- mix 3 32-bit values reversibly.
For every delta with one or two bit set, and the deltas of all three
  high bits or all three low bits, whether the original value of a,b,c
  is almost all zero or is uniformly distributed,
* If mix() is run forward or backward, at least 32 bits in a,b,c
  have at least 1/4 probability of changing.
* If mix() is run forward, every bit of c will change between 1/3 and
  2/3 of the time.  (Well, 22/100 and 78/100 for some 2-bit deltas.)
mix() was built out of 36 single-cycle latency instructions in a 
  structure that could supported 2x parallelism, like so:
      a -= b; 
      a -= c; x = (c>>13);
      b -= c; a ^= x;
      b -= a; x = (a<<8);
      c -= a; b ^= x;
      c -= b; x = (b>>13);
      ...
  Unfortunately, superscalar Pentiums and Sparcs can't take advantage 
  of that parallelism.  They've also turned some of those single-cycle
  latency instructions into multi-cycle latency instructions.  Still,
  this is the fastest good hash I could find.  There were about 2^^68
  to choose from.  I only looked at a billion or so.
--------------------------------------------------------------------
*/
#define mix(a,b,c) \
{ \
  a -= b; a -= c; a ^= (c>>13); \
  b -= c; b -= a; b ^= (a<<8); \
  c -= a; c -= b; c ^= (b>>13); \
  a -= b; a -= c; a ^= (c>>12);  \
  b -= c; b -= a; b ^= (a<<16); \
  c -= a; c -= b; c ^= (b>>5); \
  a -= b; a -= c; a ^= (c>>3);  \
  b -= c; b -= a; b ^= (a<<10); \
  c -= a; c -= b; c ^= (b>>15); \
}

/*
--------------------------------------------------------------------
hash() -- hash a variable-length key into a 32-bit value
  k     : the key (the unaligned variable-length array of bytes)
  len   : the length of the key, counting by bytes
  level : can be any 4-byte value
Returns a 32-bit value.  Every bit of the key affects every bit of
the return value.  Every 1-bit and 2-bit delta achieves avalanche.
About 36+6len instructions.

The best hash table sizes are powers of 2.  There is no need to do
mod a prime (mod is sooo slow!).  If you need less than 32 bits,
use a bitmask.  For example, if you need only 10 bits, do
  h = (h & hashmask(10));
In which case, the hash table should have hashsize(10) elements.

If you are hashing n strings (ub1 **)k, do it like this:
  for (i=0, h=0; i<n; ++i) h = hash( k[i], len[i], h);

By Bob Jenkins, 1996.  bob_jenkins@burtleburtle.net.  You may use this
code any way you wish, private, educational, or commercial.  It's free.

See http://burlteburtle.net/bob/hash/evahash.html
Use for hash table lookup, or anything where one collision in 2^32 is
acceptable.  Do NOT use for cryptographic purposes.
--------------------------------------------------------------------
*/

static inline ub4 _hash(const ub1 *k, ub4 length, ub4 initval)
//register ub1 *k;        /* the key */
//register ub4  length;   /* the length of the key */
//register ub4  initval;    /* the previous hash, or an arbitrary value */
{
    /*register*/ ub4 a,b,c,len;

   /* Set up the internal state */
   len = length;
   a = b = 0x9e3779b9;  /* the golden ratio; an arbitrary value */
   c = initval;           /* the previous hash value */

   /*---------------------------------------- handle most of the key */
   while (len >= 12)
   {
      a += (k[0] +((ub4)k[1]<<8) +((ub4)k[2]<<16) +((ub4)k[3]<<24));
      b += (k[4] +((ub4)k[5]<<8) +((ub4)k[6]<<16) +((ub4)k[7]<<24));
      c += (k[8] +((ub4)k[9]<<8) +((ub4)k[10]<<16)+((ub4)k[11]<<24));
      mix(a,b,c);
      k += 12; len -= 12;
   }

   /*------------------------------------- handle the last 11 bytes */
   c += length;
   switch(len)              /* all the case statements fall through */
   {
   case 11: c+=((ub4)k[10]<<24);
   case 10: c+=((ub4)k[9]<<16);
   case 9 : c+=((ub4)k[8]<<8);
      /* the first byte of c is reserved for the length */
   case 8 : b+=((ub4)k[7]<<24);
   case 7 : b+=((ub4)k[6]<<16);
   case 6 : b+=((ub4)k[5]<<8);
   case 5 : b+=k[4];
   case 4 : a+=((ub4)k[3]<<24);
   case 3 : a+=((ub4)k[2]<<16);
   case 2 : a+=((ub4)k[1]<<8);
   case 1 : a+=k[0];
     /* case 0: nothing left to add */
   }
   mix(a,b,c);
   /*-------------------------------------------- report the result */
   return c;
}

ub4 hash(const ub1 *k, ub4 length, ub4 initval){
    return _hash(k, length, initval);
}

/*============================================================================*/

/** Get the bucket for a hashcode in a hash table.
 *
 * @param table to get bucket from
 * @param hashcode to get bucket for
 * @return bucket
 */
inline HTBucket * get_bucket(HashTable *table, Hashcode hashcode){
    return table->buckets + (hashcode % table->buckets_n);
}

/** Initialize a hash table.
 *
 * @param table to initialize
 */
static void HashTable_init(HashTable *table){
    int i;

    for(i = 0; i < table->buckets_n; i++){
        HTBucket *bucket = get_bucket(table, i);
        bucket->head = NULL;
        bucket->count = 0;
    }
    table->entry_count = 0;
}

/** Allocate a new hashtable.
 * If the number of buckets is not positive the default is used.
 *
 * @param buckets_n number of buckets
 * @return new hashtable or null
 */
HashTable *HashTable_new(int buckets_n){
    HashTable *z = ALLOCATE(HashTable);
    if(!z) goto exit;
    if(buckets_n <= 0){
        buckets_n = HT_BUCKETS_N;
    }
    z->buckets = (HTBucket*)allocate(buckets_n * sizeof(HTBucket));
    if(!z->buckets){
        deallocate(z);
        z = NULL;
        goto exit;
    }
    z->buckets_n = buckets_n;
    HashTable_init(z);
  exit:
    return z;
}

/** Free a hashtable.
 * Any entries are removed and freed.
 *
 * @param h hashtable (ignored if null)
 */
void HashTable_free(HashTable *h){
    if(h){
        HashTable_clear(h);
        deallocate(h->buckets);
        deallocate(h);
    }
}

/** Push an entry on the list in the bucket for a given hashcode.
 *
 * @param table to add entry to
 * @param hashcode for the entry
 * @param entry to add
 */
static inline void push_on_bucket(HashTable *table, Hashcode hashcode,
				  HTEntry *entry){
    HTBucket *bucket;
    HTEntry *old_head;

    bucket = get_bucket(table, hashcode);
    old_head = bucket->head;
    bucket->count++;
    bucket->head = entry;
    entry->next = old_head;
}

/** Change the number of buckets in a hashtable.
 * No-op if the number of buckets is not positive.
 * Existing entries are reallocated to buckets based on their hashcodes.
 * The table is unmodified if the number of buckets cannot be changed.
 *
 * @param table hashtable
 * @param buckets_n new number of buckets
 * @return 0 on success, error code otherwise
 */
int HashTable_set_buckets_n(HashTable *table, int buckets_n){
    int err = 0;
    HTBucket *old_buckets = table->buckets;
    int old_buckets_n = table->buckets_n;
    int i;

    if(buckets_n <= 0){
        err = -EINVAL;
        goto exit;
    }
    table->buckets = (HTBucket*)allocate(buckets_n * sizeof(HTBucket));
    if(!table->buckets){
        err = -ENOMEM;
        table->buckets = old_buckets;
        goto exit;
    }
    table->buckets_n = buckets_n;
    for(i=0; i < old_buckets_n; i++){
        HTBucket *bucket = old_buckets + i;
        HTEntry *entry, *next;
        for(entry = bucket->head; entry; entry = next){
            next = entry->next;
            push_on_bucket(table, entry->hashcode, entry);
        }
    }
    deallocate(old_buckets);
  exit:
    return err;
}

/** Adjust the number of buckets so the table is neither too full nor too empty.
 * The table is unmodified if adjusting fails.
 *
 * @param table hash table
 * @param buckets_min minimum number of buckets (use default if 0 or negative)
 * @return 0 on success, error code otherwise
 */
int HashTable_adjust(HashTable *table, int buckets_min){
    int buckets_n = 0;
    int err = 0;
    if(buckets_min <= 0) buckets_min = HT_BUCKETS_N;
    if(table->entry_count >= table->buckets_n){
        // The table is dense - expand it.
        buckets_n = 2 * table->buckets_n;
    } else if((table->buckets_n > buckets_min) &&
              (4 * table->entry_count < table->buckets_n)){
        // The table is more than minimum size and sparse - shrink it.
        buckets_n = 2 * table->entry_count;
        if(buckets_n < buckets_min) buckets_n = buckets_min;
    }
    if(buckets_n){
        err = HashTable_set_buckets_n(table, buckets_n);
    }
    return err;
}

/** Allocate a new entry for a given value.
 *
 * @param value to put in the entry
 * @return entry, or 0 on failure
 */
HTEntry * HTEntry_new(Hashcode hashcode, void *key, void *value){
    HTEntry *z = ALLOCATE(HTEntry);
    if(z){
        z->hashcode = hashcode;
        z->key = key;
        z->value = value;
    }
    return z;
}

/** Free an entry.
 *
 * @param z entry to free
 */
inline void HTEntry_free(HTEntry *z){
    if(z){
        deallocate(z);
    }
}

/** Free an entry in a hashtable.
 * The table's entry_free_fn is used is defined, otherwise 
 * the HTEntry itself is freed.
 *
 * @param table hashtable
 * @param entry to free
 */
inline void HashTable_free_entry(HashTable *table, HTEntry *entry){
    if(!entry) return;
    if(table && table->entry_free_fn){
        table->entry_free_fn(table, entry);
    } else {
        HTEntry_free(entry);
    }
}

/** Get the first entry satisfying a test from the bucket for the
 * given hashcode.
 *
 * @param table to look in
 * @param hashcode indicates the bucket
 * @param test_fn test to apply to elements
 * @param arg first argument to calls to test_fn
 * @return entry found, or 0
 */
inline HTEntry * HashTable_find_entry(HashTable *table, Hashcode hashcode,
				      TableTestFn *test_fn, TableArg arg){
    HTBucket *bucket;
    HTEntry *entry = NULL;
    HTEntry *next;

    bucket = get_bucket(table, hashcode);
    for(entry = bucket->head; entry; entry = next){
        next = entry->next;
        if(test_fn(arg, table, entry)){
            break;
        }
    }
    return entry;
}

/** Test hashtable keys for equality.
 * Uses the table's key_equal_fn if defined, otherwise pointer equality.
 *
 * @param key1 key to compare
 * @param key2 key to compare
 * @return 1 if equal, 0 otherwise
 */
inline int HashTable_key_equal(HashTable *table, void *key1, void *key2){
    if(table->key_size){
        return memcmp(key1, key2, table->key_size) == 0;
    }
    return (table->key_equal_fn ? table->key_equal_fn(key1, key2) : key1 == key2);
}

/** Compute the hashcode of a hashtable key.
 * The table's key_hash_fn is used if defined, otherwise the address of
 * the key is hashed.
 *
 * @param table hashtable
 * @param key to hash
 * @return hashcode
 */
inline Hashcode HashTable_key_hash(HashTable *table, void *key){
    if(table->key_size){
        return _hash(key, table->key_size, 0);
    }
    return (table->key_hash_fn 
            ? table->key_hash_fn(key)
            : hash_hvoid(0, &key, sizeof(key)));
}

/** Test if an entry has a given key.
 *
 * @param arg containing key to test for
 * @param table the entry is in
 * @param entry to test
 * @return 1 if the entry has the key, 0 otherwise
 */
static inline int has_key(TableArg arg, HashTable *table, HTEntry *entry){
    return HashTable_key_equal(table, arg.ptr, entry->key);
}

/** Get an entry with a given key.
 *
 * @param table to search
 * @param key to look for
 * @return entry if found, null otherwise
 */
inline HTEntry * HashTable_get_entry(HashTable *table, void *key){
    Hashcode hashcode;
    HTBucket *bucket;
    HTEntry *entry = NULL;
    HTEntry *next;

    hashcode = HashTable_key_hash(table, key);
    bucket = get_bucket(table, hashcode);
    for(entry = bucket->head; entry; entry = next){
        next = entry->next;
        if(HashTable_key_equal(table, key, entry->key)){
            break;
        }
    }
    return entry;
}

/** Get the value of an entry with a given key.
 *
 * @param table to search
 * @param key to look for
 * @return value if an entry was found, null otherwise
 */
inline void * HashTable_get(HashTable *table, void *key){
    HTEntry *entry = HashTable_get_entry(table, key);
    return (entry ? entry->value : 0);
}

/** Print the buckets in a table.
 *
 * @param table to print
 */
void show_buckets(HashTable *table, IOStream *io){
    int i,j ;
    IOStream_print(io, "entry_count=%d buckets_n=%d\n", table->entry_count, table->buckets_n);
    for(i=0; i < table->buckets_n; i++){
        if(0 || table->buckets[i].count>0){
            IOStream_print(io, "bucket %3d %3d %10p ", i,
                        table->buckets[i].count,
                        table->buckets[i].head);
            for(j = table->buckets[i].count; j>0; j--){
                IOStream_print(io, "+");
            }
            IOStream_print(io, "\n");
        }
    }
    HashTable_print(table, io); 
}
    
/** Print an entry in a table.
 *
 * @param entry to print
 * @param arg a pointer to an IOStream to print to
 * @return 0
 */
static int print_entry(TableArg arg, HashTable *table, HTEntry *entry){
    IOStream *io = (IOStream*)arg.ptr;
    IOStream_print(io, " b=%4lx h=%08lx |-> e=%8p k=%8p v=%8p\n",
                entry->hashcode % table->buckets_n,
                entry->hashcode,
                entry, entry->key, entry->value);
    return 0;
}

/** Print a hash table.
 *
 * @param table to print
 */
void HashTable_print(HashTable *table, IOStream *io){
    IOStream_print(io, "{\n");
    HashTable_map(table, print_entry, (TableArg){ ptr: io });
    IOStream_print(io, "}\n");
}
/*==========================================================================*/

/** Add an entry to the bucket for the
 * given hashcode.
 *
 * @param table to insert in
 * @param hashcode indicates the bucket
 * @param key to add an entry for
 * @param value to add an entry for
 * @return entry on success, 0 on failure
 */
inline HTEntry * HashTable_add_entry(HashTable *table, Hashcode hashcode, void *key, void *value){
    HTEntry *entry = HTEntry_new(hashcode, key, value);
    if(entry){
        push_on_bucket(table, hashcode, entry);
        table->entry_count++;
    }
    return entry;
}

/** Move the front entry for a bucket to the correct point in the bucket order as
 * defined by the order function. If this is called every time a new entry is added
 * the bucket will be maintained in sorted order.
 *
 * @param table to modify
 * @param hashcode indicates the bucket
 * @param order entry comparison function
 * @return 0 if an entry was moved, 1 if not
 */
int HashTable_order_bucket(HashTable *table, Hashcode hashcode, TableOrderFn *order){
    HTEntry *new_entry = NULL, *prev = NULL, *entry = NULL;
    HTBucket *bucket;
    int err = 1;

    bucket = get_bucket(table, hashcode);
    new_entry = bucket->head;
    if(!new_entry || !new_entry->next) goto exit;
    for(entry = new_entry->next; entry; prev = entry, entry = entry->next){
        if(order(new_entry, entry) <= 0) break;
    }
    if(prev){
        err = 0;
        bucket->head = new_entry->next; 
        new_entry->next = entry;
        prev->next = new_entry;
    }
  exit:
    return err;
}

/** Add an entry to a hashtable.
 * The entry is added to the bucket for its key's hashcode.
 *
 * @param table to insert in
 * @param key to add an entry for
 * @param value to add an entry for
 * @return entry on success, 0 on failure
 */
inline HTEntry * HashTable_add(HashTable *table, void *key, void *value){
    return HashTable_add_entry(table, HashTable_key_hash(table, key), key, value);
}

/** Remove entries satisfying a test from the bucket for the
 * given hashcode. 
 *
 * @param table to remove from
 * @param hashcode indicates the bucket
 * @param test_fn test to apply to elements
 * @param arg first argument to calls to test_fn
 * @return number of entries removed
 */
inline int HashTable_remove_entry(HashTable *table, Hashcode hashcode,
				  TableTestFn *test_fn, TableArg arg){
    HTBucket *bucket;
    HTEntry *entry, *prev = NULL, *next;
    int removed_count = 0;

    bucket = get_bucket(table, hashcode);
    for(entry = bucket->head; entry; entry = next){
        next = entry->next;
        if(test_fn(arg, table, entry)){
            if(prev){
                prev->next = next;
            } else {
                bucket->head = next;
            }
            bucket->count--;
            table->entry_count--;
            removed_count++;
            HashTable_free_entry(table, entry);
            entry = NULL;
        }
        prev = entry;
    }
    return removed_count;
}

/** Remove entries with a given key. 
 *
 * @param table to remove from
 * @param key of entries to remove
 * @return number of entries removed
 */
inline int HashTable_remove(HashTable *table, void *key){
    Hashcode hashcode;
    HTBucket *bucket;
    HTEntry *entry, *prev = NULL, *next;
    int removed_count = 0;

    hashcode = HashTable_key_hash(table, key);
    bucket = get_bucket(table, hashcode);
    for(entry = bucket->head; entry; entry = next){
        next = entry->next;
        if(HashTable_key_equal(table, key, entry->key)){
            if(prev){
                prev->next = next;
            } else {
                bucket->head = next;
            }
            bucket->count--;
            table->entry_count--;
            removed_count++;
            HashTable_free_entry(table, entry);
            entry = NULL;
        }
        prev = entry;
    }
    return removed_count;
}

/** Remove (and free) all the entries in a bucket.
 *
 * @param bucket to clear
 */
static inline void bucket_clear(HashTable *table, HTBucket *bucket){
    HTEntry *entry, *next;

    for(entry = bucket->head; entry; entry = next){
        next = entry->next;
        HashTable_free_entry(table, entry);
    }
    bucket->head = NULL;
    table->entry_count -= bucket->count;
    bucket->count = 0;
}

/** Remove (and free) all the entries in a table.
 *
 * @param table to clear
 */
void HashTable_clear(HashTable *table){
    int i, n = table->buckets_n;

    for(i = 0; i < n; i++){
        bucket_clear(table, table->buckets + i);
    }
}
