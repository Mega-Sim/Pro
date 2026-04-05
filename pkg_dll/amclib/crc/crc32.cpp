#include "crc32.h"


/*-------------------------- crc.c --------------------------*/

static unsigned long crc_table[256];
static int _nFirst = 1;
void _crc32_gen_table(void)                /* build the crc table */
{
    unsigned long crc, poly;
    int i, j;

    poly = 0xEDB88320L;
    for (i = 0; i < 256; i++)
        {
        crc = i;
        for (j = 8; j > 0; j--)
            {
            if (crc & 1)
                crc = (crc >> 1) ^ poly;
            else
                crc >>= 1;
            }
        crc_table[i] = crc;
        }
}

unsigned long _crc32_get_crc(char *pc, int nlen)    /* calculate the crc value */
{
    register unsigned long crc;
    if (_nFirst)
    {
    	_nFirst = 0;
    	_crc32_gen_table();
    }

    crc = 0xFFFFFFFF;
    for (int i = 0; i < nlen; i ++)
        crc = (crc>>8) ^ crc_table[ (crc ^ *pc ++) & 0xff ];

    return( crc^0xFFFFFFFF );
}

