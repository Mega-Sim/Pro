#include "stdafx.h"

#include "amc.h"
#include "amclib.h"
#include "amc_internal.h"
#include "pcdef.h"

extern INT mmc_error;

#define DPRAM_IO_DATA	124

extern "C" AMCLIB_API int write_outport(int port, unsigned long data[2])
{
	INT err;
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK)
	{
		_write_dpramregs(DPRAM_IO_DATA, data, sizeof(unsigned long)*2);
		FLUSH(AMCMD_WRITE_OUTPUT_PORT);
		mmc_error = err = _wait_for_reply(g_stAMCData.nTimeout);
		if(err != AMC_SUCCESS) 
		{
			MMCMutexUnlock();
			return err;
		}

		MMCMutexUnlock();
	}
	return err;
}

extern "C" AMCLIB_API int read_inport(int port, unsigned long data[2])
{
	INT err;
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK)
	{
		FLUSH(AMCMD_READ_INPUT_PORT);
		mmc_error = err = _wait_for_reply(g_stAMCData.nTimeout);
		if(err != AMC_SUCCESS) 
		{
			MMCMutexUnlock();
			return err;
		}
		_read_dpramregs(DPRAM_IO_DATA, data, 8);

		MMCMutexUnlock();
	}
	return err;
}

extern "C" AMCLIB_API int read_outport(int port, unsigned long data[2])
{
	INT err;
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK)
	{
		FLUSH(AMCMD_READ_OUTPUT_PORT);
		mmc_error = err = _wait_for_reply(g_stAMCData.nTimeout);
		if(err != AMC_SUCCESS) 
		{
			MMCMutexUnlock();
			return err;
		}
		_read_dpramregs(DPRAM_IO_DATA, data, 8);

		MMCMutexUnlock();
	}
	return err;
}

// GPIO write function
extern "C" AMCLIB_API int gpio_set(const unsigned char value[8])
{
	INT err;
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK)
	{
		_write_dpramregs(DPRAM_IO_DATA, value, 8);
		FLUSH(AMCMD_WRITE_OUTPUT_PORT);
		mmc_error = err = _wait_for_reply(g_stAMCData.nTimeout);
		if(err != AMC_SUCCESS) 
		{
			MMCMutexUnlock();
			return err;
		}

		MMCMutexUnlock();
	}
	return err;
}

// GPIO read function
extern "C" AMCLIB_API void gpio_get(unsigned char value[8])
{
	read_inport(0, (unsigned long*)value);
}

extern "C" AMCLIB_API int set_outport_bit(int port, unsigned long bit)
{
	INT err;
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK)
	{
		_write_dpramregs(DPRAM_IO_DATA, &bit, sizeof(unsigned long));
		FLUSH(AMCMD_SET_OUTPUT_PORT);
		mmc_error = err = _wait_for_reply(g_stAMCData.nTimeout);
		if(err != AMC_SUCCESS) 
		{
			MMCMutexUnlock();
			return err;
		}

		MMCMutexUnlock();
	}
	return err;
}

extern "C" AMCLIB_API int reset_outport_bit(int port, unsigned long bit)
{
	INT err;
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK)
	{
		_write_dpramregs(DPRAM_IO_DATA, &bit, sizeof(unsigned long));
		FLUSH(AMCMD_RESET_OUTPUT_PORT);
		mmc_error = err = _wait_for_reply(g_stAMCData.nTimeout);
		if(err != AMC_SUCCESS) 
		{
			MMCMutexUnlock();
			return err;
		}

		MMCMutexUnlock();
	}
	return err;
}

