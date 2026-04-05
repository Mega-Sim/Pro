#include  	"pcdef.h"
#include	"amc_internal.h"


/**********
*	FUNCTION NAME	: get_filter(axis, *coeff)
*	FUNCTION       : Get Gain Value from DSP
*********************************************************************/
INT get_filter(INT ax, pUINT coeff)
{
	return (Gain_RW(ax,(pINT)coeff,GET_GAIN_VALUE,0,TEMPORARY));
}

/**********
*	FUNCTION NAME	: set_filter(axis, *coeff)
*	FUNCTION       : Set Gain Value TO   DSP
*********************************************************************/
INT set_filter(INT ax, pUINT coeff)
{
	return (Gain_RW(ax,(pINT)coeff,PUT_GAIN_VALUE,1,TEMPORARY));
}

/**********
*	FUNCTION NAME	: fset_filter(axis, *coeff)
*	FUNCTION       : Set Gain Value TO   DSP
*********************************************************************/
INT fset_filter(INT ax, pINT coeff)//2011.10.8 Warning, pUINT => pINT
{
	return (Gain_RW(ax,(pINT)coeff,PUT_GAIN_VALUE,1,BOOT_SAVE));
}

/**********
*	FUNCTION NAME	: get_v_filter(axis, *coeff)
*	FUNCTION       : Get Gain Value from DSP
*********************************************************************/
INT get_v_filter(INT ax, pUINT coeff)
{
	return (Gain_RW(ax,(pINT)coeff,GET_VGAIN_VALUE,0,TEMPORARY));
}

/**********
*	FUNCTION NAME	: set_v_filter(axis, *coeff)
*	FUNCTION       : Set Gain Value TO   DSP
*********************************************************************/
INT set_v_filter(INT ax, pUINT coeff)//2011.10.13 Warning, pINT => pUINT
{
	return (Gain_RW(ax,(pINT)coeff,PUT_VGAIN_VALUE,1,TEMPORARY));
}

/**********
*	FUNCTION NAME	: fset_v_filter(axis, *coeff)
*	FUNCTION       : Set Gain Value TO   DSP
*********************************************************************/
INT fset_v_filter(INT ax, pINT coeff)
{
	return (Gain_RW(ax,(pINT)coeff,PUT_VGAIN_VALUE,1,BOOT_SAVE));
}

/**********
*	FUNCTION NAME	: set_p_integration(INT ax, INT mode)
*	FUNCTION       : Set Position Integration Mode
*********************************************************************/
INT set_p_integration(INT ax, INT mode)
{
	INT	bn,jnt;
	INT err;

	if(err = Find_Bd_Jnt(ax,&bn,&jnt))
	{
		mmc_error = err;
		return	err;
	}
	if(mode<IN_STANDING || mode>IN_ALWAYS)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDINoBootWrite(ax,mode, PUT_POS_I_MODE));
}

INT fset_p_integration(INT ax, INT mode)
{
	INT	bn,jnt;
	INT err;

	if(err = Find_Bd_Jnt(ax,&bn,&jnt))
	{
		mmc_error = err;
		return	err;
	}
	if(mode<IN_STANDING || mode>IN_ALWAYS)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDIWrite(ax,mode, PUT_POS_I_MODE));
}

/**********
*	FUNCTION NAME	: get_p_integration(INT ax, INT *mode)
*	FUNCTION       : Get Position Integration Mode
*********************************************************************/
INT get_p_integration(INT ax, pINT mode)
{
	INT	bn,jnt;
	INT err;

	if(err = Find_Bd_Jnt(ax,&bn,&jnt))
	{
		mmc_error = err;
		return	err;
	}
	return (CDIRead(ax,mode, GET_POS_I_MODE));
}

/**********
*	FUNCTION NAME	: set_v_integration(INT ax, INT mode)
*	FUNCTION       : Set Velocity Integration Mode
*********************************************************************/
INT set_v_integration(INT ax, INT mode)
{
	INT	bn,jnt;
	INT err;

	if(err = Find_Bd_Jnt(ax,&bn,&jnt))
	{
		mmc_error = err;
		return	err;
	}
	if(mode<IN_STANDING || mode>IN_ALWAYS)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDINoBootWrite(ax,mode, PUT_VEL_I_MODE));
}

INT fset_v_integration(INT ax, INT mode)
{
	INT	bn,jnt;
	INT err;

	if(err = Find_Bd_Jnt(ax,&bn,&jnt))
	{
		mmc_error = err;
		return	err;
	}
	if(mode<IN_STANDING || mode>IN_ALWAYS)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDIWrite(ax,mode, PUT_VEL_I_MODE));
}

/**********
*	FUNCTION NAME	: get_v_integration(INT ax, INT *mode)
*	FUNCTION       : Get Velocity Integration Mode
*********************************************************************/
INT get_v_integration(INT ax, pINT mode)
{
	INT	bn,jnt;
	INT err;

	if(err = Find_Bd_Jnt(ax,&bn,&jnt))
	{
		mmc_error = err;
		return	err;
	}
	return (CDIRead(ax,mode, GET_VEL_I_MODE));
}

