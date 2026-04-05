
#include  	"pcdef.h"
#include	"amc_internal.h"
#include	"log.h"

/**********
*	FUNCTION NAME	: in_sequence(INT ax)
*	FUNCTION       : TRUE = axis in motion process
*                    FALSE = not in position
*                    MMC_TIMEOUT_ERR = MutexLock Error 
*********************************************************************/
#ifndef MDF_FUNC	
INT in_sequence(INT ax)
{
	INT err;
	unsigned char status, *pi;
    //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
    if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return  MMC_MUTEXLOCK_ERROR; 
	pi = (unsigned char *) &AxisDpram[ax]->In_Sequence;
	read_dpram_char_filtering(pi, &status);
	MMCMutexUnlock ();
	if(status)		return	TRUE;
	return	FALSE;
}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT in_sequence(INT ax, pINT chk_err)
{
    MYLOG("in_sequence\n");
    
    INT err;
	unsigned char status, *pi;
	char filter_i=0;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
        //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
        *chk_err = MMC_MUTEXLOCK_ERROR; 
		return 0;
	}
	pi = (unsigned char *) &AxisDpram[ax]->In_Sequence;

//v2.9.04 , 20120607 syk 필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_char_filtering(pi, &status);
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
	if(status)		return	TRUE;
	return	FALSE;
}
#endif


/**********
*	FUNCTION NAME	: in_motion(INT ax)
*	FUNCTION       : TRUE = in motion
*                    FALSE = not in position
*                    MMC_TIMEOUT_ERR = MutexLock Error 
*********************************************************************/
#ifndef MDF_FUNC	
INT in_motion(INT ax)
{
	INT err,status2, *p4i;
	INT2 status1, *pi;
    //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR -> MMC_MUTEXLOCK_ERROR
    if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR;  
	pi = (INT2 *) &AxisDpram[ax]->Vel;
	read_dpram_int_filtering(pi, &status1);
	p4i = (int *) &AxisDpram[ax]->Long_Type1;
	read_dpram_int4_filtering(p4i, &status2);

	MMCMutexUnlock ();
	if(status1)		return	TRUE;
	if(status2)		return	TRUE;
	return	FALSE;
}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT in_motion(INT ax, pINT chk_err)
{
    MYLOG("in_motion\n");
    
    INT err,status2, *p4i;
	INT2 status1, *pi;
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
		err = read_dpram_int_filtering(pi, &status1);
		if(err==0) break;
	}

	if(err !=0)
	{
		MMCMutexUnlock ();
		*chk_err = err; 
		return 0;
	}

	p4i = (int *) &AxisDpram[ax]->Long_Type1;

//v2.9.04 , 20120607 syk 필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int4_filtering(p4i, &status2);
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
	if(status1)		return	TRUE;
	if(status2)		return	TRUE;
	return	FALSE;
}
#endif


/**********
*	FUNCTION NAME	: in_position(INT ax)
*	FUNCTION       : TRUE = in position
*                    FALSE = not in position
*                    MMC_TIMEOUT_ERR = MutexLock Error 
*********************************************************************/
#ifndef MDF_FUNC	
INT in_position(INT ax)
{
	INT err;
	unsigned char status, *pi;
    //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR -> MMC_MUTEXLOCK_ERROR
    if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR;  
	pi = (unsigned char *) &AxisDpram[ax]->In_Pos_Flag;
	read_dpram_char_filtering(pi, &status);
	MMCMutexUnlock ();
	if(status)		return	TRUE;
	return	FALSE;
}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT in_position(INT ax, pINT chk_err)
{
    MYLOG("in_position\n");
    
    INT err;
	unsigned char status, *pi;
	char filter_i=0;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
        //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
        *chk_err = MMC_MUTEXLOCK_ERROR;  
		return 0;
	}
	pi = (unsigned char *) &AxisDpram[ax]->In_Pos_Flag;

//v2.9.04 , 20120607 syk 필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_char_filtering(pi, &status);
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
	if(status)		return	TRUE;
	return	FALSE;
}
#endif


/**********
*	FUNCTION NAME	: motion_done(axis)
*	FUNCTION       : Returns TRUE if !in_motion
*********************************************************************/
INT motion_done(INT ax)
{
    MYLOG("motion_done\n");

#ifndef MDF_FUNC	
	if((!in_motion(ax)) && (!in_sequence(ax)))	return	TRUE;
	return	FALSE;
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_im_v, tmp_im_e;
	int tmp_is_v, tmp_is_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_im_v=in_motion(ax,&tmp_im_e);
		if(tmp_im_e==MMC_OK) break;
	}

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_is_v=in_sequence(ax,&tmp_is_e);
		if(tmp_im_e==MMC_OK) break;
	}

	if((!tmp_im_v) && (!tmp_is_v))	return	TRUE;
	return	FALSE;
#endif
}


/**********
*	FUNCTION NAME	: axis_done(axis)
*	FUNCTION       : Returns TRUE if motion_done && in_position
*********************************************************************/
INT axis_done(INT ax)
{
    MYLOG("axis_done\n");

#ifndef MDF_FUNC	
	if((motion_done(ax) && in_position(ax))) return TRUE;
	return	FALSE;
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경

	int tmp_ip_v, tmp_ip_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_ip_v=in_position(ax,&tmp_ip_e);
		if(tmp_ip_e==MMC_OK) break;
	}

	if(motion_done(ax) && tmp_ip_v)	return	TRUE;
	return	FALSE;

#endif	
}

/**********
*	FUNCTION NAME	: axis_state(ax)
*	FUNCTION       : Returns the cause of an exception event
*********************************************************************/
#ifndef MDF_FUNC	
INT axis_state(INT ax)
{
	INT	st;

	CDIRead(ax, &st, GET_AXIS_STAT);
	return	st;
}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT axis_state(INT ax, pINT chk_err)
{
    MYLOG("axis_state\n");
    
    INT	st;

	*chk_err= CDIRead(ax, &st, GET_AXIS_STAT);
	return	st;
}
#endif


/**********
*	FUNCTION NAME	: axis_source(ax)
*	FUNCTION       : Returns the cause of an exception event
*********************************************************************/
#ifndef MDF_FUNC
INT axis_source(INT ax)
{
	INT2 err, status, *pi;
    //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR -> MMC_MUTEXLOCK_ERROR
    if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return MMC_MUTEXLOCK_ERROR;  
	pi = (INT2 *) &AxisDpram[ax]->AxisSource;
	read_dpram_int_filtering(pi, &status);
	MMCMutexUnlock ();
	return	status;
}	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT axis_source(INT ax, pINT chk_err)
{
    MYLOG("axis_source\n");
    
    INT2 err, status, *pi;
	char filter_i=0;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
        //DSP TIMEOUT ERROR 와 구분하기위해 ERROR 변경 MMC_TIMEOUT_ERR->MMC_MUTEXLOCK_ERROR
        *chk_err = MMC_MUTEXLOCK_ERROR;  
		return 0;
	}
	pi = (INT2 *) &AxisDpram[ax]->AxisSource;

//v2.9.04 , 20120607 syk 필터링 방식 변경 -> 1회 5번 read , 총 3회.
	for(filter_i =0;filter_i <3; filter_i++)
	{
		err = read_dpram_int_filtering(pi, &status);
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
	return	status;
}
#endif

#if defined(__AMC_V70)
/**********
*	FUNCTION NAME	: system_status(pCHAR p_status)
*	FUNCTION       : Returns power monitering status
*********************************************************************/
INT system_status(pCHAR p_status)
{
	INT	err;
	INT ax =1;
	INT	bn,jnt;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
		if ((mmc_error = err = MMCCommCheck (1, &bn, GET_SYSTEM_STATUS, jnt)) == MMC_OK)
			*p_status = (char)AxisDpram[ax]->Char_Type[1];

	MMCMutexUnlock();
	return	err;

}

