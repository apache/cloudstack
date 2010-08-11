# -*- mode: Makefile; -*-
#============================================================================
XEN_ROOT        = ../../..
include $(XEN_ROOT)/tools/Rules.mk

.PHONY: all
all:

.PHONY: install
install:
	$(INSTALL_DIR) $(DESTDIR)$(SBINDIR)
	$(INSTALL_PROG) vn $(DESTDIR)$(SBINDIR)

.PHONY: clean
clean:
