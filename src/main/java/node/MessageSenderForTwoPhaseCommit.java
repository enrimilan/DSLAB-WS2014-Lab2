package node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MessageSenderForTwoPhaseCommit implements Runnable {

	private int tcpPort;
	private String host;
	private String message;
	private AlivePacketSender aps;

	public MessageSenderForTwoPhaseCommit(int tcpPort, String host, String message, AlivePacketSender aps){
		this.tcpPort = tcpPort;
		this.host = host;
		this.message = message;
		this.aps = aps;
	}

	/**
	 * Sends !share, !commit, !rollback messages to another node.
	 */
	@Override
	public void run() {
		Socket socket = null;
		try {
			socket = new Socket(host,tcpPort);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out.println(message);
			if(message.startsWith("!share")){
				String response = in.readLine();
				if(response.equals("!nok")){
					aps.disagree();
				}
			}
			socket.close();
		} 
		catch (UnknownHostException e) {} 
		catch (IOException e) {}
	}
}