/**********
*	FUNCTION NAME	: system_moni_enable(char axno,char state)
*	FUNCTION       : set power monitering enable/disable
*********************************************************************/
INT system_moni_enable(char axno,char state) // axno : 0 ~ 3 , state = 0(FALSE),1(TRUE)
{

	int err;
	INT comm;
	int ax =1;

	if(axno<POWER3P3 || axno>POWER12M)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if(state< FALSE || state> TRUE) 
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return   MMC_ILLEGAL_PARAMETER;
	}

	if ((err = MMCMutexLock ()) != MMC_OK)
	{
		mmc_error = err;
		return err;
	}
	// 모니터링 enable/disable선택 
	if(state==TRUE)				comm = SET_SYSTEM_MONI_ENABLE;	// comm = 192 
	if(state==FALSE)			comm = SET_SYSTEM_MONI_DISABLE;	// comm = 193
										
	_write_dpramreg(AXIS_REG, (CHAR)ax);
	AxisDpram[ax]->Char_Type[1] = (char)axno;		// 모니터링 소스 선택 
	_flush_dpram((CHAR)comm);
	if(_wait_for_reply(3000) != AMC_SUCCESS)
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}

	MMCMutexUnlock();
	mmc_error = MMC_OK;
	return	MMC_OK;
}

INT get_system_moni_enable(char *state) 
{
	INT	err;
	INT ax =1;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;

	_flush_dpram(GET_SYSTEM_MONI_ENABLE);
	if(_wait_for_reply(3000) != AMC_SUCCESS)
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}

	*state = AxisDpram[ax]->Char_Type[1];

	MMCMutexUnlock();
	mmc_error = MMC_OK;
	return	MMC_OK;
}


INT get_system_monitering_value(char axno,int *val, int *raw_val, int *compare_val) // axno : 0 ~ 7 , val = 전압(0~3), ADCTODAC(4~7)
{
	INT	bn, jnt, err;
	int ax =1;

	if(axno<POWER3P3 || axno>AXIS3_VEL)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Char_Type[1] = (char)axno;
		if ((mmc_error = err = MMCCommCheck (1, &bn, GET_SYSTEM_MONI_VALUE, jnt)) == MMC_OK)
		{
			*val = AxisDpram[ax]->Long_Type;
			*raw_val = (int)AxisDpram[ax]->Int_Type[3] & 0xffff;
			*compare_val =  (int)AxisDpram[ax]->Int_Type[4];
		}
	}
	MMCMutexUnlock ();
	return	err;
}


INT set_monitering_Threshold_percent(char axno,char pcnt) // axno : 0 ~ 3
{
	INT	err;
	int ax =1;

	if(axno<POWER3P3 || axno>POWER12M)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if(pcnt<PERCENT0 || pcnt>PERCENT100)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;


	_write_dpramreg(AXIS_REG, (CHAR)ax);
	AxisDpram[ax]->Char_Type[1] = (char)axno;		// 모니터링 소스 선택 
	AxisDpram[ax]->Char_Type[0] = (char)pcnt;		// 모니터링 소스 선택 

	_flush_dpram(SET_MONITERING_THRESHOLD_PERCENT);
	if(_wait_for_reply(3000) != AMC_SUCCESS)
	{
		MMCMutexUnlock ();
		mmc_error = MMC_TIMEOUT_ERR;
		return MMC_TIMEOUT_ERR;
	}

	MMCMutexUnlock ();
	mmc_error = MMC_OK;
	return	err;
}


INT get_monitering_Threshold_percent(char axno,char *pcnt) // axno : 0 ~ 3
{
	INT	bn, jnt, err;
	int ax =1;

	if(axno<POWER3P3 || axno>POWER12M)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Char_Type[1] = (char)axno;
		if ((mmc_error = err = MMCCommCheck (1, &bn, GET_MONITERING_THRESHOLD_PERCENT, jnt)) == MMC_OK)
		{
			*pcnt = AxisDpram[ax]->Char_Type[0];
		}
	}
	MMCMutexUnlock ();
	return	err;
}
#endif

/**********
*	FUNCTION NAME	: clear_status(ax)
*	FUNCTION       : Reset controller status on an axis
*********************************************************************/
INT clear_status(INT ax)
{
    MYLOG("clear_status\n");
    
    INT err;
#ifndef MDF_FUNC	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_im_v, tmp_im_e;
	int loop_i=0;
#endif
    
    //주행중 servo off시키고 clear_status를 바로 실행하면 dsp에서 아직 vel값이 갱신되지 않을수 있다.
	Sleep(1);				

#ifndef MDF_FUNC	
	if(err = in_motion(ax))
	{
		mmc_error = MMC_ON_MOTION;
		return	MMC_ON_MOTION;
	}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_im_v=in_motion(ax,&tmp_im_e);
		if(tmp_im_e==MMC_OK) break;
	}
    err= tmp_im_v;

	if(tmp_im_v)
	{
		mmc_error = MMC_ON_MOTION;
		return	MMC_ON_MOTION;
	}
