package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;

import javax.crypto.Mac;

import org.bouncycastle.util.encoders.Base64;

import model.NodeInfo;

public class ClientHandler implements Runnable {

	private boolean running = true;
	private Socket clientSocket;
	private CloudController cloudController;
	private PrintWriter out;
	private BufferedReader in;
	private boolean loggedIn = false;
	private int position = -1;
	private Mac hMac;

	public ClientHandler(Socket clientSocket, CloudController cloudController){
		this.clientSocket = clientSocket;
		this.cloudController = cloudController;
		this.hMac = cloudController.getHMAC();
	}

	/**
	 * Stops handling requests from the clients.
	 * @throws IOException
	 */
	public void stopRunning() throws IOException{
		running = false;
		if(loggedIn){
			cloudController.setUserOffline(position);
			loggedIn = false;
			position = -1;
		}
		if(clientSocket != null) clientSocket.close();
		if(in != null) in.close();
		if(out != null) out.close();
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
	private String prependRequestWithHMAC(String message){
		hMac.update(message.getBytes());
		return new String(Base64.encode(hMac.doFinal())) +" " + message;
	}

	/**
	 * Handles requests from clients and changes their state.
	 */
	@Override
	public void run() {
		try {
			while(running){
				out = new PrintWriter(clientSocket.getOutputStream(), true);
				in = new BufferedReader( new InputStreamReader(clientSocket.getInputStream()));
				String request="";
				String response="";
				while ((request = in.readLine()) != null) {
					String[] parts = request.split("\\s+");

					//handle !login request
					if(parts[0].equals("!login")){
						if(parts.length != 3){
							response ="Too many parameters!";
						}
						else if(loggedIn){
							response = "You are already logged in!";
						}
						else{
							position = cloudController.setUserOnline(parts[1], parts[2]);
							if(position !=-1 && position != -2){
								loggedIn = true;
								response = "Successfully logged in.";
							}
							else if(position == -2 ){
								response = "You are already logged in!";
							}
							else{
								response = "Wrong username or password.";
							}
						}
					}

					//handle !logout request
					else if(parts[0].equals("!logout")){
						if(parts.length != 1){
							response ="No parameters allowed!";
						}
						else if(!loggedIn){
							response = "You are not logged in!";
						}
						else{
							cloudController.setUserOffline(position);
							loggedIn = false;
							position = -1;
							response = "Logged out successfully.";
						}

					}

					//handle !credits request
					else if(parts[0].equals("!credits")){
						if(parts.length != 1){
							response ="No parameters allowed!";
						}
						else if(!loggedIn){
							response = "You are not logged in!";
						}
						else{
							response = "You have "+cloudController.getCredits(position)+ " credits left.";
						}
					}

					//handle !buy request
					else if(parts[0].equals("!buy")){
						if(parts.length != 2){
							response ="Too many parameters!!";
						}
						else if(!loggedIn){
							response = "You are not logged in!";
						}
						else if(Long.valueOf(parts[1]).longValue()<=0){
							response = "The amount of credits should be greater than 0!";
						}
						else{
							response = "You now have "+cloudController.modifyCredits(position,Long.valueOf(parts[1]).longValue())+ " credits.";
						}
					}

					//handle !list request
					else if(parts[0].equals("!list")){
						if(parts.length != 1){
							response ="Too many parameters!!";
						}
						else if(!loggedIn){
							response = "You are not logged in!";
						}
						else{
							response = cloudController.getAvailableOperations();
						}
					}

					//handle !compute request
					else if(parts[0].equals("!compute")){
						cloudController.increaseStatistic(request);
						if(!loggedIn){
							response = "You are not logged in!";
						}
						else if(((parts.length - 2)/2)*50>cloudController.getCredits(position)){
							response = "You don't have enough credits to perform this operation.";
						}
						else{
							String result = parts[1]; //first operand
							int i = 3;
							int nrOfOperations = 0;
							while(i<parts.length){
								NodeInfo node = cloudController.getNodeWithLowestUsage(parts[i-1]);
								try {
									if(node == null){
										result = "No nodes available for at least one operation.";
										nrOfOperations = 0;
										break;
									}
									else{
										nrOfOperations++;
										Socket socket = new Socket(node.getAddress(),node.getTcpPort());
										PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
										BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
										String message = prependRequestWithHMAC("!compute "+result +" " +parts[i-1]+" "+parts[i]);
										out.println(message);
										result = in.readLine();
										String[] splittedResult = result.split("\\s+");
										String plaintext = "";
										for(int j = 1; j<splittedResult.length; j++){
											plaintext = plaintext + splittedResult[j]+" ";
										}
										result = plaintext.trim();
										if(!HMACsAreEqual(splittedResult[0],result)||splittedResult[1].equals("!tampered")){
											result = "Incorrect Hash";
											nrOfOperations = 0;
											break;
										}
										if(result.contains("Error: division by 0")){
											break;
										}
										//at this point, the request has been processed successfully.
										if(result.startsWith("-")){
											node.increaseUsage(50*(result.length()-1));
										}
										else{
											node.increaseUsage(50*result.length());
										}

										i=i+2;
										socket.close();
									}
								}
								catch(ConnectException e){
									node.setStatus(false);
								}
								catch(SocketException e){
									node.setStatus(false);
								}
								catch (UnknownHostException e) {} 
								catch (IOException e) {}
							}
							cloudController.modifyCredits(position, -50*nrOfOperations);
							nrOfOperations = 0;
							response = result;
						}
					}
					out.println(response);
				}
			}
			stopRunning();
		} catch (IOException e) {}
	}
}
