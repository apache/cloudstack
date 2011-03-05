XEN_ROOT=../../..
BLKTAP_ROOT := ..
include $(XEN_ROOT)/tools/Rules.mk

SUBDIRS-y         :=
SUBDIRS-y         += lib

IBIN               = vhd-util vhd-update
INST_DIR           = $(SBINDIR)

CFLAGS            += -Werror
CFLAGS            += -Wno-unused
CFLAGS            += -I../include
CFLAGS            += -D_GNU_SOURCE

ifeq ($(CONFIG_X86_64),y)
CFLAGS            += -fPIC
endif

ifeq ($(VHD_STATIC),y)
CFLAGS            += -static
endif

LIBS              := -Llib -lvhd
ifeq ($(CONFIG_Linux),y)
LIBS              += -luuid
endif

# Get gcc to generate the dependencies for us.
CFLAGS            += -Wp,-MD,.$(@F).d
DEPS               = .*.d

all: subdirs-all build

build: $(IBIN)

LIBS_DEPENDS	  := lib/libvhd.so lib/vhd.a
$(LIBS_DEPENDS):subdirs-all

vhd-util: vhd-util.o $(LIBS_DEPENDS)
	$(CC) $(CFLAGS) -o vhd-util vhd-util.o $(LDFLAGS) $(LIBS)

vhd-update: vhd-update.o $(LIBS_DEPENDS)
	$(CC) $(CFLAGS) -o vhd-update vhd-update.o $(LDFLAGS) $(LIBS)

install: all
	$(MAKE) subdirs-install
	$(INSTALL_DIR) -p $(DESTDIR)$(INST_DIR)
	$(INSTALL_PROG) $(IBIN) $(DESTDIR)$(INST_DIR)

clean: subdirs-clean
	rm -rf *.o *~ $(DEPS) $(IBIN)

.PHONY: all build clean install vhd-util vhd-update

-include $(DEPS)
