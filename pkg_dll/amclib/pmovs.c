#include <stdio.h>
#include "pcdef.h"
#include "amc_internal.h"


/**********
*	FUNCTION NAME : map_axes(INT, INT *)
*	FUNCTION      : Setup coordinate axis map
*********************************************************************/
INT map_axes( INT n_axes, pINT map_array)
{
	INT	i;

	for(i=0; i<n_axes; i++){
		if((map_array[i]>=TOTAL_AXIS_NUM)||(map_array[i]<0))
			return	MMC_INVALID_AXIS;
		Mf.Axis[i]=map_array[i];
	}
	Mf.Len=n_axes;
	Mf.MapFunc=1;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME : set_move_speed(double speed)
*	FUNCTION      : Set vector velocity
*********************************************************************/
INT set_move_speed(double  speed)
{
	if(speed <= 0.0 || speed > MMC_VEL_LIMIT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return 	MMC_ILLEGAL_PARAMETER;
	}

	Mf.L_Vel=(float)speed;
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME : set_move_accel(double accel)
*	FUNCTION      : Set vector acceleration
*********************************************************************/
INT set_move_accel(INT  accel)
{
	if(accel <= 0 || accel > MMC_ACCEL_LIMIT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return MMC_ILLEGAL_PARAMETER;
	}
	Mf.L_Acc=accel;
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME : set_arc_division(double degrees)
*	FUNCTION      : set INTerpolation arc segment length
*********************************************************************/
INT set_arc_division(double  degrees)
{
	if(degrees <= 0.0 ||  degrees > 1000.0)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return MMC_ILLEGAL_PARAMETER;
	}

	Mf.L_Deg=(float)degrees;
	mmc_error = MMC_OK;
	return	MMC_OK;
}


/**********
*	FUNCTION NAME : all_done()
*	FUNCTION      : reads if the coordinated motion is complete
*********************************************************************/
INT all_done(VOID)
{
	INT	i,chk;

	for(i=0,chk=1; i<Mf.Len; i++)
		if(!axis_done(Mf.Axis[i]))		chk=0;
	return	chk;
}

/**********
*	FUNCTION NAME : move_2(double x, double y)
*	FUNCTION      : Add a 2 axis point to the path
*********************************************************************/
INT move_2(double x, double y)
{
	if(Mf.MapFunc==0)
	{
		mmc_error = MMC_NO_MAP;
		return	MMC_NO_MAP;
	}
	Mf.Pos[0]=(float)x;//2011.10.8, warning
	Mf.Pos[1]=(float)y;//2011.10.8, warning

    return (CP_Move(2,TRAPEZOID));
}

/**********
*	FUNCTION NAME : move_3(double x, double y, double z)
*	FUNCTION      : Add a 3 axis point to the path
*********************************************************************/
INT move_3(double x, double y, double z)
{
	if(Mf.MapFunc==0)	
	{ 
		mmc_error = MMC_NO_MAP;
		return	MMC_NO_MAP;
	}
	Mf.Pos[0]=(float)x;
	Mf.Pos[1]=(float)y;
	Mf.Pos[2]=(float)z;

	return (CP_Move(3,TRAPEZOID));
}

/**********
*	FUNCTION NAME : move_4(double x, double y, double z, double w)
*	FUNCTION      : Add a 4 axis point to the path
*********************************************************************/
INT move_4(double x, double y, double z, double w)
{
	if(Mf.MapFunc==0)	
    { 
		mmc_error = MMC_NO_MAP; 
		return	MMC_NO_MAP; 
	}

    Mf.Pos[0]=(float)x;
	Mf.Pos[1]=(float)y;
	Mf.Pos[2]=(float)z;
	Mf.Pos[3]=(float)w;

	return (CP_Move(4,TRAPEZOID));
}

/**********
*	FUNCTION NAME : smove_2(double x, double y)
*	FUNCTION      : Add a 2 axis point to the path
*********************************************************************/
INT smove_2(double x, double y)
{
	if(Mf.MapFunc==0)
	{
		mmc_error = MMC_NO_MAP;
		return	MMC_NO_MAP;
	}

    Mf.Pos[0]=(float)x;
	Mf.Pos[1]=(float)y;

	return (CP_Move(2,S_CURVE));
}

/**********
*	FUNCTION NAME : smove_3(double x, double y, double z)
*	FUNCTION      : Add a 3 axis point to the path
*********************************************************************/
INT smove_3(double x, double y, double z)
{
	if(Mf.MapFunc==0)
	{
		mmc_error = MMC_NO_MAP;
		return	MMC_NO_MAP;
	}

	Mf.Pos[0]=(float)x;
	Mf.Pos[1]=(float)y;
	Mf.Pos[2]=(float)z;

	return (CP_Move(3,S_CURVE));
}

/**********
*	FUNCTION NAME : smove_4(double x,double y,double z,double w)
*	FUNCTION      : Add a 4 axis point to the path
*********************************************************************/
INT smove_4(double x,double y, double z, double w)
{
	if(Mf.MapFunc==0)
	{
		mmc_error = MMC_NO_MAP;
		return	MMC_NO_MAP;
	}

	Mf.Pos[0]=(float)x;
	Mf.Pos[1]=(float)y;
	Mf.Pos[2]=(float)z;
	Mf.Pos[3]=(float)w;

	return (CP_Move(4,S_CURVE));
}

/**********
*	FUNCTION NAME : smove_n(double *x)
*	FUNCTION      : Add a n axes point to the path
*********************************************************************/
INT smove_n(pDOUBLE  x)
{
	INT	i;

	if(Mf.MapFunc==0)
	{
		mmc_error = MMC_NO_MAP;
		return	MMC_NO_MAP;
	}

	for(i=0; i<Mf.Len; i++)		Mf.Pos[Mf.Axis[i]]=(float)x[i];

	return (CP_Move(Mf.Len,S_CURVE));
}

/**********
*	FUNCTION NAME : Find_MMC_Num(INT axes)
*	FUNCTION      : Find Action MMC Board
*********************************************************************/
INT	Find_MMC_Num(INT axes)
{
	if(axes < BootFrame[0].Action_Axis_Num)			return MMC_BD1;
	else if(axes < BootFrame[1].Action_Axis_Num)	return MMC_BD2;
	else if(axes < BootFrame[2].Action_Axis_Num)	return MMC_BD3;
	else if(axes < BootFrame[3].Action_Axis_Num)	return MMC_BD4;
	else	return FUNC_ERR;
}

/**********
*	FUNCTION NAME : WriteCommDpram(INT axis)
*	FUNCTION      : Write Command Dpram Frame Region
*********************************************************************/
INT	W_A_CommDpram(INT bd_num, INT ax)
{
	INT	jnt;

	if(bd_num)
		jnt=ax - BootFrame[bd_num-1].Action_Axis_Num;
	else
		jnt=ax;

	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Vel + jnt * 4, &Mf.Vel[ax], 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Acc + jnt * 2, &Mf.Acc[ax], 2);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Dcc + jnt * 2, &Mf.Dcc[ax], 2);

	float fval;
	if(Mf.Pos[ax]>=0.0)
	{
		fval = (float)(Mf.Pos[ax]+0.5);
		_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Pos + jnt * 4, &fval, 4);
	} else {
		fval = (float)(Mf.Pos[ax]-0.5);
		_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Pos + jnt * 4, &fval, 4);
	}
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Axis, &ax, 2);

	return	MMC_OK;
}
/**********
*	FUNCTION NAME : WriteCommDpram(INT bd_num)
*	FUNCTION      : Write Command Dpram Frame Region
*********************************************************************/
INT	W_G_CommDpram(INT bd_num)
{
	INT i;
	INT nval;

	nval = 0;
    for (i = 0; i < BD_AXIS_NUM; i++)
    {
        _write_dpramregs(DPRAM_COMM_BASEOFS + CD_Axis + i * 2, &nval, 2);
    }
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_L_Vel, &Mf.L_Vel, 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_L_Acc, &Mf.L_Acc, 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_L_Deg, &Mf.L_Deg, 4);

	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Ox, &Mf.Ox, 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Oy, &Mf.Oy, 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Angle, &Mf.Angle, 4);
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_CirDir, &Mf.Cir_Dir, 4);


	return	MMC_OK;
}


