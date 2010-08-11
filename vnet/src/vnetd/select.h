/*
 * Copyright (C) 2003 - 2004 Mike Wray <mike.wray@hp.com>.
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
#ifndef _VFC_SELECT_H_
#define _VFC_SELECT_H_

/** Set of file descriptors for select.
 */
typedef struct SelectSet {
    int n;
    fd_set rd, wr, er;
} SelectSet;

enum {
    SELECT_READ  = 1,
    SELECT_WRITE = 2,
    SELECT_ERROR = 4,
};

extern void SelectSet_zero(SelectSet *set);
extern void SelectSet_add(SelectSet *set, int fd, int mode);
extern void SelectSet_add_read(SelectSet *set, int fd);
extern void SelectSet_add_write(SelectSet *set, int fd);
extern void SelectSet_add_error(SelectSet *set, int fd);
extern int SelectSet_select(SelectSet *set, struct timeval *timeout);

static inline int SelectSet_in(SelectSet *set, int fd){
    return ((fd >= 0)
            ? ((FD_ISSET(fd, &set->rd) ? SELECT_READ : 0) |
               (FD_ISSET(fd, &set->wr) ? SELECT_WRITE : 0) |
               (FD_ISSET(fd, &set->er) ? SELECT_ERROR : 0))
            : 0);
}

static inline int SelectSet_in_read(SelectSet *set, int fd){
    return (fd >= 0) && FD_ISSET(fd, &set->rd);
}

static inline int SelectSet_in_write(SelectSet *set, int fd){
    return (fd >= 0) && FD_ISSET(fd, &set->wr);
}

static inline int SelectSet_in_err(SelectSet *set, int fd){
    return (fd >= 0) && FD_ISSET(fd, &set->er);
}

#endif /* ! _VFC_SELECT_H_ */
