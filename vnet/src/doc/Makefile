#!/usr/bin/make -f
# -*- mode: Makefile; -*-
XEN_ROOT        = ../../..
include $(XEN_ROOT)/tools/Rules.mk

VERSION = 1.0
HEADER  = Vnet

PS2PDF		:= ps2pdf
DVIPS		:= dvips
LATEX		:= latex
LATEX2HTML	:= latex2html
DOXYGEN		:= doxygen
POD2MAN		:= pod2man

DOC_MAN5SRC	:= $(wildcard man/*.pod.5)
DOC_MAN1SRC	:= $(wildcard man/*.pod.1)
DOC_MAN1	:= $(patsubst man/%.pod.1,man1/%.1,$(DOC_MAN1SRC))
DOC_MAN5	:= $(patsubst man/%.pod.5,man5/%.5,$(DOC_MAN5SRC))

.PHONY: all man clean install

.PHONY: all
all: man

.PHONY: man
man:
	@if which $(POD2MAN) 1>/dev/null 2>/dev/null; then \
	$(MAKE) $(DOC_MAN1) $(DOC_MAN5); fi

man1/%.1: man/%.pod.1 Makefile
	$(INSTALL_DIR) $(@D)
	$(POD2MAN) --release=$(VERSION) --name=`echo $@ | sed 's/^man1.//'| \
		sed 's/.1//'` -s 1 -c $(HEADER) $< $@

man5/%.5: man/%.pod.5 Makefile
	$(INSTALL_DIR) $(@D)
	$(POD2MAN) --release=$(VERSION) --name=`echo $@ | sed 's/^man5.//'| \
		sed 's/.5//'` -s 5 -c $(HEADER) $< $@

.PHONY: clean
clean:
	@$(RM) -rf man5
	@$(RM) -rf man1

.PHONY:  install
 install: all
	$(INSTALL_DIR) $(DESTDIR)$(MANDIR)
	$(CP) -dR man1 $(DESTDIR)$(MANDIR)
	$(CP) -dR man5 $(DESTDIR)$(MANDIR)

