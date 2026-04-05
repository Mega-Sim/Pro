#ifndef		__AMC_INTERNAL_H
#define		__AMC_INTERNAL_H

#include "amcdef.h"

#include "cmdset.h"


#ifdef AMCLIB_EXPORTS
#define AMCLIB_API __declspec(dllexport)
#else
#define AMCLIB_API __declspec(dllimport)
#endif



#ifdef __cplusplus
extern "C"
{
#endif


AMCLIB_API int p2p_move(int axis, unsigned short cmd, 
							unsigned long pos, unsigned short acc, 
							unsigned short dcc, float vel);

/****     I/O Port Control     ****/
//AMCLIB_API int set_io(int port, unsigned long data[2]);
//AMCLIB_API int get_out_io(int port, unsigned long data[2]);

AMCLIB_API int gpio_set(const unsigned char value[8]);
AMCLIB_API void gpio_get(unsigned char value[8]);
/***********************************/

////////////////////////////////////////////////////////////////////
AMCLIB_API int dspreg_set(const unsigned int addr, const unsigned int data);
AMCLIB_API int dspreg_get(const unsigned int addr, unsigned int *data);
////////////////////////////////////////////////////////////////////
AMCLIB_API int _wait_for_reply(unsigned int timeout);
AMCLIB_API BOOL _write_dpramreg(unsigned long addr, unsigned char val);
AMCLIB_API BOOL _read_dpramreg(unsigned long addr, unsigned char *val);//2.8.05, 2011.10.20
AMCLIB_API BOOL _write_dpramregs(unsigned long saddr, const void *value, int len);
AMCLIB_API BOOL _read_dpramregs(unsigned long saddr, void *value, int len);
AMCLIB_API void _flush_dpram(unsigned char cmd);

/*	PINIT.C		*/
AMCLIB_API INT		DpramAddr (VOID);
AMCLIB_API INT		Addr_init (VOID);
AMCLIB_API INT		MMC_Bd_Num_Chk (VOID);
AMCLIB_API INT		Para_Ini (VOID);
AMCLIB_API VOID	int_Set_Vect (VOID);
AMCLIB_API VOID	int_Disable (VOID);
AMCLIB_API INT		PUT_Boot_Frame (VOID);
AMCLIB_API INT		PUT_Axis_Boot_Frame (INT);
AMCLIB_API INT		SRAM_Addr_Init (VOID);

/*	PACFG.C		*/
AMCLIB_API INT 		Stepper(INT, INT, INT, INT);

/*	PFRAME.C		*/
/*	PTRAJ.C		*/
/*	PSTATUS.C		*/
/*	PMOVS.C		*/
AMCLIB_API INT		CP_Move(INT, INT);
AMCLIB_API INT		Circle_Move(VOID);
AMCLIB_API INT		Find_MMC_Num(INT);
AMCLIB_API INT		W_G_CommDpram(INT);
AMCLIB_API INT		W_A_CommDpram(INT, INT);
AMCLIB_API INT		get_Buflast_command(INT, pDOUBLE);
AMCLIB_API INT		spl_line_move(INT,pDOUBLE);
AMCLIB_API INT   	spl_arc_move(INT, double, double, pDOUBLE, double, INT, INT);
/*INT 		rect_move(pDOUBLE, pDOUBLE, pDOUBLE, pDOUBLE);*/


/*	PLIMIT.C		*/
/*	PIO.C		*/
AMCLIB_API INT		SetReset_Bit_IO(INT, INT);
AMCLIB_API INT		Find_IO_Bit(INT);
AMCLIB_API INT		Find_IO_Port(INT);

/*	PGAIN.C		*/
AMCLIB_API INT		Gain_RW(INT, pINT, INT, INT, INT);

/*	PSAVE.C		*/
AMCLIB_API INT		BootFrameRead(BOOL *pBool);
AMCLIB_API INT		BootFrameStore(VOID);
AMCLIB_API INT		ChkSumRead(INT, CHAR *);
AMCLIB_API INT		ChkSumStore(INT);
AMCLIB_API INT		ChkSumReStore(VOID);
AMCLIB_API INT		InitChkSum(INT);
AMCLIB_API INT		check_sum(INT);


/*	PETC.C		*/
INT		set_boot_axis(INT, INT);


#if 0
/*	PSTOP.C		*/
/*	PLIB.C		*/
AMCLIB_API INT		MMCMutexLock (void);
AMCLIB_API INT		MMCMutexUnlock (void);
AMCLIB_API INT		CommWrite(INT,INT);
AMCLIB_API INT		CDIWrite(INT, INT, INT);
AMCLIB_API INT		CDIRead(INT, pINT, INT);
AMCLIB_API INT		CDLWrite(INT ,LONG, INT);
AMCLIB_API INT		CDLRead(INT ,pLONG, INT);
AMCLIB_API INT		CDFWrite(INT, FLOAT, INT);
AMCLIB_API INT		CDFRead(INT, pFLOAT, INT);
AMCLIB_API INT		CDFDWrite(INT ax, double pos, INT comm);
AMCLIB_API INT		CDI3Write(INT, LONG, INT, INT);
AMCLIB_API INT		CDI3Read(INT, pLONG, pINT, INT);
AMCLIB_API INT		MMCCommCheck(INT, pINT, INT, INT);
AMCLIB_API INT		Find_Bd_Jnt(INT, pINT, pINT);
/*	PLIB1.C		*/
AMCLIB_API INT		CDINoBootWrite(INT ,INT , INT);
AMCLIB_API INT		CDFNoBootWrite(INT ,FLOAT, INT);
AMCLIB_API INT		CDFDNoBootWrite(INT, double, INT);
AMCLIB_API INT		CDI3NoBootWrite(INT ,LONG, INT, INT);
#endif
AMCLIB_API INT		Init_Boot_Frame(INT);//2011.10.8, Warning

/*	PHOME.C		*/
/*	PINT.C		*/
/*	PAMP.C		*/
/*	PANALOG.C		*/
AMCLIB_API INT		Find_Analog_Channel(INT);
/*	PINTER.C 		*/

AMCLIB_API INT		mmc_interrupt_count (pINT);		/* hello */
AMCLIB_API INT 	set_dpram_addr(INT, LONG);
AMCLIB_API INT 	error_message1(INT, pCHAR);
AMCLIB_API pCHAR	_error_message(INT);

AMCLIB_API INT mmc_axes(INT, pINT);
AMCLIB_API INT mmc_all_axes(VOID);


/*#if defined(_WINDOWS) || defined(WIN32)*/
/* In windows programs, there is no easy way to export its global variable,
* other than functions to other module referencing this DLL.*/
AMCLIB_API INT 	get_mmc_error( VOID);
AMCLIB_API LONG	get_version( VOID);
AMCLIB_API INT	get_axis_num( VOID);
AMCLIB_API INT	get_bd_num( VOID);

AMCLIB_API INT io_interrupt_enable(INT, INT);
AMCLIB_API INT io_interrupt_on_stop(INT, INT);
AMCLIB_API INT io_interrupt_on_e_stop(INT, INT);
//2.05.22, 2.08.04, 2011.10.13 사용하지 않는 명령어 삭제
//AMCLIB_API INT io_interrupt_pcirq(INT, INT);

AMCLIB_API INT io_interrupt_pcirq_eoi(INT);

AMCLIB_API INT	set_interpolation(INT, pINT,pLONG,INT);
AMCLIB_API INT	frames_interpolation(INT);


#ifdef __cplusplus
}
#endif



#endif




