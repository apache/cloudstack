XEN_ROOT = ../..
include $(XEN_ROOT)/tools/Rules.mk

CFLAGS  += $(CFLAGS_libxenctrl)
LDFLAGS += $(LDFLAGS_libxenctrl)

SUBDIRS-y :=
SUBDIRS-y += include
SUBDIRS-y += lvm
SUBDIRS-y += vhd
SUBDIRS-y += drivers

clean:
	rm -rf *.a *.so *.o *.rpm $(LIB) *~ $(DEPS) TAGS

.PHONY: all clean install
all clean install: %: subdirs-%
