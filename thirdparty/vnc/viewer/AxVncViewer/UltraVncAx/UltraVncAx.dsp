# Microsoft Developer Studio Project File - Name="UltraVncAx" - Package Owner=<4>
# Microsoft Developer Studio Generated Build File, Format Version 6.00
# ** DO NOT EDIT **

# TARGTYPE "Win32 (x86) Dynamic-Link Library" 0x0102

CFG=UltraVncAx - Win32 Debug
!MESSAGE This is not a valid makefile. To build this project using NMAKE,
!MESSAGE use the Export Makefile command and run
!MESSAGE 
!MESSAGE NMAKE /f "UltraVncAx.mak".
!MESSAGE 
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "UltraVncAx.mak" CFG="UltraVncAx - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "UltraVncAx - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "UltraVncAx - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 

# Begin Project
# PROP AllowPerConfigDependencies 0
# PROP Scc_ProjName ""
# PROP Scc_LocalPath ""
CPP=cl.exe
MTL=midl.exe
RSC=rc.exe

!IF  "$(CFG)" == "UltraVncAx - Win32 Debug"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 1
# PROP BASE Output_Dir "Debug"
# PROP BASE Intermediate_Dir "Debug"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 1
# PROP Output_Dir "Debug"
# PROP Intermediate_Dir "Debug"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MTd /W3 /Gm /ZI /Od /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /YX"stdhdrs.h" /FD /GZ /c
# ADD CPP /nologo /MTd /W3 /Gm /GX /ZI /Od /I "..\omnithread" /I ".." /D "_DEBUG" /D "_MBCS" /D "WIN32" /D "_WINDOWS" /D "_USRDLL" /D "_ULTRAVNCAX_" /D "__NT__" /D "_WINSTATIC" /D "__WIN32__" /FR /YX"stdhdrs.h" /FD /GZ /c
# ADD BASE RSC /l 0x410 /d "_DEBUG"
# ADD RSC /l 0x410 /i "..\res\\" /d "_DEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:windows /dll /debug /machine:I386 /pdbtype:sept
# ADD LINK32 comctl32.lib winmm.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib wsock32.lib /nologo /subsystem:windows /dll /debug /machine:I386 /out:"Debug/vmopsvnc.dll" /pdbtype:sept
# Begin Custom Build - Performing registration
OutDir=.\Debug
TargetPath=.\Debug\vmopsvnc.dll
InputPath=.\Debug\vmopsvnc.dll
SOURCE="$(InputPath)"

"$(OutDir)\regsvr32.trg" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	regsvr32 /s /c "$(TargetPath)" 
	echo regsvr32 exec. time > "$(OutDir)\regsvr32.trg" 
	
# End Custom Build

!ELSEIF  "$(CFG)" == "UltraVncAx - Win32 Release"

# PROP BASE Use_MFC 0
# PROP BASE Use_Debug_Libraries 0
# PROP BASE Output_Dir "Release"
# PROP BASE Intermediate_Dir "Release"
# PROP BASE Target_Dir ""
# PROP Use_MFC 0
# PROP Use_Debug_Libraries 0
# PROP Output_Dir "Release"
# PROP Intermediate_Dir "Release"
# PROP Ignore_Export_Lib 0
# PROP Target_Dir ""
# ADD BASE CPP /nologo /MT /W3 /O1 /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "_ATL_STATIC_REGISTRY" /D "_ATL_MIN_CRT" /YX"stdhdrs.h" /FD /c
# ADD CPP /nologo /MT /W3 /GX /Zi /O1 /I "..\omnithread" /I ".." /D "NDEBUG" /D "_MBCS" /D "_ATL_STATIC_REGISTRY" /D "WIN32" /D "_WINDOWS" /D "_USRDLL" /D "_ULTRAVNCAX_" /D "__NT__" /D "_WINSTATIC" /D "__WIN32__" /FR /YX"stdhdrs.h" /FD /c
# ADD BASE RSC /l 0x410 /d "NDEBUG"
# ADD RSC /l 0x410 /i "..\res\\" /d "NDEBUG"
BSC32=bscmake.exe
# ADD BASE BSC32 /nologo
# ADD BSC32 /nologo
LINK32=link.exe
# ADD BASE LINK32 kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:windows /dll /machine:I386
# ADD LINK32 comctl32.lib winmm.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib wsock32.lib /nologo /subsystem:windows /dll /debug /machine:I386 /out:"Release/vmopsvnc.dll"
# Begin Custom Build - Performing registration
OutDir=.\Release
TargetPath=.\Release\vmopsvnc.dll
InputPath=.\Release\vmopsvnc.dll
SOURCE="$(InputPath)"

