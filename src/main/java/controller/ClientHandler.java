package controller;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.bouncycastle.util.encoders.Base64;

import channel.AESChannel;
import channel.Base64Channel;
import channel.Channel;
import channel.HmacChannel;
import channel.IntegrityException;
import channel.RSAChannel;
import channel.TcpChannel;
import model.NodeInfo;

public class ClientHandler implements Runnable {

	private boolean running = true;
	private Socket clientSocket;
	private CloudController cloudController;
	private String key;
	private String hmacKey;
	private String keysDir;
	private int position = -1;
	private TcpChannel tcpChannel;
	private AESChannel aesChannel;

	public ClientHandler(Socket clientSocket, CloudController cloudController, String key, String hmacKey, String keysDir) throws IOException{
		this.clientSocket = clientSocket;
		this.cloudController = cloudController;
		this.key = key;
		this.hmacKey = hmacKey;
		this.keysDir = keysDir;
		this.tcpChannel = new TcpChannel(clientSocket);
	}

	/**
	 * Stops handling requests from the clients.
	 * @throws IOException
	 */
	public void stopRunning() throws IOException{
		running = false;
		if(position != -1){
			cloudController.setUserOffline(position);
			position = -1;
		}
		if(clientSocket != null) clientSocket.close();
		tcpChannel.close();
	}

	private void authenticate(){
		String username ="";
		RSAChannel rsaChannel = null;
		try {
			rsaChannel = new RSAChannel(new Base64Channel(tcpChannel),new File(key));
			String[] authenticationMessageParts = (new String(rsaChannel.read())).split("\\s+");
			username = authenticationMessageParts[1];
			rsaChannel.sendSecondMessage(authenticationMessageParts[2].getBytes(), keysDir+"/"+username+".pub.pem");
			aesChannel = new AESChannel(new Base64Channel(tcpChannel),rsaChannel.getKey(),rsaChannel.getInitializationVector());
			byte[] message = aesChannel.read();
			if(!Arrays.equals(message,Base64.decode(rsaChannel.getChallenge()))){
				aesChannel.write("Challenges not equal!".getBytes());
				stopRunning();
			}
			else{
				position = cloudController.setUserOnline(username);
				if(position != -1){
					aesChannel.write("Successfully authenticated!".getBytes());
				}
				else{
					aesChannel.write("You are already authenticated somewhere else!".getBytes());
				}
			}

		}catch (IOException e) {
			
		}
	}

	/**
	 * Handles requests from clients and changes their state.
	 */
	@Override
	public void run() {
		try {
			while(running){
				if(position == -1){
					authenticate();
				}
				else{
					String request="";
					String response="";
					request = new String(aesChannel.read());
					String[] partsOfTheRequest = request.split("\\s+");

					//handle !logout request
					if(partsOfTheRequest[0].equals("!logout")){
						if(partsOfTheRequest.length != 1){
							response ="No parameters allowed!";
						}
						else{
							cloudController.setUserOffline(position);
							position = -1;
							response = "Logged out successfully.";
						}
					}

					//handle !credits request
					else if(partsOfTheRequest[0].equals("!credits")){
						if(partsOfTheRequest.length != 1){
							response ="No parameters allowed!";
						}
						else{
							response = "You have "+cloudController.getCredits(position)+ " credits left.";
						}
					}

					//handle !buy request
					else if(partsOfTheRequest[0].equals("!buy")){
						if(partsOfTheRequest.length != 2){
							response ="Too many parameters!!";
						}
						else if(Long.valueOf(partsOfTheRequest[1]).longValue()<=0){
							response = "The amount of credits should be greater than 0!";
						}
						else{
							response = "You now have "+cloudController.modifyCredits(position,Long.valueOf(partsOfTheRequest[1]).longValue())+ " credits.";
						}
					}

					//handle !list request
					else if(partsOfTheRequest[0].equals("!list")){
						if(partsOfTheRequest.length != 1){
							response ="Too many parameters!!";
						}
						else{
							response = cloudController.getAvailableOperations();
						}
					}

					//handle !compute request
					else if(partsOfTheRequest[0].equals("!compute")){
						cloudController.increaseStatistics(request);
						if(((partsOfTheRequest.length - 2)/2)*50>cloudController.getCredits(position)){
							response = "You don't have enough credits to perform this operation.";
						}
						else{
							String result = partsOfTheRequest[1]; //first operand
							int i = 3;
							int nrOfOperations = 0;
							while(i<partsOfTheRequest.length){
								NodeInfo node = cloudController.getNodeWithLowestUsage(partsOfTheRequest[i-1]);
								try {
									if(node == null){
										result = "No nodes available for at least one operation.";
										nrOfOperations = 0;
										break;
									}
									else{
										nrOfOperations++;
										Socket socket = new Socket(node.getAddress(),node.getTcpPort());
										String message = "!compute "+result +" " +partsOfTheRequest[i-1]+" "+partsOfTheRequest[i];
										try{
											Channel hC = new HmacChannel(new TcpChannel(socket),hmacKey);
											hC.write(message.getBytes());	
											result = new String(hC.read());
										}
										catch(IntegrityException e){
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
					aesChannel.write(response.getBytes());
				}

			}
			stopRunning();
		} catch (IOException e) {}
	}
}