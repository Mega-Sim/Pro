package uohtcontroller;


/**
 * OcsProcessVersionVO Class, OCS 3.0 for Unified FAB
 * 
 * @author Jongwon.Jung
 * 
 * @date   2019. 07. 25.
 * @version 3.0
 * 
 * Copyright 2019 by SEMES.
 * 
 * This software is the confidential and proprietary information
 * of SEMES. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with SEMES.
 */

public class OcsProcessVersionVO {
	private String process_name;
	private String primary_Version;
	private String primary_Bulid_Date;
	private String secondary_Version;
	private String secondary_Bulid_Date;
	
	public String getProcess_name() {
		return process_name;
	}
	public void setProcess_name(String process_name) {
		this.process_name = process_name;
	}
	public String getPrimary_Version() {
		return primary_Version;
	}
	public void setPrimary_Version(String primary_Version) {
		this.primary_Version = primary_Version;
	}
	public String getPrimary_Bulid_Date() {
		return primary_Bulid_Date;
	}
	public void setPrimary_Bulid_Date(String primary_Bulid_Date) {
		this.primary_Bulid_Date = primary_Bulid_Date;
	}
	public String getSecondary_Version() {
		return secondary_Version;
	}
	public void setSecondary_Version(String secondary_Version) {
		this.secondary_Version = secondary_Version;
	}
	public String getSecondary_Bulid_Date() {
		return secondary_Bulid_Date;
	}
	public void setSecondary_Bulid_Date(String secondary_Bulid_Date) {
		this.secondary_Bulid_Date = secondary_Bulid_Date;
	}

	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("DB STATE : ");
//		sb.append("inserviceHost [").append(inserviceHost).append("] hostState[" ).append(hostState).append("] remoteState [").append(remoteState).append("]");
		return sb.toString();
	}
	
	
	
}
