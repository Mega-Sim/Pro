/*------------------------------------
*	Axis Controller Board PGM Start
*------------------------------------*/

#include "pcdef.h"
#include "pcglb.h"
#include "amc_internal.h"
#include "log.h"

#include <stdio.h>


/**********
*	FUNCTION NAME	: mmc_init ()
*	FUNCTION		: MMC Board Initial
*********************************************************************/
INT mmc_init (VOID)
{
    MYLOG("mmc_init\n");

    INT	i;
	INT err;

    // Mutex를 많이 unlock한다.
	for (i = 0; i < 10; i ++) MMCMutexUnlock();

	mmc_error = err = MMC_OK;

	if ((mmc_error = err = Para_Ini ()) != MMC_OK) return	err;
	for (i=0; i<Active_Axis_Num; i++)
	{
		amp_fault_set (i);			/* Amp Fault Set */
		set_amp_enable (i, FALSE);	/* Amp Disable */
	}

	// PC와 DSP의 버전을 검사
	int ver_dsp, ver_pc;
	ver_pc = version_chk_pc();
	err = version_chk(0, &ver_dsp);

    if(err==MMC_OK)
	{
		if (ver_pc != ver_dsp)	err = AMC_VERSION_ERROR;
	}
	
	return	mmc_error = err;
}

/**********
*	FUNCTION NAME	: DpramAddr ()
*	FUNCTION		: DPRAM Address Initial for SETUP program
*********************************************************************/
INT DpramAddr (VOID)
{
	INT		i,j;

	for (i=0; i<MMC_BOARD_NUM; i++)
		for (j=0; j<BD_AXIS_NUM; j++)
			BootFrame[i].Dpram_Addr[j] = (Dpram_Addr[i][0] + 0x30*j);

	return	MMC_OK;
}

/**********
*	FUNCTION NAME	: set_dpram_addr(INT bdnum, LONG addr)
*	FUNCTION		: DPRAM Address Initial
*********************************************************************/
INT set_dpram_addr (INT bdnum, LONG addr)
{
	int	j;
	INT err;

	if (bdnum<0 || bdnum>3)	
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	for(j=0;j<BD_AXIS_NUM;j++) 
    {
		BootFrame[bdnum].Dpram_Addr[j]=(addr + 0x30*j);
		Dpram_Addr[bdnum][j]=GetPhysicalAddr(BootFrame[bdnum].Dpram_Addr[j]);
	}
	if(mmc_error = err = BootFrameStoreBd(bdnum, NULL))	return	err;

	mmc_error = MMC_OK;
	return	MMC_OK;
}

LONG get_dpram_addr(INT bdnum)
{
	return BootFrame[bdnum % 4].Dpram_Addr[0];
}

/**********
*	FUNCTION NAME	: Addr_init()
*	FUNCTION       : Address Initial
*********************************************************************/
INT Addr_init (VOID)
{
    MYLOG("Addr_init\n");

	unsigned char	*tmp_ptr;
	INT				i, j;
	unsigned char val;

	/* Create a mutex for axis control. */
	hMutex_Axis_Comm = CreateMutex(NULL, FALSE, TEXT("MMC_AXIS_COMM"));

    for (i=0; i<MMC_BOARD_NUM; i++)
	{
		tmp_ptr = (unsigned char *)GetPhysicalAddr (BootFrame[i].Dpram_Addr[0]);
		for (j=0; j<BD_AXIS_NUM; j++)
		{
			Dpram_Addr[i][j]= (unsigned long)(tmp_ptr+(j*0x30));
			if(Dpram_Addr[i][j]==1)
			{
				mmc_error = MMC_WINNT_DRIVER_OPEN_ERROR;
				return MMC_WINNT_DRIVER_OPEN_ERROR;
			}
		}
		FreeAddr[i]=Dpram_Addr[i][0];
		CommDpram[i]=(COMM_DPRAM_TYPE	*)(Dpram_Addr[i][0] + 0x180);
		AxisInfo[i]=(CHAR *)(Dpram_Addr[i][0]+0x3FC);
		Ack2Dsp[i]=(CHAR *)(Dpram_Addr[i][0]+0x3FD);
		Int2Dsp[i]=(CHAR *)(Dpram_Addr[i][0]+0x3FE);

		// clear interrupt line, if PC was waked by DSP already.
		_read_dpramreg(ADDR_DSP_ACK, &val);

		DpramExistChk[i][0]=(CHAR *)(Dpram_Addr[i][0]);
		DpramExistChk[i][1]=(CHAR *)(Dpram_Addr[i][0]+1);
	}
	fLoadOk = 1;
	return	MMC_OK;
}

