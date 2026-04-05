package ocsmanager;

import javax.swing.UIManager;
import java.awt.*;

/**
 * <p>
 * Title: OCS Manager
 * </p>
 * <p>
 * Description: Reticle MCS의 OCS Operation을 관리하는 모듈. 제어하는 OCS 1 System당 하나의
 * 실행모듈이 실행됨
 * </p>
 * <p>
 * Copyright: Copyright (c) 2005
 * </p>
 * <p>
 * Company: 삼성전자 기술총괄 메카트로닉스연구소
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */

public class OCSManager {
	boolean packFrame = false;

	//Construct the application
	public OCSManager(boolean bConsole) {
		if (bConsole == false) {
			OCSManagerMainFrame frame = new OCSManagerMainFrame();

			//Validate frames that have preset sizes
			//Pack frames that have useful preferred size info, e.g. from their layout
			if (packFrame) {
				frame.pack();
			} else {
				frame.validate();
			}
			//Center the window
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension frameSize = frame.getSize();
			if (frameSize.height > screenSize.height) {
				frameSize.height = screenSize.height;
			}
			if (frameSize.width > screenSize.width) {
				frameSize.width = screenSize.width;
			}
			frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
			frame.setVisible(true);
		} else {
			OCSManagerMain ocsMain = new OCSManagerMain(null);
		}
	}

	//Main method
	public static void main(String[] args) {
		boolean bConsole = false;

		if (args.length > 0) {
			if (args[0].equals("-console"))
				bConsole = true;
		}

		if (bConsole == false) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		new OCSManager(bConsole);
	}
}
