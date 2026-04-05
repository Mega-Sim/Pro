
#include "pcdef.h"
#include "amc_internal.h"
#include "log.h"

/**********
*	FUNCTION NAME	: get_counter(INT ax, double *pos)
*	FUNCTION       : Get Feecback Position From DSP
*********************************************************************/
INT get_counter(INT ax, pDOUBLE pos)
{
	LONG	pos_d;
	INT err;

	if(mmc_error = err = CDLRead(ax,&pos_d,GET_A_POSITION))	return err;
	*pos=(double)pos_d;
	mmc_error = MMC_OK;
	return	MMC_OK;
}
/**********
*	FUNCTION NAME	: get_sync_position(double *pos_m, double *pos_s)
*	FUNCTION       : Get Synchronous Feecback Position From DSP
*********************************************************************/
INT get_sync_position(pDOUBLE pos_m, pDOUBLE pos_s)
{
	LONG	pos_d[2];
	INT err;

	if(mmc_error = err = CDLRead(SyncMotion.Master,pos_d,GET_ES_POSITION))	return err;
	*pos_m=(double)pos_d[0];
	*pos_s=(double)pos_d[1];
	mmc_error = MMC_OK;
	return	MMC_OK;
}


/**********
*	FUNCTION NAME	: set_cleanvoltage
*	FUNCTION       : 
*********************************************************************/
int set_cleanvoltage(double cv)
{
	return (CDFWrite(0, (float)(cv), PUT_CLEANVOLTAGE));
}

/**********
*	FUNCTION NAME	: set_position(INT ax, double pos)
*	FUNCTION       : Set Actual & Counter Register Position To DSP
*********************************************************************/
INT set_position(INT ax, double pos)
{
	return (CDLWrite(ax,(long)(pos),PUT_POSITION));
}
/**********
*	FUNCTION NAME	: get_position(INT ax, double *pos)
*	FUNCTION       : Get Actual Position From DSP
*********************************************************************/
INT get_position(INT ax, pDOUBLE pos)
{
    MYLOG("get_position\n");
    
    LONG	pos_d;
	INT err;
	if(mmc_error = err = CDLRead(ax,&pos_d,GET_E_POSITION))	return err;
	*pos=(double)pos_d;
	mmc_error = MMC_OK;
	return	MMC_OK;

}

/**********
*	FUNCTION NAME	: set_command(INT ax, double pos)
*	FUNCTION       : Set Actual & Counter Register Position To DSP
*********************************************************************/
INT set_command(INT ax, double pos)
{
	return (CDLWrite(ax,(long)(pos),PUT_C_POSITION));
}
/**********
*	FUNCTION NAME	: get_command(INT ax, double *pos)
*	FUNCTION       : Get Desired Position From DSP
*********************************************************************/
INT get_command(INT ax, pDOUBLE pos)
{
	long	pos_d;
	INT err;

	if(mmc_error = err = CDLRead(ax,&pos_d,GET_C_POSITION))	return err;
	*pos=(double)pos_d;
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: get_error(INT ax, double *pos)
*	FUNCTION       : Get Desired Position From DSP
*********************************************************************/
INT get_error(INT ax, pDOUBLE error)
{
	float	error_f;
	INT err;

	if(mmc_error = err = CDFRead(ax,&error_f,GET_ERROR))	return	err;
	*error=(double)error_f;
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: get_com_velocity(INT ax, double *pos)
*	FUNCTION       : Get Command Vel From DSP
*                    MMC_TIMEOUT_ERR = MutexLock Error
*********************************************************************/
#ifndef MDF_FUNC
INT get_com_velocity(INT ax)
{
	INT err, comv, *pi;
    //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
    if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR;
	pi = (int *) &AxisDpram[ax]->Long_Type1;
	read_dpram_int4_filtering(pi, &comv);
	MMCMutexUnlock ();
	return	comv;
}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경

INT get_com_velocity(INT ax, pINT chk_err)
{
	INT err, comv, *pi;
	char filter_i=0;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
        //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
        *chk_err = MMC_MUTEXLOCK_ERROR;
		return 0;
	}
	pi = (int *) &AxisDpram[ax]->Long_Type1;

//v2.9.04 , 20120607 syk 필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &comv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		*chk_err = err; 
		return 0;
	}

	MMCMutexUnlock ();
	
	*chk_err = MMC_OK;
	return	comv;
}

#endif

/**********
*	FUNCTION NAME	: get_act_velocity(INT ax, double *pos)
*	FUNCTION       : Get Actual Vel From DSP
*                    MMC_TIMEOUT_ERR = MutexLock Error
*********************************************************************/
#ifndef MDF_FUNC	
INT get_act_velocity(INT ax)
{
	INT	actv, *pi;
	int err;							
    //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR; 
	pi = (int *) &AxisDpram[ax]->Float_Type1;	
	read_dpram_int4_filtering(pi, &actv);		
	MMCMutexUnlock ();
	return actv;
}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT get_act_velocity(INT ax, pINT chk_err)
{
	INT	actv, *pi;
	int err;	
	char filter_i=0;
	
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) 
	{
        //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
        *chk_err = MMC_MUTEXLOCK_ERROR; 
		return 0;
	}
	pi = (int *) &AxisDpram[ax]->Float_Type1;	

