
#include  	"pcdef.h"
#include  	"log.h"

/**********
*	FUNCTION NAME	: set_positive_sw_limit(INT ax, double limit, INT action)
*	FUNCTION       : Set Positive S/W Limit & Event
*********************************************************************/
INT set_positive_sw_limit(INT ax, double limit, INT action)
{
    MYLOG("set_positive_sw_limit\n");
    
    LONG limit_l;

	if(action<0 || action>ABORT_EVENT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if((long)limit>MMC_POS_SW_LIMIT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return   MMC_ILLEGAL_PARAMETER;
	}
	limit_l=(LONG) (limit);

	return	(CDI3NoBootWrite(ax,limit_l,action,PUT_SW_UPPER_LIMIT));
}

INT fset_positive_sw_limit(INT ax, double limit, INT action)
{
	LONG	limit_l;

	if(action<0 || action>ABORT_EVENT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	if((long)limit>MMC_POS_SW_LIMIT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return   MMC_ILLEGAL_PARAMETER;
	}
	limit_l=(LONG)(limit);
	return	(CDI3Write(ax,limit_l,action,PUT_SW_UPPER_LIMIT));
}
/**********
*	FUNCTION NAME	: get_positive_sw_limit(INT ax, float *limit, INT *action)
*	FUNCTION       : Get Positive S/W Limit & Event
*********************************************************************/
INT get_positive_sw_limit(INT ax, pDOUBLE limit, pINT action)
{
    MYLOG("get_positive_sw_limit\n");
    
    LONG limit_l;
	INT err;
	
    if(mmc_error = err = CDI3Read(ax,&limit_l,action,GET_SW_UPPER_LIMIT))	return	err;

    *limit=(double)limit_l;
	mmc_error = MMC_OK;
	return	MMC_OK;
}

INT fget_positive_sw_limit(INT ax, double *limit, INT *action)
{
	INT	bn,jnt;
	INT err;
	if(mmc_error = err = Find_Bd_Jnt(ax, &bn, &jnt))	return err;

	*action=(int)BootFrame[bn].SwUpper_LimitSt[jnt];
	*limit=(double)BootFrame[bn].SwUpper_Limit[jnt];

	mmc_error = MMC_OK;
	return	MMC_OK;
}
/**********
*	FUNCTION NAME	: set_negative_sw_limit(INT ax, double limit, INT action)
*	FUNCTION       : Set Negative S/W Limit & Event
*********************************************************************/
INT set_negative_sw_limit(INT ax, double limit, INT action)
{
    MYLOG("Axis[%d] set_negative_sw_limit\n", ax);
    
    LONG	limit_l;

	if(action<0 || action>ABORT_EVENT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if((long)limit<MMC_NEG_SW_LIMIT) 
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return   MMC_ILLEGAL_PARAMETER;
	}
	limit_l=(long)(limit);

	return	(CDI3NoBootWrite(ax,limit_l,action,PUT_SW_LOWER_LIMIT));
}

INT fset_negative_sw_limit(INT ax, double limit, INT action)
{
	LONG	limit_l;

	if(action<0 || action>ABORT_EVENT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	if((long)limit<MMC_NEG_SW_LIMIT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return   MMC_ILLEGAL_PARAMETER;
	}
	limit_l=(LONG)(limit);
	return	(CDI3Write(ax,limit_l,action,PUT_SW_LOWER_LIMIT));
}

/**********
*	FUNCTION NAME	: get_positive_sw_limit(INT ax, double *limit, INT *action)
*	FUNCTION       : Get Positive S/W Limit & Event
*********************************************************************/
INT		get_negative_sw_limit (INT ax, pDOUBLE limit, pINT action)
{
    MYLOG("get_negative_sw_limit\n");
    
    LONG limit_l;
	INT err;

	if (mmc_error = err = CDI3Read (ax, &limit_l, action, GET_SW_LOWER_LIMIT)) return err;
	*limit = (double)limit_l;
	mmc_error = MMC_OK;
	return	MMC_OK;
}
INT fget_negative_sw_limit(INT ax, double *limit, INT *action)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax, &bn, &jnt))	return err;

	*action=(int)BootFrame[bn].SwLower_LimitSt[jnt];
	*limit=(double)BootFrame[bn].SwLower_Limit[jnt];

	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: get_vel_curve(INT ax, pDOUBLE limit, pINT action)
*	FUNCTION       : Get curve vel Limit & Event
*********************************************************************/
INT		get_vel_curve(INT ax, pINT limit, pINT action)	//profile 상하 bnad margin 설정
{
	LONG	limit_l;
	INT err;
	if (mmc_error = err = CDI3Read (ax, &limit_l, action, GET_VEL_CURVE)) return err;
	*limit = (int)((limit_l * 81920)/2019.4);
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_vel_curve(INT ax, int limit, INT action)
*	FUNCTION       : Set curve vel Limit & Event
*********************************************************************/
INT set_vel_curve(INT ax, INT limit, INT action)	//profile 상하 bnad margin 설정
{
    MYLOG("Axis[%d] set_vel_curve\n", ax);
    
    int i_limit=0;
	
    if(action<0 || action>1)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if(limit<0 || limit> 819200) 
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return   MMC_ILLEGAL_PARAMETER;
	}
	i_limit = (int)((limit * 2019.4) / 81920); 

	return	(CDI3NoBootWrite(ax,(LONG)i_limit,action,PUT_VEL_CURVE));
}

/**********
*	FUNCTION NAME	: get_actvel_margin(INT ax, pINT limit, pINT action, pINT time)
*	FUNCTION       : actvel_margin & Event & time read
*********************************************************************/
INT		get_actvel_margin(INT ax, pINT limit, pINT action, pINT time)	//외부 요인에 의한 과속 검출을 위해
{
	INT		bn, jnt, err;

	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		if ((mmc_error = err = MMCCommCheck (1, &bn, GET_ACTVEL_MARGIN, jnt)) == MMC_OK)
		{
			*limit = AxisDpram[ax]->Long_Type;
			*action = (INT)AxisDpram[ax]->Char_Type[0];
			*time = (INT)AxisDpram[ax]->Int_Type[0];
		}
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: set_vel_curve(INT ax, int limit, INT action)
*	FUNCTION       : actvel_margin & Event & time write
*********************************************************************/
INT set_actvel_margin(INT ax, INT limit, INT action, INT time)			//외부 요인에 의한 과속 검출을 위해
{
    MYLOG("Axis[%d] set_actvel_margin\n", ax);
    
    INT		bn, jnt, err;

	if(action<NO_EVENT || action>ABORT_EVENT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if(limit<0 || limit> 819200) 
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return   MMC_ILLEGAL_PARAMETER;
	}
	
	if(time<0 || time> 10000) 
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return   MMC_ILLEGAL_PARAMETER;
	}
	
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
    
    if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[ax]->Long_Type = limit;
		AxisDpram[ax]->Int_Type[0] = time;
		AxisDpram[ax]->Char_Type[0] = (CHAR)action;

		mmc_error = err = MMCCommCheck (1, &bn, PUT_ACTVEL_MARGIN, jnt);
	}
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: get_accel_limit(INT ax, INT *limit)
*	FUNCTION       : Get Acceleration Limit
*********************************************************************/
INT get_accel_limit(INT ax, pINT limit)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;

	*limit=BootFrame[bn].Accel_Limit[jnt];
	mmc_error = MMC_OK;
	return MMC_OK;
}