#endif
	err = CommWrite(ax,PUT_CLEAR_STATUS);
	mmc_error = err;
	return err;
}

/**********
*	FUNCTION NAME	: clear_stop(ax)
*	FUNCTION       : Reset controller stop status on an axis
*********************************************************************/
INT clear_stop(INT ax)
{
	MYLOG("clear_stop\n");
    
    INT err;
#ifndef MDF_FUNC	
	if(mmc_error = err = in_motion(ax))
	{
		mmc_error = MMC_ON_MOTION;
		return	MMC_ON_MOTION;
	}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_im_v, tmp_im_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_im_v=in_motion(ax,&tmp_im_e);
		if(tmp_im_e==MMC_OK) break;
	}
    mmc_error= err= tmp_im_v;

	if(tmp_im_v)
	{
		mmc_error = MMC_ON_MOTION;
		return	MMC_ON_MOTION;
	}
#endif
	err = CommWrite(ax,PUT_CLEAR_STOP);
	mmc_error= err;
	return err;
}

/**********
*	FUNCTION NAME	: frames_clear(ax)
*	FUNCTION       : Reset Frame Command
*********************************************************************/
INT frames_clear(INT ax)
{
    MYLOG("frames_clear\n");
    
    if((ax >= TOTAL_AXIS_NUM) || ax<0)
	{
		mmc_error = MMC_INVALID_AXIS;
		return	MMC_INVALID_AXIS;
	}
    
    if(CommWrite(ax, PUT_FRAMES_CLEAR))	return	mmc_error;
	
    mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: frames_left(INT ax)
*	FUNCTION       : Returns TRUE if frames left
*********************************************************************/
#ifndef MDF_FUNC
INT frames_left(INT ax)
{
	INT		left;

	if(CDIRead(ax,&left,GET_EMPTY_FRAME))	return	FUNC_ERR;
	return	left;
}	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
INT frames_left(INT ax, pINT chk_err)
{
    MYLOG("frames_left\n");
    
    INT		left, err;

	if(err= CDIRead(ax,&left,GET_EMPTY_FRAME))
	{
		*chk_err =err;
		return	FUNC_ERR;
	}

	*chk_err = MMC_OK;
	return	left;
}
#endif


INT axis_all_status(int ax, int *pIStatus, int *pIO64, double *pValue)
{

#ifndef MDF_FUNC
	pIStatus[0] = axis_source(ax);
	pIStatus[1] = in_sequence(ax);
	pIStatus[2] = get_com_velocity(ax);
	pIStatus[3] = get_act_velocity(ax);
	pIStatus[5] = in_position(ax);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5유형 사용자open함수 원형 변경으로 인해 수정
	int chk_err_aas;
/////////////////////////////////////////////////////////////
	pIStatus[0] = axis_source(ax,&chk_err_aas);
	if(chk_err_aas !=MMC_OK)	return chk_err_aas;
/////////////////////////////////////////////////////////////
	pIStatus[1] = in_sequence(ax,&chk_err_aas);
	if(chk_err_aas !=MMC_OK)	return chk_err_aas;
/////////////////////////////////////////////////////////////
	pIStatus[2] = get_com_velocity(ax,&chk_err_aas);
	if(chk_err_aas !=MMC_OK)	return chk_err_aas;
/////////////////////////////////////////////////////////////
	pIStatus[3] = get_act_velocity(ax,&chk_err_aas);
	if(chk_err_aas !=MMC_OK)	return chk_err_aas;
/////////////////////////////////////////////////////////////
	pIStatus[5] = in_position(ax,&chk_err_aas);
	if(chk_err_aas !=MMC_OK)	return chk_err_aas;
#endif

	pIStatus[4] = motion_done(ax);
	pIStatus[6] = axis_done(ax);

	chk_err_aas = get_io64(0, &pIO64[0]);
	if(chk_err_aas !=MMC_OK)	return chk_err_aas;

	chk_err_aas = get_out64(0, &pIO64[2]);
	if(chk_err_aas !=MMC_OK)	return chk_err_aas;

	get_position(ax, &pValue[0]);
	get_command(ax, &pValue[1]);
	get_error(ax, &pValue[2]);

	return MMC_OK;
}

