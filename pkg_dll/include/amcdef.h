#ifndef __AMCDEF_H__
#define __AMCDEF_H__

#ifdef AMCLIB_EXPORTS
#define AMCLIB_API __declspec(dllexport)
#else
#define AMCLIB_API __declspec(dllimport)
#endif


/*-----------------------------------------------------------
*	True and False
*---------------------------------------------------------*/
#define		TRUE				1
#define		FALSE				0

/*-----------------------------------------------------------
*	High and Low
*---------------------------------------------------------*/
#define		HIGH				1
#define		LOW  				0

/*-----------------------------------------------------------
*	High and Low
*---------------------------------------------------------*/
#define		CIR_CCW 			0
#define		CIR_CW  			1


/*-----------------------------------------------------------
*	Event Source Status defines
*---------------------------------------------------------*/
#define		ST_NONE					0x0000
#define		ST_HOME_SWITCH			0x0001
#define		ST_POS_LIMIT			0x0002
#define		ST_NEG_LIMIT   			0x0004
#define		ST_AMP_FAULT			0x0008
#if (defined(__AMC_SMD) || defined(__AMC_29x))
#define		ST_A_LIMIT    			0x0010
#endif

#if defined(__AMC_V70)
#define		ST_SYSTEM_INSEC    		0x0010
#endif

#define		ST_V_LIMIT  			0x0020
#define		ST_X_NEG_LIMIT 			0x0040
#define		ST_X_POS_LIMIT			0x0080
#define		ST_ERROR_LIMIT			0x0100
#define		ST_PC_COMMAND  			0x0200
#define		ST_OUT_OF_FRAMES    	0x0400
#define		ST_AMP_POWER_ONOFF  	0x0800

/*-----------------------------------------------------------
*	Event defines
*---------------------------------------------------------*/
#define		NO_EVENT			0 	/* ignore a condition */
#define		STOP_EVENT			1	/* generate a stop event */
#define 	E_STOP_EVENT		2 	/* generate an e_stop event */
#define		ABORT_EVENT			3 	/* disable PID control, and disable the amplifier */

/*-----------------------------------------------------------
*	Digital Filter Defines
*---------------------------------------------------------*/
#define		GAIN_NUMBER			5	/* elements expected get/set_filter(...) */
#define		GA_P				0	/* proportional gain */
#define		GA_I				1	/* integral gain */
#define		GA_D				2	/* derivative gain-damping term */
#define		GA_F       			3	/* velocity feed forward */
#define		GA_ILIMIT 			4	/* integration summing limit */

/*-----------------------------------------------------------
*	Error Defines
*---------------------------------------------------------*/
#define		MAX_ERROR_LEN				80 	/* maximum length for error massage string */
#define		MMC_OK						0	/* no problems */
#define		MMC_NOT_INITIALIZED			1	/* be sure to call mmc_init(...) */
#define		MMC_TIMEOUT_ERR				2	/* DPRAM Communication Error */
#define		MMC_INVALID_AXIS			3	/* axis out of range or not configured */
#define		MMC_ILLEGAL_ANALOG			4	/* analog channel < 0 or > 4 */
#define		MMC_ILLEGAL_IO				5	/* illegal I/O port */
#define		MMC_ILLEGAL_PARAMETER		6	/* move with zero accel or velocity */
#define		MMC_NO_MAP					7 	/* The map_axes(...) funcation has not been called */
#define		MMC_AMP_FAULT				8 	/* amp fault occured */
#define		MMC_ON_MOTION				9  	/* Motion is not completed */
#define		MMC_NON_EXIST				10	/* MMC Board is not exist */
#define		MMC_BOOT_OPEN_ERROR			11	/* MMC Boot File Read/Write Error*/
#define		MMC_CHKSUM_OPEN_ERROR		12	/* MMC Checksum File Read/Write Error*/
#define		MMC_WINNT_DRIVER_OPEN_ERROR	13	/* MMC Windows NT Driver Open Error*/
#define		MMC_BOOTPARAM_SIZE_ERROR	14	/* DSP sysparameter size is not mismatch with PC syspraam */
#define		MMC_BOOTPARAM_CRC_ERROR		15	/* DSP sysparameter size is not mismatch with PC syspraam */
#define		MMC_BOOTPARAM_NOT_EXIST		16	/* "AMCPARAM.INI" File not exist. 2007.6.25, ckyu */
#define		AMC_VERSION_ERROR			17	/* DSP와 PC간 라이브러리 버전이 다른 경우, 2.8.05, 2011.10.20*/
#define		FUNC_ERR					-1	/* Function Error				*/

