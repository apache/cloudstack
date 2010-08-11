/*
 * Copyright (C) 2004, 2005 Mike Wray <mike.wray@hp.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the 
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free software Foundation, Inc.,
 * 59 Temple Place, suite 330, Boston, MA 02111-1307 USA
 *
 */
#include <linux/config.h>
#include <linux/module.h>

#include <linux/types.h>
#include <linux/kernel.h>
#include <linux/errno.h>

#include <asm/uaccess.h>

#include <linux/slab.h>

#include <linux/proc_fs.h>
#include <linux/string.h>

#include <linux/net.h>
#include <linux/in.h>
#include <linux/inet.h>
#include <linux/netdevice.h>

#include <sa.h>
#include "vif.h"
#include "vnet.h"
#include "varp.h"
#include "vnet_dev.h"
#include "vnet_eval.h"
#include "vnet_forward.h"

#include "iostream.h"
#include "kernel_stream.h"
#include "mem_stream.h"
#include "sys_string.h"
#include "sys_net.h"
#include "sxpr_parser.h"

#define MODULE_NAME "VNET"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

/** @file
 *
 * Kernel interface to files in /proc.
 * todo: Add a sysfs interface using kobject.
 */

#define PROC_ROOT "/proc/"
#define PROC_ROOT_LEN 6
#define MODULE_ROOT PROC_ROOT "vnet"

enum {
    VNET_POLICY = 1,
    VNET_VNETS,
    VNET_VIFS,
    VNET_VARP,
    VNET_PEERS,
};

typedef struct proc_dir_entry ProcEntry;
typedef struct inode Inode;
typedef struct file File;

static int proc_open_fn(struct inode *inode, File *file);
//static ssize_t proc_read_fn(File *file, char *buffer, size_t count, loff_t *offset);
//static ssize_t proc_write_fn(File *file, const char *buffer, size_t count, loff_t *offset) ;
//static int proc_flush_fn(File *file);
static loff_t proc_lseek_fn(File * file, loff_t offset, int orig);
static int proc_ioctl_fn(struct inode *inode, File *file, unsigned opcode, unsigned long arg);
//static int proc_release_fn(struct inode *inode, File *file);

static int ProcEntry_has_name(ProcEntry *entry, const char *name, int namelen){
    dprintf("> name=%.*s entry=%.*s\n", namelen, name, entry->namelen, entry->name);
    if(!entry || !entry->low_ino) return FALSE;
    if(entry->namelen != namelen) return FALSE;
    return memcmp(name, entry->name, namelen) == 0;
}

// Set f->f_error on error?
// Does interface stop r/w on first error?
// Is release called after an error?
//

static int proc_get_parser(File *file, Parser **val){
    int err = 0;
    Parser *parser = NULL;
    parser = file->private_data;
    if(!parser){
        parser = Parser_new();
        if(!parser){
            err = -ENOMEM;
            goto exit;
        }
        file->private_data = parser;
    }
  exit:
    *val = parser;
    return err;
}

static int proc_open_fn(Inode *inode, File *file){
    // User open.
    // Return errcode or 0 on success.
    // Can stuff data in file->private_data (void*).
    // Get entry from
    //ProcEntry *entry = (ProcEntry *)inode->u.generic_ip;
    //file->private_data = NULL;
    //file->f_dentry->d_ino is inode.
    // Check for user privilege - deny otherwise.
    // -EACCESS
    int err = 0;
    dprintf(">\n");
    file->private_data = NULL;
    return err;
}

static ssize_t proc_read_fn(File *file, char *buffer,
                            size_t count, loff_t *offset){
    // User read.
    // Copy data to user buffer, increment offset by count, return count.
    dprintf(">\n");
    count = 0;
    //if(copy_to_user(buffer, data, count)){
    //    return -EFAULT;
    //}
    //*offset += count;
    return count;
}

#if 0
static ssize_t proc_write_fn(File *file, const char *buffer,
                             size_t count, loff_t *offset) {
    return -EINVAL;
}
#endif


#if 0
static int proc_flush_fn(File *file){
    // User flush.
    int writing = (file->f_flags & O_ACCMODE) == O_WRONLY;
    int f_count = atomic_read(&file->f_count);
    if (writing && f_count == 1) {
        ProcEntry *pentry = (ProcEntry *)file->f_dentry->d_inode->u.generic_ip;
        // ...
    }
  return retval;
}
#endif

#ifndef SEEK_SET
enum {
    /** Offset from start. */
    SEEK_SET = 0,
    /** Offset from current position. */
    SEEK_CUR = 1,
    /** Offset from size of file. */
    SEEK_END = 2
};
#endif /* !SEEK_SET */

