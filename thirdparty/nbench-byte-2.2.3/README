February 18, 2003
-----------------
Bug-fix release.

December 9, 1997
----------------
This release is based on beta release 2 of BYTE Magazine's BYTEmark
benchmark program (previously known as BYTE's Native Mode
Benchmarks). This document covers the Native Mode (a.k.a. Algorithm
Level) tests; benchmarks designed to expose the capabilities of a
system's CPU, FPU, and memory system.

Running a "make" will create the binary if all goes well. It is called
"nbench" and performs a suite of 10 tests and compares the results to
a Dell Pentium 90 with 16 MB RAM and 256 KB L2 cache running MSDOS and
compiling with the Watcom 10.0 C/C++ compiler. If you define -DLINUX
during compilation (the default) then you also get a comparison to an
AMD K6/233 with 32 MB RAM and 512 KB L2-cache running Linux 2.0.32 and
using a binary which was compiled with GNU gcc version 2.7.2.3 and GNU
libc-5.4.38.

For more verbose output specify -v as an argument.

The primary web site is: http://www.tux.org/~mayer/linux/bmark.html

The port to Linux/Unix was done by Uwe F. Mayer <mayer@tux.org>.

The index-split was done by Andrew D. Balsa, and reflects the
realization that memory management is important in CPU design. The
original tests have been left alone, however, the tests NUMERIC SORT,
FP EMULATION, IDEA, and HUFFMAN now constitute the integer-arithmetic
focused benchmark index, while the tests STRING SORT, BITFIELD, and
ASSIGNMENT make up the new memory index.

The algorithms were not changed from the source which was obtained
from the BYTE web site at http://www.byte.com/bmark/bmark.htm on
December 14, 1996.  However, the source was modified to better work
with 64-bit machines (in particular the random number generator was
modified to always work with 32 bit, no matter what kind of hardware
you run it on). Furthermore, for some of the algorithms additional
resettings of the data was added to increase the consistency across
different hardware. Some extra debugging code was added, which has no
impact on normal runs.

In case there is uneven system load due to other processes while this
benchmark suite executes, it might take longer to run than on an
unloaded system. This is because the benchmark does some statistical
analysis to make sure that the reported results are statistically
significant, and an increased variation in individual runs requires
more runs to achieve the required statistical confidence.

This is a single-threaded benchmark and is not designed to measure the
performance gain on multi-processor machines.

For details and customization read bdoc.txt.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
