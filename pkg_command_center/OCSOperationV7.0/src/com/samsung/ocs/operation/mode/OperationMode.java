package com.samsung.ocs.operation.mode;

import com.samsung.ocs.manager.impl.model.TrCmd;
import com.samsung.ocs.operation.constant.OperationConstant.OPERATION_MODE;

/**
 * OperationMode Interface, OCS 3.0 for Unified FAB
 * 
 * @author Kwangyoung.Im
 * @author Mokmin.Park
 * @author Youngmin.Moon
 * @author Younkook.Kang
 * @author Wongeun.Lee
 * 
 * @date   2011. 6. 21.
 * @version 3.0
 * 
 * Copyright 2011 by Samsung Electronics, Inc.,
 * 
 * This software is the confidential and proprietary information
 * of Samsung Electronics, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Samsung.
 */

public interface OperationMode {
	public abstract boolean controlVehicle();
	public abstract OPERATION_MODE getOperationMode();
	public abstract void setPreviousOperationMode(OPERATION_MODE mode);
	public abstract void setTrCmd(TrCmd trCmd);
}