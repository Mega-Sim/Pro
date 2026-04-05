#include "stdafx.h"
#include "amclib.h"

#if 0
AMCLIB_API AMCBOOL in_sequence(int axis)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return 0;
	return _read_dpramreg(AXIS_BASEADDR(axis)+3);
}

//extern "C" __declspec(dllexport) int in_sequence(int axis)
//{
//	return amc_in_sequence(axis);
//}

//extern "C" __declspec(dllexport) int in_motion(int axis)
//{
//	/*
//	축의 속도명령이 0이면 0, 아니면 1을 돌려 준다.
//	*/
//	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return 0;
//	return 1;
//}

AMCLIB_API int in_postion(int axis)
{
	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return 0;
	return 1;
}

//extern "C" __declspec(dllexport) int motion_done(int axis)
//{
//	/*
//	!in_sequence && !in_motion 일때 1을 돌려준다.
//	*/
//	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return 0;
//	return 1;
//}


//extern "C" __declspec(dllexport) int axis_done(int axis)
//{
//	/*
//	motion_donw && in_position일때 1을 돌려 준다.
//	*/
//	if (g_stAMCData.hDevice==INVALID_HANDLE_VALUE) return 0;
//	return 1;
//}

#endif