static loff_t proc_lseek_fn(File * file, loff_t offset, int from){
    // User lseek.
    dprintf(">\n");
    switch(from){
    case SEEK_SET:
        break;
    case SEEK_CUR:
	offset += file->f_pos;
        break;
    case SEEK_END:
	return -EINVAL;
    default:
	return -EINVAL;
    }
    if(offset < 0) return -EINVAL;    
    file->f_pos = offset;
    return offset;
}

static int proc_ioctl_fn(Inode *inode, File *file,
                         unsigned opcode, unsigned long arg){
    // User ioctl.
    dprintf(">\n");
    return 0;
}

static ssize_t proc_policy_write_fn(File *file, const char *buffer,
                             size_t count, loff_t *offset) {
    // User write.
    // Copy data into kernel space from buffer.
    // Increment offset by count, return count (or code).
    int err = 0;
    char *data = NULL;
    Parser *parser = NULL;

    err = proc_get_parser(file, &parser);
    if(err) goto exit;
    data = allocate(count);
    if(!data){
        err = -ENOMEM;
        goto exit;
    }
    err = copy_from_user(data, buffer, count);
    if(err) goto exit;
    *offset += count;
    err = Parser_input(parser, data, count);
  exit:
    deallocate(data);
    err = (err < 0 ? err : count);
    return err;
}

static int proc_policy_release_fn(Inode *inode, File *file){
    // User close.
    // Cleanup file->private_data, return errcode.
    int err = 0;
    Parser *parser = NULL;
    Sxpr obj, l;

    dprintf(">\n");
    err = proc_get_parser(file, &parser);
    if(err) goto exit;
    err = Parser_input(parser, NULL, 0);
    if(err) goto exit;
    obj = parser->val;
    for(l = obj; CONSP(l); l = CDR(l)){
        err = vnet_eval(CAR(l), iostdout, NULL);
        if(err) break;
    }
  exit:
    Parser_free(parser);
    file->private_data = NULL;
    dprintf("< err=%d\n", err);
    return err;
}

static int proc_io_open(Inode *inode, File *file, IOStream **val){
    int err = 0;
    IOStream *io = mem_stream_new();
    if(!io){
        err = -ENOMEM;
        goto exit;
    }
    file->private_data = io;
  exit:
    *val = (err ? NULL: io);
    return err;
}

static ssize_t proc_io_read_fn(File *file, char *buffer,
                               size_t count, loff_t *offset){
    // User read.
    // Copy data to user buffer, increment offset by count, return count.
    int err = 0;
    char kbuf[1024] = {};
    int kbuf_n = sizeof(kbuf);
    int k, n = 0;
    char *ubuf = buffer;
    IOStream *io = file->private_data;

    dprintf(">\n");
    if(!io) goto exit;
    while(n < count){
        k = count - n;
        if(k > kbuf_n){
            k = kbuf_n;
        }
        k = IOStream_read(io, kbuf, k);
        if(k <= 0) break;
        if(copy_to_user(ubuf, kbuf, k)){
            err = -EFAULT;
            goto exit;
        }
        n += k;
        ubuf += k;
    }
    *offset += n;
  exit:
    return (err ? err : n);
}

static int proc_io_release_fn(Inode *inode, File *file){
    // User close.
    int err = 0;
    IOStream *io = file->private_data;
    if(io) IOStream_close(io);
    dprintf("< err=%d\n", err);
    return err;
}

static int proc_vnets_open_fn(Inode *inode, File *file){
    int err = 0;
    IOStream *io;
    if(proc_io_open(inode, file, &io)) goto exit;
    vnet_print(io);
  exit:
    return err;
}

static int proc_vifs_open_fn(Inode *inode, File *file){
    int err = 0;
    IOStream *io;
    if(proc_io_open(inode, file, &io)) goto exit;
    vif_print(io);
  exit:
    return err;
}

static int proc_peers_open_fn(Inode *inode, File *file){
    int err = 0;
    IOStream *io;
    if(proc_io_open(inode, file, &io)) goto exit;
    vnet_peer_print(io);
  exit:
    return err;
}

static int proc_varp_open_fn(Inode *inode, File *file){
    int err = 0;
    IOStream *io;
    if(proc_io_open(inode, file, &io)) goto exit;
    varp_print(io);
  exit:
    return err;
}

static struct file_operations proc_policy_ops = {
    open:    proc_open_fn,
    read:    proc_read_fn,
    write:   proc_policy_write_fn,
    //flush:   proc_flush_fn,
    llseek:  proc_lseek_fn,
    ioctl:   proc_ioctl_fn,
    release: proc_policy_release_fn,
};

static struct file_operations proc_vnets_ops = {
    open:    proc_vnets_open_fn,
    read:    proc_io_read_fn,
    release: proc_io_release_fn,
};

