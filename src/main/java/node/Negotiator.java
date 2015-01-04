package node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import controller.CloudController;

/**
 * The negotiator, as its name suggests, negotiates the resources a node can claim.
 * This means, the node has to get accepted by all nodes that are online.
 * As a result of that, the node will or will not join the cloud.
 */
public class Negotiator {

	private Node node;
	private AlivePacketSender aps;
	private DatagramSocket socket;
	private String controllerHost;
	private int controllerUdpPort;
	private int nodeRmin;
	private boolean allNodesAgreed = true;
	private ExecutorService executor;

	public Negotiator(Node node, AlivePacketSender aps, DatagramSocket socket, String controllerHost, int controllerUdpPort, int nodeRmin){
		this.node = node;
		this.aps = aps;
		this.socket = socket;
		this.controllerHost = controllerHost;
		this.controllerUdpPort = controllerUdpPort;
		this.nodeRmin = nodeRmin;
	}

	/**
	 * Sets allNodesAgreed to false if at least one Node sent a !nok message back to the initiator.
	 */
	public synchronized void disagree() {
		allNodesAgreed = false;
	}

	/**
	 * The node has first to negotiate the resources it can claim, before it can join the cloud.
	 * @throws IOException
	 */
	public void negotiate() throws IOException{
		socket.setSoTimeout(2000);
		String infoMessage = "";
		while(true){
			sendHelloMessage();
			try {
				infoMessage = receiveInfoMessage();
				socket.setSoTimeout(0);
				break;
			}
			catch (SocketTimeoutException e) {
				// no response received after 2 seconds. continue sending !hello messages
			}
		}

		//actual beginning of the Two-Phase Commit.
		String[] parts = infoMessage.split("\\s+");
		int nrOfOnlineNodes = parts.length - 2;
		int resourceLevelForEachNode = Integer.valueOf(parts[parts.length-1])/(nrOfOnlineNodes+1);
		if(nodeRmin>resourceLevelForEachNode){
			disagree();
		}
		sendTcpMessageToNodes(nrOfOnlineNodes, parts, "!share "+resourceLevelForEachNode);
		node.setNewResourceLevel(resourceLevelForEachNode);
		if(allNodesAgreed){
			sendTcpMessageToNodes(nrOfOnlineNodes, parts, "!commit "+resourceLevelForEachNode);
			node.commit(resourceLevelForEachNode);
		}
		else{
			sendTcpMessageToNodes(nrOfOnlineNodes, parts, "!rollback "+resourceLevelForEachNode);
			node.rollback();
			aps.stopRunning();
			System.out.println("Can't join cloud!");
		}
	}

	/**
	 * Sends a !hello message to the cloud.
	 * @throws IOException
	 */
	private void sendHelloMessage() throws IOException{
		String msg = "!hello";
		byte[] buf = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(controllerHost), controllerUdpPort);
		socket.send(packet);
	}

	/**
	 * Receives the !info message from the cloud.
	 * @return the !info message. Check {@link CloudController#prepareInfoMessage() prepareInfoMessage} to see what this message contains. 
	 * @throws IOException
	 */
	private String receiveInfoMessage() throws IOException{
		byte[] buf = new byte[4096];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet); //waits forever until it receives the !init msg
		return new String(packet.getData()).trim();
	}

	/**
	 * Sends a message to another node using the {@link MessageSenderForTwoPhaseCommit} runnable object.
	 * @param nrOfOnlineNodes 
	 * @param parts the connection information (host:port)
	 * @param message !share, !commit or !rollback message
	 */
	private void sendTcpMessageToNodes(int nrOfOnlineNodes, String[] parts, String message){
		executor = Executors.newCachedThreadPool();
		for(int i=1; i<=nrOfOnlineNodes; i++){
			String[] splitted = parts[i].split(":");
			executor.submit(new MessageSenderForTwoPhaseCommit(Integer.valueOf(splitted[1]), splitted[0], message, this));
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} 
		catch (InterruptedException e) {}
	}
}