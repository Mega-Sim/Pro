
#include  	"pcdef.h"
#include	"amc_internal.h"
#include	"log.h"


/**********
*	FUNCTION NAME	: set_amp_enable_level(INT axno, INT level)
*	FUNCTION       : Set Amp Power On Level
*********************************************************************/
INT set_amp_enable_level(INT axno,INT level)
{
	return(CDIWrite(axno,level,PUT_SERVO_ON_LEVEL));
}

INT fset_amp_enable_level(INT axno,INT level)
{
	return(CDIWrite(axno,level,PUT_SERVO_ON_LEVEL));
}

/**********
*	FUNCTION NAME	: get_amp_enable_level(INT axno, INT *level)
*	FUNCTION       : Get Amp Power On Level
*********************************************************************/
INT get_amp_enable_level(INT axno,pINT level)
{
	return(CDIRead(axno,level,GET_SERVO_ON_LEVEL));
}


/**********
*	FUNCTION NAME	: set_amp_enable(INT axno, INT state)
*	FUNCTION       : Amp Enable(1)/Disable(0)
*********************************************************************/
static UCHAR __amp_on_secure_key[8] = {0x20,0x10,0x12,0x01,0x75,0x31,0x30,0x39};
static UCHAR __amp_off_secure_key[8]= {0xDF,0xEF,0xED,0xFE,0x8A,0xCE,0xCF,0xC6};

INT set_amp_enable(INT axno,INT state)
{
	//----------------------------------------------------------------
	// 이전 amp on/off 방법이 일반 커멘드처리와 같이 dsp 단독 커멘드
	// 기록방식 --> dsp + FPGA 방식으로 변경되어 기존 루틴을 재사용할수
	// 없어 새로 작성되었다.
    MYLOG("Axis[%d] set_amp_enable -> ", axno);
    if(state)
        MYLOG("Enable\n");
    else
        MYLOG("Disable\n");
	
    INT	bn,jnt;

	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;

	if ((mmc_error=MMCMutexLock())==MMC_OK)
	{
		if ((mmc_error=Find_Bd_Jnt (axno, &bn, &jnt))==MMC_OK)
		{
			INT	j;
			INT comm;
			if(state==TRUE)
			{
				comm = MSERVO_ON;
				// AMP_ON의 경우: DSP보다 FPGA가 먼저 동작하도록 하여야함
				//(1)---secure key write
				for (j=0; j<8; j++) 
					_write_dpramreg(DPOFS_AMPON_BASE+axno, __amp_on_secure_key[j]);

				//(2)---write command
				_write_dpramreg(AXIS_REG, (CHAR)axno);
				_flush_dpram((CHAR)comm);
				if(_wait_for_reply(3000) == AMC_SUCCESS)
					mmc_error = MMC_OK;
				else
					mmc_error = MMC_TIMEOUT_ERR;

                // timeout_err발생했는데 amc의 servo가 on되어 있는 것을 방지하기 위해(amc상태 이상으로 판단) 추가 
				if(mmc_error == MMC_TIMEOUT_ERR)  
				{
					comm = MSERVO_OFF;
					// AMP_OFF의 경우:  FPGA보다 DSP가 먼저 동작하도록 하여야함
					//(1)---write command
					_write_dpramreg(AXIS_REG, (CHAR)axno);
					_flush_dpram((CHAR)comm);

					//(2)---secure key write
					for (j=0; j<8; j++) 
						_write_dpramreg(DPOFS_AMPON_BASE+axno, __amp_off_secure_key[j]);
				}
			}
			else
			{
				comm = MSERVO_OFF;
				// AMP_OFF의 경우:  FPGA보다 DSP가 먼저 동작하도록 하여야함
				//(1)---write command
				_write_dpramreg(AXIS_REG, (CHAR)axno);
				_flush_dpram((CHAR)comm);
				if(_wait_for_reply(3000) == AMC_SUCCESS)
					mmc_error = MMC_OK;
				else
					mmc_error = MMC_TIMEOUT_ERR;
				//(2)---secure key write
				for (j=0; j<8; j++) 
					_write_dpramreg(DPOFS_AMPON_BASE+axno, __amp_off_secure_key[j]);
			}
		}
		MMCMutexUnlock();
	}
	return	mmc_error;
}

/**********
*	FUNCTION NAME	: amp_on(INT axno)
*	FUNCTION       : Amp Power On
*********************************************************************/
INT get_amp_enable(INT axno,pINT state)
{
	int err, status;

	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;
	
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;

	status = (int)(AxisDpram[axno]->AxisSource & ST_AMP_POWER_ONOFF);
	MMCMutexUnlock ();

	if (status)	return	*state=FALSE;
	return	*state=TRUE;
}

/**********
*	FUNCTION NAME	: amp_fault_reset(INT axno)
*	FUNCTION       : Amp Fault Clear
*********************************************************************/
INT amp_fault_reset(INT axno)
{
    MYLOG("amp_fault_reset\n");

	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;
	return(CommWrite(axno,ALARM_RESET));
}