/**********
*	FUNCTION NAME	: set_accel_limit(INT ax, INT limit)
*	FUNCTION       : Set Acceleration Limit
*********************************************************************/
INT set_accel_limit(INT ax, INT limit)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;
	if(limit<0 || limit>MMC_ACCEL_LIMIT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	BootFrame[bn].Accel_Limit[jnt]=limit;

    mmc_error = MMC_OK;
	return MMC_OK;
}

/**********
*	FUNCTION NAME	: get_vel_limit(INT ax, double *limit)
*	FUNCTION       : Get Velocity Limit
*********************************************************************/
INT get_vel_limit(INT ax, pDOUBLE limit)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;

	*limit=(double)BootFrame[bn].Vel_Limit[jnt];
	mmc_error = MMC_OK;
	return MMC_OK;
}

/**********
*	FUNCTION NAME	: set_vel_limit(INT ax, double limit)
*	FUNCTION       : Set Velocity Limit
*********************************************************************/
INT set_vel_limit(INT ax, double limit)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;
	if(limit<=0 || limit>MMC_VEL_LIMIT)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	BootFrame[bn].Vel_Limit[jnt]=(int)(limit);

	mmc_error = MMC_OK;
	return MMC_OK;
}

/**********
*	FUNCTION NAME	: set_positive_limit(INT ax, INT action)
*	FUNCTION       : Set Positive Limit Event
*********************************************************************/
INT set_positive_limit(INT ax, INT act)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;
	if(act<0 || act>ABORT_EVENT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDINoBootWrite(ax,act, PUT_POS_EVENT));
}

