// amclib.cpp : Defines the entry point for the DLL application.
//
#include "stdafx.h"
#include "amclib.h"
#include "amc_internal.h"
#include "pcdef.h"
#include <direct.h>

#include "../driver/amc_driver.h"

#define OUTPUT_PORT_ADDR	0x300
#define INPUT_PORT_ADDR		0x308

#include <winioctl.h>
#include <stdarg.h>
#include <stdio.h>
#include "log.h"

extern BOOL myDeviceIoControl(
	_In_ HANDLE hDevice,
	_In_ DWORD dwIoControlCode,
	_In_reads_bytes_opt_(nInBufferSize) LPVOID lpInBuffer,
	_In_ DWORD nInBufferSize,
	_Out_writes_bytes_to_opt_(nOutBufferSize, *lpBytesReturned) LPVOID lpOutBuffer,
	_In_ DWORD nOutBufferSize,
	_Out_opt_ LPDWORD lpBytesReturned,
	//_Inout_opt_ LPOVERLAPPED lpOverlapped);	
	_In_ DWORD lpOverlapped);

extern BOOL checkComm();


#if 0
#define DeviceIoControl_Ex(hDevice, dwIoControlCode, lpInBuffer, nInBufferSize, lpOutBuffer, nOutBufferSize, lpBytesReturned, lpOverlapped) \
 do { myDeviceIoControl(hDevice, dwIoControlCode, lpInBuffer, nInBufferSize, lpOutBuffer, nOutBufferSize, lpBytesReturned, lpOverlapped);\
	  MYLOG("DeviceIoCtrl: HDL=%d, CD=%p, inbuf=%p, size=%d, outbuf=%p, size=%d, returned=%d, T_Log=%p\n", \
		  __FILE__, __LINE__, hDevice, dwIoControlCode, lpInBuffer, nInBufferSize, lpOutBuffer, nOutBufferSize,lpBytesReturned, lpOverlapped); \
} while(0)
#else
#define DeviceIoControl_Ex(hDevice, dwIoControlCode, lpInBuffer, nInBufferSize, lpOutBuffer, nOutBufferSize, lpBytesReturned, lpOverlapped) \
	 myDeviceIoControl(hDevice, dwIoControlCode, lpInBuffer, nInBufferSize, lpOutBuffer, nOutBufferSize, lpBytesReturned, lpOverlapped)
#endif

AMCDATA g_stAMCData;
char *g_pDpramAddr;

static int g_nInstance = 0;
static HANDLE g_hAmcOpen;

extern "C" void ResetAmcDataEvent()
{
	ResetEvent(g_stAMCData.hRetEvent);
}


void DoDllClose()
{
	if (g_stAMCData.hDevice != INVALID_HANDLE_VALUE)
	{
		// 혹시 있을지 모르는 켜진상태로 종료하는 것을
		// 확실하게 오프 시킨다.
		set_amp_enable(0, 0);
		set_amp_enable(1, 0);
		set_amp_enable(2, 0);
		set_amp_enable(3, 0);

		CloseHandle(g_stAMCData.hDevice);
		g_stAMCData.hDevice = INVALID_HANDLE_VALUE;
	}
	if (g_stAMCData.hRetEvent != INVALID_HANDLE_VALUE)
	{
		CloseHandle(g_stAMCData.hRetEvent);
		g_stAMCData.hRetEvent = INVALID_HANDLE_VALUE;
	}
}


BOOL APIENTRY DllMain( HANDLE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved
					 )
{
    switch (ul_reason_for_call)
	{
		case DLL_PROCESS_ATTACH:
			if (g_nInstance == 0)
			{
				//g_hAmcOpen = OpenMutex(NULL, 
				memset(&g_stAMCData, 0, sizeof(AMCDATA));
				g_stAMCData.hDevice = INVALID_HANDLE_VALUE;
				g_stAMCData.hRetEvent = INVALID_HANDLE_VALUE;
			}
			break;

		case DLL_THREAD_ATTACH:
			break;
		case DLL_THREAD_DETACH:
			break;
		case DLL_PROCESS_DETACH:
			if (g_nInstance == 1)
			{
				DoDllClose();
			}
			break;
    }
    return TRUE;
}

