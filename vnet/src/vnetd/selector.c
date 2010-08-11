/*
 * Copyright (C) 2005 Mike Wray <mike.wray@hp.com>.
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
#include <stdlib.h>
#include <stdbool.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>

#include "connection.h"
#include "selector.h"

#define MODULE_NAME "select"
#define DEBUG 1
#undef DEBUG
#include "debug.h"

void Selector_init(Selector *sel){
    INIT_LIST_HEAD(&sel->list);
}

/** Close a selector and remove it from its list.
 *
 * @param sel selector (may be null)
 */
void Selector_close(Selector *sel){
    if(!sel) return;
    dprintf(">\n");
    if(sel->close){
        sel->close(sel);
    }
    if(sel->list.next
       && sel->list.next != LIST_POISON1
       && !list_empty(&sel->list)){
        list_del_init(&sel->list);
    }
}

/** Add a selector to a select set.
 * The selector is closed if it has no 'select' function,
 * or it has one and it returns an error.
 *
 * @param sel selector
 * @param set select set
 */
int Selector_select(Selector *sel, SelectSet *set){
    int err = -EINVAL;
    dprintf(">\n");
    if(sel->select){
        err = sel->select(sel, set);
    }
    if(err){
        Selector_close(sel);
    }
    return err;
}

/** Call a selector with a select set.
 * The selector is closed if it has no 'selected' function,
 * or it has one and it returns an error.
 *
 * @param sel selector
 * @param set select set
 */
int Selector_selected(Selector *sel, SelectSet *set){
    int err = -EINVAL;
    dprintf(">\n");
    if(sel->selected){
        err = sel->selected(sel, set);
    }
    if(err){
        Selector_close(sel);
    }
    return err;
}

int conn_select_fn(Selector *sel, SelectSet *set){
    int err = -EINVAL;
    Conn *conn = sel->data;

    dprintf(">\n");
    if(conn){
        err = 0;
        SelectSet_add(set, conn->sock, conn->mode);
    }
    return err;
}

int conn_selected_fn(Selector *sel, SelectSet *set){
    int err = -EINVAL;
    Conn *conn = sel->data;

    dprintf(">\n");
    if(conn){
        err = Conn_handle(conn, set);
    }
    return err;
}

void conn_close_fn(Selector *sel){
    Conn *conn = sel->data;
    
    wprintf("> sel=%p\n", sel);
    if(conn){
        Conn_close(conn);
    }
}

void Selector_conn_init(Selector *sel, Conn *conn,
                        int mode, void *data,
                        int (*fn)(struct Conn *conn, int mode)){
    conn->mode = SELECT_READ;
    conn->data = data;
    conn->fn = fn;
    sel->data = conn;
    sel->select = conn_select_fn;
    sel->close  = conn_close_fn;
    sel->selected = conn_selected_fn;
}

