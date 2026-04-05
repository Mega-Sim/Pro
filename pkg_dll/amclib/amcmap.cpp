// amclib.cpp : Defines the entry point for the DLL application.
//

#include "stdafx.h"
#include "amc.h"
#include "pcdef.h"
#include "amc_internal.h"

extern "C" AMCLIB_API int download_map_info(char* pMap, int len)
{
	return DownloadLongBlock(pMap, (UINT) 0X2000, len);
}

