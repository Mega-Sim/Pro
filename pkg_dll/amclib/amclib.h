
// The following ifdef block is the standard way of creating macros which make exporting 
// from a DLL simpler. All files within this DLL are compiled with the AMCLIB_EXPORTS
// symbol defined on the command line. this symbol should not be defined on any project
// that uses this DLL. This way any other project whose source files include this file see 
// AMCLIB_API functions as being imported from a DLL, wheras this DLL sees symbols
// defined with this macro as being exported.
#ifndef		_AMCLIB_H
#define		_AMCLIB_H

#include "cmdset.h"
#include "amc.h"

typedef struct __AMCDATA
{
	HANDLE hDevice;
	HANDLE hRetEvent;
	unsigned int nTimeout;
}AMCDATA;

extern AMCDATA g_stAMCData;

#define FLUSH(a) ResetEvent(g_stAMCData.hRetEvent);_write_dpramreg(FLUSH_ADDR, a)
//#define SET_AXIS(a) _write_dpramreg(AXIS_REG, a)

/*

// This class is exported from the amclib.dll
class AMCLIB_API CAmclib {
public:
	CAmclib(void);
	// TODO: add your methods here.
};

extern AMCLIB_API int nAmclib;

AMCLIB_API int fnAmclib(void);
*/



#endif

