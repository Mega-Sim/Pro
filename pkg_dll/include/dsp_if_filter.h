#ifndef		__DSP_IF_FILTER_H
#define		__DSP_IF_FILTER_H

//#include "dsp_if.h"
//#include "../Appl-include/amc_filter.h"



int _dsp_set_position_lpf(int ax, int nfreq);
int _dsp_set_position_notch_filter(int ax, int nfreq);
int _dsp_set_velocity_lpf(int ax, int nfreq);
int _dsp_set_velocity_notch_filter(int ax, int nfreq);


int _dsp_get_position_lpf(int ax, int *pnfreq);
int _dsp_get_position_notch_filter(int ax, int *pnfreq);
int _dsp_get_velocity_lpf(int ax, int *pnfreq);
int _dsp_get_velocity_notch_filter(int ax, int *pnfreq);

#endif