/**********
*	FUNCTION NAME	: amp_fault_set(INT axno)
*	FUNCTION       : Amp Fault Preset
*********************************************************************/
INT amp_fault_set(INT axno)
{
    MYLOG("Axis[%d] amp_fault_set\n", axno);

    if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;
    return(CommWrite(axno,ALARM_SET));
}

/**********
*	FUNCTION NAME	: set_control(INT axno, INT control)
*	FUNCTION       : Set Velocity/Torque Control
*********************************************************************/
INT set_control(INT axno,INT control)
{
	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;

	return(CDINoBootWrite(axno,control,PUT_VT_CONTROL));
}

INT fset_control(INT axno,INT control)
{
	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;

	return(CDIWrite(axno,control,PUT_VT_CONTROL));
}

/**********
*	FUNCTION NAME	: set_pulse_ratio(INT axno, INT pgratio)
*	FUNCTION       : Set Pulse Rate
*********************************************************************/
INT set_pulse_ratio(INT axno,INT pgratio)
{
	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;

	return(CDIWrite(axno,pgratio,PUT_PULSE_RATIO));
}

/**********
*	FUNCTION NAME	: get_pulse_ratio(INT axno, INT pgratio)
*	FUNCTION       : get Pulse rate
*********************************************************************/
INT get_pulse_ratio(INT axno,pINT pgratio)
{
	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;

	return(CDIRead(axno,pgratio,GET_PULSE_RATIO));
}

/**********
*	FUNCTION NAME	: get_control(INT axno, INT control)
*	FUNCTION       : get Velocity/Torque Control
*********************************************************************/
INT get_control(INT axno,pINT control)
{
	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;

	return(CDIRead(axno,control,GET_VT_CONTROL));
}

