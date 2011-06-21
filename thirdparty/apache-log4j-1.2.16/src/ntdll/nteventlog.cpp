/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef NtEventLogAppender_h
#define NtEventLogAppender_h

#ifdef __GNUC__
typedef long long __int64;
#endif

#include "org_apache_log4j_Priority.h"
#include "org_apache_log4j_nt_NTEventLogAppender.h"
#include <windows.h>
#include <jni.h>


HINSTANCE gModule = 0;

class EventSourceMap {
#if _WIN64
    enum { MAX_SOURCES = 256 };
    HANDLE* sources;
public:
    EventSourceMap() {
        sources = (HANDLE*) calloc(MAX_SOURCES, sizeof(*sources));
    }

    ~EventSourceMap() {
        free(sources);
    }

    jint createKey(HANDLE handle) {
        if (handle != 0) {
            //
            //   find first available null entry (excluding sources[0])
            //
            for(int i = 1; i < MAX_SOURCES; i++) {
                if (InterlockedCompareExchangePointer(sources + i, handle, 0) == 0) {
                    return i;
                }
            }
        }
        return 0;
    }

    HANDLE getHandle(jint key) {
        if (key >= 1 && key < MAX_SOURCES) {
            return sources[key];
        }
        return 0;
    }

    HANDLE releaseHandle(jint key) {
        if (key >= 1 && key < MAX_SOURCES) {
            return InterlockedExchangePointer(sources + key, 0);
        }
        return 0;
    }
#else
public:
    EventSourceMap() {
    }

    jint createKey(HANDLE handle) {
        return (jint) handle;
    }

    HANDLE getHandle(jint key) {
        return (HANDLE) key;
    }

    HANDLE releaseHandle(jint key) {
        return (HANDLE) key;
    }
#endif
} gEventSources;

/*
 * Convert log4j Priority to an EventLog category. Each category is
 * backed by a message resource so that proper category names will
 * be displayed in the NT Event Viewer.
 */
WORD getCategory(jint priority) {
  WORD category = 1;
  if (priority >= org_apache_log4j_Priority_DEBUG_INT) {
      category = 2;
      if (priority >= org_apache_log4j_Priority_INFO_INT) {
          category = 3;
          if (priority >= org_apache_log4j_Priority_WARN_INT) {
             category = 4;
             if (priority >= org_apache_log4j_Priority_ERROR_INT) {
                category = 5;
                if (priority >= org_apache_log4j_Priority_FATAL_INT) {
                    category = 6;
                }
             }
          }
      }
  }
  return category;
}

/*
 * Convert log4j Priority to an EventLog type. The log4j package
 * supports 8 defined priorites, but the NT EventLog only knows
 * 3 event types of interest to us: ERROR, WARNING, and INFO.
 */
WORD getType(jint priority) {
  WORD type = EVENTLOG_SUCCESS;
  if (priority >= org_apache_log4j_Priority_INFO_INT) {
      type = EVENTLOG_INFORMATION_TYPE;
      if (priority >= org_apache_log4j_Priority_WARN_INT) {
          type = EVENTLOG_WARNING_TYPE;
          if (priority >= org_apache_log4j_Priority_ERROR_INT) {
             type = EVENTLOG_ERROR_TYPE;
          }
      }
  }
  return type;
}

HKEY regGetKey(wchar_t *subkey, DWORD *disposition) {
  HKEY hkey = 0;
  RegCreateKeyExW(HKEY_LOCAL_MACHINE, subkey, 0, NULL, 
		 REG_OPTION_NON_VOLATILE, KEY_SET_VALUE, NULL, 
		 &hkey, disposition);
  return hkey;
}

void regSetString(HKEY hkey, wchar_t *name, wchar_t *value) {
  RegSetValueExW(hkey, name, 0, REG_EXPAND_SZ, 
      (LPBYTE)value, (wcslen(value) + 1) * sizeof(wchar_t));
}

