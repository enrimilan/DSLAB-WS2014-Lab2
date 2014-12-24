package controller;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import channel.Channel;
import channel.HmacChannel;
import channel.IntegrityException;
import channel.TcpChannel;
import model.NodeInfo;

public class ClientHandler implements Runnable {

	private boolean running = true;
	private Socket clientSocket;
	private CloudController cloudController;
	private boolean loggedIn = false;
	private int position = -1;
	private TcpChannel tcpChannel;

	public ClientHandler(Socket clientSocket, CloudController cloudController) throws IOException{
		this.clientSocket = clientSocket;
		this.tcpChannel = new TcpChannel(clientSocket);
		this.cloudController = cloudController;
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
		tcpChannel.close();
	}

	/**
	 * Handles requests from clients and changes their state.
	 */
	@Override
	public void run() {
		try {
			while(running){
				String request="";
				String response="";
				request = tcpChannel.readString();
				String[] partsOfTheRequest = request.split("\\s+");

				//handle !login request
				if(partsOfTheRequest[0].equals("!login")){
					if(partsOfTheRequest.length != 3){
						response ="Too many parameters!";
					}
					else if(loggedIn){
						response = "You are already logged in!";
					}
					else{
						position = cloudController.setUserOnline(partsOfTheRequest[1], partsOfTheRequest[2]);
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
				else if(partsOfTheRequest[0].equals("!logout")){
					if(partsOfTheRequest.length != 1){
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
				else if(partsOfTheRequest[0].equals("!credits")){
					if(partsOfTheRequest.length != 1){
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
				else if(partsOfTheRequest[0].equals("!buy")){
					if(partsOfTheRequest.length != 2){
						response ="Too many parameters!!";
					}
					else if(!loggedIn){
						response = "You are not logged in!";
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
					else if(!loggedIn){
						response = "You are not logged in!";
					}
					else{
						response = cloudController.getAvailableOperations();
					}
				}

				//handle !compute request
				else if(partsOfTheRequest[0].equals("!compute")){
					cloudController.increaseStatistics(request);
					if(!loggedIn){
						response = "You are not logged in!";
					}
					else if(((partsOfTheRequest.length - 2)/2)*50>cloudController.getCredits(position)){
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
										Channel hC = new HmacChannel(new TcpChannel(socket),cloudController.getSecret());
										hC.write(message.getBytes());	
										result = new String(hC.read());
									}
									catch(IntegrityException e){
										result = "Incorrect Hash";
										nrOfOperations = 0;
										break;
									} catch (InvalidKeyException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (NoSuchAlgorithmException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									catch (IOException e){
										e.printStackTrace();
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
							catch (IOException e) {
								e.printStackTrace();
							}
						}
						cloudController.modifyCredits(position, -50*nrOfOperations);
						nrOfOperations = 0;
						response = result;
					}
				}
				tcpChannel.writeString(response);
			}
			stopRunning();
		} catch (IOException e) {}
	}
}