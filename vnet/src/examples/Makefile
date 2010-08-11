# -*- mode: Makefile; -*-
#============================================================================
XEN_ROOT        = ../../..
include $(XEN_ROOT)/tools/Rules.mk

XEN_SCRIPT_DIR  = $(DESTDIR)/etc/xen/scripts

.PHONY: all
all:

.PHONY: install
install:
	$(INSTALL_DIR) $(XEN_SCRIPT_DIR)
	$(INSTALL_PROG) network-vnet $(XEN_SCRIPT_DIR)
	$(INSTALL_PROG) vnet-insert $(XEN_SCRIPT_DIR)

.PHONY: clean
clean:
