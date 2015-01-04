package node;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import channel.TcpChannel;

/**
 * A simple thread that sends !share, !commit, !rollback messages to other nodes.
 */
public class MessageSenderForTwoPhaseCommit implements Runnable {

	private int tcpPort;
	private String host;
	private String message;
	private Negotiator negotiator;
	
	public MessageSenderForTwoPhaseCommit(int tcpPort, String host, String message, Negotiator negotiator){
		this.tcpPort = tcpPort;
		this.host = host;
		this.message = message;
		this.negotiator = negotiator;
	}

	/**
	 * Sends !share, !commit, !rollback messages to another node.
	 */
	@Override
	public void run() {
		Socket socket = null;
		TcpChannel tcpChannel = null;
		try {
			socket = new Socket(host,tcpPort);
			tcpChannel = new TcpChannel(socket);
			tcpChannel.writeString(message);
			if(message.startsWith("!share")){
				String response = tcpChannel.readString();
				if(response.equals("!nok")){
					negotiator.disagree();
				}
			}
		} 
		catch (UnknownHostException e) {} 
		catch (IOException e) {}
		finally{
			try{
				if(socket != null) socket.close();
				if(tcpChannel != null) tcpChannel.close();
			}
			catch(IOException e){}
		}
	}
}