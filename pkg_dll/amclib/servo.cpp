#include "stdafx.h"
#include "amclib.h"

#if 0
AMCLIB_API int servo_on(int axis)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	int ret;
	_write_dpramreg(AXIS_REG, axis);
	
	FLUSH(AMCMD_SERVO_ON);
	ret = _wait_for_reply(g_stAMCData.nTimeout);
	if(ret != AMC_SUCCESS) return ret;

	return AMC_SUCCESS;
}

AMCLIB_API int servo_off(int axis)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return AMC_NOTOPENED;

	int ret;
	_write_dpramreg(AXIS_REG, axis);
	
	FLUSH(AMCMD_SERVO_OFF);
	ret = _wait_for_reply(g_stAMCData.nTimeout);
	if(ret != AMC_SUCCESS) return ret;

	return AMC_SUCCESS;
}

AMCLIB_API int set_amp_enable(int axis, int status)
{
	int ret;
	if(status==0)
		ret = servo_off(axis);
	else
		ret = servo_on(axis);
	return ret;
}
#endif