extern "C" char AMC_WORKDIR[300];

#include <stdio.h>

extern "C" DWORD GetPhysicalAddr (unsigned long addr)
{
	return (DWORD)g_pDpramAddr + addr;
}


BOOL CreateFolder(char *pszdir)
{
	int nLen = strlen(pszdir);
	char *psz = new char[nLen + 1];
	char *tmpc = new char[nLen + 1];

	strcpy(psz, pszdir);
	// 맨뒤에 있는 경로 구분문자를 없앤다.
	if (psz[nLen-1] == '/' || psz[nLen-1] == '\\')
	{
		psz[--nLen] = 0;
	}

	// 경로스트링 내부의 경로를 Top -> Bottom으로 내려가면서 생성한다.
	char *p = strchr(psz, '\\');
	if (p == NULL) p = strchr(psz, '/');

	if (p != NULL)
	{
		while (strchr(p + 1, '/') != NULL || strchr(p + 1, '\\') != NULL)
		{
			if (strchr(p + 1, '/') != NULL) p = strchr(p, '/');
			else p = strchr(p + 1, '\\');
			if (p == NULL) break;	// 물론 여기서 이럴일은 없다.

			// Top 경로를 복사한다.
			strncpy(tmpc, psz, p-psz);
			tmpc[p-psz] = 0;

			// 경로를 생성한다.
			mkdir(tmpc);
		}
		mkdir(psz);
	}

	delete[] tmpc;
	delete[] psz;

	return TRUE;
}


///////////////////////////////////Service 180////////////////////////////////////
extern "C" 
{
	extern INT mmc_error;
	extern INT MMCMutexLock(void);
	extern INT MMCMutexUnlock(void);
};
#include <chrono>
extern "C" int ReturnAMCData(AMC_CMD* Cmd, AMC_DATA *ReturnData)
{
	//using time_ns = std::chrono::time_point<std::chrono::system_clock, std::chrono::nanoseconds>;

    if ((Cmd->Cmd) && (Cmd->Cmd != 1)) {
        MYLOG("AMC Cmd : %04x Cmd Type : %d/%d/%d/%d\n", Cmd->Cmd, Cmd->CmdAxis[0].CmdType, Cmd->CmdAxis[1].CmdType, Cmd->CmdAxis[2].CmdType, Cmd->CmdAxis[3].CmdType);
        if (Cmd->CmdAxis[0].CmdType == 7) {
            MYLOG("ACC : %d, DCC :%d, SPD : %d, POS :%d\n", Cmd->CmdAxis[0].Accel, Cmd->CmdAxis[0].Decel, Cmd->CmdAxis[0].Speed, Cmd->CmdAxis[0].Distance);
        }
    }
	//MYLOG("[mingi]crc : %d\n", Cmd->crc);

    if (MMCMutexLock() == MMC_OK)
	{
		if (_write_dpramregs(16 * 12, (char*)Cmd, sizeof(AMC_CMD)) != 1)
		{
			TMLOG("Write fail!!!!\n");
			MMCMutexUnlock();
			return 2;	// Write fail
		}
		FLUSH(GETAMCDATA70);
		auto wake_up_at = std::chrono::system_clock::now();
		if(AMC_SUCCESS != _wait_for_reply(100))   //CIS
		{
			auto elapsed = std::chrono::system_clock::now() - wake_up_at;
			int sec = elapsed.count() / 1000000000L;
			int msec = elapsed.count() % 1000000000L / 1000000;
			int usec = elapsed.count() % 1000000000L / 1000 - msec * 1000;
			TMLOG("Time Out[%5d.%03d:%03d]\n", sec, msec, usec);
			printf("Time Out\n");
			MMCMutexUnlock();
			return 3;	// Time Out fail
		}
		
		if(!_read_dpramregs(16*12, ReturnData, sizeof(AMC_DATA) ))
		{
			TMLOG("Read fail!!!!\n");
			MMCMutexUnlock();
			return 4;	// Read fails
		}
		MMCMutexUnlock();		
        return 1;
	}
	TMLOG("MutexLock fail!!!!\n");
	return 5;	// MutexLock fail
}