static struct file_operations proc_vifs_ops = {
    open:    proc_vifs_open_fn,
    read:    proc_io_read_fn,
    release: proc_io_release_fn,
};

static struct file_operations proc_peers_ops = {
    open:    proc_peers_open_fn,
    read:    proc_io_read_fn,
    release: proc_io_release_fn,
};

static struct file_operations proc_varp_ops = {
    open:    proc_varp_open_fn,
    read:    proc_io_read_fn,
    release: proc_io_release_fn,
};

static ProcEntry *proc_fs_root = &proc_root;

static int proc_path_init(const char *path, const char **rest){
    int err = 0;

    if(!path){
        err = -EINVAL;
        goto exit;
    }
    if(*path == '/'){
        if(strncmp(PROC_ROOT, path, PROC_ROOT_LEN)){
            err = -EINVAL;
        } else {
            path += PROC_ROOT_LEN;
        }
    }
  exit:
    *rest = path;
    return err;
}

/** Parse a path relative to `dir'. If dir is null or the proc root
 * the path is relative to "/proc/", and the leading "/proc/" may be
 * supplied.
 *
 */
static ProcEntry * ProcFS_lookup(const char *path, ProcEntry *dir){
    const char *pathptr = path, *next = NULL;
    ProcEntry *entry, *result = NULL;
    int pathlen;

    if(dir && (dir != proc_fs_root)){
        entry = dir;
    } else {
        if(proc_path_init(path, &pathptr)) goto exit;
        entry = proc_fs_root;
    }
    if(!pathptr || !*pathptr) goto exit;
    while(1){
        next = strchr(pathptr, '/');
        pathlen = (next ? next - pathptr : strlen(pathptr));
        for(entry = entry->subdir; entry ; entry = entry->next) {
            if(ProcEntry_has_name(entry, pathptr, pathlen)) break;
        }
        if (!entry) break;
        if(!next){
            result = entry;
            break;
        }
        pathptr = next + 1;
    }
  exit:
    return result;
}

static ProcEntry *ProcFS_register(const char *name, ProcEntry *dir,
                                  int val, struct file_operations *ops){
    mode_t mode = 0;
    ProcEntry *entry;

    entry = create_proc_entry(name, mode, dir);
    if(entry){
        entry->proc_fops = ops;
        entry->data = (void*)val; // Whatever data we need.
    }
    return entry;
}

static ProcEntry *ProcFS_mkdir(const char *name, ProcEntry *parent){
    ProcEntry *entry = NULL;
    entry = ProcFS_lookup(name, parent);
    if(!entry){
        const char *path;
        if(proc_path_init(name, &path)) goto exit;
        entry = proc_mkdir(path, parent);
    }
  exit:
    return entry;
}

static void ProcFS_remove(const char *name, ProcEntry *parent){
    remove_proc_entry(name, parent);
}

static void ProcFS_rmrec_entry(ProcEntry *entry){
    if(entry){
        // Don't want to remove /proc itself!
        if(entry->parent == entry) return;
        while(entry->subdir){
            ProcFS_rmrec_entry(entry->subdir);
        }
        dprintf("> remove %s\n", entry->name);
        ProcFS_remove(entry->name, entry->parent);
    }
}

static void ProcFS_rmrec(const char *name, ProcEntry *parent){
    ProcEntry *entry;

    dprintf("> name=%s\n", name);
    entry = ProcFS_lookup(name, parent);
    if(entry){
        ProcFS_rmrec_entry(entry);
    }
    dprintf("<\n");
}

void __init ProcFS_init(void){
    ProcEntry *root_entry;
    ProcEntry *policy_entry;
    ProcEntry *vnets_entry;
    ProcEntry *vifs_entry;
    ProcEntry *peers_entry;
    ProcEntry *varp_entry;

    dprintf(">\n");
    root_entry = ProcFS_mkdir(MODULE_ROOT, NULL);
    if(!root_entry) goto exit;
    policy_entry = ProcFS_register("policy", root_entry, VNET_POLICY, &proc_policy_ops);
    vnets_entry = ProcFS_register("vnets", root_entry, VNET_VNETS, &proc_vnets_ops);
    vifs_entry = ProcFS_register("vifs", root_entry, VNET_VIFS, &proc_vifs_ops);
    peers_entry = ProcFS_register("peers", root_entry, VNET_PEERS, &proc_peers_ops);
    varp_entry = ProcFS_register("varp", root_entry, VNET_VARP, &proc_varp_ops);
  exit:
    dprintf("<\n");
}

void __exit ProcFS_exit(void){
    dprintf(">\n");
    ProcFS_rmrec(MODULE_ROOT, NULL);
    dprintf("<\n");
}
