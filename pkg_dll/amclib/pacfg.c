#include "stdafx.h"
#include "amc.h"
#include	"pcdef.h"

#include	"amc_internal.h"//2011.10.8 Warning 제거용


static char *__err_msg[]=
{
	"No error!",		//0
	"Boot Memory has been corrupted",	// 1
	"DPRAM Communication error",		// 2
	"Non existent axis",				// 3
	"Illegal Analog Input Channel",		// 4
	"Illegal I/O Port",					// 5
	"Illegal Parameter",				// 6
	"Not Define Map axis",				// 7
	"AMP Fault",						// 8
	"Motion is not completed",			// 9
	"MMC Board is not exist",			// 10
	"MMC Boot File Read/Write Error",	// 11
	"MMC Checksum File Read/Write Error",	// 12
	"MMC Win NT Driver Open error",			// 13
	"Event Occured",						// 14
	"AMP Driver Power Off Status",			// 15
	"MMC Data FileSave Directory Open error",	// 16
	"MMC_INVALID_CPMOTION_GROUP",				// 17
	"Move with zero velocity or over velocity limit",		// 18
	"move with zero accel or over accel limit",				// 19
	"Function Error",
};

extern int mmc_error;
AMCLIB_API INT get_local_error()
{
	return mmc_error;
}

AMCLIB_API char * _error_message(int code)
{
	if (code > 19) code = 20;
	if (code < 0) code = 0;
	return __err_msg[code];
}
AMCLIB_API int error_message(int code, char *dst)
{
	if (dst != NULL) strcpy(dst, _error_message(code));
	return strlen(_error_message(code));
}

