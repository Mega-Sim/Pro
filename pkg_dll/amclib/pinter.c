
#include  	"pcdef.h"
#include	"amc_internal.h"

/**********
*	FUNCTION NAME	: set_interpolation(INT, INT *,LONG *,INT)
*	FUNCTION       : Moving with Main delt_s
*********************************************************************/
INT set_interpolation(INT Len, pINT ax, pLONG idelt_s, INT flag)
{
	INT	i,cnt,jnt, err=MMC_OK;
	INT	bd_num[TOTAL_AXIS_NUM],bn[MMC_BOARD_NUM],bnn[MMC_BOARD_NUM];

	if ((mmc_error = err = MMCMutexLock ())!=MMC_OK) return err;
	for(i=0; i<Len; i++) Mf.Pos[ax[i]]=(float)idelt_s[i];
	for(i=0;i<MMC_BOARD_NUM;i++)	bn[i]=0;
	for(i=0,cnt=0; i<Len; i++){
		if((bd_num[i]=Find_MMC_Num(ax[i]))<0)	
		{
			MMCMutexUnlock();
			mmc_error = MMC_INVALID_AXIS;
			return MMC_INVALID_AXIS;
		}
		if(!bn[bd_num[i]]){
			W_G_CommDpram(bd_num[i]);
			bnn[cnt]=bd_num[i];
			cnt++;
		}

		W_A_CommDpram(bd_num[i],ax[i]);

		if(bd_num[i])	jnt=ax[i] - BootFrame[bd_num[i]-1].Action_Axis_Num;
		else           jnt=ax[i];
		{
			int minter = MAIN_INTERPOLATION;
			_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Command, &minter, 2);
			_write_dpramregs(DPRAM_COMM_BASEOFS + CD_Acc + jnt * 2, &flag, 2);
		}
		bn[bd_num[i]]=1;
	}
	mmc_error = err = MMCCommCheck(cnt,bnn,PUT_INTERPOLATION,0);

	MMCMutexUnlock ();
	return	err;
}