void Addr_Release()
{
	CloseHandle(hMutex_Axis_Comm);
}


/**********
*	FUNCTION NAME : SRAM_Addr_Init()
*	FUNCTION      : Initial SRAM Address for Data Backup
*********************************************************************/
INT SRAM_Addr_Init(VOID)
{
//#if OSTYPE==OS_LYNX && defined (__SRAM_SAVE)
//	CHAR		name[20], *addr;
//	INT			flag;
//	unsigned LONG	size;
//
//	strcpy(name,"BaseOfAXIS_Data");
//	addr=(char *)(AXIS_SRAM_BASE);	/* Start address of MMC BOOTFRAME */
//	size=0x1000;	/* (4096bytes (minimum size)  * 1) */
//	flag=SM_READ | SM_WRITE;
//
//	smem_create(name, addr, size, SM_DETACH);
//	smem_remove(name);
//	BackAC_Data = (BOOT_FRAME_TYPE *)smem_create(name, addr, size, flag);
//	if(BackAC_Data==0)  {
//		printf("Backup memory assign failure\n");
//		return	MMC_NOT_INITIALIZED;
//	}
//	BackAC_Chk = (char *)(&BackAC_Data[MMC_BOARD_NUM]);
//#endif
	return	MMC_OK;
}

extern void ResetAmcDataEvent();
/**********
*	FUNCTION NAME : MMC_Bd_Num_Chk()
*	FUNCTION      : MMC On Board Number Count
*********************************************************************/
INT MMC_Bd_Num_Chk ()
{
    MYLOG("MMC_Bd_Num_Chk\n");

    int i, j, dummy;
	INT err;

	if (mmc_error = err = MMCMutexLock ()) return err;
	MMC_Bd_Num=0;

	ResetAmcDataEvent();
	dummy = *Ack2Dsp[0];

	for (i=0; i<MMC_BOARD_NUM; i++)
	{
		dummy = *Ack2Dsp[i];
		*Ack2Dsp[i] = 0;
		*Int2Dsp[i] = PUT_EXIST_CHK;
		_flush_dpram(PUT_EXIST_CHK);
		if(_wait_for_reply(1000) == AMC_SUCCESS) MMC_Bd_Num ++;

		for (j=0; j<32000; j++)
		{
			if (*Ack2Dsp[i] == PUT_EXIST_CHK)
			{
				MMC_Bd_Num++;
				break;
			}
		}
	}

	MMCMutexUnlock ();
	if(MMC_Bd_Num)	
	{
		mmc_error = MMC_OK;
		return	MMC_OK;
	}
	mmc_error = MMC_NON_EXIST;
	return	MMC_NON_EXIST;
}
/**********
*	FUNCTION NAME : MMC_Axis_Num_Chk()
*	FUNCTION      : MMC On Board Axis Number Count
*********************************************************************/
INT MMC_Axis_Num_Chk ()
{
    MYLOG("MMC_Axis_Num_Chk\n");
    
    int i = 0, j;

	Active_Axis_Num = 0;
	for (j=0; j<MMC_Bd_Num; j++)
	{
		_read_dpramregs(DPRAM_COMM_BASEOFS + CD_AxisNum, &i, 2);

		if (i<=0 || i>8)
		{
			if (!j) BootFrame[j].Axis_Num = 1;
			else    BootFrame[j].Axis_Num = 0;
		}
		else 
		{
			_read_dpramregs(DPRAM_COMM_BASEOFS + CD_AxisNum, &i, 2);
			BootFrame[j].Axis_Num = i;
		}
		Active_Axis_Num += BootFrame[j].Axis_Num;
		BootFrame[j].Action_Axis_Num = Active_Axis_Num;
	}
	return	MMC_OK;
}

void MakeDpramAddress()
{
	int bd_num, i, ax_num = 0;
	for (bd_num=0; bd_num<MMC_Bd_Num; bd_num++)
		for (i=0; i<BootFrame[bd_num].Axis_Num; i++, ax_num++)
			AxisDpram[ax_num]=(AXIS_DPRAM_TYPE *)Dpram_Addr[bd_num][i];

}

