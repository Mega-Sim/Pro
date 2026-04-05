#include <stdio.h>
#include  	"pcdef.h"

/**********
*	FUNCTION NAME	: io_interrupt_enable(INT state)
*	FUNCTION       : En/Dis host CPU interrupt (User i/o bit0,IRQ3)
*********************************************************************/
INT io_interrupt_enable(INT bn, INT state)
{
	INT	ax;

	if(bn<0 || bn>=MMC_Bd_Num)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	if(state<FALSE || state>TRUE)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if(bn==0)	ax=0;
	else			ax=BootFrame[bn-1].Action_Axis_Num;

	return (CDINoBootWrite(ax,state, PUT_INT_ENABLE));
}

INT fio_interrupt_enable(INT bn, INT state)
{
	INT	ax;

	if(bn<0 || bn>=MMC_Bd_Num)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	if(state<FALSE || state>TRUE)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	if(bn==0)	ax=0;
	else			ax=BootFrame[bn-1].Action_Axis_Num;

	return (CDIWrite(ax,state, PUT_INT_ENABLE));
}

/**********
*	FUNCTION NAME	: io_interrupt_on_stop(INT ax, INT state)
*	FUNCTION       : Specified Axis interrupt stop action define
*********************************************************************/
INT io_interrupt_on_stop(INT ax, INT state)
{
	if(state<FALSE || state>TRUE)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDINoBootWrite(ax,state, PUT_INT_STOP));
}

INT fio_interrupt_on_stop(INT ax, INT state)
{
	if(state<FALSE || state>TRUE)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDIWrite(ax,state, PUT_INT_STOP));
}

/**********
*	FUNCTION NAME	: io_interrupt_on_e_stop(INT ax, INT state)
*	FUNCTION       : Specified Axis interrupt e_stop action define
*********************************************************************/
INT io_interrupt_on_e_stop(INT ax, INT state)
{
	if(state<FALSE || state>TRUE)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDINoBootWrite(ax,state, PUT_INT_E_STOP));
}

INT fio_interrupt_on_e_stop(INT ax, INT state)
{
	if(state<FALSE || state>TRUE)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}
	return (CDIWrite(ax,state, PUT_INT_E_STOP));
}

/**********
*	FUNCTION NAME	: io_interrupt_pcirq_eoi(INT bn)
*	FUNCTION       : Specified Axis interrupt pc interrupt action define
*********************************************************************/
INT io_interrupt_pcirq_eoi(INT bn)
{
	INT	dummy;
	CHAR	*ptr;

	if(bn<0 || bn>=MMC_Bd_Num)
	{
		mmc_error = MMC_ILLEGAL_PARAMETER;
		return	MMC_ILLEGAL_PARAMETER;
	}

	ptr=(CHAR *)(Dpram_Addr[bn][0]+0x3FF);
	dummy=*ptr;

	mmc_error = MMC_OK;
	return	MMC_OK;
}

