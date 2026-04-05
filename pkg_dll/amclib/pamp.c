#include  	"pcdef.h"

/**********
*	FUNCTION NAME	: set_amp_fault(INT axis, INT action)
*	FUNCTION       : Set amp fault input action
*********************************************************************/
INT set_amp_fault(INT ax, INT act)
{
// MMC_ILLEGAL_PARAMETER에 대한 비교문없음, 다른함수(event설정 함수)와 같은 구조로 변경
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;
	if(act<0 || act>ABORT_EVENT)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	
	return(CDINoBootWrite(ax,act,PUT_AMP_FAULT));
}

INT fset_amp_fault(INT ax, INT act)
{
//MMC_ILLEGAL_PARAMETER에 대한 비교문없음, 다른함수(event설정 함수)와 같은 구조로 변경
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;
	if(act<0 || act>ABORT_EVENT)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	return(CDIWrite(ax,act,PUT_AMP_FAULT));
}

/**********
*	FUNCTION NAME	: get_amp_fault(INT axis, INT *action)
*	FUNCTION       : Get amp fault input action
*********************************************************************/
INT get_amp_fault(INT ax, pINT act)
{
//다른함수(get_xxx_)와 같은 구조로 변경
	INT	bn,jnt;
	INT err;
	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;	
	return(CDIRead(ax,act,GET_AMP_FAULT));
}

INT fget_amp_fault(INT ax, pINT act)
{
	INT	bn,jnt;
	INT err;

	if (err = Find_Bd_Jnt(ax, &bn, &jnt))	
	{
		mmc_error = err;
		return err;
	}
	*act=(int)BootFrame[bn].Amp_Fault_Event[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_amp_fault_level(INT axis, INT level)
*	FUNCTION       : Set the active level of the amp fault input
*********************************************************************/
INT set_amp_fault_level(INT ax, INT level)
{
	if(level<LOW || level>HIGH)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return(CDIWrite(ax,level,PUT_AMP_FAULT_LEVEL));
}

INT fset_amp_fault_level(INT ax, INT level)
{
	if(level<LOW || level>HIGH)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return(CDIWrite(ax,level,PUT_AMP_FAULT_LEVEL));
}

/**********
*	FUNCTION NAME	: get_amp_fault_level(INT axis, INT *action)
*	FUNCTION       : Get the active level of the amp fault input
*********************************************************************/
INT get_amp_fault_level(INT ax, pINT level)
{
//다른함수(get_xxx_level)와 같은 구조로 변경
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;	
	return(CDIRead(ax,level,GET_AMP_FAULT_LEVEL));
}

/**********
*	FUNCTION NAME	: set_amp_reset_level(INT axis, INT level)
*	FUNCTION       : Set the active level of the amp reset
*********************************************************************/
INT set_amp_reset_level(INT ax, INT level)
{
	if(level<LOW || level>HIGH)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return(CDIWrite(ax,level,PUT_AMP_RESET_LEVEL));
}

INT fset_amp_reset_level(INT ax, INT level)
{
	if(level<LOW || level>HIGH)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return(CDIWrite(ax,level,PUT_AMP_RESET_LEVEL));
}

/**********
*	FUNCTION NAME	: get_amp_reset_level(INT axis, INT *action)
*	FUNCTION       : Get the active level of the amp reset
*********************************************************************/
INT get_amp_reset_level(INT ax, pINT level)
{
	return(CDIRead(ax,level,GET_AMP_RESET_LEVEL));
}