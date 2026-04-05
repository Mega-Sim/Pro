#include "stdafx.h"
#include "amclib.h"
#include "pcdef.h"
#include "amc_internal.h"

int velocity_move(int axis, float vel, int accel)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	INT err;
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, axis);
		_write_dpramregs(DPRAM_COMM_BASEOFS+4+(4*axis), &vel, sizeof(float));
		_write_dpramregs(DPRAM_COMM_BASEOFS+36+(2*axis), &accel, sizeof(unsigned short));
		FLUSH(AMCMD_VELOCITY_MOVE);

		err = _wait_for_reply(g_stAMCData.nTimeout);
		if(err != AMC_SUCCESS) 
		{
			MMCMutexUnlock ();
			return err;
		}

		MMCMutexUnlock();
	}

	return err;
}

int p2p_move(int axis, unsigned short cmd, unsigned long pos, unsigned short acc, unsigned short dcc, float vel)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	INT err;

	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, axis);
		_write_dpramregs(DPRAM_COMM_BASEOFS, &cmd, sizeof(unsigned short));
		_write_dpramregs(DPRAM_COMM_BASEOFS + 68 + 4 * axis, &pos, sizeof(unsigned long));
		_write_dpramregs(DPRAM_COMM_BASEOFS + 52 + 2 * axis, &acc, sizeof(unsigned short));
		_write_dpramregs(DPRAM_COMM_BASEOFS + 148 + 2 * axis, &dcc, sizeof(unsigned short));
		_write_dpramregs(DPRAM_COMM_BASEOFS + 4 + 4 * axis, &vel, sizeof(float));

		_flush_dpram(AMCMD_P2P_MOVE);

		err = _wait_for_reply(g_stAMCData.nTimeout);
		if(err != AMC_SUCCESS)
		{
			MMCMutexUnlock ();
			return err;
		}

		MMCMutexUnlock();
	}

	return err;
}

int v_move(int axis, float velo, int accel)
{
	return velocity_move(axis, velo, accel);
}

