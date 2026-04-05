#ifndef __AMC_H__
#define __AMC_H__

#ifdef AMCLIB_EXPORTS
#define AMCLIB_API __declspec(dllexport)
#else
#define AMCLIB_API __declspec(dllimport)
#endif

#include "amc_define.h"
#include "amcdef.h"

//Version : 9 00 05
#define VER_MAJOR      9
#define VER_MINOR      0
#define VER_BUILD      0 // Not used 
#define VER_REVISION   5        


#define VERSION_PCLIB   VER_MAJOR*10000 + VER_MINOR*100 + VER_REVISION	 // OHT V8.2

#define STRINGIFY2(x) #x
#define STRINGIFY(x)  STRINGIFY2(x)

#define VER_STR  STRINGIFY(VER_MAJOR) "." STRINGIFY(VER_MINOR) "." STRINGIFY(VER_REVISION)


#define MDF_FUNC 1 ////2.5.25v2.8.07���� ���� 120120 syk, �����open�Լ� ���� �������� ���� ����

struct amc_gain
{
	unsigned short pgain;  // Proportional Gain
	unsigned short igain;  // Integral Gain
	unsigned short dgain;  // Derivative Gain
	unsigned short fgain;  // Feedforword Gain
	unsigned short ilimit;  // Integration summing limit
};


#define	JPC_AXIS		4

#define	TRAPEZOID		1
#define	S_CURVE			2
#define	T_RELATIVE		3
#define	S_RELATIVE		4
#define	PARABOLIC		5

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
#define		AMC_VERSION_ERROR			17	/* DSP�� PC�� ���̺귯�� ������ �ٸ� ���? 2.8.05, 2011.10.20*/
#define		FUNC_ERR					-1	/* Function Error				*/

#define		MMC_ILLEGAL_PARAM_MOVE_DS	14	/* ���� �߰��� �����ڵ� */

