package controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * The NodeListener waits for !alive and !hello messages from the nodes. See the {@link #run() run} method for more details.
 */
public class NodeListener implements Runnable{

	private CloudController cloudController;
	private boolean running = true;
	private DatagramSocket socket;

	public NodeListener(int udpPort, CloudController cloudController) {
		this.cloudController = cloudController;
		try {
			this.socket = new DatagramSocket(udpPort);
		} 
		catch (SocketException e) {}
	}

	/**
	 * Stops the NodeListener.
	 */
	public void stopRunning(){
		running = false;
		if(socket != null) socket.close();
	}

	/**
	 * Waits for !alive and !hello(in that case a !init message is sent back) messages from nodes and registers(only if the Two-Phase Commit was successful) or updates these nodes to the cloud controller. See also {@link CloudController#updateNodes() updateNodes} how the check is performed.
	 */
	@Override
	public void run() {
		while(running){
			byte[] buf = new byte[4096];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(packet); //waits forever until it receives a packet
				InetAddress address = packet.getAddress();
				String message = new String(packet.getData()).trim();
				String[] parts = message.split("\\s+");
				if(parts[0].equals("!hello")){
					byte[] initBuf = cloudController.prepareInfoMessage().getBytes();
					DatagramPacket initPacket = new DatagramPacket(initBuf, initBuf.length, InetAddress.getByName(packet.getAddress().getHostAddress()), packet.getPort());
					socket.send(initPacket);
				}
				else if(parts[0].equals("!alive")){
					String tcpPort = parts[1];
					String operators = parts[2];
					cloudController.updateNodes(address, new Integer(tcpPort), operators, System.currentTimeMillis());
				}
			}
			catch (SocketException e) {}
			catch (IOException e) {}
		}
	}
}