"$(OutDir)\regsvr32.trg" : $(SOURCE) "$(INTDIR)" "$(OUTDIR)"
	regsvr32 /s /c "$(TargetPath)" 
	echo regsvr32 exec. time > "$(OutDir)\regsvr32.trg" 
	
# End Custom Build

!ENDIF 

# Begin Target

# Name "UltraVncAx - Win32 Debug"
# Name "UltraVncAx - Win32 Release"
# Begin Group "Source Files"

# PROP Default_Filter "cpp;c;cxx;rc;def;r;odl;idl;hpj;bat"
# Begin Source File

SOURCE=..\AboutBox.cpp
# End Source File
# Begin Source File

SOURCE=..\AccelKeys.cpp
# End Source File
# Begin Source File

SOURCE=..\AuthDialog.cpp
# End Source File
# Begin Source File

SOURCE=..\buildtime.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnection.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionCacheRect.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionClipboard.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionCopyRect.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionCoRRE.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionCursor.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionFile.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionFullScreen.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionHextile.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionRaw.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionRRE.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionTight.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionUltra.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionZlib.cpp
# End Source File
# Begin Source File

SOURCE=..\ClientConnectionZlibHex.cpp
# End Source File
# Begin Source File

SOURCE=..\d3des.c
# End Source File
# Begin Source File

SOURCE=..\Daemon.cpp
# End Source File
# Begin Source File

SOURCE=..\rfb\dh.cpp
# End Source File
# Begin Source File

SOURCE=..\DSMPlugin\DSMPlugin.cpp
# End Source File
# Begin Source File

SOURCE=..\Exception.cpp
# End Source File
# Begin Source File

SOURCE=..\FileTransfer.cpp
# End Source File
# Begin Source File

SOURCE=..\Flasher.cpp
# End Source File
# Begin Source File

SOURCE=..\FullScreenTitleBar.cpp
# End Source File
# Begin Source File

SOURCE=.\Helpers.cpp
# End Source File
# Begin Source File

SOURCE=..\KeyMap.cpp
# End Source File
# Begin Source File

SOURCE=..\Log.cpp
# End Source File
# Begin Source File

SOURCE=..\LowLevelHook.cpp
# End Source File
# Begin Source File

SOURCE=..\lzo\minilzo.c
# End Source File
# Begin Source File

SOURCE=..\MRU.cpp
# End Source File
# Begin Source File

SOURCE=..\SessionDialog.cpp
# End Source File
# Begin Source File

SOURCE=.\StdAfx.cpp
# End Source File
# Begin Source File

SOURCE=..\stdhdrs.cpp

!IF  "$(CFG)" == "UltraVncAx - Win32 Debug"

# ADD CPP /Yc"stdhdrs.h"

!ELSEIF  "$(CFG)" == "UltraVncAx - Win32 Release"

# ADD CPP /YX"stdhdrs.h"

!ENDIF 

# End Source File
# Begin Source File

SOURCE=..\TextChat.cpp
# End Source File
# Begin Source File

SOURCE=.\UltraVncAx.cpp
# End Source File
# Begin Source File

SOURCE=.\UltraVncAx.def
# End Source File
# Begin Source File

SOURCE=.\UltraVncAx.idl
# ADD MTL /tlb ".\UltraVncAx.tlb" /h "UltraVncAx.h" /iid "UltraVncAx_i.c" /Oicf
# End Source File
# Begin Source File