//v2.9.04 , 20120607 syk 필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		*chk_err = err; 
		return 0;
	}

	MMCMutexUnlock ();

	*chk_err = MMC_OK;
	return actv;
}
#endif

/**********
*	FUNCTION NAME	: get_com_velocity_rpm(INT ax, double *pos)
*	FUNCTION       : Get Command Vel(RPM) From DSP
*                    MMC_TIMEOUT_ERR = MutexLock Error
*********************************************************************/
#ifndef MDF_FUNC	
INT get_com_velocity_rpm(INT ax)
{
	INT2 err, comv, *pi;
    //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
    if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR; 
	pi = (INT2 *) &AxisDpram[ax]->Vel;
	read_dpram_int_filtering(pi, &comv);
	MMCMutexUnlock ();

	return	comv;
}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT get_com_velocity_rpm(INT ax, pINT chk_err)
{
	INT2 err, comv, *pi;
	char filter_i=0;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
        //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
        *chk_err = MMC_MUTEXLOCK_ERROR; 
		return 0;
	}
	pi = (INT2 *) &AxisDpram[ax]->Vel;

//v2.9.04 , 20120607 syk 필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int_filtering(pi, &comv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		*chk_err = err; 
		return 0;
	}

	MMCMutexUnlock ();

	*chk_err = MMC_OK;
	return	comv;

}
#endif


/**********
*	FUNCTION NAME	: get_com_velocity(INT ax, double *pos)
*	FUNCTION       : Get Actual Vel(RPM) From DSP
*                    MMC_TIMEOUT_ERR = MutexLock Error
*********************************************************************/
#ifndef MDF_FUNC	
INT get_act_velocity_rpm(INT ax)
{
	INT2 err, actv, *pi;
    //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
    if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR; 
	pi = (INT2 *) &AxisDpram[ax]->Actual_Vel;
	read_dpram_int_filtering(pi, &actv);
	MMCMutexUnlock ();

	return	actv;
}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT get_act_velocity_rpm(INT ax, pINT chk_err)
{
	INT2 err, actv, *pi;
	char filter_i=0;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
        //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
        *chk_err = MMC_MUTEXLOCK_ERROR; 
		return 0;
	}
	pi = (INT2 *) &AxisDpram[ax]->Actual_Vel;

//v2.9.04 , 20120607 syk 필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int_filtering(pi, &actv);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		*chk_err = err; 
		return 0;
	}

	MMCMutexUnlock ();

	*chk_err = MMC_OK;
	return	actv;

}
#endif


/**********
*	FUNCTION NAME	: get_error_status(INT ax, pINT pStatus)
*	FUNCTION       : 실행중 발생하는 에러가 있는지를 검사한다. (축번호와는 관계없음)
*********************************************************************/
INT get_error_status(INT ax, pINT pStatus)
{
	INT err;
	if ((mmc_error = err = MMCMutexLock ()) == MMC_OK) 
	{
		_write_dpramreg(AXIS_REG, (CHAR)ax);

		// Wake DSP!. Send Command
		_flush_dpram(GET_ERROR_STATUS);
		mmc_error = err = _wait_for_reply(3000);
		*pStatus = AxisDpram[ax]->Long_Type;

		MMCMutexUnlock();
	}	
	return err;
}

/**********
*	FUNCTION NAME	: get_fw_version(INT ax, pINT pStatus)
*	FUNCTION       : slave의 firmware 버전을 읽는다.
                     0 : RSA Driving Rear
					 1 : RSA Driving Front
					 2 : RSA Hoist
					 3 : RSA Slide
					 4 : Fine Slave Rear
					 5 : Fine Slave Front
*********************************************************************/
INT get_fw_version(INT nSlaveNum, pINT pVer)
{
	
	INT err;
	if ((mmc_error = err = MMCMutexLock()) == MMC_OK)
	{
		_write_dpramreg(AXIS_REG, (CHAR)nSlaveNum);

		// Wake DSP!. Send Command
		_flush_dpram(GETVERSION);
		mmc_error = err = _wait_for_reply(3000);
		*pVer = AxisDpram[0]->Long_Type;

		MMCMutexUnlock();
	}
	return err;
}

/**********
*	FUNCTION NAME	: get_ecm_version(pINT pStatus)
*	FUNCTION       : EC-Master의 firmware 버전을 읽는다.
*********************************************************************/
INT get_ecm_version(pINT pVer)
{
	INT err;
	if ((mmc_error = err = MMCMutexLock()) == MMC_OK)
	{
		// Wake DSP!. Send Command
		_flush_dpram(GETECMVERSION);
		mmc_error = err = _wait_for_reply(3000);
		*pVer = AxisDpram[0]->Long_Type;

		MMCMutexUnlock();
	}
	return err;
}
