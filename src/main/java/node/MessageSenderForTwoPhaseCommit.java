package node;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import channel.TcpChannel;

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
			TcpChannel tcpChannel = new TcpChannel(socket);
			tcpChannel.writeString(message);
			if(message.startsWith("!share")){
				String response = tcpChannel.readString();
				if(response.equals("!nok")){
					aps.disagree();
				}
			}
			socket.close();
			tcpChannel.close();
		} 
		catch (UnknownHostException e) {} 
		catch (IOException e) {}
	}
}