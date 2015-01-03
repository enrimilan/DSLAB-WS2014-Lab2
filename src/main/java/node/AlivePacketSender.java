package node;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import controller.CloudController;

/**
 * From time to time, the node needs to send !alive packets to the cloud controller to demonstrate it is still online and is ready to handle client requests.
 * This is done by the AlivePacketSender. In order to send such packets, the node has first to negotiate ({@link #negotiate() negotiate})  the resources it can claim. 
 * See the {@link #run() run} method for more details.
 */
public class AlivePacketSender implements Runnable {

	private int tcpPort;
	private String controllerHost;
	private int controllerUdpPort;
	private int nodeAlive;
	private String nodeOperators;
	private Node node;
	private DatagramSocket socket;
	private boolean running = true;
	private boolean allNodesAgreed = true;
	private ExecutorService executor;

	public AlivePacketSender(int tcpPort, String controllerHost, int controllerUdpPort, int nodeAlive, String nodeOperators, Node node) {
		this.tcpPort = tcpPort;
		this.controllerHost = controllerHost;
		this.controllerUdpPort = controllerUdpPort;
		this.nodeAlive = nodeAlive;
		this.nodeOperators = nodeOperators;
		this.node = node;
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
	private void negotiate() throws IOException{
		sendHelloMessage();
		String infoMessage = receiveInfoMessage();
		//actual beginning of the Two-Phase Commit.
		String[] parts = infoMessage.split("\\s+");
		int nrOfOnlineNodes = parts.length - 2;
		int resourceLevelForEachNode = Integer.valueOf(parts[parts.length-1])/(nrOfOnlineNodes+1);
		if(node.getNodeRmin()>resourceLevelForEachNode){
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
			stopRunning();
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

	/**
	 * Stops sending isALive messages to the cloud controller.
	 */
	public void stopRunning(){
		running = false;
		if(socket != null) socket.close();
	}

	/**
	 * Negotiates first, if the Two-Phase Commit was successful, it then sends to the cloud controller every nodeAlive milliseconds isAlive messages, containing the tcp port of this node and its supported operations.
	 */
	@Override
	public void run() {
		try {
			socket = new DatagramSocket();
			negotiate();
			while(running){
				String alivePacket = "!alive "+tcpPort+" "+nodeOperators;
				byte[] buf = alivePacket.getBytes();
				DatagramPacket request = new DatagramPacket(buf, buf.length, InetAddress.getByName(controllerHost), controllerUdpPort);
				socket.send(request);
				Thread.sleep(nodeAlive);
			}
		} 
		catch (IOException e) {} 
		catch (InterruptedException e) {}
	}
}