///////////////////////////////////Service 178, 179////////////////////////////////////
#define Trace_Data_Transfer_Buffer_Size_M	51
#define Trace_Data_Count_M					25

#define Trace_Data_Transfer_Buffer_Size_C	61
#define Trace_Data_Count_C					20

extern "C" int GatheringTraceData()
{
    MYLOG("GatheringTraceData\n");

    int TraceDataBuffer[Trace_Data_Transfer_Buffer_Size_M];
	int i, loop_break;
	char axno;
	char strFile[300];

	FILE *fp;

	SYSTEMTIME st;
	GetLocalTime(&st);
		
	for(axno = 0 ; axno < 4 ; axno++)
	{
		loop_break = 1;
		sprintf(strFile, "D:\\Log\\AMC_AXIS[%d]_M_TRACE_%04d%02d%02d%02d%02d%02d.log", axno,st.wYear, st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond);

		fp = fopen(strFile, "w");
		if(fp == NULL) return 0;

		while(loop_break)
		{
			if(MMCMutexLock() == MMC_OK)
			{
				_write_dpramreg(AXIS_REG, (CHAR)axno);
				FLUSH(TRACEUPDATE70);

				if(AMC_SUCCESS != _wait_for_reply(2000))
				{
					MMCMutexUnlock();
					if(fp != NULL) fclose(fp);

                    return 0;
				}
				_read_dpramregs(16*12, TraceDataBuffer, sizeof(TraceDataBuffer));
				MMCMutexUnlock();		
			}
			else
			{
				if(fp != NULL) fclose(fp);

				return 0;
			}

			for(i=0 ; i<TraceDataBuffer[Trace_Data_Transfer_Buffer_Size_M-1] ; i++)
			{
				if(i % Trace_Data_Count_M)
				{
					if(fp!=NULL) fprintf(fp, "%d\t",TraceDataBuffer[i] );
				}
				else
				{
					if(fp!=NULL) fprintf(fp, "\n%d\t",TraceDataBuffer[i] );
				}
			}
			
			if(TraceDataBuffer[Trace_Data_Transfer_Buffer_Size_M-1] != (Trace_Data_Transfer_Buffer_Size_M-1))
			{
				loop_break = 0;
			}
		}	
		if(fp!=NULL) fclose(fp);
	}		
	
	MMCMutexUnlock();
	return 1;
}

extern "C" int GatheringTraceData_1()
{
    MYLOG("GatheringTraceData_1\n");
    
    int TraceDataBuffer[Trace_Data_Transfer_Buffer_Size_C];
	int i, loop_break;
	char axno;
	char strFile[300];

	FILE *fp;

	SYSTEMTIME st;
	GetLocalTime(&st);
		
	for(axno = 0 ; axno < 4 ; axno++)
	{
		loop_break = 1;
		sprintf(strFile, "D:\\Log\\AMC_AXIS[%d]_C_TRACE_%04d%02d%02d%02d%02d%02d.log", axno,st.wYear, st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond);

		fp = fopen(strFile, "w");
		if(fp == NULL) return 0;

		while(loop_break)
		{
			if(MMCMutexLock() == MMC_OK)			
			{
				_write_dpramreg(AXIS_REG, (CHAR)axno);
				FLUSH(TRACEUPDATE70_1);

				if(AMC_SUCCESS != _wait_for_reply(2000))
				{
					MMCMutexUnlock();
					if(fp != NULL) fclose(fp);

					return 0;
				}
				_read_dpramregs(16*12, TraceDataBuffer, sizeof(TraceDataBuffer));
				MMCMutexUnlock();		
			}
			else
			{
				if(fp != NULL) fclose(fp);

				return 0;
			}

			for(i=0 ; i<TraceDataBuffer[Trace_Data_Transfer_Buffer_Size_C-1] ; i++)
			{
				if(i % Trace_Data_Count_C)
				{
					if(fp!=NULL) fprintf(fp, "%d\t",TraceDataBuffer[i]);
				}
				else
				{
					if(fp!=NULL) fprintf(fp, "\n%d\t",TraceDataBuffer[i] );
				}
			}
			
			if(TraceDataBuffer[Trace_Data_Transfer_Buffer_Size_C-1] != (Trace_Data_Transfer_Buffer_Size_C-1))
			{
				loop_break = 0;
			}
		}	
		if(fp!=NULL) fclose(fp);
	}		
	MMCMutexUnlock();
	
    return 1;
}


