package uohtcontroller;

import com.samsung.ocs.VersionInfo;

/**
 * <p>Title: Universal Communication Interface</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2010</p>
 * <p>Company: Samsung Electronics</p>
 * @author Kwangyoung Im, Youngmin Moon
 * @version 1.0
 */

public class UOHTController {

  //Construct the application
  public UOHTController() {
    UOCSMain main = new UOCSMain();
  }

  //Main method
  public static void main(String[] args) {
    String strArg = "";
    if (args.length > 0) {
      strArg = args[0].toUpperCase();
    }
    if (strArg.equals("-VERSION")) {
//      System.out.println("Version : " + UOCSMain.m_strVersionID);
    	System.out.println("VERSION	: [" + VersionInfo.getString("VERSION") + "]");
  		System.out.println("BUILDID	: [" + VersionInfo.getString("BUILDID") + "]");
    }
    else if (strArg.equals("") || strArg.equals("-CONSOLE")) {
      new UOHTController();
    }
  }
}
