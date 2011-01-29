
UltraVncAxps.dll: dlldata.obj UltraVncAx_p.obj UltraVncAx_i.obj
	link /dll /out:UltraVncAxps.dll /def:UltraVncAxps.def /entry:DllMain dlldata.obj UltraVncAx_p.obj UltraVncAx_i.obj \
		kernel32.lib rpcndr.lib rpcns4.lib rpcrt4.lib oleaut32.lib uuid.lib \

.c.obj:
	cl /c /Ox /DWIN32 /D_WIN32_WINNT=0x0400 /DREGISTER_PROXY_DLL \
		$<

clean:
	@del UltraVncAxps.dll
	@del UltraVncAxps.lib
	@del UltraVncAxps.exp
	@del dlldata.obj
	@del UltraVncAx_p.obj
	@del UltraVncAx_i.obj
