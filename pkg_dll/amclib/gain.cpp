#include "stdafx.h"
#include "amclib.h"

#include "pcdef.h"
#include "amc_internal.h"

#define PGAIN_OFFSET	16
#define IGAIN_OFFSET	18
#define DGAIN_OFFSET	20
#define FGAIN_OFFSET	22
#define ILIMIT_OFFSET	24


extern INT mmc_error;

int set_gain(int axis, const struct amc_gain *gv)
{
	INT err;
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK)
	{
		_write_dpramreg(AXIS_REG, axis);
		_write_dpramregs(AXIS_BASEADDR(axis)+ PGAIN_OFFSET, gv, sizeof(struct amc_gain));
		FLUSH(AMCMD_SET_GAIN);
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

int get_gain(int axis, struct amc_gain *gv)
{
	INT err;
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK)
	{
		_write_dpramreg(AXIS_REG, axis);
		FLUSH(AMCMD_GET_GAIN);
		mmc_error = err = _wait_for_reply(g_stAMCData.nTimeout);
		if(err != AMC_SUCCESS) 
		{
			MMCMutexUnlock();
			return err;
		}

		_read_dpramregs(AXIS_BASEADDR(axis)+ PGAIN_OFFSET, gv, sizeof(struct amc_gain));

		MMCMutexUnlock();
	}
	return err;
}

int set_vgain(int axis, const struct amc_gain *gv)
{
	INT err;
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK)
	{
		_write_dpramreg(AXIS_REG, axis);
		_write_dpramregs(AXIS_BASEADDR(axis)+ PGAIN_OFFSET, gv, sizeof(struct amc_gain));
		FLUSH(AMCMD_SET_VGAIN);
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

int get_vgain(int axis, struct amc_gain *gv)
{
	INT err;
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK)
	{
		_write_dpramreg(AXIS_REG, axis);
		FLUSH(AMCMD_GET_VGAIN);
		mmc_error = err = _wait_for_reply(g_stAMCData.nTimeout);
		if(err != AMC_SUCCESS) 
		{
			MMCMutexUnlock();
			return err;
		}

		_read_dpramregs(AXIS_BASEADDR(axis)+ PGAIN_OFFSET, gv, sizeof(struct amc_gain));

		MMCMutexUnlock();
	}
	return err;
}


int set_vfilter(int axis, int coeff[5])
{
	struct amc_gain gv;
	gv.pgain = coeff[0];
	gv.igain = coeff[1];
	gv.dgain = coeff[2];
	gv.fgain = coeff[3];
	gv.ilimit = coeff[4];
	return set_vgain(axis, &gv);
}

int get_vfilter(int axis, int coeff[5])
{
	int ret;
	struct amc_gain gv;
	
	ret = get_vgain(axis, &gv);
	coeff[0] = gv.pgain;
	coeff[1] = gv.igain;
	coeff[2] = gv.dgain;
	coeff[3] = gv.fgain;
	coeff[4] = gv.ilimit;

	return ret;
}