CRITICAL_SECTION __csMovexLock;

extern "C" {
void LockMovex() { EnterCriticalSection(&__csMovexLock); }
void UnlockMovex() { LeaveCriticalSection(&__csMovexLock); }
};

extern "C" AMCBOOL amc_open(int intr, unsigned long dpram, char* workdir)
{
//	int intr = 9;
//	unsigned long dpram = 0xd0000;
    MYLOG("amc_open\n");
	TMLOG("========= ATLAS DLL Version : %d.%02d.%02d =========\n", VER_MAJOR, VER_MINOR, VER_REVISION);
	TMLOG("amc_open\n");
    
    DWORD n;

	if ( intr != 11 )
	{
		return AMCFALSE;
	}
	if(!checkComm())
		return AMCFALSE;

	if (g_stAMCData.hDevice!=INVALID_HANDLE_VALUE)
	{
		// Initialize...
		return AMCTRUE;
	}

	g_hAmcOpen = OpenMutex(MUTEX_ALL_ACCESS, FALSE, TEXT("AmcOpen"));
	if (g_hAmcOpen != NULL) 
	{
		CloseHandle(g_hAmcOpen);
		return AMCFALSE;
	}
	g_hAmcOpen = CreateMutex(NULL, TRUE, TEXT("AmcOpen"));
    
    g_stAMCData.hDevice = CreateFile("ATLAS_HANDLE", GENERIC_READ | GENERIC_WRITE,
        0, 0, CREATE_ALWAYS, FILE_FLAG_OVERLAPPED, 0);
    
    if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE)
	{
		return AMCFALSE;
	}
	g_nInstance = 1;

	g_stAMCData.hRetEvent = CreateEvent(NULL, TRUE, FALSE, RET_EVENT_USER);
	g_stAMCData.nTimeout = 3000;

	// Initialize...
	AMC_INITIALIZE_IRP initirp;
	initirp.m_nIntrNum = intr;
	initirp.m_DpramPhyAddr = dpram;
	initirp.m_pDpramVirAddr = 0;

    DeviceIoControl_Ex(g_stAMCData.hDevice,IOCTL_INIT_AMCDRV, &initirp, sizeof(AMC_INITIALIZE_IRP),
													&initirp, sizeof(AMC_INITIALIZE_IRP), &n, NULL);
    MYLOG("initirp<: intr=%d, phyaddr=%p,virtaddr=%p\n", initirp.m_nIntrNum, initirp.m_DpramPhyAddr, initirp.m_pDpramVirAddr);
    
    if(n != sizeof(AMC_INITIALIZE_IRP))
	{
		CloseHandle(g_stAMCData.hDevice);
		g_stAMCData.hDevice = INVALID_HANDLE_VALUE;

		CloseHandle(g_stAMCData.hRetEvent);
		g_stAMCData.hRetEvent = INVALID_HANDLE_VALUE;

		if (g_hAmcOpen != NULL) CloseHandle(g_hAmcOpen);
		g_hAmcOpen = NULL;

		return AMCFALSE;
	}
	if(initirp.m_pDpramVirAddr==0)
	{
		CloseHandle(g_stAMCData.hDevice);
		g_stAMCData.hDevice = INVALID_HANDLE_VALUE;

		CloseHandle(g_stAMCData.hRetEvent);
		g_stAMCData.hRetEvent = INVALID_HANDLE_VALUE;

		if (g_hAmcOpen != NULL) CloseHandle(g_hAmcOpen);
		g_hAmcOpen = NULL;

		return AMCFALSE;
	}
	g_pDpramAddr = initirp.m_pDpramVirAddr;

	if (workdir[strlen(workdir)-1] == '\\');
	else if (workdir[strlen(workdir)-1] == '/');
	else strcat(workdir, "\\");

	strcpy(AMC_WORKDIR, workdir);

	InitializeCriticalSection(&__csMovexLock);
	return AMCTRUE;
}


