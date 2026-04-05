#ifndef __AMC_MAP_H
#define __AMC_MAP_H

// Added by ckyu.
// 2006.12.08

// The following ifdef block is the standard way of creating macros which make exporting 
// from a DLL simpler. All files within this DLL are compiled with the AMCLIB_EXPORTS
// symbol defined on the command line. this symbol should not be defined on any project
// that uses this DLL. This way any other project whose source files include this file see 
// AMCLIB_API functions as being imported from a DLL, wheras this DLL sees symbols
// defined with this macro as being exported.


#include "cmdset.h"
#include "amc.h"

typedef struct 
{
	UINT	uiCrc32;
	UINT	uiTotLen;
	UINT	uiTotBlk;
} MAP_HEADER;

extern MAP_HEADER g_stMapHeader;


#endif