#define		MMC_MOVE_LASTDS				30	/* move_ds�� ������ n-1 ��° �������� ���� ������ �˸� 20120301���� syk*/
#define		MMC_MUTEXLOCK_ERROR			31	/* MUTEXLOCK ERROR /dsp timeout error�� �����ϱ� ���� �и�  20120316 2.9.2 syk*/
#define		MMC_FILTERING_ERROR			32	/* MUTEXLOCK ERROR /dsp timeout error�� �����ϱ� ���� �и�  20120316 2.9.2 syk*/
#ifdef __cplusplus
extern "C"
{
#endif
    
//////////////////////////////////////////Service 180////////////////////////////////////////////////////
//#pragma pack(1)
typedef struct _CMD_AXIS
{
	volatile char CmdType;	// Move (P,N,S,DS - TimeBase, AccelBase) / reset / SetHome  / SetOffset
	volatile int Accel;		// ���ӵ�(Cts/s) or �ð�(msec)
	volatile int Decel;		// ���ӵ�(Cts/s) or �ð�(msec)
	volatile int Speed;		// �ӵ�(Cts/s)
	volatile int Distance;	// �Ÿ�(Cts)
	volatile int TriggerCtrlInfo;
}CMD_AXIS;
//#pragma pack()

//#pragma pack(1)
typedef struct _DETECT_INFO
{
	volatile char	UseFollowControlFlag;	///< ���� ���� ���?����: true �� ���? false�� �̻��?
#if defined(__AMC_V8x)
	volatile char OHTDetectType;		///< OHT���� ���� Area Type
	volatile char OBSDetectType;		///< ��ֹ�?���� ���� Area Type
#else
	volatile char	Type;					///< Link�� Type
#endif
	volatile int	NodeCount;				///< ���?����
	volatile int	DistSumOfStraightPath;	///< ���� ���� �̾����� ���������� ��ġ (Cts)
	volatile int	FollowCommandType;		
	volatile int	PBSIgnoreFlag;	
#if defined(__AMC_V8x)
	volatile int Reserved1;
	volatile int Reserved2;
#endif
}DETECT_INFO;
//#pragma pack()

//#pragma pack(1)
typedef struct _PAUSE_INFO
{
	volatile int	Pause;		
	volatile int	Commander;	
}PAUSE_INFO;
//#pragma pack()

//#pragma pack(1)
// for change node
typedef struct _OPTIONAL_NODE_INFO
{
	volatile char NodeType;				///<	0x05 : Tag-type Optional Node In, 0x06 : Distance-type Optional Node In, 0x07 : Combo-type Optional Node In
	volatile int DistON;				///<Optional Node1 ������ �Ÿ�
	volatile int DistAfterON;			///<Optional Node1 ���Ŀ� ������ �ٲ��?��ġ
	volatile char OHTDetectTypeOpt;		///<Optional Node1 ���� ������ Type(����)
	volatile char OBSDetectTypeOpt;		///<Optional Node1 ���� ������ Type(��ֹ�?

	volatile int Reserved1;
	volatile int Reserved2;
}OPTIONAL_NODE_INFO;

typedef struct _AMC_CMD
{
	volatile int	Cmd;			///< bit ���� ���?: CMD_TYPE bitfield ����
	volatile int	Output[4];	///< Output ����
	CMD_AXIS		CmdAxis[4];	///< �� ���?����
	DETECT_INFO		DetectInfo;	///< ���� ���� Type ������ ���� ����
	PAUSE_INFO		PauseInfo;
#if defined(__AMC_V8x)
	OPTIONAL_NODE_INFO	OptNodeInfo;
#endif
    volatile int    crc;
	volatile int	Sound;		///< OHT Main Sound Command
	volatile int	Reserved_cmd1;
	volatile int	Reserved_cmd2;
}AMC_CMD;
//#pragma pack()

#pragma pack(1)
typedef struct _NODE_CHECK_DATA
{
	volatile int NodeCountOfPreCheck;	///< ���?����
	volatile int NodeCount;				///< ���?����
	
	// SetHome, setOffset �� �ϸ� ���� ��ġ�� �� ������ ������
	volatile int PositionOfDrivingTagPreCheck;	///< ���� ���?���� ������ ��ġ		
	volatile int PositionOfDrivingTag;			///< ���� ���?������ ��ġ			 
	volatile int PositionOfTransTagFirst;		///< ������ ���?������ ��ġ(Front)
	volatile int PositionOfTransTagFinal;		///< ������ ���?������ ��ġ(Rear)
	
}NODE_CHECK_DATA;
#pragma pack()

#pragma pack(1)
typedef struct _AMC_DATA
{
	// Input/Output Data
	volatile int	Input[4];						///< InPut ����
	volatile int	Output[4];					///< Output ����

	// Axis Data
	volatile int	Source[4];					///< �ະ source
	volatile int	State[4];						///< �ະ state
	volatile char	Running[4];					///< �ະ �����̴� �� Ȯ��
	volatile char	InPosition[4];				///< �ະ ���� �Ϸ� Ȯ��
	volatile char	AmpEnable[4];					///< �ະ ���� ���?���� Ȯ��(Servo On, ~~)
	volatile int	Position[4];					///< �ະ ��ġ (Cts)
	volatile int	Velocity[4];					///< �ະ �ӵ� (Cts/sec)
	volatile int	FollowingError[4];			///< �ະ ���� (Cts)
	volatile int	PositionOfSmallAddCheck[4];	///< Small Add ���� �� ���� ���� ��ġ�� ( �̰���,�̻��?�� 0.0 / ���� ��ġ�� 0�� ���?1�� ���� )  (Cts)

	// AMC Log
	volatile int	TargetVel[4];					///< Sample �� �ӵ� Reffernce
	volatile int	TargetPos[4];					///< Sample �� ��ġ
	volatile int	FinalPos[4];					///< ������ ��ġ
	
	volatile int	AxisFlag1[4];
	volatile int	AxisFlag2[4];

	volatile int	VoltageMonitor[2];
	volatile int 	PauseState;
	NODE_CHECK_DATA NodeCheckData;				///< OHTv7 �� ���� �߰� ����
	
}AMC_DATA;
#pragma pack()

#pragma pack(1)
typedef struct _CMD_ANALYSIS
{	
	unsigned int SetOutBit					: 1;		///< I/O�� Output ���� : ��
	unsigned int ClearNodeCount			: 1;		///< Node Count Clear : ��
	
	unsigned int MoveAxis1					: 1;		///< 1�� �̵� ���?: ��
	unsigned int MoveAxis2					: 1;		///< 2�� �̵� ���?: ��
	unsigned int MoveAxis3					: 1;		///< 3�� �̵� ���?: ��
	unsigned int MoveAxis4					: 1;		///< 4�� �̵� ���?: ��

	unsigned int SetDetectInfo				: 1;		///< ���� ���� Type ���� : ��
	unsigned int SetGearRatioOfDriving		: 1;		//1	���� ���� /< ���� Gear �� ���� : ��
	unsigned int SetDecelLimitOfDriving	: 1;		//1	���� ���� /< ���� Limit ���� : ��

	unsigned int CompensateRearEncoder		: 1;		///< Rear���� Front ������ �����ִ� ���?: ��

	unsigned int PauseControl				: 1;

	unsigned int Reserved					: 21;
}CMD_ANALYSIS;
#pragma pack(0)

#pragma pack(1)
typedef struct _DRIVING_PARAM
{
	//2 ���� �⺻ ������
	volatile int	GearRatioOfDriving;		///< ���� Gear ��(Cts/m) --> 1m �� 1mm �̳��� ����
	
	//2 ��������
	volatile int	OHTDetectAreaDist[6];		///< ���� ���� ���� ������ ���� �Ÿ� (���� Cts)
	volatile int	OHTDetectAreaSpeed[6];	///< ���� ���� ���� ������ ���� �ӵ� (���� Cts/s)
	volatile int	OBSDetectAreaDist[6];		///< ��ֹ�?���� ���� ������ ���� �Ÿ� (���� Cts)
	volatile int	OBSDetectAreaSpeed[6];	///< ��ֹ�?���� ���� ������ ���� �ӵ� (���� Cts/s)
	volatile int	AccelOfNormalPath;		///< �������������� ���� ��
	volatile int	DecelOfNormalPath;		///< �������������� ���� ��
	volatile int	AccelOfCurvePath;		///< ����������� ���� ��
	volatile int	DecelOfCurvePath;		///< ����������� ���� ��
}DRIVING_PARAM;
#pragma pack()

#pragma pack(1)
typedef union _CMD_ANALYSIS_UNION
{
	CMD_ANALYSIS CmdAnalysis;	///< AMC_CMD.Cmd�� bitfield
	int Cmd;
}CMD_ANALYSIS_UNION;
#pragma pack()

// AMC_CMD_BIT_ANALYSIS
typedef enum _MOTION_CONTROLLER_CMD_TYPE
{
	CMD_TYPE_SET_OUTBIT					= 0,
	CMD_TYPE_CLEAR_NODE_COUNT			,
	CMD_TYPE_MOVE_AXIS1					,
	CMD_TYPE_MOVE_AXIS2					,
	CMD_TYPE_MOVE_AXIS3					,
	CMD_TYPE_MOVE_AXIS4					,
	CMD_TYPE_SET_DETECT_INFO			,
	CMD_TYPE_SET_GEAR_RATIO_OF_DRIVING	,
	CMD_TYPE_SET_DECEL_LIMIT_OF_DRIVING	,
	CMD_TYPE_COMPENSATE_REAR_ENCODER	,
	CMD_TYPE_PAUSE_CONTROL
}MOTION_CONTROLLER_CMD_TYPE;

//2	Trigger Command
#pragma pack(1)
typedef struct _TRIGGER_CTRL_INFO
{	
	unsigned int StartOnSpeedDown		: 1;		//<���?�ΰ� ����( 0:�ٷ� ����, 1:���� ���������� ���� ������ ���?�ΰ�
	unsigned int UseCurrSpeed			: 2;		//<�����?�ӵ�( 0: ���?���� �ӵ�, 1: ���� �ӵ�, 2:Min(��ɼӵ�? ����ӵ�?, 4:MAX(��ɼӵ�? ����ӵ�? )
	unsigned int UseSmallAdd			: 1;		//<Small Add ( 0: NoUse, 1: Use )
	unsigned int TriggerIONumber		: 8;		//< I/O ��ȣ (0~255): ��
	unsigned int IsTriggerOnRisingEdge	: 1;		//<Triger Type ( 0: Falling Edge, 1: Rising Edge )
	unsigned int AdditionalDist		    : 19;	    //< 1ȸ �߰� �Ÿ� ( 0~2^19 / ���� ������ ���� Ÿ�� �Ÿ����� ȹ��): 524288cts --> ����: 4599mm
}TRIGGER_CTRL_INFO;
#pragma pack()

// AMC_CMD.Cmd �� ���� �����?�м��ϱ� ���� union
#pragma pack(1)
typedef union _TRIGGER_CTRL_INFO_UNION
{
	TRIGGER_CTRL_INFO TriggerInfo;	///< AMC_CMD.Cmd�� bitfield
	int Cmd;
}TRIGGER_CTRL_INFO_UNION;
#pragma pack()

AMCLIB_API int ReturnAMCData(AMC_CMD* Cmd, AMC_DATA *ReturnData);

AMCLIB_API int GatheringTraceData();
AMCLIB_API int GatheringTraceData_1();

/***************************************************************/
AMCLIB_API AMCBOOL amc_open(int intr, unsigned long dpram, char *workdir);
AMCLIB_API void amc_close();
AMCLIB_API INT 	mmc_init(void);

AMCLIB_API INT version_chk_pc();

// �������� ���ϵ� ���� 20100�̶��? ������ 2.01.00��.
AMCLIB_API INT version_chk(int bn /*0 always */, pINT ver); 

AMCLIB_API INT v_move( INT ax, double vel, INT acc);
AMCLIB_API INT set_filter(INT ax, pUINT);
AMCLIB_API INT set_v_filter(INT ax, pUINT);
AMCLIB_API INT set_control(INT ax, INT ctrl);
AMCLIB_API INT set_closed_loop(INT ax, INT cfg);
AMCLIB_API INT set_unipolar(INT ax, INT);
AMCLIB_API INT set_feedback(INT, INT);
AMCLIB_API INT set_servo(INT ax);
AMCLIB_API INT set_amp_enable(INT ax,INT enable);

// stop����
AMCLIB_API INT set_stop(INT ax);
AMCLIB_API INT set_stop_rate(INT ax, INT rate); // rate unit is msec.
AMCLIB_API INT fset_stop_rate(INT, INT);
AMCLIB_API INT get_stop_rate(INT ax, pINT prate); // rate unit is msec.
AMCLIB_API INT fget_stop_rate(INT, pINT);

// e-stop����
AMCLIB_API INT set_e_stop(INT ax);
AMCLIB_API INT set_e_stop_rate(INT ax, INT rate); // rate unit is msec.
AMCLIB_API INT fset_e_stop_rate(INT, INT);
AMCLIB_API INT get_e_stop_rate(INT ax, pINT prate); // rate unit is msec.
AMCLIB_API INT fget_e_stop_rate(INT, pINT);

AMCLIB_API INT start_move( INT, double, double, INT);
AMCLIB_API INT set_position(INT, double);
AMCLIB_API INT set_cleanvoltage(double);
AMCLIB_API INT get_counter(INT, pDOUBLE);

AMCLIB_API INT set_command(INT, double);
AMCLIB_API INT get_command(INT, pDOUBLE);
AMCLIB_API INT get_error(INT, pDOUBLE);

AMCLIB_API INT set_encoder_direction(int ax, int cwccw);
AMCLIB_API INT fset_encoder_direction(int ax, int cwccw);
AMCLIB_API INT get_encoder_direction(int ax, int *pcwccw);
AMCLIB_API INT fget_encoder_direction(int ax, int *pcwccw);

#ifndef MDF_FUNC	
AMCLIB_API INT get_com_velocity(INT);
AMCLIB_API INT get_act_velocity(INT);
AMCLIB_API INT get_com_velocity_rpm(INT);
AMCLIB_API INT get_act_velocity_rpm(INT);

AMCLIB_API INT home_switch(INT);
AMCLIB_API INT pos_switch(INT);
AMCLIB_API INT neg_switch(INT);
AMCLIB_API INT amp_fault_switch(INT);

AMCLIB_API INT in_sequence(INT);
AMCLIB_API INT in_motion(INT);
AMCLIB_API INT in_position(INT);
AMCLIB_API INT axis_state(INT);
AMCLIB_API INT frames_left(INT);
AMCLIB_API INT axis_source(INT);

// 2008.1.7, ckyu
AMCLIB_API INT get_bit(int bitno);
AMCLIB_API INT get_outbit(int bitno);

AMCLIB_API INT get_unipolar(INT);
#else			//2.5.25v2.8.07���� ���� 120120 syk, 5�� ���� �����open�Լ� ���� ����
AMCLIB_API INT get_com_velocity(INT ax, pINT chk_err);
AMCLIB_API INT get_act_velocity(INT ax, pINT chk_err);
AMCLIB_API INT get_com_velocity_rpm(INT ax, pINT chk_err);
AMCLIB_API INT get_act_velocity_rpm(INT ax, pINT chk_err);

AMCLIB_API INT home_switch(INT ax, pINT chk_err);
AMCLIB_API INT pos_switch(INT ax, pINT chk_err);
AMCLIB_API INT neg_switch(INT ax, pINT chk_err);
AMCLIB_API INT amp_fault_switch(INT ax, pINT chk_err);

AMCLIB_API INT in_sequence(INT ax, pINT chk_err);
AMCLIB_API INT in_motion(INT ax, pINT chk_err);
AMCLIB_API INT in_position(INT ax, pINT chk_err);
AMCLIB_API INT axis_state(INT ax, pINT chk_err);
AMCLIB_API INT frames_left(INT ax, pINT chk_err);
AMCLIB_API INT axis_source(INT ax, pINT chk_err);

#if defined(__AMC_V70)
AMCLIB_API INT system_status(pCHAR p_status);
AMCLIB_API INT get_system_moni_enable(char *state); 
AMCLIB_API INT system_moni_enable(char axno,char state);
AMCLIB_API INT get_system_monitering_value(char axno,int *val, int *raw_val, int *compare_val);
AMCLIB_API INT set_monitering_Threshold_percent(char axno,char pcnt);
AMCLIB_API INT get_monitering_Threshold_percent(char axno,char *pcnt);
#endif

AMCLIB_API INT get_bit(int bitno, pINT chk_err);
AMCLIB_API INT get_outbit(int bitno, pINT chk_err);

AMCLIB_API INT get_unipolar(INT ax, pINT chk_err);
#endif

AMCLIB_API INT get_dac_output(INT ax, pINT pvolt);
AMCLIB_API INT set_dac_output(INT ax, INT volt);
AMCLIB_API INT set_analog_offset(INT ax, INT volt);
AMCLIB_API INT get_analog_offset(INT ax, pINT pvolt);

AMCLIB_API INT set_amp_enable_level(INT,INT);
AMCLIB_API INT get_amp_enable_level(INT,pINT);
AMCLIB_API INT get_amp_enable(INT,pINT);
AMCLIB_API INT amp_fault_reset(INT);
AMCLIB_API INT fget_control(INT,pINT);
AMCLIB_API INT fset_control(INT,INT);
AMCLIB_API INT set_pulse_ratio(INT axno,INT pgratio);
AMCLIB_API INT get_pulse_ratio(INT axno,pINT pgratio);
AMCLIB_API INT get_control(INT ax,pINT);
AMCLIB_API INT set_electric_gear(INT, double);
AMCLIB_API INT fset_electric_gear(INT, double);
AMCLIB_API INT get_electric_gear(INT, pDOUBLE);

AMCLIB_API INT set_step_mode(INT, INT);
AMCLIB_API INT fset_step_mode(INT axis, INT mode);
AMCLIB_API INT get_step_mode(INT, pINT);
AMCLIB_API INT fget_step_mode(INT, pINT);

AMCLIB_API INT set_sync_map_axes(INT, INT);
AMCLIB_API INT set_sync_control(INT);
AMCLIB_API INT get_sync_control(pINT);
AMCLIB_API INT set_sync_gain(FLOAT);
AMCLIB_API INT get_sync_gain(pFLOAT);
AMCLIB_API INT get_sync_position(pDOUBLE, pDOUBLE);

AMCLIB_API INT compensation_pos(INT, pINT, pDOUBLE, pINT);

AMCLIB_API INT get_io64(INT, pINT);
AMCLIB_API INT set_io64(INT, pINT);
AMCLIB_API INT get_out64(INT port, INT value[2]);

#if (defined(__AMC_SMD) || defined(__AMC_V70))
// 120820 2.9.8 syk  
AMCLIB_API INT set_io_output(INT port, pINT value);
AMCLIB_API INT get_io_output(INT port, INT value[8]);
AMCLIB_API INT get_io_input(INT, pINT);
AMCLIB_API INT set_io_count(int, int);
AMCLIB_API INT get_io_count(pINT, pINT);
#endif

AMCLIB_API INT set_bit(INT);
AMCLIB_API INT reset_bit(INT);

AMCLIB_API INT fio_interrupt_pcirq(INT, INT);

AMCLIB_API INT dwell(INT, LONG);
AMCLIB_API INT io_trigger(INT,INT,INT);
AMCLIB_API INT fio_interrupt_on_stop(INT, INT);

AMCLIB_API INT set_positive_sw_limit(INT, double, INT );
AMCLIB_API INT fset_positive_sw_limit(INT, double, INT);
AMCLIB_API INT get_positive_sw_limit(INT, pDOUBLE, pINT);
AMCLIB_API INT fget_positive_sw_limit(INT, pDOUBLE, pINT);

AMCLIB_API INT set_negative_sw_limit(INT, double, INT);
AMCLIB_API INT fset_negative_sw_limit(INT, double, INT);
AMCLIB_API INT get_negative_sw_limit(INT, pDOUBLE, pINT);
AMCLIB_API INT fget_negative_sw_limit(INT, pDOUBLE, pINT);
AMCLIB_API INT set_vel_curve(INT, INT, INT);
AMCLIB_API INT get_vel_curve(INT, pINT, pINT);

AMCLIB_API INT set_actvel_margin(INT, INT, INT, INT);
AMCLIB_API INT get_actvel_margin(INT, pINT, pINT, pINT);

AMCLIB_API INT get_accel_limit(INT, pINT);
AMCLIB_API INT set_accel_limit(INT, INT );
AMCLIB_API INT get_vel_limit(INT, pDOUBLE);
AMCLIB_API INT set_vel_limit(INT, double );

AMCLIB_API INT set_positive_limit(INT, INT );
AMCLIB_API INT fset_positive_limit(INT, INT );
AMCLIB_API INT get_positive_limit(INT, pINT);
AMCLIB_API INT fget_positive_limit(INT, pINT);

AMCLIB_API INT set_negative_limit(INT, INT );
AMCLIB_API INT fset_negative_limit(INT, INT );
AMCLIB_API INT get_negative_limit(INT, pINT);
AMCLIB_API INT fget_negative_limit(INT, pINT);

AMCLIB_API INT set_in_position(INT, double);
AMCLIB_API INT fset_in_position(INT, double);
AMCLIB_API INT get_in_position(INT, pDOUBLE);
AMCLIB_API INT fget_in_position(INT, pDOUBLE);

AMCLIB_API INT set_error_limit(INT, double, INT);
AMCLIB_API INT fset_error_limit(INT, double, INT);
AMCLIB_API INT get_error_limit(INT, pDOUBLE, pINT);
AMCLIB_API INT fget_error_limit(INT, pDOUBLE, pINT);

AMCLIB_API INT set_positive_level(INT, INT );
AMCLIB_API INT get_positive_level(INT, pINT);
AMCLIB_API INT set_negative_level(INT, INT );
AMCLIB_API INT get_negative_level(INT, pINT);

AMCLIB_API INT set_oht_model_id(INT OHT_Model_Id);
AMCLIB_API INT get_oht_model_id(pINT OHT_Model_Id);

AMCLIB_API INT set_home_level(INT, INT );
AMCLIB_API INT get_home_level(INT, pINT);
AMCLIB_API INT set_home(INT, INT );
AMCLIB_API INT get_home(INT, pINT);

AMCLIB_API INT set_amp_fault_level(INT, INT );
AMCLIB_API INT get_amp_fault_level(INT, pINT);
AMCLIB_API INT fset_amp_fault(INT, INT );

AMCLIB_API INT set_amp_fault(INT, INT );
AMCLIB_API INT get_amp_fault(INT, pINT);
AMCLIB_API INT fset_filter(INT, pINT);
AMCLIB_API INT get_filter(INT, pUINT);
AMCLIB_API INT fset_v_filter(INT, pINT);
AMCLIB_API INT get_v_filter(INT ax, pUINT);

AMCLIB_API INT get_feedback(INT, pINT);
AMCLIB_API INT get_closed_loop(INT, pINT);

AMCLIB_API INT get_stepper(INT);
AMCLIB_API INT set_amp_reset_level(INT, INT );
AMCLIB_API INT get_amp_reset_level(INT, pINT);

AMCLIB_API INT set_index_required(INT, INT );
AMCLIB_API INT get_index_required(INT, pINT);
AMCLIB_API INT move( INT, double, double, INT);
AMCLIB_API INT start_r_move( INT, double, double, INT);
AMCLIB_API INT r_move( INT, double, double, INT);
AMCLIB_API INT start_s_move( INT, double, double, INT);
AMCLIB_API INT s_move( INT, double,double, INT);
AMCLIB_API INT start_rs_move( INT, double, double, INT);
AMCLIB_API INT rs_move( INT, double,double, INT);

AMCLIB_API INT start_p_move( INT, double, double, INT);
AMCLIB_API INT p_move( INT, double,double, INT);
AMCLIB_API INT start_t_move( INT, double, double, INT, INT);
AMCLIB_API INT t_move( INT, double,double, INT, INT);
AMCLIB_API INT start_ts_move( INT, double, double, INT, INT);
AMCLIB_API INT ts_move( INT, double,double, INT, INT);
AMCLIB_API INT start_tr_move( INT, double, double, INT, INT);
AMCLIB_API INT tr_move( INT, double,double, INT, INT);
AMCLIB_API INT start_trs_move( INT, double, double, INT, INT);
AMCLIB_API INT trs_move( INT, double,double, INT, INT);

AMCLIB_API INT start_move_all( INT, pINT, pDOUBLE, pDOUBLE, pINT);
AMCLIB_API INT move_all( INT, pINT, pDOUBLE, pDOUBLE, pINT);
AMCLIB_API INT start_s_move_all( INT, pINT, pDOUBLE, pDOUBLE, pINT);
AMCLIB_API INT s_move_all( INT, pINT, pDOUBLE, pDOUBLE, pINT);
AMCLIB_API INT start_t_move_all( INT , pINT, pDOUBLE, pDOUBLE, pINT,pINT);
AMCLIB_API INT t_move_all( INT, pINT, pDOUBLE,pDOUBLE, pINT, pINT);
AMCLIB_API INT start_ts_move_all( INT , pINT, pDOUBLE, pDOUBLE, pINT,pINT);
AMCLIB_API INT ts_move_all( INT, pINT, pDOUBLE,pDOUBLE, pINT, pINT);
AMCLIB_API INT wait_for_done( INT);
AMCLIB_API INT wait_for_all( INT, pINT);

AMCLIB_API INT		PTP_Move( INT, double, double, INT,INT,INT);
AMCLIB_API INT map_axes( INT , pINT);
AMCLIB_API INT set_move_speed(double);
AMCLIB_API INT set_move_accel(INT);
AMCLIB_API INT set_arc_division(double);
AMCLIB_API INT all_done(VOID);

AMCLIB_API INT move_2(double, double);
AMCLIB_API INT move_3(double, double, double);
AMCLIB_API INT move_4(double, double, double, double);
//AMCLIB_API INT move_n(pDOUBLE);
AMCLIB_API INT smove_2(double, double);
AMCLIB_API INT smove_3(double, double, double);
AMCLIB_API INT smove_4(double, double, double, double);
AMCLIB_API INT smove_n(pDOUBLE);
AMCLIB_API INT arc_2(double, double, double);
AMCLIB_API INT spl_line_move2(pDOUBLE);
AMCLIB_API INT spl_line_move3(pDOUBLE);
AMCLIB_API INT spl_line_move(INT len, pDOUBLE pnt1);
AMCLIB_API INT spl_arc_move2(double , double, pDOUBLE, double, INT, INT);
AMCLIB_API INT spl_arc_move3(double , double, pDOUBLE, double, INT, INT);
AMCLIB_API INT spl_arc_move(INT len, double x_center, double y_center, pDOUBLE pnt, double vel, INT acc, INT cdir);

AMCLIB_API INT motion_done(INT);
AMCLIB_API INT axis_done(INT);

AMCLIB_API INT clear_status(INT);
AMCLIB_API INT clear_stop(INT);
AMCLIB_API INT frames_clear(INT);

AMCLIB_API INT axis_all_status(int ax, int pIStatus[6], int pIO64[4], double pValue[3]);

AMCLIB_API INT set_sw_pause(int ax, int bOn);
AMCLIB_API int clear_amc_error();
AMCLIB_API INT amp_fault_set(INT);
AMCLIB_API INT get_position(INT, pDOUBLE);

AMCLIB_API INT set_encoder_offset(INT ax, INT nOfs);
AMCLIB_API INT get_encoder_offset(INT ax, INT *pnOfs);

AMCLIB_API INT amc_flush_sysparam_to_eeprom();

AMCLIB_API INT set_stepper(INT);
AMCLIB_API INT fset_stepper(INT);
AMCLIB_API INT get_micro_stepper(INT);
AMCLIB_API INT set_micro_stepper(INT);
AMCLIB_API INT fset_micro_stepper(INT);

AMCLIB_API INT fset_servo(INT);
AMCLIB_API INT fset_feedback(INT, INT);
AMCLIB_API INT fset_closed_loop(INT, INT );
AMCLIB_API INT fset_unipolar(INT, INT);

AMCLIB_API INT set_p_integration(INT, INT);
AMCLIB_API INT get_p_integration(INT, pINT);
AMCLIB_API INT set_v_integration(INT, INT);
AMCLIB_API INT get_v_integration(INT, pINT);


AMCLIB_API INT get_error_status(INT ax, pINT pStatus);
AMCLIB_API INT reload_encoder_position(int ax);
AMCLIB_API INT reload_servopack_all();

/*  Save Long block data to DSP eeprom */
AMCLIB_API INT	DownloadLongBlock(void *pMap, UINT uiEepromSaveAddr, int nBytes);
AMCLIB_API INT amc_download_map_info(char* pMap, int len);

/* ��Ʈ���� DSP�� �ְ�ޱ�����?�������?���?*/
AMCLIB_API INT amc_exchange_string(char *pMsg, int nSendByte, char *pRcvMsg, int *nRcvByte);

// 2007.3.2, ckyu
// EEPROM�� ������ ���� �ްų� ������ �����ϱ����� �뵵.
AMCLIB_API INT UploadParam(char *pMsg, UINT uiEepromSaveAddr, int nBytes);
AMCLIB_API INT DnloadParam(char *pMsg, UINT uiEepromSaveAddr, int nBytes);

// 2007.3.13, ckyu
// ��Ʈ �Ķ���͸���?�÷��ް�, ���������ϱ����� �뵵.
AMCLIB_API INT UploadSysParam(char *pMsg, int *pnBytes);
AMCLIB_API INT DnloadSysParam(char *pMsg, int nBytes);

AMCLIB_API INT Delay(LONG);

AMCLIB_API INT get_local_error();
AMCLIB_API char * _error_message(int code);
AMCLIB_API int error_message(int code, char *dst);

// 2007.6.22, ckyu. DSP�� eeprom�� sysparam�� �����ϴ� �Լ�.
AMCLIB_API INT amc_load_dsp_sysparam_with_localfile(BOOL bCopy, char *pszPath);
AMCLIB_API INT amc_save_local_sysparam_to_dsp();
AMCLIB_API INT BootFrameStoreBd (int bn, char *pszFile);

// move_x ���?
AMCLIB_API INT move_p(int ax, double a, double v);
AMCLIB_API INT move_n(int ax, double a, double v);
AMCLIB_API INT move_s(int ax, double a);
AMCLIB_API INT move_ds(int ax, double a1, double a2, double v, double m);

// move_xt���?acc�� ���ؼ� ����, ���� �ð��� ������)
AMCLIB_API INT move_pt(int ax, double acc, double v);
AMCLIB_API INT move_nt(int ax, double acc, double v);
AMCLIB_API INT move_st(int ax, double acc);
AMCLIB_API INT move_dst(int ax, double acc1, double acc2, double v, double m);

// Do next three steps. ckyu
// 1. read boot parameter from DSP
// 2. update boot parameter with AMCParam.ini file
// 3. download boot parameter to DSP
AMCLIB_API INT amc_adopt_ini_param();


// 2011.8.1, ckyu
// ���尡 15�� �̳��� �ٽ� ��Ƴ���?Ȯ�εǸ� 1�� �����Ѵ�.
// �ȱ׷��� 0
// �Ķ���ʹ�?�ִ� ���ð�(ms)
AMCLIB_API int dsp_reboot_and_chk(UINT uiTOms);

AMCLIB_API void dsp_reboot();
AMCLIB_API INT torque_limit(INT ax, INT on1off0);
AMCLIB_API DWORD GetPhysicalAddr(unsigned long addr);

// 2022.12.15, yjchoe
// Read slave version
// 0: RSA Driving Rear
// 1: RSA Driving Front
// 2: RSA Hoist
// 3: RSA Slide
// 4: Fine Slave Rear
// 5: Fine Slave Front
AMCLIB_API INT get_fw_version(INT nSlaveNum, pINT pVer);
// 2023.01.02, yjchoe
// Read EC-Master Version
AMCLIB_API INT get_ecm_version(pINT pVer);

#define		EEPROM_BOOTPARAM_MAGIC_NO			0x9abcdef0


enum
{
	CMD_MOVE_P = 1,
	CMD_MOVE_N,
	CMD_MOVE_S,
	CMD_MOVE_DS
};



enum
{
	WDT_MAINLOOP	= 1,
	WDT_EXTRA		= 2,
	WDT_SUBCONTROL	= 3,
	WDT_CONTROL		= 4
};

AMCLIB_API INT enable_wdt_reason(int reason);
AMCLIB_API INT disable_wdt_reason(int reason);
AMCLIB_API INT get_wdt_status(unsigned int *puistatus);
AMCLIB_API INT set_wdt_status(unsigned int uistatus);
AMCLIB_API INT clr_wdt_reason(int reason);

AMCLIB_API HANDLE MakeCommOverTCP(TCHAR *port_name);
AMCLIB_API BOOL CloseCommOverTCP(HANDLE h);
AMCLIB_API BOOL _SetCommMask(HANDLE h, DWORD mask);
AMCLIB_API BOOL _SetupComm(HANDLE h, DWORD in, DWORD out);
AMCLIB_API BOOL _PurgeComm(HANDLE h, DWORD arg);
AMCLIB_API BOOL _SetCommTimeouts(HANDLE h, LPCOMMTIMEOUTS tout);
AMCLIB_API BOOL _GetCommState(HANDLE h, LPDCB st);
AMCLIB_API BOOL _SetCommState(HANDLE h, LPDCB st);
AMCLIB_API BOOL _CloseHandle(HANDLE h);
AMCLIB_API BOOL _ReadFile(HANDLE h, PVOID data, DWORD len, PDWORD outlen, LPOVERLAPPED flag);
AMCLIB_API BOOL _WriteFile(HANDLE h, LPCVOID data, DWORD len, LPDWORD outlen, LPOVERLAPPED);
#pragma pack(1)

typedef struct
{
	int jtpos;
	float mfGoal_pos;
	float acc;
	float dcc;
	float vel;
	float vm_WaitPos;
	float velcmd;
	float velerr;
	float tqrcmd;
	float dtBox;
	int pos;
	short axis_source;
	short dac_code;
	char event_st;
	char vm_accflag;
	char vm_count;
	char vm_flag;
	char vm_bMoveDs;
	char vm_bPosWait;
	char vm_adv_aord;

	char cmd;
	char qhead;
	char qtail;
	char wdt_status;
	char servo_status;
	int  loop_cnt;	//0��:main, 1��:200us, 2��:1ms, 3��:DPRAM, 2011.7.21
} DBG_MSG_BUFFER;

typedef struct 
{
    int jtpos;
    float mfGoal_pos;
	int profile_limit;//    float acc;
	int actvel_margin;//    float dcc;
	float mdBasePos;
    float vm_WaitPos;
    int val;
    float velerr;
    float tqrcmd;
    float dtBox;
	int encoder_vel;//    int pos;
    short axis_source;
    short dac_code;
    char event_st;
    char vm_accflag;
    char vm_count;
    char vm_flag;
    char vm_bMoveDs;
    char vm_bPosWait;
    char vm_adv_aord;
    
	char limit_curve_chk_cnt;//    char cmd;
    char qhead;
    char qtail;
    char wdt_status;	// wdt_status()�� �ٸ����� ���?�ִ�.
    char servo_status;	// H/W���� ���� ��ȣ������ ��
 
    int loop_cnt;		// 2011.7.21(��) AMC ������ �ֿ� ������ ���� ȸ���� ī��Ʈ�ϱ� ���� �뵵
} DBG_EVENT_MSG_BUFFER;

typedef struct 
{
    float acc;
    float dcc;
    float vel;
    float virtual_pos;    
    float dtBox;    
    int pos;
	int ds_profile_point;
    int vm_count;
  	int q_pos1;
  	int q_pos2;
  	int q_pos3;
  	int q_pos4;  
	int time; 
	  	   	
	char tmp1;  
	char tmp2; 
	char vm_accflag;	
	char vm_dccflag;
	char vm_adv_aord;	
    char qhead;
    char qtail;
  	char err;
  	char err_point; 
	char motion_sort;
	char ax;  
	char buffer_no;  	   
} DBG_MOTIONMAKE_MSG_BUFFER;

typedef struct 
{
    float dtbox;
    float fvel;
    float virtual_pos; 
    int time;   
    float vel;
    int pos;
    float vm_prev_vel;
    float vm_vel;
    int vm_count; 
    int vm_acc;
    int vm_dcc;
    int dac_bias;
    int tmp4; 
    int tmp5;               
    char vm_accflag;
    char vm_dccflag;
    char qhead;
    char qtail;   
    char ds_cal_point;
 	char cal_sort;
	char ax; 
	char buffer_no1;	 
} DBG_MOTIONCAL_MSG_BUFFER;


#pragma pack()

AMCLIB_API INT get_dbg_status(UCHAR *pucstatus);
AMCLIB_API INT get_dbg_status2(UCHAR *pucstatus, char nOfs);
AMCLIB_API INT test_dpram(int ncount, HWND hWnd, UINT uiMsgID);


#ifdef __cplusplus
}
#endif

#endif
