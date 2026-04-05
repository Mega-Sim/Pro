#ifndef __CMDSET_H__
#define __CMDSET_H__

#define AXIS_BASEADDR(axis) (0x30 * axis)
#define AXIS_REG				0x3FC
#define FLUSH_ADDR				0x3FE	// DSP interrupt 하는 번지
#define	ADDR_DSP_ACK			0x3FF	// DSP 

/**********************************************************************
*************************    commands      ****************************
***********************************************************************/

#define	AMCMD_P2P_MOVE					1
#define	AMCMD_GET_GAIN 					3
#define	AMCMD_SET_GAIN					4
#define	AMCMD_GET_VGAIN 				5
#define	AMCMD_SET_VGAIN					6
#define	AMCMD_VELOCITY_MOVE				17
#define	AMCMD_WRITE_OUTPUT_PORT			18
#define	AMCMD_SET_OUTPUT_PORT			19
#define	AMCMD_RESET_OUTPUT_PORT			20

#define	AMCMD_SERVO_ON					24
#define	AMCMD_SERVO_OFF					25

#define	AMCMD_READ_OUTPUT_PORT		  	80	// dsp:put_io_out_data


#define	AMCMD_READ_INPUT_PORT		  		255

#endif