void regSetDword(HKEY hkey, wchar_t *name, DWORD value) {
  RegSetValueExW(hkey, name, 0, REG_DWORD, (LPBYTE)&value, sizeof(DWORD));
}

/*
 * Add this source with appropriate configuration keys to the registry.
 */
void addRegistryInfo(wchar_t *source) {
  const wchar_t *prefix = L"SYSTEM\\CurrentControlSet\\Services\\EventLog\\Application\\";
  DWORD disposition;
  HKEY hkey = 0;
  wchar_t subkey[256];
  
  wcscpy(subkey, prefix);
  wcscat(subkey, source);
  hkey = regGetKey(subkey, &disposition);
  if (disposition == REG_CREATED_NEW_KEY) {
    HMODULE hmodule = gModule;
    if (hmodule == NULL) {
        hmodule = GetModuleHandleW(L"NTEventLogAppender.dll");
    }
    if (hmodule != NULL) {
        wchar_t modpath[_MAX_PATH];
        DWORD modlen = GetModuleFileNameW(hmodule, modpath, _MAX_PATH - 1);
        if (modlen > 0) {
            modpath[modlen] = 0;
            regSetString(hkey, L"EventMessageFile", modpath);
            regSetString(hkey, L"CategoryMessageFile", modpath);
        }
    }
    regSetDword(hkey, L"TypesSupported", (DWORD)7);
    regSetDword(hkey, L"CategoryCount", (DWORD) 6);
  }
  RegCloseKey(hkey);
  return;
}