INT fset_positive_limit(INT ax, INT act)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;
	if(act<0 || act>ABORT_EVENT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDIWrite(ax,act, PUT_POS_EVENT));
}

/**********
*	FUNCTION NAME	: get_positive_limit(INT ax, INT *action)
*	FUNCTION       : Get Positive Limit Event
*********************************************************************/
INT get_positive_limit(INT ax, pINT act)
{
	INT	bn,jnt;
	INT err;
	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;
	return (CDIRead(ax,act, GET_POS_EVENT));
}

INT fget_positive_limit(INT ax, INT *act)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;

	*act=(int)BootFrame[bn].Pos_Limit_St[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_negative_limit(INT ax, INT action)
*	FUNCTION       : Set Negative Limit Event
*********************************************************************/
INT set_negative_limit(INT ax, INT act)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;
	if(act<0 || act>ABORT_EVENT)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDINoBootWrite(ax,act, PUT_NEG_EVENT));
}

INT fset_negative_limit(INT ax, INT act)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;
	
    if(act<0 || act>ABORT_EVENT)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDIWrite(ax,act, PUT_NEG_EVENT));
}

/**********
*	FUNCTION NAME	: get_negative_limit(INT ax, INT *action)
*	FUNCTION       : Get Negative Limit Event
*********************************************************************/
INT get_negative_limit(INT ax, pINT act)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;
	return (CDIRead(ax,act, GET_NEG_EVENT));
}

INT fget_negative_limit(INT ax, INT *act)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;

	*act=(int)BootFrame[bn].Neg_Limit_St[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_in_position(INT ax, double pos)
*	FUNCTION       : Set In Position Limit
*********************************************************************/
INT set_in_position(INT ax, double pos)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))			return	err;
	if(pos<0.0 || pos>MMC_ERROR_LIMIT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDFNoBootWrite(ax,(float)pos, PUT_IN_POSITION));
}

INT fset_in_position(INT ax, double pos)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;
	
    if(pos<0.0 || pos>MMC_ERROR_LIMIT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDFWrite(ax,(float)pos, PUT_IN_POSITION));
}

/**********
*	FUNCTION NAME	: get_in_position(INT ax, float *pos)
*	FUNCTION       : Get In Position Limit
*********************************************************************/
INT get_in_position(INT ax, pDOUBLE pos)
{
	INT	bn,jnt;
	FLOAT	pos_f;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;
	if(mmc_error = err = CDFRead(ax,&pos_f, GET_IN_POSITION))	return	err;

	*pos=(double)pos_f;
	mmc_error = MMC_OK;
	return	MMC_OK;
}

