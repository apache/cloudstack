
/*
** emfloat.h
** Header for emfloat.c
**
** BYTEmark (tm)
** BYTE Magazine's Native Mode benchmarks
** Rick Grehan, BYTE Magazine
**
** Create:
** Revision: 3/95
**
** DISCLAIMER
** The source, executable, and documentation files that comprise
** the BYTEmark benchmarks are made available on an "as is" basis.
** This means that we at BYTE Magazine have made every reasonable
** effort to verify that the there are no errors in the source and
** executable code.  We cannot, however, guarantee that the programs
** are error-free.  Consequently, McGraw-HIll and BYTE Magazine make
** no claims in regard to the fitness of the source code, executable
** code, and documentation of the BYTEmark.
**  Furthermore, BYTE Magazine, McGraw-Hill, and all employees
** of McGraw-Hill cannot be held responsible for any damages resulting
** from the use of this code or the results obtained from using
** this code.
*/

#include <stdio.h>

/* Is this a 64 bit architecture? If so, this will define LONG64 */
/* Uwe F. Mayer 15 November 1997                                 */
#include "pointer.h"

/*
** DEFINES
*/
#define u8 unsigned char
#define u16 unsigned short
#ifdef LONG64
#define u32 unsigned int
#else
#define u32 unsigned long
#endif
#define uchar unsigned char
#define ulong unsigned long

#define MAX_EXP 32767L
#define MIN_EXP (-32767L)

#define IFPF_IS_ZERO 0
#define IFPF_IS_SUBNORMAL 1
#define IFPF_IS_NORMAL 2
#define IFPF_IS_INFINITY 3
#define IFPF_IS_NAN 4
#define IFPF_TYPE_COUNT 5

#define ZERO_ZERO                       0
#define ZERO_SUBNORMAL                  1
#define ZERO_NORMAL                     2
#define ZERO_INFINITY                   3
#define ZERO_NAN                        4

#define SUBNORMAL_ZERO                  5
#define SUBNORMAL_SUBNORMAL             6
#define SUBNORMAL_NORMAL                7
#define SUBNORMAL_INFINITY              8
#define SUBNORMAL_NAN                   9

#define NORMAL_ZERO                     10
#define NORMAL_SUBNORMAL                11
#define NORMAL_NORMAL                   12
#define NORMAL_INFINITY                 13
#define NORMAL_NAN                      14

#define INFINITY_ZERO                   15
#define INFINITY_SUBNORMAL              16
#define INFINITY_NORMAL                 17
#define INFINITY_INFINITY               18
#define INFINITY_NAN                    19

#define NAN_ZERO                        20
#define NAN_SUBNORMAL                   21
#define NAN_NORMAL                      22
#define NAN_INFINITY                    23
#define NAN_NAN                         24
#define OPERAND_ZERO                    0
#define OPERAND_SUBNORMAL               1
#define OPERAND_NORMAL                  2
#define OPERAND_INFINITY                3
#define OPERAND_NAN                     4

/*
** Following already defined in NMGLOBAL.H
**
#define INTERNAL_FPF_PRECISION 4
*/

/*
** TYPEDEFS
*/

typedef struct
{
        u8 type;        /* Indicates, NORMAL, SUBNORMAL, etc. */
        u8 sign;        /* Mantissa sign */
        short exp;      /* Signed exponent...no bias */
        u16 mantissa[INTERNAL_FPF_PRECISION];
} InternalFPF;

/*
** PROTOTYPES
*/
void SetupCPUEmFloatArrays(InternalFPF *abase,
        InternalFPF *bbase, InternalFPF *cbase, ulong arraysize);
ulong DoEmFloatIteration(InternalFPF *abase,
        InternalFPF *bbase, InternalFPF *cbase,
        ulong arraysize, ulong loops);
static void SetInternalFPFZero(InternalFPF *dest,
                        uchar sign);
static void SetInternalFPFInfinity(InternalFPF *dest,
                        uchar sign);
static void SetInternalFPFNaN(InternalFPF *dest);
static int IsMantissaZero(u16 *mant);
static void Add16Bits(u16 *carry,u16 *a,u16 b,u16 c);
static void Sub16Bits(u16 *borrow,u16 *a,u16 b,u16 c);
static void ShiftMantLeft1(u16 *carry,u16 *mantissa);
static void ShiftMantRight1(u16 *carry,u16 *mantissa);
static void StickyShiftRightMant(InternalFPF *ptr,int amount);
static void normalize(InternalFPF *ptr);
static void denormalize(InternalFPF *ptr,int minimum_exponent);
void RoundInternalFPF(InternalFPF *ptr);
static void choose_nan(InternalFPF *x,InternalFPF *y,InternalFPF *z,
                int intel_flag);
static void AddSubInternalFPF(uchar operation,InternalFPF *x,
                InternalFPF *y,InternalFPF *z);
static void MultiplyInternalFPF(InternalFPF *x,InternalFPF *y,
                        InternalFPF *z);
static void DivideInternalFPF(InternalFPF *x,InternalFPF *y, 
                        InternalFPF *z);
/* static void LongToInternalFPF(long mylong, */
static void Int32ToInternalFPF(int32 mylong,
                InternalFPF *dest);
#ifdef DEBUG
static int InternalFPFToString(char *dest,
                InternalFPF *src);
#endif

/*
** EXTERNALS
*/
extern ulong StartStopwatch();
extern ulong StopStopwatch(ulong elapsed);
/* extern long randwc(long num); */
extern int32 randwc(int32 num);