/*
 * Class:     org.apache.log4j.nt.NTEventLogAppender
 * Method:    registerEventSource
 * Signature: (Ljava/lang/String;Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_apache_log4j_nt_NTEventLogAppender_registerEventSource(
   JNIEnv *env, jobject java_this, jstring server, jstring source) {

  jchar *nserver = 0;
  jchar *nsource = 0;

  if (server != 0) {
    jsize serverLen = env->GetStringLength(server);
    nserver = (jchar*) malloc((serverLen +1) * sizeof(jchar));
    env->GetStringRegion(server, 0, serverLen, nserver);
    nserver[serverLen] = 0;
  }
  if (source != 0) {
    jsize sourceLen = env->GetStringLength(source);
    nsource = (jchar*) malloc((sourceLen +1) * sizeof(jchar));
    env->GetStringRegion(source, 0, sourceLen, nsource);
    nsource[sourceLen] = 0;
  }
  addRegistryInfo((wchar_t*) nsource);
  jint handle = gEventSources.createKey(RegisterEventSourceW(
         (const wchar_t*) nserver, (const wchar_t*) nsource));
  free(nserver);
  free(nsource);
  return handle;
}

/*
 * Class:     org_apache_log4j_nt_NTEventLogAppender
 * Method:    reportEvent
 * Signature: (ILjava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_org_apache_log4j_nt_NTEventLogAppender_reportEvent(
   JNIEnv *env, jobject java_this, jint jhandle, jstring jstr, jint priority) {
  jboolean localHandle = JNI_FALSE;
  HANDLE handle = gEventSources.getHandle(jhandle);
  if (handle == 0) {
    // Client didn't give us a handle so make a local one.
    handle = RegisterEventSourceW(NULL, L"Log4j");
    localHandle = JNI_TRUE;
  }
  
  // convert Java String to character array
  jsize msgLen = env->GetStringLength(jstr);
  jchar* msg = (jchar*) malloc((msgLen + 1) * sizeof(jchar));
  env->GetStringRegion(jstr, 0, msgLen, msg);
  msg[msgLen] = 0;
  
  // This is the only message supported by the package. It is backed by
  // a message resource which consists of just '%1' which is replaced
  // by the string we just created.
  const DWORD messageID = 0x1000;
  ReportEventW(handle, getType(priority), 
	      getCategory(priority), 
	      messageID, NULL, 1, 0, (const wchar_t**) &msg, NULL);
  
  free((void *)msg);
  if (localHandle == JNI_TRUE) {
    // Created the handle here so free it here too.
    DeregisterEventSource(handle);
  }
  return;
}

/*
 * Class:     org_apache_log4j_nt_NTEventLogAppender
 * Method:    deregisterEventSource
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_apache_log4j_nt_NTEventLogAppender_deregisterEventSource(
JNIEnv *env, 
jobject java_this, 
jint handle)
{

  DeregisterEventSource(gEventSources.releaseHandle(handle));
}


//
//  Entry point which registers default event source (Log4j)
//     when invoked using regsvr32 tool.
//
//
extern "C" {
__declspec(dllexport) HRESULT __stdcall DllRegisterServer(void) {
	HRESULT hr = E_FAIL;
    HMODULE hmodule = gModule;
    if (hmodule == NULL) {
        hmodule = GetModuleHandleW(L"NTEventLogAppender.dll");
    }
    if (hmodule != NULL) {
        wchar_t modpath[_MAX_PATH];
        DWORD modlen = GetModuleFileNameW(hmodule, modpath, _MAX_PATH - 1);
        if (modlen > 0) {
            modpath[modlen] = 0;
			const wchar_t key[] = L"SYSTEM\\CurrentControlSet\\Services\\EventLog\\Application\\Log4j";
			DWORD disposition;
			HKEY hkey = 0;
  
			LONG stat = RegCreateKeyExW(HKEY_LOCAL_MACHINE, key, 0, NULL, 
				REG_OPTION_NON_VOLATILE, KEY_SET_VALUE, NULL, 
				&hkey, &disposition);
			if (stat == ERROR_SUCCESS) {
				stat = RegSetValueExW(hkey, L"EventMessageFile", 0, REG_EXPAND_SZ, 
					(LPBYTE) modpath, (wcslen(modpath) + 1) * sizeof(wchar_t));
				if(stat == ERROR_SUCCESS) {
					stat = RegSetValueExW(hkey, L"CategoryMessageFile", 0, REG_EXPAND_SZ, 
						(LPBYTE) modpath, (wcslen(modpath) + 1) * sizeof(wchar_t));
				}
				if(stat == ERROR_SUCCESS) {
					DWORD value = 7;
					stat = RegSetValueExW(hkey, L"TypesSupported", 0, REG_DWORD, (LPBYTE)&value, sizeof(DWORD));
				}
				if(stat == ERROR_SUCCESS) {
					DWORD value = 6;
					stat = RegSetValueExW(hkey, L"CategoryCount", 0, REG_DWORD, (LPBYTE)&value, sizeof(DWORD));
				}
				LONG closeStat = RegCloseKey(hkey);
				if (stat == ERROR_SUCCESS && closeStat == ERROR_SUCCESS) {
					hr = S_OK;
				}
			}
        }
    }
	return hr;
}


//
//  Entry point which unregisters default event source (Log4j)
//     when invoked using regsvr32 tool with /u option.
//
//
__declspec(dllexport) HRESULT __stdcall DllUnregisterServer(void) {
	LONG stat = RegDeleteKeyW(HKEY_LOCAL_MACHINE, 
		L"SYSTEM\\CurrentControlSet\\Services\\EventLog\\Application\\Log4j");
	return (stat == ERROR_SUCCESS || stat == ERROR_FILE_NOT_FOUND) ? S_OK : E_FAIL;
}

BOOL APIENTRY DllMain( HMODULE hModule,
                       DWORD  ul_reason_for_call,
                       LPVOID lpReserved
					 )
{
	switch (ul_reason_for_call)
	{
	case DLL_PROCESS_ATTACH:
	    gModule = hModule;
	    break;
	case DLL_PROCESS_DETACH:
	    gModule = 0;
	    break;
	    
	case DLL_THREAD_ATTACH:
	case DLL_THREAD_DETACH:
		break;
	}
	return TRUE;
}

}
#endif
