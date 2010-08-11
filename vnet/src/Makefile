# -*- mode: Makefile; -*-

ifndef VNET_ROOT
export VNET_ROOT = $(shell pwd)
include $(VNET_ROOT)/Make.env
endif

SUBDIRS:=
SUBDIRS+= examples
SUBDIRS+= scripts
#SUBDIRS+= gc
SUBDIRS+= libxutil
SUBDIRS+= vnetd
SUBDIRS+= vnet-module

.PHONY: all
all: compile

gc.tar.gz:
	wget http://www.hpl.hp.com/personal/Hans_Boehm/gc/gc_source/$@

.PHONY: gc
gc: gc.tar.gz
	tar xfz gc.tar.gz
	ln -sf gc?.? gc

$(GC_LIB_A): gc
	(cd gc && ./configure --prefix=$(GC_DIR) )
	make -C gc
	DESTDIR="" make -C gc install

.PHONY: gc-all
gc-all: $(GC_LIB_A)

.PHONY: gc-install
gc-install:

.PHONY: gc-clean
gc-clean:
	-@$(RM) -r gc?.? gc

submak = $(MAKE) -C $(patsubst %-$(1),%,$(@)) $(1)
subtgt = $(patsubst %,%-$(1),$(SUBDIRS))

%-all:
	$(call submak,all)

%-clean:
	-$(call submak,clean)

%-install:
	$(call submak,install)

.PHONY: compile
compile: $(call subtgt,all)

.PHONY: install
install: DESTDIR=
install: dist

.PHONY: dist
dist: compile $(call subtgt,install)

.PHONY: clean
clean: $(call subtgt,clean)
	-@$(RM) -r build

.PHONY: pristine
pristine: clean
	-@$(RM) gc.tar.gz

.PHONY: help
help:
	@echo 'Cleaning targets:'
	@echo '  clean     - clean subdirs and remove the build dir'
	@echo '  pristine  - clean, then remove the gc tarball'
	@echo ''
	@echo 'Installation targets:'
	@echo '  install   - build and install relative to /'
	@echo '  dist      - build and install relative to DESTDIR (default XEN_ROOT/dist/install)'
	@echo ''
	@echo 'Compilation targets:'
	@echo '  all       - same as compile'
	@echo '  compile   - build everything'
	@echo ''
	@echo 'To build everything locally use "make" or "make all"'.
	@echo 'To build and install into XEN_ROOT/dist/install use "make dist".'
	@echo 'To build and install into the system use "make dist".'
	@echo 'See ./00README and ./00INSTALL for more information.'
