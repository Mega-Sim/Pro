
#include  	"pcdef.h"

/**********
*	FUNCTION NAME	: set_home(INT ax, INT action)
*	FUNCTION       : Set Home sensor action
*********************************************************************/
INT set_home(INT ax, INT act)
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

	return (CDINoBootWrite(ax,act, PUT_HOME_EVENT));
}
INT AMCLIB_API fset_home(INT ax, INT act)
{
	return (CDIWrite(ax,act, PUT_HOME_EVENT));
}

/**********
*	FUNCTION NAME	: get_home(INT ax, INT *action)
*	FUNCTION       : Get Home sensor action
*********************************************************************/
INT get_home(INT ax, pINT act)
{
//다른함수(get_xxx)와 같은 구조로 변경
	INT	bn,jnt;
	INT err;
	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;
	return (CDIRead(ax,act, GET_HOME_EVENT));
}

INT fget_home(INT ax, pINT act)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax, &bn, &jnt))	return err;
	*act=(int)BootFrame[bn].Home_Limit_St[jnt];
	mmc_error = MMC_OK;
	return MMC_OK;
}

/**********
*	FUNCTION NAME	: set_home_level(INT ax, INT level)
*	FUNCTION       : Set Home sensor level
*********************************************************************/
INT set_home_level(INT ax, INT level)
{
//MMC_ILLEGAL_PARAMETER에 대한 비교문없음, 다른함수(xxx_level)와 같은 구조로 변경
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;
	if(level<LOW || level>HIGH)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	return (CDIWrite(ax,level, PUT_HOME_LEVEL));
}

INT	fset_home_level(INT ax, INT level)
{
	return (CDIWrite(ax,level, PUT_HOME_LEVEL));
}

/**********
*	FUNCTION NAME	: get_home_level(INT ax, INT *level)
*	FUNCTION       : Get Home sensor level
*********************************************************************/
INT get_home_level(INT ax, pINT level)
{
//다른함수(get_xxx_level)와 같은 구조로 변경
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax,&bn,&jnt))		return	err;	
	return (CDIRead(ax,level, GET_HOME_LEVEL));
}

/**********
*	FUNCTION NAME	: set_index_required(INT ax, INT index)
*	FUNCTION       : Set Index required for Homing
*********************************************************************/
INT set_index_required(INT ax, INT index)
{
	return (CDIWrite(ax,index, PUT_INDEX_REQUIRED));
}

INT fset_index_required(INT ax, INT index)
{
	return (CDIWrite(ax,index, PUT_INDEX_REQUIRED));
}

/**********
*	FUNCTION NAME	: get_index_required(INT ax, INT *index)
*	FUNCTION       : Get if Index is required for Homing
*********************************************************************/
INT get_index_required(INT ax, pINT index)
{
	return (CDIRead(ax,index, GET_INDEX_REQUIRED));
}

INT fget_index_required(INT ax, pINT index)
{
	INT	bn,jnt;
	INT err;

	if(mmc_error = err = Find_Bd_Jnt(ax, &bn, &jnt))	return err;
	*index=(int)BootFrame[bn].Home_Index[jnt];
	mmc_error = MMC_OK;
	return	MMC_OK;
}


