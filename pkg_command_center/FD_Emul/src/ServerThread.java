import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Vector;


public class ServerThread {
	
	public static void main(String[] args) {
		ServerSocket welcomeSocket = null;
		Socket connectionSocket = null;
		DataOutputStream outToClient = null;
		while (true) {
			try {
				welcomeSocket = new ServerSocket(8001);
			
				
				connectionSocket = welcomeSocket.accept();
				//				 BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				while(true) {
					String msg = "201/FD002,RUN";
					outToClient.writeBytes(msg + "\r\n");
					outToClient.flush();
					System.out.println(msg);
//					System.out.print(".");
					Thread.sleep(1000);
				}
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				
				e.printStackTrace();
				
			} finally {
				if(null!=welcomeSocket) {
					try { welcomeSocket.close(); }
					catch (IOException e) { e.printStackTrace(); }
					welcomeSocket = null;
				}
				if(null!=connectionSocket) {
					try { connectionSocket.close(); } 
					catch (IOException e) { e.printStackTrace(); }
					connectionSocket = null;
				}
			}
		}
	}
}
