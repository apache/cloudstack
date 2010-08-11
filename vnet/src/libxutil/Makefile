ifndef VNET_ROOT
export VNET_ROOT = $(shell cd .. && pwd)
include $(VNET_ROOT)/Make.env
endif

include $(XEN_ROOT)/tools/Rules.mk

LIB_SRCS :=
LIB_SRCS += allocate.c
LIB_SRCS += enum.c
LIB_SRCS += file_stream.c
#LIB_SRCS += gzip_stream.c
LIB_SRCS += hash_table.c
LIB_SRCS += iostream.c
LIB_SRCS += lexis.c
LIB_SRCS += mem_stream.c
LIB_SRCS += string_stream.c
LIB_SRCS += sxpr.c
LIB_SRCS += sxpr_parser.c
LIB_SRCS += sys_net.c
LIB_SRCS += sys_string.c
LIB_SRCS += util.c

LIB_OBJS := $(LIB_SRCS:.c=.o)
PIC_OBJS := $(LIB_SRCS:.c=.opic)

CFLAGS   += -Werror -fno-strict-aliasing $(call cc-option,$(CC),-fgnu89-inline,)
CFLAGS   += -O3
#CFLAGS   += -g

# Get gcc to generate the dependencies for us.
CFLAGS   += -Wp,-MD,.$(@F).d
DEPS     = .*.d

MAJOR    := 3.0
MINOR    := 0
LIB      := libxutil.so 
LIB      += libxutil.so.$(MAJOR)
LIB      += libxutil.so.$(MAJOR).$(MINOR)
LIB      += libxutil.a

.PHONY: all
all: build

.PHONY: build
build: #check-for-zlib
	$(MAKE) $(LIB)

gzip_stream.o: check-for-zlib

libxutil.so: libxutil.so.$(MAJOR)
	ln -sf $^ $@

libxutil.so.$(MAJOR): libxutil.so.$(MAJOR).$(MINOR)
	ln -sf $^ $@

libxutil.so.$(MAJOR).$(MINOR): $(PIC_OBJS)
	$(CC) $(CFLAGS) -Wl,$(SONAME_LDFLAG) -Wl,libxutil.so.$(MAJOR) $(SHLIB_CFLAGS) -o $@ $^

libxutil.a: $(LIB_OBJS)
	$(AR) rc $@ $^

.PHONY: check-for-zlib
check-for-zlib:
	@if [ ! -e /usr/include/zlib.h ]; then \
	echo "***********************************************************"; \
	echo "ERROR: install zlib header files (http://www.gzip.org/zlib)"; \
	echo "***********************************************************"; \
	false; \
	fi

.PHONY: install
install: build
	$(INSTALL_DIR) $(DESTDIR)$(LIBDIR)
	$(INSTALL_PROG) libxutil.so.$(MAJOR).$(MINOR) $(DESTDIR)$(LIBDIR)
	$(INSTALL_DATA) libxutil.a $(DESTDIR)$(LIBDIR)
	ln -sf libxutil.so.$(MAJOR).$(MINOR) $(DESTDIR)$(LIBDIR)/libxutil.so.$(MAJOR)
	ln -sf libxutil.so.$(MAJOR) $(DESTDIR)$(LIBDIR)/libxutil.so

.PHONY: clean
clean:
	-@$(RM) *.a *.so* *.o *.opic *.rpm 
	-@$(RM) *~
	-@$(RM) $(DEPS)

-include $(DEPS)
