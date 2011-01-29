//
// Helpers.h
//

#pragma once

//
// includes.
//

#include <windows.h>

#pragma warning ( disable : 4786 )	// identifier was truncated to '255' characters in the debug information

#include <string>
#include <vector>
typedef std::basic_string< CHAR > charstring;
typedef std::vector< charstring > charstring_vector;
typedef std::basic_string< WCHAR > widestring;
typedef std::vector< widestring > widestring_vector;

//
// prototypes.
//

charstring BstrToCharstring( BSTR bstrString );
widestring CharStringToWideString( charstring& csString );
void TrimAtBothSides( charstring& csString );
