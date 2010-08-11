# -*- mode: Makefile; -*-
#============================================================================
#
# Copyright (C) 2004 Mike Wray <mike.wray@hp.com>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by the
# Free Software Foundation; either version 2 of the License, or (at your
# option) any later version.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
# or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
# for more details.
#
# You should have received a copy of the GNU General Public License along
# with this program; if not, write to the Free software Foundation, Inc.,
# 59 Temple Place, suite 330, Boston, MA 02111-1307 USA
#============================================================================

ifndef VNET_ROOT
export VNET_ROOT = $(shell cd .. && pwd)
include $(VNET_ROOT)/Make.env
endif

#============================================================================
ifeq ($(src),)

include Makefile-$(LINUX_SERIES)

#============================================================================
else
#============================================================================
# This section is for the 2.6 kbuild.

#$(warning KBUILD_EXTMOD $(KBUILD_EXTMOD))
#$(warning src $(src))
#$(warning obj $(obj))

include $(src)/Makefile.vnet

obj-m = vnet_module.o
vnet_module-objs = $(VNET_OBJ)
vnet_module-objs += $(VNET_LIB_OBJ)

#----------------------------------------------------------------------------
# The fancy stuff in the kernel build defeats 'vpath %.c' so we can't
# use that to get the lib files compiled.
# Setup explicit rules for them using the kbuild C compile rule.

# File names in the lib dir.
remote_srcs = $(foreach file,$(VNET_LIB_SRC),$(LIBXUTIL_DIR)/$(file))

# Equivalent file names here.
local_srcs = $(foreach file,$(VNET_LIB_SRC),$(src)/$(file))

# Objects for the local names.
local_objs = $(local_srcs:.c=.o)

# Make the local objects depend on compiling the remote sources.
$(local_objs): $(src)/%.o: $(LIBXUTIL_DIR)/%.c
	$(call if_changed_rule,cc_o_c)
#----------------------------------------------------------------------------

vpath %.h $(LIBXUTIL_DIR)
EXTRA_CFLAGS += -I $(LIBXUTIL_DIR)
EXTRA_CFLAGS += -I $(src)

endif
#============================================================================