SOURCE=.\UltraVncAx.rc
# End Source File
# Begin Source File

SOURCE=.\UltraVncAxGlobalConstructor.cpp
# End Source File
# Begin Source File

SOURCE=.\UltraVncAxObj.cpp
# End Source File
# Begin Source File

SOURCE=..\vncauth.c
# End Source File
# Begin Source File

SOURCE=..\VNCOptions.cpp
# End Source File
# Begin Source File

SOURCE=..\vncviewer.cpp
# End Source File
# Begin Source File

SOURCE=..\VNCviewerApp.cpp
# End Source File
# Begin Source File

SOURCE=..\VNCviewerApp32.cpp
# End Source File
# Begin Source File

SOURCE=..\ZipUnZip32\ZipUnzip32.cpp
# End Source File
# Begin Source File

SOURCE=..\zrle.cpp
# End Source File
# End Group
# Begin Group "Header Files"

# PROP Default_Filter "h;hpp;hxx;hm;inl"
# Begin Source File

SOURCE=..\AboutBox.h
# End Source File
# Begin Source File

SOURCE=..\AccelKeys.h
# End Source File
# Begin Source File

SOURCE=..\AuthDialog.h
# End Source File
# Begin Source File

SOURCE=..\ClientConnection.h
# End Source File
# Begin Source File

SOURCE=..\d3des.h
# End Source File
# Begin Source File

SOURCE=..\Daemon.h
# End Source File
# Begin Source File

SOURCE=..\rfb\dh.h
# End Source File
# Begin Source File

SOURCE=..\DSMPlugin\DSMPlugin.h
# End Source File
# Begin Source File

SOURCE=..\Exception.h
# End Source File
# Begin Source File

SOURCE=..\FileTransfer.h
# End Source File
# Begin Source File

SOURCE=..\Flasher.h
# End Source File
# Begin Source File

SOURCE=..\FullScreenTitleBar.h
# End Source File
# Begin Source File

SOURCE=..\FullScreenTitleBarConst.h
# End Source File
# Begin Source File

SOURCE=..\KeyMap.h
# End Source File
# Begin Source File

SOURCE=..\keysymdef.h
# End Source File
# Begin Source File

SOURCE=..\Log.h
# End Source File
# Begin Source File

SOURCE=..\LowLevelHook.h
# End Source File
# Begin Source File

SOURCE=..\MRU.h
# End Source File
# Begin Source File

SOURCE=..\res\resource.h
# End Source File
# Begin Source File

SOURCE=.\Resource.h
# End Source File
# Begin Source File

SOURCE=..\rfb.h
# End Source File
# Begin Source File

SOURCE=..\SessionDialog.h
# End Source File
# Begin Source File

SOURCE=.\StdAfx.h
# End Source File
# Begin Source File

SOURCE=..\stdhdrs.h
# End Source File
# Begin Source File

SOURCE=..\TextChat.h
# End Source File
# Begin Source File

SOURCE=.\UltraVncAxCP.h
# End Source File
# Begin Source File

SOURCE=.\UltraVncAxGlobalConstructor.h
# End Source File
# Begin Source File

SOURCE=.\UltraVncAxObj.h
# End Source File
# Begin Source File

SOURCE=..\vncauth.h
# End Source File
# Begin Source File

SOURCE=..\VNCOptions.h
# End Source File
# Begin Source File

SOURCE=..\vncviewer.h
# End Source File
# Begin Source File

SOURCE=..\VNCviewerApp.h
# End Source File
# Begin Source File

SOURCE=..\VNCviewerApp32.h
# End Source File
# Begin Source File

SOURCE=..\ZipUnZip32\ZipUnZip32.h
# End Source File
# End Group
# Begin Group "Resource Files"

# PROP Default_Filter "ico;cur;bmp;dlg;rc2;rct;bin;rgs;gif;jpg;jpeg;jpe"
# Begin Source File

