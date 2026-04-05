#include <windows.h>
#include  	"pcdef.h"
#include	"amc_internal.h"

/**********
*	FUNCTION NAME	: dwell(INT ax, LONG duration)
*	FUNCTION       : Delay execution of a frame until bit level change
*********************************************************************/
INT dwell(INT ax, LONG duration)
{
	INT		bn, jnt, err;

#ifndef MDF_FUNC	
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_d_v, tmp_d_e;
	int loop_i=0;
#endif

	if (duration<=0) return	MMC_OK;
#ifndef MDF_FUNC
	while( frames_left (ax)<=(INT)0) Delay (10);	//
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_d_v=frames_left(ax,&tmp_d_e);
		if(tmp_d_e==MMC_OK) break;
	}

	while(tmp_d_v <=(INT)0)
	{
		Delay (10);
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_d_v=frames_left(ax,&tmp_d_e);
			if(tmp_d_e==MMC_OK) break;
		}
	}
#endif
	if ((mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) != MMC_OK) return err;
	if ((mmc_error = err = MMCMutexLock ()) != MMC_OK) return err;
	{
		int axdelay = AX_DELAY;
		_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Command, &axdelay, 2);
	}

	AxisDpram[ax]->Long_Type=duration;
	mmc_error = err = MMCCommCheck(1,&bn,MMC_DELAY,jnt);
	MMCMutexUnlock ();
	return	err;
}

/**********
*	FUNCTION NAME	: io_trigger(INT ax, LONG duration)
*	FUNCTION       : Delay execution of a frame until bit level change
*********************************************************************/
INT io_trigger(INT ax, INT bitNo, INT state)
{
	INT	bn,iobn,jnt;
	INT err;

#ifndef MDF_FUNC
	while( frames_left (ax)<=(INT)0) Delay (10);	//
#else			//2.5.25v2.8.07통합 버젼 120120 syk, 5번 유형 사용자open함수 원형 변경
	int tmp_d_v, tmp_d_e;
	int loop_i=0;

	for(loop_i=0; loop_i<5; loop_i++)
	{
		tmp_d_v=frames_left(ax,&tmp_d_e);
		if(tmp_d_e==MMC_OK) break;
	}

	while(tmp_d_v <=(INT)0)
	{
		Delay (10);
		for(loop_i=0; loop_i<5; loop_i++)
		{
			tmp_d_v=frames_left(ax,&tmp_d_e);
			if(tmp_d_e==MMC_OK) break;
		}
	}
#endif
	if(err = Find_Bd_Jnt(ax,&bn,&jnt))
	{
		mmc_error = err;
		return	err;
	}
	if((iobn=Find_IO_Bit(bitNo))<0)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	if(bn != iobn)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return(CDI3NoBootWrite(ax,(long)bitNo,state,PUT_IO_TRIGGER));
}

/**********
*	FUNCTION NAME	: Delay(LONG duration)
*	FUNCTION       : Support delay function
*********************************************************************/
INT Delay(LONG duration)
{
	Sleep ((DWORD)duration);
	return	MMC_OK;
}