extern "C" void Addr_Release();//2.5.20

void amc_close()
{
    MYLOG("amc_close\n");

    DWORD n;
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return;

	// 혹시 DSP가 인터럽트를 걸었다면 클리어 시킨다.
	unsigned char val;
	_read_dpramreg(ADDR_DSP_ACK, &val);

	DeleteCriticalSection(&__csMovexLock);

	if(g_stAMCData.hRetEvent!=INVALID_HANDLE_VALUE)
	{
		CloseHandle(g_stAMCData.hRetEvent);
		g_stAMCData.hRetEvent = INVALID_HANDLE_VALUE;
	}

    DeviceIoControl_Ex(g_stAMCData.hDevice,IOCTL_RELEASE_AMCDRV, 0, 0, 0, 0, &n, NULL);
	if(n != sizeof(AMC_INITIALIZE_IRP))

	CloseHandle(g_stAMCData.hDevice);
	g_stAMCData.hDevice = INVALID_HANDLE_VALUE;

	g_nInstance = 0;
	if (g_hAmcOpen != NULL) CloseHandle(g_hAmcOpen);
	g_hAmcOpen = NULL;

	Addr_Release();
}



AMCBOOL _write_dpramreg(unsigned long addr, unsigned char val)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMCFALSE;

	DWORD n;
	AMC_REGSRW_IRP irp;
	irp.m_nOffset = addr;
	irp.m_nData = val;
	irp.m_bSuccess = 0;
    DeviceIoControl_Ex(g_stAMCData.hDevice,IOCTL_WRITE_BUFREG,&irp,sizeof(irp),&irp,sizeof(irp),&n,NULL);
	if(irp.m_bSuccess==0) return AMCFALSE;

	return AMCTRUE;
}

AMCBOOL _write_dpramreg_big16(unsigned long addr, unsigned short val)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMCFALSE;

	DWORD n;
	AMC_REGSRWGRP_IRP irp;

	irp.m_nOffset = addr;
	irp.m_bSuccess = 0;
	irp.m_nCount = (unsigned long)sizeof(unsigned short);

	irp.m_arrData[0] = (val>>8) & 0xFF;
	irp.m_arrData[1] = (val) & 0xFF;

    DeviceIoControl_Ex(g_stAMCData.hDevice, IOCTL_WRITE_BUFREGS,
						&irp, sizeof(ULONG)*3+irp.m_nCount, &irp, sizeof(ULONG)*3,&n,NULL);
	if(irp.m_bSuccess==0) return AMCFALSE;

	return AMCTRUE;
}

AMCBOOL _write_dpramreg_big32(unsigned long addr, unsigned long val)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMCFALSE;

	DWORD n;
	AMC_REGSRWGRP_IRP irp;

	irp.m_nOffset = addr;
	irp.m_bSuccess = 0;
	irp.m_nCount = (unsigned long)sizeof(unsigned long);

	irp.m_arrData[0] = (unsigned char)((val>>24) & 0xFF);
	irp.m_arrData[1] = (unsigned char)((val>>16) & 0xFF);
	irp.m_arrData[2] = (unsigned char)((val>>8) & 0xFF);
	irp.m_arrData[3] = (unsigned char)((val) & 0xFF);

    DeviceIoControl_Ex(g_stAMCData.hDevice, IOCTL_WRITE_BUFREGS,
						&irp, sizeof(ULONG)*3+irp.m_nCount, &irp, sizeof(ULONG)*3,&n,NULL);
	if(irp.m_bSuccess==0) return AMCFALSE;

	return AMCTRUE;
}