/**********
*	FUNCTION NAME : Para_Ini()
*	FUNCTION      : Syatem Parameter Initialize
*********************************************************************/
INT Para_Ini (VOID)
{
    MYLOG("Para_Ini\n");

    BOOL bParamFileExist = TRUE;

	INT i;
	INT err;

	SRAM_Addr_Init ();
	if (mmc_error = err = BootFrameRead(&bParamFileExist)) return err;
	if ((mmc_error = err = Addr_init())>0) return err;
	if ((mmc_error = err = MMC_Bd_Num_Chk())>0) return err;
	for (i=0; i<MMC_Bd_Num; i++)
	{
		mmc_error = err = version_chk(i, &Version_Info[i]);
	}

	MMC_Axis_Num_Chk ();

	MakeDpramAddress();

	if (bParamFileExist == FALSE)
	{
		// amcparam.ini 파일이 없으면, 
		mmc_error = err = MMC_BOOTPARAM_NOT_EXIST;
		mmc_error = err = PUT_Boot_Frame ();
	}


	return	err;
}

/**********
*	FUNCTION NAME	: Put_Boot_Frame()
*	FUNCTION       : Load Boot Memory
*********************************************************************/
INT PUT_Boot_Frame (VOID)
{
	INT	i,bd_num,ax_num,bn[MMC_BOARD_NUM], err=MMC_OK;
	double	d_val;

	ax_num=0;
	Active_Axis_Num = 0;

	ax_num=0;
	for (bd_num=0; bd_num<MMC_Bd_Num; bd_num++)
	{
		bn[bd_num] = bd_num;
		Active_Axis_Num += BootFrame[bd_num].Axis_Num;
		for (i=0; i<BootFrame[bd_num].Axis_Num; i++, ax_num++)
		{
			d_val=(double)BootFrame[bd_num].Error_Limit[i];
			d_val=(double)BootFrame[bd_num].SwLower_Limit[i];
			d_val=(double)BootFrame[bd_num].SwUpper_Limit[i];

            d_val=BootFrame[bd_num].In_Position[i];
		}
		io_interrupt_pcirq_eoi(bd_num);
	}
	return	mmc_error = err;
}