INT fget_control(INT axno,pINT control)
{
	INT	bn,jnt;
	INT err;
	
	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;

	if (err = Find_Bd_Jnt(axno, &bn, &jnt))
	{
		mmc_error = err;
		return err;
	}

	*control=(int)BootFrame[bn].Control_Cfg[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}
/**********
*	FUNCTION NAME	: set_electric_gear(INT axno, double ratio)
*	FUNCTION       : set electric gear ratio
*********************************************************************/
INT set_electric_gear(INT axno, double ratio)
{
	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;

	if(ratio<=0.0)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return(CDFDNoBootWrite(axno,ratio,PUT_GEAR_RATIO));
}

INT fset_electric_gear(INT axno, double ratio)
{
	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;

	if(ratio<=0.0)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return(CDFDWrite(axno,ratio,PUT_GEAR_RATIO));
}

/**********
*	FUNCTION NAME	: get_electric_gear(INT axno, FLOAT *ratio)
*	FUNCTION       : get electric gear ratio
*********************************************************************/
INT get_electric_gear(INT axno, pDOUBLE ratio)
{
	float	ratio_f;
	INT err;

	if ((axno>=TOTAL_AXIS_NUM) || axno<0) 	return	mmc_error = MMC_INVALID_AXIS;

	if(mmc_error = err = CDFRead(axno, &ratio_f, GET_GEAR_RATIO))	return	err;
	*ratio=(double)ratio_f;
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_step_mode(INT axis, INT mode)
*	FUNCTION       : set pulse out mode
*********************************************************************/
INT set_step_mode(INT axis, INT mode)
{
	if ((axis>=TOTAL_AXIS_NUM) || axis<0) 	return	mmc_error = MMC_INVALID_AXIS;

	return(CDINoBootWrite(axis,mode,PUT_PULSE_MODE));
}

INT fset_step_mode(INT axis, INT mode)
{
	if ((axis>=TOTAL_AXIS_NUM) || axis<0) 	return	mmc_error = MMC_INVALID_AXIS;

	return(CDIWrite(axis,mode,PUT_PULSE_MODE));
}

/**********
*	FUNCTION NAME	: get_step_mode(INT axis, INT *mode)
*	FUNCTION       : get pulse out mode
*********************************************************************/
INT get_step_mode(INT axis, pINT mode)
{
	if ((axis>=TOTAL_AXIS_NUM) || axis<0) 	return	mmc_error = MMC_INVALID_AXIS;

	return(CDIRead(axis,mode,GET_PULSE_MODE));
}

INT fget_step_mode(INT axis,INT *mode)
{
	INT	bn,jnt;
	INT err;

	if ((axis>=TOTAL_AXIS_NUM) || axis<0) 	return	mmc_error = MMC_INVALID_AXIS;

	if (err = Find_Bd_Jnt(axis, &bn, &jnt))	
	{
		mmc_error = err;
		return err;
	}
	*mode=(int)BootFrame[bn].PulseMode[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_sync_map_axes(INT Master, INT Slave)
*	FUNCTION       : Synchronous Motion Axis Set
*********************************************************************/
INT set_sync_map_axes(INT Master, INT Slave)
{
    MYLOG("set_sync_map_axes\n");
    
    INT	bn1,bn2,jnt1,jnt2;
	INT err;

	if(err = Find_Bd_Jnt(Master, &bn1, &jnt1))
	{
		mmc_error = err;
		return	err;
	}
	if(err = Find_Bd_Jnt(Slave , &bn2, &jnt2))
	{
		mmc_error = err;
		return	err;
	}

	if((bn1 != bn2) || (jnt1==jnt2))
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if (err = MMCMutexLock ())
	{
		mmc_error = err;
		return err;
	}
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Axis, &jnt1, 2);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Axis + 2, &jnt2, 2);

    SyncMotion.Master=Master;
	SyncMotion.Slave =Slave;

	mmc_error = err = MMCCommCheck (1, &bn1, PUT_SYNC_MAP_AXES, jnt1);
	MMCMutexUnlock();

	return	err;
}

/**********
*	FUNCTION NAME	: set_sync_control(INT condition)
*	FUNCTION       : Synchronous Motion Set or Clear
*********************************************************************/
INT set_sync_control(INT condition)
{
    MYLOG("set_sync_control - [%d]\n", condition);
    
    INT err;

	if(condition==TRUE)
	{
		if (err = CommWrite(SyncMotion.Master,PUT_SYNC_CONTROL_ON))
		{
			mmc_error = err;
			return	err;
		}
		SyncMotion.Flag=TRUE;
	}
	else{
		if (err = CommWrite(SyncMotion.Master,PUT_SYNC_CONTROL_OFF))
		{
			mmc_error = err;
			return	err;
		}
		SyncMotion.Flag=FALSE;
	}
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: get_sync_control(INT *condition)
*	FUNCTION       : Synchronous Motion Read
*********************************************************************/
INT get_sync_control(pINT condition)
{
	*condition=SyncMotion.Flag;
	return	MMC_OK;
}


/**********
*	FUNCTION NAME	: set_sync_gain(FLOAT syncgain)
*	FUNCTION       : Set Synchronous Motion Gain
*********************************************************************/
INT set_sync_gain(FLOAT syncgain)
{
    MYLOG("set_sync_gain\n");
    
    return(CDFNoBootWrite(SyncMotion.Master,syncgain,PUT_SYNC_GAIN));
}

INT fset_sync_gain(FLOAT syncgain)
{
	return(CDFNoBootWrite(SyncMotion.Master,syncgain,PUT_SYNC_GAIN));
}

/**********
*	FUNCTION NAME	: get_sync_gain(INT *syncgain)
*	FUNCTION       : Get Synchronous Motion Gain
*********************************************************************/
INT get_sync_gain(pFLOAT syncgain)
{
	return(CDFRead(SyncMotion.Master,syncgain,GET_SYNC_GAIN));
}

/**********
*	FUNCTION NAME	: compensation_pos(INT len, INT *axes, double *c_pos)
*	FUNCTION       : Vision Position Compensation
*********************************************************************/
INT compensation_pos(INT len, pINT axes, pDOUBLE c_pos,pINT c_acc)
{
	INT	i,bn,bnn,jnt[BD_AXIS_NUM],joint;
	INT err;

	if(err = Find_Bd_Jnt(axes[0], &bn, &joint))
	{
		mmc_error = err;
		return	err;
	}
	for(i=0; i<len; i++)
	{
		if(err = Find_Bd_Jnt(axes[i], &bnn, &jnt[i]))
		{
			mmc_error = err;
			return	err;
		}
		if(bn != bnn)
		{
			mmc_error = MMC_ILLEGAL_PARAMETER;
			return MMC_ILLEGAL_PARAMETER;
		}
		if(c_acc[i]<=0)
		{
			mmc_error = MMC_ILLEGAL_PARAMETER;
			return MMC_ILLEGAL_PARAMETER;
		}
	}

	if (err = MMCMutexLock ())
	{
		mmc_error = err;
		return err;
	}
	
	for(i=0; i<len; i++)
    {
		int ofs = jnt[i];
		double fval = c_pos[i];

		_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Pos + ofs * 4, (long *) &fval, 4);
		_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Acc + ofs * 2, &c_acc[i], 2);
		_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Axis + ofs * 4, &axes[i], 4);
	}
    _write_dpramregs(DPRAM_COMM_BASEOFS + CD_Len, &len, 2);
	MMCMutexUnlock ();

	return	CommWrite(axes[0],PUT_COMPENSATION_POS);
}


INT version_chk_pc()
{
	return VERSION_PCLIB;
}

/**********
*	FUNCTION NAME	: version_chk(INT bn, INT *ver)
*	FUNCTION       : Version Check
*********************************************************************/
INT version_chk(INT bn, pINT ver)
{
    MYLOG("version_chk\n");
    
    INT err;
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;

	_read_dpramregs(DPRAM_COMM_BASEOFS + CD_Ver, ver, 2);
	*ver &= 0xffff;

	MMCMutexUnlock();

	mmc_error = MMC_OK;
	return	MMC_OK;
}


