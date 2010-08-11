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

#ifndef _VNETD_SELECTOR_H_
#define _VNETD_SELECTOR_H_

#include "list.h"
#include "select.h"

struct Conn;

typedef struct Selector {

    /** List the selector is linked into (if any). */
    struct list_head list;

    /** Function called by Selector_select() to add a selector to a select set.
     * The selector is closed if this returns an error (non-zero).
     */
    int (*select)(struct Selector *sel, struct SelectSet *set);

    /** Function called by Selector_selected() to notify a selector of select set.
     * The selector is closed if this returns an error (non-zero).
     */
    int (*selected)(struct Selector *sel, struct SelectSet *set);

    /** Function called by Selector_close() to close a selector.
     */
    void (*close)(struct Selector *sel);

    /** User data. */
    void *data;

} Selector;

void Selector_init(struct Selector *sel);
void Selector_close(struct Selector *sel);
int Selector_select(struct Selector *sel, struct SelectSet *set);
int Selector_selected(struct Selector *sel, struct SelectSet *set);

int conn_select_fn(struct Selector *sel, struct SelectSet *set);
int conn_selected_fn(struct Selector *sel, struct SelectSet *set);
void conn_close_fn(struct Selector *sel);
void Selector_conn_init(struct Selector *sel, struct Conn *conn,
                        int mode, void *data,
                        int (*fn)(struct Conn *conn, int mode));

#endif /* _VNETD_SELECTOR_H_ */