SOURCE=..\res\B46.ico
# End Source File
# Begin Source File

SOURCE=.\B46.ico
# End Source File
# Begin Source File

SOURCE=..\res\stat\back.bmp
# End Source File
# Begin Source File

SOURCE=..\res\background2.bmp
# End Source File
# Begin Source File

SOURCE=..\res\stat\background2.bmp
# End Source File
# Begin Source File

SOURCE=..\res\bitmap1.bmp
# End Source File
# Begin Source File

SOURCE=..\res\bitmap2.bmp
# End Source File
# Begin Source File

SOURCE=..\res\stat\bitmap4.bmp
# End Source File
# Begin Source File

SOURCE=..\res\stat\bitmap5.bmp
# End Source File
# Begin Source File

SOURCE=..\res\stat\bitmap8.bmp
# End Source File
# Begin Source File

SOURCE=..\res\stat\bitmap9.bmp
# End Source File
# Begin Source File

SOURCE=..\res\stat\both.bmp
# End Source File
# Begin Source File

SOURCE=..\res\stat\closeup.bmp
# End Source File
# Begin Source File

SOURCE=..\res\cursor1.cur
# End Source File
# Begin Source File

SOURCE=.\cursor1.cur
# End Source File
# Begin Source File

SOURCE=..\res\cursorsel.cur
# End Source File
# Begin Source File

SOURCE=.\cursorsel.cur
# End Source File
# Begin Source File

SOURCE=..\res\dir.ico
# End Source File
# Begin Source File

SOURCE=.\dir.ico
# End Source File
# Begin Source File

SOURCE=..\res\drive.ico
# End Source File
# Begin Source File

SOURCE=.\drive.ico
# End Source File
# Begin Source File

SOURCE=..\res\file.ico
# End Source File
# Begin Source File

SOURCE=.\file.ico
# End Source File
# Begin Source File

SOURCE=..\res\stat\flash.bmp
# End Source File
# Begin Source File

SOURCE=..\res\stat\front.bmp
# End Source File
# Begin Source File

SOURCE=..\res\hoch.bmp
# End Source File
# Begin Source File

SOURCE=..\res\idr_tray.ico
# End Source File
# Begin Source File

SOURCE=.\idr_tray.ico
# End Source File
# Begin Source File

SOURCE=.\idr_tray_disabled.ico
# End Source File
# Begin Source File

SOURCE=..\res\stat\mainicon.ico
# End Source File
# Begin Source File

SOURCE=..\res\stat\maximizeup.bmp
# End Source File
# Begin Source File

SOURCE=..\res\stat\minimizeup.bmp
# End Source File
# Begin Source File

SOURCE=..\res\nocursor.cur
# End Source File
# Begin Source File

SOURCE=.\nocursor.cur
# End Source File
# Begin Source File

SOURCE=..\res\stat\none.bmp
# End Source File
# Begin Source File

SOURCE=..\res\stat\pindown.bmp
# End Source File
# Begin Source File

SOURCE=..\res\stat\pinup.bmp
# End Source File
# Begin Source File

SOURCE=..\res\quer.bmp
# End Source File
# Begin Source File

SOURCE=..\res\toolbar1.bmp
# End Source File
# Begin Source File

SOURCE="..\res\ultra-logo.bmp"
# End Source File
# Begin Source File

SOURCE=.\UltraVncAxObj.rgs
# End Source File
# Begin Source File

SOURCE=..\res\vnc.bmp
# End Source File
# Begin Source File

SOURCE=..\res\vnc32.BMP
# End Source File
# Begin Source File

SOURCE=..\res\vnc64.BMP
# End Source File
# Begin Source File

SOURCE=..\res\vncviewer.exe.manifest
# End Source File
# Begin Source File

SOURCE=..\res\vncviewer.ico
# End Source File
# Begin Source File

SOURCE=.\vncviewer.ico
# End Source File
# End Group
# Begin Source File

SOURCE=.\vncviewer.exe.manifest
# End Source File
# End Target
# End Project
