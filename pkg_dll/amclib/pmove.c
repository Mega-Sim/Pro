/*********************************************************************
*	FILENAME  :  PMOVE.C
*********************************************************************/

#include "pcdef.h"
#include "amc_internal.h"
#include "log.h"

/**********
*	FUNCTION NAME : start_move(axis,position,speed,accel)
*	FUNCTION      : Trapezoid profile move & Not inposition check
********************************************************************/
INT start_move( INT ax, double pos, double vel, INT acc)
{
#ifndef MDF_FUNC	
	while((frames_left(ax) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_m_v=frames_left(ax,&tmp_m_e);
		if(tmp_m_e==MMC_OK) break;
	}

	while((tmp_m_v <=(INT)0) && !mmc_error)
	{
		Delay (10);
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(ax,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}
	}
#endif	

	if(mmc_error)	return	mmc_error;
	return (PTP_Move(ax,pos,vel,acc,acc,TRAPEZOID));
}

/**********
*	FUNCTION NAME : move(axis,position,speed,accel)
*	FUNCTION      : Trapezoid profile move & inposition check
********************************************************************/
INT move( INT ax, double pos,double vel, INT acc)
{
	INT err;
	if(mmc_error = err = start_move(ax,pos,vel,acc))	return	err;
	return	(wait_for_done(ax));
}

/**********
*	FUNCTION NAME : start_r_move(axis,position,speed,accel)
*	FUNCTION      : Trapezoid profile move & Not inposition check
********************************************************************/
INT start_r_move( INT ax, double pos, double vel, INT acc)
{
#ifndef MDF_FUNC	
	while((frames_left(ax) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_m_v=frames_left(ax,&tmp_m_e);
		if(tmp_m_e==MMC_OK) break;
	}

	while((tmp_m_v <=(INT)0) && !mmc_error)
	{
		Delay (10);
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(ax,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}
	}
#endif
	if(mmc_error)	return	mmc_error;
	return (PTP_Move(ax,pos,vel,acc,acc, T_RELATIVE));
}

/**********
*	FUNCTION NAME : r_move(axis,position,speed,accel)
*	FUNCTION      : Trapezoid profile move & inposition check
********************************************************************/
INT r_move( INT ax, double pos,double vel, INT acc)
{
	INT err;
	if(mmc_error = err = start_r_move(ax,pos,vel,acc))	return	err;
	return	(wait_for_done(ax));
}

/**********
*	FUNCTION NAME : start_s_move(axis,position,speed,accel)
*	FUNCTION      : S_Curve profile move & Not inposition check
********************************************************************/
INT start_s_move( INT ax, double pos, double vel, INT acc)
{
    MYLOG("start_s_move\n");

#ifndef MDF_FUNC	
	while((frames_left(ax) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_m_v=frames_left(ax,&tmp_m_e);
		if(tmp_m_e==MMC_OK) break;
	}

	while((tmp_m_v <=(INT)0) && !mmc_error)
	{
		Delay (10);
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(ax,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}
	}
#endif	
	if(mmc_error)	return	mmc_error;
	return (PTP_Move(ax,pos,vel,acc,acc,S_CURVE));
}

/**********
*	FUNCTION NAME : s_move(axis,position,speed,accel)
*	FUNCTION      : S_Curve profile move & inposition check
********************************************************************/
INT s_move( INT ax, double pos,double vel, INT acc)
{
	INT err;
	if(mmc_error = err = start_s_move(ax,pos,vel,acc))	return	err;
	return	(wait_for_done(ax));
}

/**********
*	FUNCTION NAME : start_rs_move(axis,position,speed,accel)
*	FUNCTION      : S_Curve profile move & Not inposition check
********************************************************************/
INT start_rs_move( INT ax, double pos, double vel, INT acc)
{
#ifndef MDF_FUNC	
	while((frames_left(ax) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_m_v=frames_left(ax,&tmp_m_e);
		if(tmp_m_e==MMC_OK) break;
	}

	while((tmp_m_v <=(INT)0) && !mmc_error)
	{
		Delay (10);
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(ax,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}
	}
#endif	
	if(mmc_error)	return	mmc_error;
	return (PTP_Move(ax,pos,vel,acc,acc,S_RELATIVE));
}

/**********
*	FUNCTION NAME : rs_move(axis,position,speed,accel)
*	FUNCTION      : S_Curve profile move & inposition check
********************************************************************/
INT rs_move( INT ax, double pos,double vel, INT acc)
{
	INT err;
	if(mmc_error = err = start_rs_move(ax,pos,vel,acc))	return	err;
	return	(wait_for_done(ax));
}

/**********
*	FUNCTION NAME : start_p_move(axis,position,speed,accel)
*	FUNCTION      : Parabolic profile move & Not inposition check
********************************************************************/
INT	start_p_move( INT ax, double pos, double vel, INT acc)
{
#ifndef MDF_FUNC	
	while((frames_left(ax) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_m_v=frames_left(ax,&tmp_m_e);
		if(tmp_m_e==MMC_OK) break;
	}

	while((tmp_m_v <=(INT)0) && !mmc_error)
	{
		Delay (10);
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(ax,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}
	}
#endif
	if(mmc_error)	return	mmc_error;
	return (PTP_Move(ax,pos,vel,acc,acc,PARABOLIC));
}

/**********
*	FUNCTION NAME : p_move(axis,position,speed,accel)
*	FUNCTION      : Parabolic profile move & inposition check
********************************************************************/
INT	p_move( INT ax, double pos,double vel, INT acc)
{
	INT err;
	if(mmc_error = err = start_p_move(ax,pos,vel,acc))	return	err;
	return	(wait_for_done(ax));
}

/**********
*	FUNCTION NAME : start_t_move(axis,position,speed,accel)
*	FUNCTION      : Trapezoid profile move & Not inposition check
********************************************************************/
INT start_t_move( INT ax, double pos, double vel, INT acc, INT dcc)
{
#ifndef MDF_FUNC	
	while((frames_left(ax) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_m_v=frames_left(ax,&tmp_m_e);
		if(tmp_m_e==MMC_OK) break;
	}

	while((tmp_m_v <=(INT)0) && !mmc_error)
	{
		Delay (10);
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(ax,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}
	}
#endif	
	if(mmc_error)	return	mmc_error;
	return (PTP_Move(ax,pos,vel,acc,dcc,TRAPEZOID));
}

/**********
*	FUNCTION NAME : t_move(axis,position,speed,accel)
*	FUNCTION      : Trapezoid profile move & inposition check
********************************************************************/
INT t_move( INT ax, double pos,double vel, INT acc, INT dcc)
{
	INT err;
	if(mmc_error = err = start_t_move(ax,pos,vel,acc,dcc))	return	err;
	return	(wait_for_done(ax));
}

/**********
*	FUNCTION NAME : start_ts_move(axis,position,speed,accel)
*	FUNCTION      : S_Curve profile move & Not inposition check
********************************************************************/
INT start_ts_move( INT ax, double pos, double vel, INT acc, INT dcc)
{
#ifndef MDF_FUNC	
	while((frames_left(ax) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_m_v=frames_left(ax,&tmp_m_e);
		if(tmp_m_e==MMC_OK) break;
	}

	while((tmp_m_v <=(INT)0) && !mmc_error)
	{
		Delay (10);
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(ax,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}
	}
#endif	
	if(mmc_error)	return	mmc_error;
	return (PTP_Move(ax,pos,vel,acc,dcc,S_CURVE));
}

/**********
*	FUNCTION NAME : ts_move(axis,position,speed,accel)
*	FUNCTION      : S_Curve profile move & inposition check
********************************************************************/
INT ts_move( INT ax, double pos,double vel, INT acc, INT dcc)
{
	INT err;
	if(mmc_error = err = start_ts_move(ax,pos,vel,acc,dcc))	return	err;
	return	(wait_for_done(ax));
}

/**********
*	FUNCTION NAME : start_tr_move(axis,position,speed,accel)
*	FUNCTION      : Trapezoid profile move & Not inposition check
********************************************************************/
INT start_tr_move( INT ax, double pos, double vel, INT acc, INT dcc)
{
#ifndef MDF_FUNC	
	while((frames_left(ax) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_m_v=frames_left(ax,&tmp_m_e);
		if(tmp_m_e==MMC_OK) break;
	}

	while((tmp_m_v <=(INT)0) && !mmc_error)
	{
		Delay (10);
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(ax,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}
	}
#endif	
	if(mmc_error)	return	mmc_error;
	return (PTP_Move(ax,pos,vel,acc,dcc, T_RELATIVE));
}

/**********
*	FUNCTION NAME : tr_move(axis,position,speed,accel)
*	FUNCTION      : Trapezoid profile move & inposition check
********************************************************************/
INT tr_move( INT ax, double pos,double vel, INT acc, INT dcc)
{
	INT err;
	if(mmc_error = err = start_tr_move(ax,pos,vel,acc,dcc))	return	err;
	return	(wait_for_done(ax));
}

/**********
*	FUNCTION NAME : start_trs_move(axis,position,speed,accel)
*	FUNCTION      : S_Curve profile move & Not inposition check
********************************************************************/
INT start_trs_move( INT ax, double pos, double vel, INT acc, INT dcc)
{
#ifndef MDF_FUNC	
	while((frames_left(ax) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_m_v=frames_left(ax,&tmp_m_e);
		if(tmp_m_e==MMC_OK) break;
	}

	while((tmp_m_v <=(INT)0) && !mmc_error)
	{
		Delay (10);
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(ax,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}
	}
#endif	
	if(mmc_error)	return	mmc_error;
	return (PTP_Move(ax,pos,vel,acc,dcc,S_RELATIVE));
}

/**********
*	FUNCTION NAME : trs_move(axis,position,speed,accel)
*	FUNCTION      : Trapezoid profile move & inposition check
********************************************************************/
INT trs_move( INT ax, double pos,double vel, INT acc, INT dcc)
{
	INT err;
	if(mmc_error = err = start_trs_move(ax,pos,vel,acc,dcc))	return	err;
	return	(wait_for_done(ax));
}

/**********
*	FUNCTION NAME : start_move_all(len,*ax,*pos,*vel,*accel)
*	FUNCTION      : Multi Axes Trapezoid profile move
********************************************************************/
INT start_move_all( INT len, pINT ax, pDOUBLE pos, pDOUBLE vel, pINT acc)
{
	INT	i;
	INT err = MMC_OK;
#ifndef MDF_FUNC	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;
#endif

	for(i=0; i<len; i++)
	{
#ifndef MDF_FUNC	
		while((frames_left(i) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(i,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}

		while((tmp_m_v <=(INT)0) && !mmc_error)
		{
			Delay (10);
			for(loop_i=0; loop_i<5; loop_i++)
			{
				tmp_m_v=frames_left(i,&tmp_m_e);
				if(tmp_m_e==MMC_OK) break;
			}
		}
#endif	
		if(mmc_error)	return	mmc_error;
		if(ax[i] <0 || ax[i] >= Active_Axis_Num)
		{
			mmc_error = MMC_INVALID_AXIS;
			return MMC_INVALID_AXIS;
		}
		if(mmc_error = err = PTP_Move(ax[i], pos[i],vel[i],acc[i],acc[i],TRAPEZOID))
			return	err;
	}
	mmc_error = err;
	return	err;
}

/**********
*	FUNCTION NAME : move_all(axis,position,speed,accel)
*	FUNCTION      : Multi Axes Trapezoid profile move & pos check
********************************************************************/
INT move_all( INT len, pINT ax, pDOUBLE pos,pDOUBLE vel, pINT acc)
{
	INT err;
	if(mmc_error = err = start_move_all(len,ax,pos,vel,acc))	return	err;
	wait_for_all(len,ax);
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME : start_s_move_all(len,*ax,*pos,*vel,*accel)
*	FUNCTION      : Multi Axes S_Curve profile move
********************************************************************/
INT start_s_move_all( INT len, pINT ax, pDOUBLE pos, pDOUBLE vel, pINT acc)
{
	INT	i;
	INT err = MMC_OK;
#ifndef MDF_FUNC	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;
#endif

	for(i=0; i<len; i++){
#ifndef MDF_FUNC	
		while((frames_left(i) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(i,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}

		while((tmp_m_v <=(INT)0) && !mmc_error)
		{
			Delay (10);
			for(loop_i=0; loop_i<5; loop_i++)
			{
				tmp_m_v=frames_left(i,&tmp_m_e);
				if(tmp_m_e==MMC_OK) break;
			}
		}
#endif
		if(mmc_error)	return	mmc_error;
		if(ax[i] <0 || ax[i] >= Active_Axis_Num)
		{
			mmc_error = MMC_INVALID_AXIS;
			return MMC_INVALID_AXIS;
		}
		if(mmc_error = err = PTP_Move(ax[i], pos[i],vel[i],acc[i],acc[i],S_CURVE))
			return	err;
	}
	mmc_error = err;
	return	err;
}

/**********
*	FUNCTION NAME : s_move_all(axis,position,speed,accel)
*	FUNCTION      : Multi Axes S_Curve profile move & pos check
********************************************************************/
INT s_move_all( INT len, pINT ax, pDOUBLE pos,pDOUBLE vel, pINT acc)
{
	INT err;
	if(mmc_error = err = start_s_move_all(len,ax,pos,vel,acc))	return	err;
	wait_for_all(len,ax);
	return	err;
}

/**********
*	FUNCTION NAME : start_t_move_all(len,*ax,*pos,*vel,*accel,*decel)
*	FUNCTION      : Multi Axes S_Curve profile move
********************************************************************/
INT start_t_move_all( INT len, pINT ax, pDOUBLE pos, pDOUBLE vel, pINT acc,pINT dcc)
{
	INT	i;
	INT err;
#ifndef MDF_FUNC	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;
#endif

	for(i=0; i<len; i++)
	{
#ifndef MDF_FUNC	
		while((frames_left(i) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(i,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}

		while((tmp_m_v <=(INT)0) && !mmc_error)
		{
			Delay (10);
			for(loop_i=0; loop_i<5; loop_i++)
			{
				tmp_m_v=frames_left(i,&tmp_m_e);
				if(tmp_m_e==MMC_OK) break;
			}
		}
#endif
		if(mmc_error)	return	mmc_error;
		if(ax[i] <0 || ax[i] >= Active_Axis_Num)	
		{
			mmc_error = MMC_INVALID_AXIS;
			return MMC_INVALID_AXIS;
		}
		if(mmc_error = err = PTP_Move(ax[i], pos[i],vel[i],acc[i],dcc[i],TRAPEZOID))
			return	err;
	}
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME : t_move_all(axis,position,speed,accel,decel)
*	FUNCTION      : Multi Axes S_Curve profile move & pos check
********************************************************************/
INT t_move_all( INT len, pINT ax, pDOUBLE pos, pDOUBLE vel, pINT acc, pINT dcc)
{
	INT err;
	if(mmc_error = err = start_t_move_all(len,ax,pos,vel,acc,dcc))	return	err;
	wait_for_all(len,ax);
	return	err;
}

/**********
*	FUNCTION NAME : start_t_move_all(len,*ax,*pos,*vel,*accel,*decel)
*	FUNCTION      : Multi Axes S_Curve profile move
********************************************************************/
INT start_ts_move_all( INT len, pINT ax, pDOUBLE pos, pDOUBLE vel, pINT acc,pINT dcc)
{
	INT	i;
	INT err;
#ifndef MDF_FUNC	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;
#endif

	for(i=0; i<len; i++)
	{
#ifndef MDF_FUNC	
		while((frames_left(i) <= (INT) 0) && !mmc_error)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경

		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(i,&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}

		while((tmp_m_v <=(INT)0) && !mmc_error)
		{
			Delay (10);
			for(loop_i=0; loop_i<5; loop_i++)
			{
				tmp_m_v=frames_left(i,&tmp_m_e);
				if(tmp_m_e==MMC_OK) break;
			}
		}
#endif
		if(mmc_error)	return	mmc_error;
		if(ax[i] <0 || ax[i] >= Active_Axis_Num)
		{
			mmc_error = MMC_INVALID_AXIS;
			return MMC_INVALID_AXIS;
		}
		if(mmc_error = err = PTP_Move(ax[i], pos[i],vel[i],acc[i],dcc[i],S_CURVE))
			return	err;
	}
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME : t_move_all(axis,position,speed,accel,decel)
*	FUNCTION      : Multi Axes S_Curve profile move & pos check
********************************************************************/
INT ts_move_all( INT len, pINT ax, pDOUBLE pos,pDOUBLE vel, pINT acc, pINT dcc)
{
	INT err;
	if(mmc_error = err = start_t_move_all(len,ax,pos,vel,acc,dcc))	return	err;
	wait_for_all(len,ax);
	return	err;
}

/**********
*	FUNCTION NAME : wait_for_done(INT axis)
*	FUNCTION      : Wait for axis move done
********************************************************************/
INT wait_for_done( INT ax)
{
	while(!axis_done(ax) && !mmc_error);
	if(mmc_error)		return	mmc_error;
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME : wait_for_all(INT len, INT *axis)
*	FUNCTION      : Wait for all axis move done
********************************************************************/
INT wait_for_all( INT len, pINT ax)
{
	INT	i,chk=1;

	while(chk){
		for(i=0,chk=0; i<len; i++)
			if(!axis_done(ax[i]) && !mmc_error)	chk=1;
	}
	if(mmc_error)	return	mmc_error;
	mmc_error = MMC_OK;
	return	MMC_OK;
}
/**********
*	FUNCTION NAME : v_move(axis,position,speed,accel)
*	FUNCTION      : Constant Velocity Move with an acceleration
********************************************************************/
INT v_move( INT ax, double vel, INT acc)
{
	return (PTP_Move(ax,0.0,vel,acc,acc,VELOCITY));
}


/**********
*	FUNCTION NAME : PTP_Move(axis,position,speed,accel,profile)
*	FUNCTION      : PTP Motion Service
********************************************************************/
INT	PTP_Move( INT ax, double pos, double vel, INT acc,INT dcc, INT profile)
{
    MYLOG("PTP_Move\n");
    
    INT	bn,jnt,comm, err=MMC_OK;

#ifndef MDF_FUNC	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_pm_v, tmp_pm_e;
	int loop_i=0;
#endif

	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt))!=MMC_OK) return err;

	if ((mmc_error = err = MMCMutexLock ())!=MMC_OK) return err;

#ifndef MDF_FUNC	
	if ((mmc_error = err = axis_state(ax)) != NO_EVENT)
	{
		mmc_error = MMC_AMP_FAULT;
		MMCMutexUnlock();
		return MMC_AMP_FAULT;
	}
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_pm_v= axis_state(ax,&tmp_pm_e);
		if(tmp_pm_e==MMC_OK) break;
	}
	mmc_error = err = tmp_pm_v;
	
	if (tmp_pm_v != NO_EVENT)
	{
		mmc_error = MMC_AMP_FAULT;
		MMCMutexUnlock();
		return MMC_AMP_FAULT;
	}

#endif

	Mf.Pos[ax]=(float)(pos);// *BootFrame[0].GearRatio[ax]);//2011.10.8 기어비 기능 삭제, warning
	Mf.Vel[ax]=(float)(vel);// *BootFrame[0].GearRatio[ax]);//2011.10.8 기어비 기능 삭제, warning

	if(Mf.Pos[ax]< BootFrame[bn].SwLower_Limit[jnt] ||
		Mf.Pos[ax]> BootFrame[bn].SwUpper_Limit[jnt])
	{
		mmc_error = err =MMC_ILLEGAL_PARAMETER;
		goto quit_PTP;
	}

	if (profile != VELOCITY)
	{
		if (Mf.Vel[ax]<=0.0 || Mf.Vel[ax]> BootFrame[bn].Vel_Limit[jnt])
		{
			mmc_error = err =MMC_ILLEGAL_PARAMETER;
			goto quit_PTP;
		}
	}
	else
	{
		if(Mf.Vel[ax]<-BootFrame[bn].Vel_Limit[jnt] || Mf.Vel[ax]> BootFrame[bn].Vel_Limit[jnt])
		{
			mmc_error = err =MMC_ILLEGAL_PARAMETER;
			goto quit_PTP;
		}
	}

	if(acc<0 || acc> BootFrame[bn].Accel_Limit[jnt])
	{
		mmc_error = err =MMC_ILLEGAL_PARAMETER;
		goto quit_PTP;
	}

	Mf.Acc[ax]=acc;
	Mf.Dcc[ax]=dcc;

	W_A_CommDpram(bn, ax);

	if(profile == VELOCITY)
	{
		comm=VEL_MOVE;
	}
	else if(profile == TRAPEZOID)
	{
		CommDpram[bn]->Command=AX_T_MOVE;
		comm=PTP_MOVE;
	}
	else if(profile == S_CURVE)
	{

		CommDpram[bn]->Command=AX_S_MOVE;
		comm=PTP_MOVE;
	}
	else if(profile == T_RELATIVE)
	{
		CommDpram[bn]->Command=AX_R_MOVE;
		comm=PTP_MOVE;
	}
	else if(profile == S_RELATIVE)
	{
		CommDpram[bn]->Command=AX_RS_MOVE;
		comm=PTP_MOVE;
	}
	else if(profile == PARABOLIC)
	{
		CommDpram[bn]->Command=AX_P_MOVE;
		comm=PTP_MOVE;
	}

	mmc_error = err = MMCCommCheck(1,&bn,comm,jnt);

	if (err == MMC_OK)
	{
		int st = MMC_OK;
		mmc_error = err = get_error_status(ax, &st);
		if (err == MMC_OK)
		{
			if (st != MMC_OK) mmc_error = err = st;
		}
	}
quit_PTP:
	MMCMutexUnlock ();
	return	err;
}
