# -*- mode: Makefile; -*-
#----------------------------------------------------------------------------
# Copyright (C) 2004 Mike Wray <mike.wray@hp.com>.
#
# This library is free software; you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation; either version 2.1 of the
# License, or  (at your option) any later version. This library is 
# distributed in the  hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this library; if not, write to the Free Software Foundation,
# Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
#----------------------------------------------------------------------------

VNET_ROOT ?= $(shell cd .. && pwd)
include $(VNET_ROOT)/Make.env

.PHONY: all
all: vnetd

#----------------------------------------------------------------------------

# Comment out when outside xen.
#include $(XEN_ROOT)/tools/Rules.mk

INSTALL_PROG ?= $(INSTALL) -m0755 -p

INCLUDES += -I$(LIBXUTIL_DIR)
INCLUDES += -I$(VNET_MODULE_DIR)
INCLUDES += -I$(shell pwd)

#----------------------------------------------------------------------------
# GC.

INCLUDES += -I$(GC_INCLUDE)
#LIBS += -L$(GC_LIB_DIR)
#CPPFLAGS += -D USE_GC

# Sometimes linux/atomic.h is not #ifdef __KERNEL__.
CPPFLAGS += -D __ARCH_I386_ATOMIC__

#----------------------------------------------------------------------------
CFLAGS += -O3
CFLAGS += $(INCLUDES) $(LIBS)

LDFLAGS += $(LIBS)

# Dependencies. Gcc generates them for us.
CFLAGS += -Wp,-MD,.$(@F).d
PROG_DEP = .*.d

vpath %.c $(LIBXUTIL_DIR)
vpath %.c $(VNET_MODULE_DIR)

IPATHS:=$(INCLUDES:-I=)
vpath %.h $(IPATHS)

#----------------------------------------------------------------------------
VNETD_SRC:=
VNETD_SRC+= connection.c
VNETD_SRC+= select.c
VNETD_SRC+= timer.c
VNETD_SRC+= spinlock.c
VNETD_SRC+= skbuff.c
VNETD_SRC+= vnetd.c

VNETD_SRC+= skb_util.c
VNETD_SRC+= sxpr_util.c
VNETD_SRC+= timer_util.c
VNETD_SRC+= etherip.c
VNETD_SRC+= vnet.c
VNETD_SRC+= vnet_eval.c
VNETD_SRC+= vnet_forward.c
VNETD_SRC+= vif.c
VNETD_SRC+= tunnel.c
VNETD_SRC+= sa.c
VNETD_SRC+= varp.c

#----------------------------------------------------------------------------
LIB_SRC:=
LIB_SRC+= allocate.c
LIB_SRC+= enum.c
LIB_SRC+= file_stream.c
LIB_SRC+= hash_table.c
LIB_SRC+= iostream.c
LIB_SRC+= lexis.c
LIB_SRC+= socket_stream.c
LIB_SRC+= string_stream.c
LIB_SRC+= sxpr.c
LIB_SRC+= sxpr_parser.c
LIB_SRC+= sys_net.c
LIB_SRC+= sys_string.c
LIB_SRC+= util.c

VNETD_SRC+=$(LIB_SRC)

VNETD_OBJ := $(VNETD_SRC:.c=.o)

#VNETD_LIBS:= $(GC_LIB_SO)
#VNETD_LIBS:= -lgc
#VNETD_LIBS:= $(GC_LIB_A)

vnetd: $(VNETD_OBJ)
	$(CC) $(CFLAGS) -o $@ $^ $(VNETD_LIBS) -ldl -lpthread

.PHONY: install
install: vnetd
	$(INSTALL_DIR) $(DESTDIR)$(SBINDIR)
	$(INSTALL_PROG) vnetd $(DESTDIR)$(SBINDIR)

.PHONY: clean
clean:
	-@$(RM) *.a *.o *~
	-@$(RM) vnetd
	-@$(RM) $(PROG_DEP)

-include $(PROG_DEP)