INT fget_in_position(INT ax, double *pos)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))	return	err;

	*pos=(double)BootFrame[bn].In_Position[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_error_limit(INT ax, double limit, INT action)
*	FUNCTION       : Set Error Window Limit & Event
*********************************************************************/
INT set_error_limit(INT ax, double limit, INT action)
{
	INT	bn,jnt;
	LONG	limit_l;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))					return	err;
	if(limit<=0.0 || limit>MMC_ERROR_LIMIT)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	limit_l=(LONG)(limit);
	return (CDI3NoBootWrite(ax,limit_l, action, PUT_ERR_LIMIT));
}

INT fset_error_limit(INT ax, double limit, INT action)
{
	INT	bn,jnt;
	LONG	limit_l;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))					return	err;
	if(limit<=0.0 || limit>MMC_ERROR_LIMIT)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	limit_l=(LONG)(limit);
	return (CDI3Write(ax,limit_l, action, PUT_ERR_LIMIT));
}

/**********
*	FUNCTION NAME	: get_error_limit(INT ax, double *limit, INT *action)
*	FUNCTION       : Get Error Window Limit & Event
*********************************************************************/
INT get_error_limit(INT ax, pDOUBLE limit, pINT action)
{
	INT	bn,jnt;
	LONG	limit_l;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;
	if(mmc_error = err = CDI3Read(ax,&limit_l, action, GET_ERR_LIMIT))	return	err;
	
	*limit=(double)limit_l;
	mmc_error = MMC_OK;
	return	MMC_OK;
}

INT fget_error_limit(INT ax, double *limit, INT *action)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;

	*limit=(double)BootFrame[bn].Error_Limit[jnt];
	*action=(int)BootFrame[bn].Error_Limit_St[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_positive_level(INT ax, INT level)
*	FUNCTION       : Set Positive sensor level
*********************************************************************/
INT set_positive_level(INT ax, INT level)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;
	if(level<LOW || level>HIGH)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDIWrite(ax,level, PUT_POS_LEVEL));
}

/**********
*	FUNCTION NAME	: get_positive_level(INT ax, INT *level)
*	FUNCTION       : Get Positive sensor level
*********************************************************************/
INT get_positive_level(INT ax, pINT level)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;
	return (CDIRead(ax,level, GET_POS_LEVEL));
}

/**********
*	FUNCTION NAME	: set_negative_level(INT ax, INT level)
*	FUNCTION       : Set Negative sensor level
*********************************************************************/
INT set_negative_level(INT ax, INT level)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;
	if(level<LOW || level>HIGH)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDIWrite(ax,level, PUT_NEG_LEVEL));
}

/**********
*	FUNCTION NAME	: get_negative_level(INT ax, INT *level)
*	FUNCTION       : Get Negative sensor level
*********************************************************************/
INT get_negative_level(INT ax, pINT level)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;
	return (CDIRead(ax,level, GET_NEG_LEVEL));
}

INT set_oht_model_id(INT OHT_Model_Id)
{
    MYLOG("set_oht_model_id\n");
    
    INT	bn, jnt, err;

	if((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
    	return err;
    }
    
	if((mmc_error = err = Find_Bd_Jnt (0, &bn, &jnt)) == MMC_OK)
	{
		AxisDpram[0]->Int_Type[0] = OHT_Model_Id;
		mmc_error = err = MMCCommCheck (1, &bn, SET_OHT_MODEL_ID, jnt);
	}
	MMCMutexUnlock ();
	return	err;
}

INT get_oht_model_id(pINT OHT_Model_Id)
{
    MYLOG("get_oht_model_id\n");
    
    INT	bn, jnt, err;

	if((mmc_error = err = MMCMutexLock ()) != MMC_OK)
	{
    	return err;
    }
	
	if((mmc_error = err = Find_Bd_Jnt (0, &bn, &jnt)) == MMC_OK)
	{
		if((mmc_error = err = MMCCommCheck (1, &bn, GET_OHT_MODEL_ID, jnt)) == MMC_OK)
		{
            *OHT_Model_Id = (INT)AxisDpram[0]->Int_Type[0];
		}
	}
	MMCMutexUnlock ();
	return	err;
}