AMCBOOL _write_dpramreg_bigfloat(unsigned long addr, float val)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMCFALSE;

	DWORD n;
	AMC_REGSRWGRP_IRP irp;

	irp.m_nOffset = addr;
	irp.m_bSuccess = 0;
	irp.m_nCount = (unsigned long)sizeof(unsigned long);

	irp.m_arrData[0] = (unsigned char)(((*((unsigned long*)&val))>>24) & 0xFF);
	irp.m_arrData[1] = (unsigned char)(((*((unsigned long*)&val))>>16) & 0xFF);
	irp.m_arrData[2] = (unsigned char)(((*((unsigned long*)&val))>>8) & 0xFF);
	irp.m_arrData[3] = (unsigned char)(((*((unsigned long*)&val))) & 0xFF);

    DeviceIoControl_Ex(g_stAMCData.hDevice, IOCTL_WRITE_BUFREGS,
						&irp, sizeof(ULONG)*3+irp.m_nCount, &irp, sizeof(ULONG)*3,&n,NULL);
	if(irp.m_bSuccess==0) return AMCFALSE;

	return AMCTRUE;
}

AMCBOOL _read_dpramreg(unsigned long addr, unsigned char *val)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMCFALSE;

	DWORD n;
	AMC_REGSRW_IRP irp;
	irp.m_nOffset = addr;
	irp.m_bSuccess = 0;
    DeviceIoControl_Ex(g_stAMCData.hDevice,IOCTL_READ_BUFREG,&irp,sizeof(irp),&irp,sizeof(irp),&n,NULL);
	*val = (unsigned char)irp.m_nData;

	if(irp.m_bSuccess==0) return AMCFALSE;
	return AMCTRUE;
}

AMCBOOL _write_dpramregs(unsigned long saddr, const void *value, int len)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMCFALSE;

	DWORD n;
	AMC_REGSRWGRP_IRP irp;
	int i;
	const unsigned char *val = (const unsigned char*)value;


	irp.m_nOffset = saddr;
	irp.m_bSuccess = 0;
	irp.m_nCount = (unsigned long)len;

	for(i=0;i<len;i++) irp.m_arrData[i] = val[i];

    DeviceIoControl_Ex(g_stAMCData.hDevice, IOCTL_WRITE_BUFREGS,
						&irp, sizeof(ULONG)*3+len, &irp, sizeof(ULONG)*3,&n,NULL);
	if(irp.m_bSuccess==0) return AMCFALSE;

	return AMCTRUE;
}

AMCBOOL _read_dpramregs(unsigned long saddr, void *value, int len)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMCFALSE;

	unsigned char* val = (unsigned char*)value;
	DWORD n;
	int i;
	AMC_REGSRWGRP_IRP irp;
	irp.m_nOffset = saddr;
	irp.m_bSuccess = 0;
	irp.m_nCount = (unsigned long)len;

    DeviceIoControl_Ex(g_stAMCData.hDevice,IOCTL_READ_BUFREGS,
					&irp, sizeof(ULONG)*3, &irp, sizeof(ULONG)*3+len,&n,NULL);

	if(irp.m_bSuccess==0) return AMCFALSE;
	for(i=0;i<len;i++) val[i] = irp.m_arrData[i];
	return AMCTRUE;
}

void _flush_dpram(unsigned char cmd)
{
	FLUSH(cmd);
}

int _wait_for_reply(unsigned int timeout)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	if(WaitForSingleObject(g_stAMCData.hRetEvent, timeout)==WAIT_TIMEOUT) return MMC_TIMEOUT_ERR;  
	ResetEvent(g_stAMCData.hRetEvent);
	return AMC_SUCCESS;
}