/**********
*	FUNCTION NAME	: Put_Axis_Boot_Frame()
*	FUNCTION		: Load Boot One Axis Memory
*********************************************************************/
INT PUT_Axis_Boot_Frame (INT ax)
{
	double	d_val;
	INT		bn, jnt, err=MMC_OK;
	INT		coeff[GAIN_NUMBER];

	if (mmc_error = err = Find_Bd_Jnt (ax, &bn, &jnt)) return err;
	if ((mmc_error = err = MMCMutexLock ())!=MMC_OK) return err;

	coeff[0] = BootFrame[bn].PGain[jnt];
	coeff[1] = BootFrame[bn].IGain[jnt];
	coeff[2] = BootFrame[bn].DGain[jnt];
	coeff[3] = BootFrame[bn].FGain[jnt];
	coeff[4] = BootFrame[bn].ILimit[jnt];

    if ((err=set_filter (ax,(pUINT)coeff))) goto quit_Axis_Boot_Frame;

	coeff[0] = BootFrame[bn].VPgain[jnt];
	coeff[1] = BootFrame[bn].VIgain[jnt];
	coeff[2] = BootFrame[bn].VDgain[jnt];
	coeff[3] = BootFrame[bn].VFgain[jnt];
	coeff[4] = BootFrame[bn].VIlimit[jnt];

	if ((err=set_v_filter (ax,(pUINT)coeff))) goto quit_Axis_Boot_Frame;
	if ((err=set_p_integration (ax, BootFrame[bn].PosImode[jnt])))
		goto quit_Axis_Boot_Frame;
	if ((err=set_v_integration (ax, BootFrame[bn].VelImode[jnt])))
		goto quit_Axis_Boot_Frame;

	if (BootFrame[bn].Motor_Type[jnt]==STEPPER)
	{
		if ((err=set_stepper (ax))) goto quit_Axis_Boot_Frame;
	}
	else if (BootFrame[bn].Motor_Type[jnt]==MICRO_STEPPER)
	{
		if ((err=set_micro_stepper (ax))) goto quit_Axis_Boot_Frame;
	}
	else
		if ((err=set_servo (ax))) goto quit_Axis_Boot_Frame;

	if ((err=set_home_level(ax,(INT)BootFrame[bn].Home_Level[jnt])))            goto	quit_Axis_Boot_Frame;
	if ((err=set_positive_level(ax,(INT)BootFrame[bn].Pos_Level[jnt])))         goto	quit_Axis_Boot_Frame;
	if ((err=set_negative_level(ax,(INT)BootFrame[bn].Neg_Level[jnt])))         goto	quit_Axis_Boot_Frame;
	if ((err=set_amp_fault_level(ax,(INT)BootFrame[bn].Amp_Level[jnt])))        goto	quit_Axis_Boot_Frame;
	if ((err=set_amp_reset_level(ax,(INT)BootFrame[bn].Amp_Reset_Level[jnt])))  goto	quit_Axis_Boot_Frame;
	if ((err=set_amp_enable_level(ax,(INT)BootFrame[bn].Amp_OnLevel[jnt])))     goto	quit_Axis_Boot_Frame;

	if((err=set_home(ax,(INT)BootFrame[bn].Home_Limit_St[jnt])))                goto	quit_Axis_Boot_Frame;
	if((err=set_positive_limit(ax,(INT)BootFrame[bn].Pos_Limit_St[jnt])))       goto	quit_Axis_Boot_Frame;
	if((err=set_negative_limit(ax,(INT)BootFrame[bn].Neg_Limit_St[jnt])))       goto	quit_Axis_Boot_Frame;
	if((err=set_amp_fault(ax,(INT)BootFrame[bn].Amp_Fault_Event[jnt])))         goto	quit_Axis_Boot_Frame;

	if((err=set_electric_gear(ax,BootFrame[0].GearRatio[ax])))                  goto	quit_Axis_Boot_Frame;

	d_val=(double)BootFrame[bn].Error_Limit[jnt];
	if((err=set_error_limit(ax,d_val,(INT)BootFrame[bn].Error_Limit_St[jnt])))  goto quit_Axis_Boot_Frame;
	d_val=(double)BootFrame[bn].SwLower_Limit[jnt];
	if((err=set_negative_sw_limit(ax,d_val,(INT)BootFrame[bn].SwLower_LimitSt[jnt]))) goto quit_Axis_Boot_Frame;
	d_val=(double)BootFrame[bn].SwUpper_Limit[jnt];
	if((err=set_positive_sw_limit(ax,d_val,(INT)BootFrame[bn].SwUpper_LimitSt[jnt]))) goto quit_Axis_Boot_Frame;

	if((err=set_control(ax,(INT)BootFrame[bn].Control_Cfg[jnt])))               goto	quit_Axis_Boot_Frame;
	if((err=set_feedback(ax,(INT)BootFrame[bn].Encoder_Cfg[jnt])))              goto	quit_Axis_Boot_Frame;
	if((err=set_unipolar(ax,(INT)BootFrame[bn].Voltage_Cfg[jnt])))              goto	quit_Axis_Boot_Frame;
	if((err=set_closed_loop(ax,(INT)BootFrame[bn].Loop_Cfg[jnt])))              goto	quit_Axis_Boot_Frame;
	if((err=set_index_required(ax,(INT)BootFrame[bn].Home_Index[jnt])))         goto	quit_Axis_Boot_Frame;

	if((err=set_stop_rate(ax,(INT)BootFrame[bn].Stop_Rate[jnt])))               goto	quit_Axis_Boot_Frame;
	if((err=set_e_stop_rate(ax,(INT)BootFrame[bn].E_Stop_Rate[jnt])))           goto	quit_Axis_Boot_Frame;

	d_val=(double)BootFrame[bn].In_Position[jnt];
	if((err=set_in_position(ax,d_val)))	                                        goto	quit_Axis_Boot_Frame;

	if((err=set_step_mode(ax,(INT)BootFrame[bn].PulseMode[jnt])))               goto	quit_Axis_Boot_Frame;
	if((err=set_pulse_ratio(ax,BootFrame[bn].PulseRatio[jnt])))                 goto	quit_Axis_Boot_Frame;


quit_Axis_Boot_Frame:
	MMCMutexUnlock ();
	return 	mmc_error = err;
}


/**********
*	FUNCTION NAME	: error_message(INT code, CHAR *dst)
*	FUNCTION       : Copy Error Message To *Dst
*********************************************************************/
/*
INT error_message1(INT code, CHAR *dst)
{
	strcpy(dst,Error_Msg[code]);
	return	MMC_OK;
}
*/


/**********
*	FUNCTION NAME : error_message(INT	code)
*	FUNCTION      : Return Error Message Pointer
*********************************************************************/
/*
pCHAR _error_message(INT		code)
{
	return	Error_Msg[code];
}
*/

