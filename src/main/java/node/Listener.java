package node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import model.ComputationRequestInfo;

import org.bouncycastle.util.encoders.Base64;

import util.Config;

/**
 * Listens for requests. In case of !compute request, if the calculation is successful, the resulting number is sent back to the cloud controller. 
 * Otherwise, the cloud controller is informed about the reason of the failure. Each time, a log file is created. See the {@link #run() run} method for more details.
 */
public class Listener implements Runnable {

	private boolean running = true;
	private ServerSocket serverSocket;
	private int tcpPort;
	private Node node;
	private int nodeRmin;
	private PrintWriter out;
	private BufferedReader in;
	private Mac hMac;

	public Listener(int tcpPort, Node node, int nodeRmin){
		this.node = node;
		this.tcpPort = tcpPort;
		this.nodeRmin = nodeRmin;
		Key secretKey = new SecretKeySpec(node.getSecret().getBytes(),"HmacSHA256");
		try {
			hMac = Mac.getInstance("HmacSHA256");
			hMac.init(secretKey);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Stops listening for computation requests.
	 */
	public void stopRunning(){
		running = false;
		try {
			if(serverSocket != null) serverSocket.close();
		} 
		catch (IOException e) {}
	}
	
	/**
	 * Checks whether the HMAC of the received plaintext is equal to the HMAC that was sent by the communication partner
	 * @param receivedHMAC
	 * @param receivedPlainText
	 * @return true, if the HMACs are equal
	 */
	private boolean HMACsAreEqual(String receivedHMAC, String receivedPlaintext){
		// computedHash is the HMAC of the received plaintext 
		hMac.update(receivedPlaintext.getBytes());
		byte[] computedHash = hMac.doFinal();
		// receivedHash is the HMAC that was sent by the communication partner
		byte[] receivedHash = Base64.decode(receivedHMAC.getBytes());
		return MessageDigest.isEqual(computedHash, receivedHash);
	}
	
	/**
	 * Prepends a given message with a new HMAC
	 * @param message
	 * @return the message with the HMAC prepended
	 */
	private String prependResponseWithHMAC(String message){
		hMac.update(message.getBytes());
		return new String(Base64.encode(hMac.doFinal())) +" " + message;
	}

	/**
	 * Listens for requests from the cloud controller/node and performs the requested operation.
	 * After it has completely processed a request and sent the response back to the cloud controller/node, the respective Socket is closed. 
	 * For any new request, a new Socket is created.
	 */
	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(tcpPort);
			while(running){
				Socket clientSocket = serverSocket.accept();
				in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
				String request = in.readLine();
				String splittedExp[] = request.split("\\s+");
				String response = "";
				
				if(splittedExp[0].startsWith("!getLogs")){
					ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
					outputStream.flush();
					ArrayList<ComputationRequestInfo> logs = node.getLogs();
					for(ComputationRequestInfo c : logs){
						outputStream.writeObject(c);
					}
					outputStream.close();
				}
				else if(splittedExp[0].startsWith("!share")){
					out = new PrintWriter(clientSocket.getOutputStream(), true);
					int resources = Integer.valueOf(splittedExp[1]);
					if(nodeRmin>resources){
						response = "!nok";
					}
					else{
						response = "!ok";
						node.setNewResourceLevel(resources);
					}
					out.println(response);
					out.close();
				}
				else if(splittedExp[0].startsWith("!commit")){
					int resources = Integer.valueOf(splittedExp[1]);
					node.commit(resources);
				}
				else if(splittedExp[0].startsWith("!rollback")){
					node.rollback();
				}
				else if(splittedExp[1].startsWith("!compute")){
					out = new PrintWriter(clientSocket.getOutputStream(), true);
					String plaintext = "!compute";
					for(int i = 2; i<splittedExp.length; i++){
						plaintext = plaintext +" "+ splittedExp[i];
					}
					if(!HMACsAreEqual(splittedExp[0],plaintext)){
						response = "!tampered !compute "+splittedExp[2] + " " +splittedExp[3] + " "+splittedExp[4];
					}
					else{
						if(splittedExp[3].equals("+")) response = ""+(Integer.valueOf(splittedExp[2]) + Integer.valueOf(splittedExp[4]));
						if(splittedExp[3].equals("-")) response = ""+(Integer.valueOf(splittedExp[2]) - Integer.valueOf(splittedExp[4]));
						if(splittedExp[3].equals("*")) response = ""+(Integer.valueOf(splittedExp[2]) * Integer.valueOf(splittedExp[4]));
						if(splittedExp[3].equals("/")) {
							if(Integer.valueOf(splittedExp[4]) != 0){
								double tmp = (Double.valueOf(splittedExp[2]) / Double.valueOf(splittedExp[4]));
								if(tmp<0){
									response = ""+(-Math.round(-tmp));
								}
								else{
									response = ""+Math.round(tmp);
								}
							}
							else{
								response = "Error: division by 0";
							}
						}
						node.createLogFile(splittedExp[2] + " " +splittedExp[3] + " "+splittedExp[4], response);
					}
					out.println(prependResponseWithHMAC(response));
					out.close();
				}
				clientSocket.close();
			}
		}
		catch(SocketException e){}
		catch (IOException e) {}
	}
}