#define		MMC_ILLEGAL_PARAM_MOVE_DS	14	/* 새로 추가된 에러코드 */

/*-----------------------------------------------------------
*	MMC Board Define
*---------------------------------------------------------*/
#define		MMC_BOARD_NUM			1	/* Total DSP board number		*/
#define		BD_AXIS_NUM				4	/* DSP 1-bd axes number			*/
#define		TOTAL_AXIS_NUM			(MMC_BOARD_NUM * BD_AXIS_NUM)
												/* Total Action Axes Number		*/

#define		MMC_BD1					0	/* DSP board #1				*/
#define		MMC_BD2					1	/* DSP board #2				*/
#define		MMC_BD3					2	/* DSP board #3				*/
#define		MMC_BD4					3	/* DSP board #4				*/

#define		MAX_FRAME_NUM			50	/* Max Q_Buffer Number			*/

/*-----------------------------------------------------------
*	General Defines
*---------------------------------------------------------*/
#define		POSITIVE					1
#define		NEGATIVE					0

/*-----------------------------------------------------------
*	Motor Type
*---------------------------------------------------------*/
#define		SERVO_MOTOR    0
#define		STEPPER        1
#define		MICRO_STEPPER  2

/*-----------------------------------------------------------
*	Feedback Configuration
*---------------------------------------------------------*/
#define		FB_ENCODER		0
#define		FB_UNIPOLAR		1
#define		FB_BIPOLAR		2

/*-----------------------------------------------------------
*	Control_Loop Method
*---------------------------------------------------------*/
#define		OPEN_LOOP		0
#define		CLOSED_LOOP		1

/*-----------------------------------------------------------
*	Control Method
*---------------------------------------------------------*/
#define		V_CONTROL		0
#define		T_CONTROL		1

#define		IN_STANDING		0
#define		IN_ALWAYS		1

#define		TWO_PULSE		0
#define		SIGN_PULSE		1
/*-----------------------------------------------------------
*	Limit Vlaue
*---------------------------------------------------------*/
#define		MMC_ACCEL_LIMIT		2500
#define		MMC_VEL_LIMIT		(100*8192)
#define		MMC_POS_SW_LIMIT	2147483647
#define		MMC_NEG_SW_LIMIT	-2147483647
#define		MMC_ERROR_LIMIT	35000
#define		MMC_PULSE_RATIO	255
/*-----------------------------------------------------------
*	Type Define
*---------------------------------------------------------*/
#define		CHAR		char
#define		UINT		unsigned int
#define		INT			int
#define		LONG		long
#define		FLOAT		float
#define		VOID		void

#if defined(__AMC_V70)
#define		POWER3P3	0
#define		POWER5P		1
#define		POWER12P	2	
#define		POWER12M	3
#define		AXIS0_VEL	4
#define		AXIS1_VEL	5
#define		AXIS2_VEL	6
#define		AXIS3_VEL	7
#endif

/*#ifdef WIN32*/
/* WIN32 programs which do not use the WINAPI doesn't need to include
*  the "windows.h", so the WIN32 sections are moved here.*/
typedef		CHAR*		pCHAR;		
typedef     INT*		pINT;		
typedef     UINT*		pUINT;		
typedef		double*		pDOUBLE	;
typedef		LONG*		pLONG;	
typedef		FLOAT*		pFLOAT;	
/*#else*/

/*#ifdef _WINDOWS*/
/* Windows programs should access far reference to library functions
*  and their agrement whatever memory model was used other than large.
*  Also, the function calling convention should be pascal.*/
/*#define	API		far pascal _export*/
/*#define	pCHAR	CHAR far **/
/*#define	pINT	INT far **/
/*#define	pDOUBLE	double far **/
/*#define	pLONG	LONG far **/
/*#define	pFLOAT	FLOAT far **/

/*#endif*/
#ifdef __cplusplus
extern "C" {
#endif


#ifdef __cplusplus
}
#endif

#endif
