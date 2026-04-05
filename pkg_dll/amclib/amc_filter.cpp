#include "stdafx.h"
#include "pcdef.h"
#include "../include/amc_filter.h"
#include "amc_internal.h"


extern "C" 
{
extern INT mmc_error;
extern INT MMCMutexLock(void);
extern INT MMCMutexUnlock(void);
};


static int __set_filter(int ax, int where, int ntype, int nfreq)
{
	INT err;
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (CHAR)ax);
		AxisDpram[ax]->Int_Type[0] = where;
		AxisDpram[ax]->Int_Type[1] = ntype;
		AxisDpram[ax]->Int_Type[2] = nfreq;

		// Wake DSP!. Send Command
		_flush_dpram(AMC_FILTER_SET);
		mmc_error = err = _wait_for_reply(3000);

		MMCMutexUnlock ();
		
		if (err != AMC_SUCCESS)
		{
			mmc_error = MMC_TIMEOUT_ERR;
			return MMC_TIMEOUT_ERR;
		}
	}

	return (err);
}

AMCLIB_API int set_position_lpf(int ax, int nfreq)
{
	return __set_filter(ax, 0, 0, nfreq);
}
AMCLIB_API int set_position_notch_filter(int ax, int nfreq)
{
	return __set_filter(ax, 0, 3, nfreq);
}
AMCLIB_API int set_velocity_lpf(int ax, int nfreq)
{
	return __set_filter(ax, 1, 0, nfreq);
}
AMCLIB_API int set_velocity_notch_filter(int ax, int nfreq)
{
	return __set_filter(ax, 1, 3, nfreq);
}


static int __get_filter(int ax, int where, int ntype, int *pnfreq)
{
	INT err;
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (CHAR)ax);
		AxisDpram[ax]->Int_Type[0] = where;
		AxisDpram[ax]->Int_Type[1] = ntype;

		// Wake DSP!. Send Command
		_flush_dpram(AMC_FILTER_GET);
		mmc_error = err = _wait_for_reply(3000);

		*pnfreq = (int)AxisDpram[ax]->Int_Type[2];
		MMCMutexUnlock ();

		if (err != AMC_SUCCESS)
		{
			mmc_error = MMC_TIMEOUT_ERR;
			return MMC_TIMEOUT_ERR;
		}
	}

	return (err);
}

AMCLIB_API int get_position_lpf(int ax, int *pnfreq)
{
	return __get_filter(ax, 0, 0, pnfreq);
}
AMCLIB_API int get_position_notch_filter(int ax, int *pnfreq)
{
	return __get_filter(ax, 0, 3, pnfreq);
}
AMCLIB_API int get_velocity_lpf(int ax, int *pnfreq)
{
	return __get_filter(ax, 1, 0, pnfreq);
}
AMCLIB_API int get_velocity_notch_filter(int ax, int *pnfreq)
{
	return __get_filter(ax, 1, 3, pnfreq);
}
