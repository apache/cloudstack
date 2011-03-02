
LIBVHD-MAJOR     = 1.0
LIBVHD-MINOR     = 0
LIBVHD-SONAME    = libvhd.so.$(LIBVHD-MAJOR)

LVM-UTIL-OBJ    := ../../lvm/lvm-util.o

LIBVHD-BUILD    := libvhd.a

CFLAGS          += -m32
CFLAGS          += -Werror
CFLAGS          += -Wno-unused
CFLAGS          += -I../../include
CFLAGS          += -D_GNU_SOURCE
CFLAGS          += -fPIC
CFLAGS          += -g

ifeq ($(CONFIG_Linux),y)
LIBS            := -luuid
endif

# Get gcc to generate the dependencies for us.
CFLAGS          += -Wp,-MD,.$(@F).d
DEPS             = .*.d

LIB-SRCS        := libvhd.c
LIB-SRCS        += libvhd-journal.c
LIB-SRCS        += vhd-util-coalesce.c
LIB-SRCS        += vhd-util-create.c
LIB-SRCS        += vhd-util-fill.c
LIB-SRCS        += vhd-util-modify.c
LIB-SRCS        += vhd-util-query.c
LIB-SRCS        += vhd-util-read.c
LIB-SRCS        += vhd-util-repair.c
LIB-SRCS        += vhd-util-resize.c
LIB-SRCS        += vhd-util-revert.c
LIB-SRCS        += vhd-util-set-field.c
LIB-SRCS        += vhd-util-snapshot.c
LIB-SRCS        += vhd-util-scan.c
LIB-SRCS        += vhd-util-check.c
LIB-SRCS        += relative-path.c
LIB-SRCS        += atomicio.c

LIB-OBJS         = $(patsubst %.c,%.o,$(LIB-SRCS))
LIB-OBJS        += $(LVM-UTIL-OBJ)


all: build

build: $(LIBVHD-BUILD)

libvhd.a: $(LIB-OBJS)
	$(AR) rc $@ $^

clean:
	rm -rf *.a *.so* *.o *~ $(DEPS) $(LIBVHD)

.PHONY: all build clean install libvhd

-include $(DEPS)
