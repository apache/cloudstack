// CRYPTO LIBRARY FOR EXCHANGING KEYS
// USING THE DIFFIE-HELLMAN KEY EXCHANGE PROTOCOL

// The diffie-hellman can be used to securely exchange keys
// between parties, where a third party eavesdropper given
// the values being transmitted cannot determine the key.

// Implemented by Lee Griffiths, Jan 2004.
// This software is freeware, you may use it to your discretion,
// however by doing so you take full responsibility for any damage
// it may cause.

// Hope you find it useful, even if you just use some of the functions
// out of it like the prime number generator and the XtoYmodN function.

// It would be great if you could send me emails to: lee.griffiths@first4internet.co.uk
// with any suggestions, comments, or questions!

// Enjoy.

// Adopted to ms-logon for ultravnc by marscha, 2006.

#include "dh.h"
#include <tchar.h>

class Except {
private:
	char *info;
public:
	Except(const char *info_ = NULL);
	virtual ~Except(){ if (info) delete [] info; };
};

Except::Except(const char *info_) {
	if (info_ != NULL) {
		info = new char[strlen(info_)+1];
		strcpy(info, info_);
	}
}

DH::DH() : maxNum(((unsigned __int64) 1) << DH_MAX_BITS) {
	srand((unsigned) time(NULL));
}

DH::DH(unsigned __int64 generator, unsigned __int64 modulus)
	: gen(generator), mod(modulus),
	  maxNum(((unsigned __int64) 1) << DH_MAX_BITS) {
	if (gen > maxNum || mod > maxNum)
		throw Except("Input exceeds maxNum");
	if (gen > mod)
		throw Except("Generator is larger than modulus");
	srand((unsigned) time(NULL));
}

DH::~DH() { cleanMem(); }

unsigned __int64 rng(unsigned __int64 limit) {
	return ((((unsigned __int64) rand()) * rand() * rand ()) % limit);
}

//Performs the miller-rabin primality test on a guessed prime n.
//trials is the number of attempts to verify this, because the function
//is not 100% accurate it may be a composite.  However setting the trial
//value to around 5 should guarantee success even with very large primes
bool DH::millerRabin (unsigned __int64 n, unsigned int trials) { 
	unsigned __int64 a = 0; 

	for (unsigned int i = 0; i < trials; i++) { 
		a = rng(n - 3) + 2;// gets random value in [2..n-1] 
		if (XpowYmodN(a, n - 1, n) != 1) return false; //n composite, return false 
	}
	return true; // n probably prime 
} 

//Generates a large prime number by
//choosing a randomly large integer, and ensuring the value is odd
//then uses the miller-rabin primality test on it to see if it is prime
//if not the value gets increased until it is prime
unsigned __int64 DH::generatePrime() {
	unsigned __int64 prime = 0;

	do {
		unsigned __int64 start = rng(maxNum);
		prime = tryToGeneratePrime(start);
	} while (!prime);
	return prime;
}
 
unsigned __int64 DH::tryToGeneratePrime(unsigned __int64 prime) {
	//ensure it is an odd number
	if ((prime & 1) == 0)
		prime += 1;

	unsigned __int64 cnt = 0;
	while (!millerRabin(prime, 25) && (cnt++ < DH_RANGE) && prime < maxNum) {
		prime += 2;
		if ((prime % 3) == 0) prime += 2;
	}
	return (cnt >= DH_RANGE || prime >= maxNum) ? 0 : prime;
}
 
//Raises X to the power Y in modulus N
//the values of X, Y, and N can be massive, and this can be 
//achieved by first calculating X to the power of 2 then 
//using power chaining over modulus N
unsigned __int64 DH::XpowYmodN(unsigned __int64 x, unsigned __int64 y, unsigned __int64 N) {
	unsigned __int64 result = 1;
	const unsigned __int64 oneShift63 = ((unsigned __int64) 1) << 63;
	
	for (int i = 0; i < 64; y <<= 1, i++){
		result = result * result % N;
		if (y & oneShift63)
			result = result * x % N;
	}
	return result;
}

void DH::createKeys() {
	gen = generatePrime();
	mod = generatePrime();

	if (gen > mod) {
		unsigned __int64 swap = gen;
		gen  = mod;
		mod  = swap;
	}
}

unsigned __int64 DH::createInterKey() {
	priv = rng(maxNum);
	return pub = XpowYmodN(gen,priv,mod);
}

unsigned __int64 DH::createEncryptionKey(unsigned __int64 interKey) {
	if (interKey >= maxNum)
		throw Except("interKey larger than maxNum");
	return key = XpowYmodN(interKey,priv,mod);
}

void DH::cleanMem(DWORD flags) { // marscha (TODO): SecureZeroMemory?
	gen  = 0;
	mod  = 0;
	priv = 0;
	pub  = 0;
	
	if (flags != DH_CLEAN_ALL_MEMORY_EXCEPT_KEY)
		key = 0;
}

unsigned __int64 DH::getValue(DWORD flags) {
	switch (flags) {
		case DH_MOD:
			return mod;
		case DH_GEN:
			return gen;
		case DH_PRIV:
			return priv;
		case DH_PUB:
			return pub;
		case DH_KEY:
			return key;
		default:
			return (unsigned __int64) 0;
	}
}

int bits(__int64 number){
	for (unsigned int i = 0; i < 64; i++){
		number /= 2;
		if (number < 2) return i;
	}
	return 0;
}

bool int64ToBytes(const unsigned __int64 integer, char* const bytes) {
	for (int i = 0; i < 8; i++) {
		bytes[i] = (unsigned char) (integer >> (8 * (7 - i)));
	}
	return true;
}

unsigned __int64 bytesToInt64(const char* const bytes) {
	unsigned __int64 result = 0;
	for (int i = 0; i < 8; i++) {
		result <<= 8;
		result += (unsigned char) bytes[i];
	}
	return result;
}

bool vncWc2Mb(char* multibyte, WCHAR* widechar, int length) {
	multibyte[0] = '\0';
	int origlen = wcslen(widechar);
	if (origlen > length)
		return false;
	int newlen = WideCharToMultiByte(
		CP_ACP,		// code page
		0,			// performance and mapping flags
		widechar,	// address of wide-character string
		origlen,	// number of characters in string
		multibyte,	// address of buffer for new string
		length,		// size of buffer
		NULL, NULL );

	if (newlen >= length)
		return false;
	multibyte[newlen]= '\0';
	return newlen ? true : false;
}