/**********
*	FUNCTION NAME	: Gain_RD(ax, *coeff, comm, flag)
*	FUNCTION       : Gain Read/Write Service
*********************************************************************/
INT Gain_RW (INT ax, INT *coeff, INT comm, INT flag, INT sa_no)
{
	INT		bn, jnt;
	INT err;

	if (err = Find_Bd_Jnt (ax, &bn, &jnt))
	{
		mmc_error = err;
		return err;
	}
	if (err = MMCMutexLock ())
	{
		mmc_error = err;
		return err;
	}

	if (flag)
	{	/* write */
		if (sa_no)
		{	/* save to file	*/
			if(comm==PUT_GAIN_VALUE)    
			{
			
				AxisDpram[ax]->Int_Type[0]=(INT2)coeff[GA_P];		
				AxisDpram[ax]->Int_Type[1]=(INT2)coeff[GA_I];
				AxisDpram[ax]->Int_Type[2]=(INT2)coeff[GA_D];
				AxisDpram[ax]->Int_Type[3]=(INT2)coeff[GA_F];
				AxisDpram[ax]->Int_Type[4]=(INT2)coeff[GA_ILIMIT];				
				
				BootFrame[bn].PGain[jnt] =(INT4)AxisDpram[ax]->Int_Type[0];			
				BootFrame[bn].IGain[jnt] =(INT4)AxisDpram[ax]->Int_Type[1];			
				BootFrame[bn].DGain[jnt] =(INT4)AxisDpram[ax]->Int_Type[2];			
				BootFrame[bn].FGain[jnt] =(INT4)AxisDpram[ax]->Int_Type[3];			
				BootFrame[bn].ILimit[jnt]=(INT4)AxisDpram[ax]->Int_Type[4];			
// 주의 : 위에서 (int2)로 cast된 후 (int4)다시 cast되는 경우 상위 16bit가 사라지게 되어 이상한 값을 갖을수 있음
// ex-양수) 0x12345678 ->(int2) -> 0x5678 ->(int4) -> 0x00005678
// ex-음수) 0xffff5678 ->(int2) -> 0x5678 ->(int4) -> 0x00005678(음수에서 양수로 바뀜)
// AxisDpram[ax]->Int_Type[0]은 int2이고 BootFrame[bn].PGain[jnt] 은 int로 type이 다르지만, 현재 구조를 바꿀수 없음(바뀌면 전체 구조가 흔들림)
// 현재 16bit이상 안쓴다고함 ->그래서 현재 코드 상태 유지
			}
			else
			{
				AxisDpram[ax]->Int_Type[0]=(INT2)coeff[GA_P];
				AxisDpram[ax]->Int_Type[1]=(INT2)coeff[GA_I];
				AxisDpram[ax]->Int_Type[2]=(INT2)coeff[GA_D];
				AxisDpram[ax]->Int_Type[3]=(INT2)coeff[GA_F];
				AxisDpram[ax]->Int_Type[4]=(INT2)coeff[GA_ILIMIT];

				BootFrame[bn].VPgain[jnt] =(INT4)AxisDpram[ax]->Int_Type[0];
				BootFrame[bn].VIgain[jnt] =(INT4)AxisDpram[ax]->Int_Type[1];
				BootFrame[bn].VDgain[jnt] =(INT4)AxisDpram[ax]->Int_Type[2];
				BootFrame[bn].VFgain[jnt] =(INT4)AxisDpram[ax]->Int_Type[3];
				BootFrame[bn].VIlimit[jnt]=(INT4)AxisDpram[ax]->Int_Type[4];
			}
		}
		else
		{
			if(comm==PUT_GAIN_VALUE)
			{
				AxisDpram[ax]->Int_Type[0]=(INT2)coeff[GA_P];
				AxisDpram[ax]->Int_Type[1]=(INT2)coeff[GA_I];
				AxisDpram[ax]->Int_Type[2]=(INT2)coeff[GA_D];
				AxisDpram[ax]->Int_Type[3]=(INT2)coeff[GA_F];
				AxisDpram[ax]->Int_Type[4]=(INT2)coeff[GA_ILIMIT];
			}
			else
			{
				AxisDpram[ax]->Int_Type[0]=(INT2)coeff[GA_P];
				AxisDpram[ax]->Int_Type[1]=(INT2)coeff[GA_I];
				AxisDpram[ax]->Int_Type[2]=(INT2)coeff[GA_D];
				AxisDpram[ax]->Int_Type[3]=(INT2)coeff[GA_F];
				AxisDpram[ax]->Int_Type[4]=(INT2)coeff[GA_ILIMIT];
			}
		}
	}

	if (err = MMCCommCheck (1,&bn,comm,jnt))
	{
		MMCMutexUnlock ();
		mmc_error = err;
		return	err;
	}

	if (!flag)
	{
		if (comm==GET_GAIN_VALUE)
		{
			coeff[GA_P]=(INT4)AxisDpram[ax]->Int_Type[0];
			coeff[GA_I]=(INT4)AxisDpram[ax]->Int_Type[1];
			coeff[GA_D]=(INT4)AxisDpram[ax]->Int_Type[2];
			coeff[GA_F]=(INT4)AxisDpram[ax]->Int_Type[3];
			coeff[GA_ILIMIT]=(INT4)AxisDpram[ax]->Int_Type[4];
		}
		else
		{
			coeff[GA_P]=(INT4)AxisDpram[ax]->Int_Type[0];
			coeff[GA_I]=(INT4)AxisDpram[ax]->Int_Type[1];
			coeff[GA_D]=(INT4)AxisDpram[ax]->Int_Type[2];
			coeff[GA_F]=(INT4)AxisDpram[ax]->Int_Type[3];
			coeff[GA_ILIMIT]=(INT4)AxisDpram[ax]->Int_Type[4];
		}
	}

	MMCMutexUnlock ();
	return	MMC_OK;
}
