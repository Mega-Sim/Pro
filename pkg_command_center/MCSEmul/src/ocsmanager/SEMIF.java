package ocsmanager;

public interface SEMIF {
	String m_strHSMSState = null;

	int Initialize();

	boolean SetTSCName(String tscName);

	int CallProc(MyHashtable msg);

	void SendMicroTC(MyHashtable msg);

	void SendMicroTCForStage(MyHashtable msg);

	short SendMsg(String str);

	short SendS2F41(MyHashtable msg);

	int sendS2F41(String cmd, String carrierId, String carrierLocId);

	int sendS2F49(String cmd, boolean isValid);

	boolean isDBConnected();

	boolean loadReportConfig(MyHashtable msg);

	boolean loadLinkEventReportConfig(MyHashtable msg);

	boolean loadEnabledCEIDConfig(MyHashtable msg);
}