/************************************************************************/
/************************************************************************/
INT	CP_Move(INT len, INT type_profile)
{
	INT	i,err=MMC_OK;
	INT	bn1,bn2,jnt[TOTAL_AXIS_NUM];
#ifndef MDF_FUNC	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_m_v, tmp_m_e;
	int loop_i=0;
#endif

	for(i=0; i<len; i++){
#ifndef MDF_FUNC	
		while(frames_left(Mf.Axis[i]) <= 0)	Delay(10);
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_m_v=frames_left(Mf.Axis[i],&tmp_m_e);
			if(tmp_m_e==MMC_OK) break;
		}
		
		while(tmp_m_v <= 0)
		{
			Delay (10);
			for(loop_i=0; loop_i<5; loop_i++)
			{
				tmp_m_v=frames_left(Mf.Axis[i],&tmp_m_e);
				if(tmp_m_e==MMC_OK) break;
			}
		}
#endif
	}

	if((bn1=Find_MMC_Num(Mf.Axis[0]))<0)	
	{
		mmc_error = MMC_INVALID_AXIS;
		return	MMC_INVALID_AXIS;
	}
	for(i=1; i<len; i++){
		if((bn2=Find_MMC_Num(Mf.Axis[i]))<0)
		{
			mmc_error=MMC_INVALID_AXIS;
			return	mmc_error;
		}
		if(bn1 != bn2)
		{
			mmc_error=MMC_INVALID_AXIS;
			return	mmc_error;
		}
	}

	for(i=0; i<len; i++){
		mmc_error = err = Find_Bd_Jnt(Mf.Axis[i], &bn1, &jnt[i]);
		if(err)
		{
			return	err;
		}
	}

	if ((mmc_error = err = MMCMutexLock ())!=MMC_OK) return err;

	for(i=0; i<BD_AXIS_NUM; i++)
	{
		int nval = 0;
		_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Axis + i * 2, &nval, 2);
	}

	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Temp_Acc, &type_profile, 2);
	
	for (i = 0; i < len; i ++)
	{
		int nval;
		nval = (int)Mf.L_Vel; _write_dpramregs(DPRAM_COMM_BASEOFS + CD_Vel + jnt[i]*4, &nval, 4);
		nval = Mf.L_Acc; _write_dpramregs(DPRAM_COMM_BASEOFS + CD_Acc + jnt[i]*2, &nval, 2);

		nval = 1;
		_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Axis + jnt[i]*2, &nval, 2);
		nval = (int)Mf.Pos[i];
		_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Pos + jnt[i]*4, &nval, 4);
	}
	_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Len, &len, 2);

	mmc_error = err = MMCCommCheck(1,&bn1,CP_LINE_MOVE,0);

	MMCMutexUnlock ();

	return	err;
}

INT	get_Buflast_command(INT ax, double *Pos)
{
	LONG	pos_d;
	INT err;

	if(mmc_error = err = CDLRead(ax,&pos_d,GET_BUF_POSITION))	return err;
	*Pos=(double)pos_d;
	mmc_error = MMC_OK;
	return	MMC_OK;
}
