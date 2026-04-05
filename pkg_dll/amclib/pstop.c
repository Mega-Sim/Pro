
#include  	"pcdef.h"
#include	"amc_internal.h"//2011.10.8 Warning Á¦°Å¿ë


/**********
*	FUNCTION NAME	: set_stop(INT ax)
*	FUNCTION       : Generate a STOP_EVENT for an axis
*********************************************************************/
INT set_stop(INT ax)
{
	return(CommWrite(ax,PUT_STOP_EVENT));
}

/**********
*	FUNCTION NAME	: set_stop_rate(INT ax, INT rate)
*	FUNCTION       : Set deceleration rate for a STOP_EVENT
*********************************************************************/
INT set_stop_rate(INT ax, INT rate)
{
	INT	bn,jnt;
	INT err;
	
	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))			return	err;
	if(rate<0 || rate>BootFrame[bn].Accel_Limit[jnt])
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	return(CDINoBootWrite(ax,rate,PUT_STOP_RATE));
}

INT fset_stop_rate(INT ax, INT rate)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))			return	err;
	if(rate<0 || rate>BootFrame[bn].Accel_Limit[jnt])
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	return(CDIWrite(ax,rate,PUT_STOP_RATE));
}

/**********
*	FUNCTION NAME	: get_stop_rate(INT ax, INT rate)
*	FUNCTION       : Get deceleration rate for a STOP_EVENT
*********************************************************************/
INT get_stop_rate(INT ax, pINT rate)
{
	return(CDIRead(ax,rate,GET_STOP_RATE));
}

INT fget_stop_rate(INT ax, pINT rate)
{
	INT	bn,jnt;
	INT err;
	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))			return	err;

	*rate=(int)BootFrame[bn].Stop_Rate[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_e_stop(INT ax)
*	FUNCTION       : Generate a E_STOP_EVENT for an axis
*********************************************************************/
INT set_e_stop(INT ax)
{
	return(CommWrite(ax,PUT_E_STOP_EVENT));
}

/**********
*	FUNCTION NAME	: set_e_stop_rate(INT ax, INT rate)
*	FUNCTION       : Set deceleration rate for a E_STOP_EVENT
*********************************************************************/
INT set_e_stop_rate(INT ax, INT rate)
{
	INT	bn,jnt;
	INT err;
	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))			return	err;
	if(rate<0 || rate>BootFrame[bn].Accel_Limit[jnt])
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return(CDINoBootWrite(ax,rate,PUT_E_STOP_RATE));
}

INT fset_e_stop_rate(INT ax, INT rate)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))			return	err;
	if(rate<0 || rate>BootFrame[bn].Accel_Limit[jnt])
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return(CDIWrite(ax,rate,PUT_E_STOP_RATE));
}

/**********
*	FUNCTION NAME	: get_e_stop_rate(INT ax, INT *rate)
*	FUNCTION       : Get deceleration rate for a E_STOP_EVENT
*********************************************************************/
INT get_e_stop_rate(INT ax, pINT rate)
{
	return(CDIRead(ax,rate,GET_E_STOP_RATE));
}

INT fget_e_stop_rate(INT ax, pINT rate)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))			return	err;
	*rate=(int)BootFrame[bn].E_Stop_Rate[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}

INT torque_limit(INT ax, INT on1off0)
{
	INT err;
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (unsigned char)ax);
		AxisDpram[0]->Int_Type[0] = on1off0;

		// Wake DSP!. Send Command
		_flush_dpram(AMC_TORQUE_LIMIT);
		mmc_error = err = _wait_for_reply(3000);

		MMCMutexUnlock ();
	}
	return err;
}