/**********
*	FUNCTION NAME	: set_abs_encoder(INT axis)
*	FUNCTION       : Reset Absolute Encoder Port
*********************************************************************/
INT set_abs_encoder (INT axis)
{
	INT err;
	if (mmc_error = err = CommWrite (axis, PUT_ABS_ENCODER_RESET)) return err;
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: mmc_axes(INT bdNum)
*	FUNCTION       : Read number of configured axes on board
*********************************************************************/
INT mmc_axes (INT bdNum, pINT axes)
{
	INT err;
	mmc_error = err = MMC_OK;
	if (bdNum<0 || bdNum>3) err=MMC_ILLEGAL_PARAMETER;
	else *axes = BootFrame[bdNum].Axis_Num;
	return	mmc_error = err;
}

/**********
*	FUNCTION NAME	: mmc_all_axes(void)
*	FUNCTION       : Read number of total configured axes
*********************************************************************/
INT mmc_all_axes (VOID)
{
	return	Active_Axis_Num;
}

/**********
*	FUNCTION NAME	: get_stepper(INT ax)
*	FUNCTION       : Returns TRUE if an axis is configured as a stepper
*********************************************************************/
INT get_stepper (INT ax)
{
	return	(Stepper(ax, GET_MOTOR_TYPE,STEPPER,TEMPORARY));
}

INT fget_stepper(INT ax)
{
	return	(Stepper(ax, GET_MOTOR_TYPE,STEPPER,BOOT_SAVE));
}

INT get_micro_stepper(INT ax)
{
	return	(Stepper(ax, GET_MOTOR_TYPE,MICRO_STEPPER,TEMPORARY));
}

INT fget_micro_stepper(INT ax)
{
	return	(Stepper(ax, GET_MOTOR_TYPE,MICRO_STEPPER,BOOT_SAVE));
}

/**********
*	FUNCTION NAME	: mmc_set_stepper(INT ax)
*	FUNCTION       : Configured an axis as a stepper
*********************************************************************/
INT set_stepper(INT ax)
{
	return	(Stepper(ax, SET_MOTOR_TYPE,STEPPER,TEMPORARY));
}

INT fset_stepper(INT ax)
{
	return	(Stepper(ax, SET_MOTOR_TYPE,STEPPER,BOOT_SAVE));
}

INT set_micro_stepper(INT ax)
{
	return	(Stepper(ax, SET_MOTOR_TYPE,MICRO_STEPPER,TEMPORARY));
}

INT fset_micro_stepper(INT ax)
{
	return	(Stepper(ax, SET_MOTOR_TYPE,MICRO_STEPPER,BOOT_SAVE));
}

/**********
*	FUNCTION NAME	: mmc_set_servo(INT ax)
*	FUNCTION       : Returns TRUE if an axis is configured as a servo
*********************************************************************/
INT set_servo(INT ax)
{
	return	(Stepper(ax, SET_MOTOR_TYPE,SERVO_MOTOR,TEMPORARY));
}

INT fset_servo(INT ax)
{
	return	(Stepper(ax, SET_MOTOR_TYPE,SERVO_MOTOR,BOOT_SAVE));
}

/**********
*	FUNCTION NAME	: set_feedback(INT ax, INT device)
*	FUNCTION       : Set feedback device
*********************************************************************/
INT set_feedback(INT ax, INT device)
{
	if(device<FB_ENCODER || device > FB_BIPOLAR)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return	(CDINoBootWrite(ax, device, PUT_FEEDBACK_DEVICE));
}

INT fset_feedback(INT ax, INT device)
{
	if(device<FB_ENCODER || device > FB_BIPOLAR)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return	(CDIWrite(ax, device, PUT_FEEDBACK_DEVICE));
}

/**********
*	FUNCTION NAME	: get_feedback(INT ax, INT *device)
*	FUNCTION       : Get feedback device
*********************************************************************/
INT get_feedback(INT ax, pINT device)
{
	return	(CDIRead(ax, device, GET_FEEDBACK_DEVICE));
}

INT fget_feedback(INT ax, pINT device)
{
	INT	bn,jnt;
	INT err;

	if(err = Find_Bd_Jnt(ax, &bn, &jnt))
	{
		mmc_error = err;
		return err;
	}
	*device=(int)BootFrame[bn].Encoder_Cfg[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_closed_loop(INT ax, INT loop)
*	FUNCTION       : Set closed loop control
*********************************************************************/
INT set_closed_loop(INT ax, INT loop)
{
	if(loop<OPEN_LOOP || loop > CLOSED_LOOP)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return	(CDINoBootWrite(ax, loop, PUT_CLOSED_LOOP));
}

INT fset_closed_loop(INT ax, INT loop)
{
	if(loop<OPEN_LOOP || loop > CLOSED_LOOP)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return	(CDIWrite(ax, loop, PUT_CLOSED_LOOP));
}

/**********
*	FUNCTION NAME	: get_closed_loop(INT ax, INT *loop)
*	FUNCTION       : Get closed loop control,TRUE=C,FALSE=O
*********************************************************************/
INT get_closed_loop(INT ax, pINT loop)
{
	return	(CDIRead(ax, loop, GET_CLOSED_LOOP));
}

INT fget_closed_loop(INT ax, pINT loop)
{
	INT	bn,jnt;
	INT err;

	if (err = Find_Bd_Jnt(ax, &bn, &jnt))
	{
		mmc_error = err;
		return err;
	}
	*loop=(int)BootFrame[bn].Loop_Cfg[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_unipolar(INT ax, INT state)
*	FUNCTION       : Set Unipolar voltage feedback (TRUE[unipolar])
*********************************************************************/
INT set_unipolar(INT ax, INT state)
{
	return	(CDINoBootWrite(ax, state, PUT_VOLTAGE_DEVICE));
}

INT fset_unipolar(INT ax, INT state)
{
	return	(CDIWrite(ax, state, PUT_VOLTAGE_DEVICE));
}

/**********
*	FUNCTION NAME	: is_unipolar(INT ax)
*	FUNCTION       : Returns TRUE if axis is unipolar
*********************************************************************/
#ifndef MDF_FUNC	
INT get_unipolar(INT ax)
{
	INT	state;
	INT err;

	if(err = CDIRead(ax, &state, GET_VOLTAGE_DEVICE))
	{
		mmc_error = err;
		return	err;
	}
	if(state)		return	TRUE;
	else				return	FALSE;
}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT get_unipolar(INT ax, pINT chk_err)
{
	INT	state;
	INT err;

	if(err = CDIRead(ax, &state, GET_VOLTAGE_DEVICE))
	{
		*chk_err= mmc_error = err;
		return	0;
	}

	*chk_err= MMC_OK;
	if(state)		return	TRUE;
	else				return	FALSE;
}
#endif


INT fget_unipolar(INT ax)
{
	INT	bn,jnt;
	INT err;

	if(err = Find_Bd_Jnt(ax, &bn, &jnt))	
	{
		mmc_error = err;
		return 	err;
	}
	if(BootFrame[bn].Voltage_Cfg[jnt])  return	TRUE;
	else											return	FALSE;
}

/**********
*	FUNCTION NAME	: Stepper(INT ax, INT comm, INT type, INT Boot)
*	FUNCTION       : Axis Configured
*********************************************************************/
INT 	Stepper (INT ax, INT comm, INT mtype, INT Boot)
{
	INT	type, bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		if (comm==SET_MOTOR_TYPE)
		{
			if (mtype==MICRO_STEPPER)	AxisDpram[ax]->Char_Type[0]=MICRO_STEPPER;
			else if (mtype==STEPPER)	AxisDpram[ax]->Char_Type[0]=STEPPER;
			else						AxisDpram[ax]->Char_Type[0]=SERVO_MOTOR;
		}
		if ((mmc_error = err = MMCCommCheck (1, &bn, comm, jnt)) == MMC_OK)
		{
			if (comm==GET_MOTOR_TYPE)
			{
				if (Boot==BOOT_SAVE) type = BootFrame[bn].Motor_Type[jnt];
				else				 type = AxisDpram[ax]->Char_Type[0];

				if ((type==STEPPER) && (mtype==STEPPER)) err = TRUE;
				else if ((type==MICRO_STEPPER) && (mtype==MICRO_STEPPER)) err=TRUE;
				else err = FALSE;
			}
			else if (Boot==BOOT_SAVE)
			{
				BootFrame[bn].Motor_Type[jnt]= (CHAR) mtype;
			}
		}
	}
	MMCMutexUnlock ();
	return	mmc_error = err;
}
