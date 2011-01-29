@echo off
echo:>afxres.h
brc32.exe -r -fovncviewer.res vncviewer.rc
del afxres.h
