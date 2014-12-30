package client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;

import util.Config;
import util.Keys;
import channel.AESEncryptedChannel;
import channel.Channel;
import channel.ClientAuthenticator;
import channel.RSAEncryptedChannel;
import channel.TcpChannel;
import cli.Command;
import cli.Shell;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private String controllerHost;
	private int controllerTcpPort;
	private Shell shell;
	private Socket socket;
	private Channel tcpChannel;
	private boolean authenticated = false;

	private AESEncryptedChannel aesEncryptedChannel;
	private PublicKey sendKey;
	private PrivateKey receiveKey;
	private ClientAuthenticator clientAuthenticator;
	
	
	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Client(String componentName, Config config, InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.shell = new Shell(componentName, userRequestStream, userResponseStream);
	}
	
	/**
	 * Reads the host and tcp port of the controller from the client's properties file.
	 */
	private void readClientProperties(){
		controllerHost = config.getString("controller.host");
		controllerTcpPort = config.getInt("controller.tcp.port");
	}
	
	/**
	 * Creates a Socket and connects to the cloud controller, also gets the outputstream(for outgoing messages) and the intputstream(for ingoing messages).
	 * If nothing went wrong i.e the cloud controller is not offline, the shell will be started.
	 * If the cloud controller is offline, a usage message is printed and the client exits immediately.
	 */
	private void connectToCloudControllerAndStartShell(){
		try {
			socket = new Socket(controllerHost,controllerTcpPort);
			this.tcpChannel = new TcpChannel(socket);
			startShell();
		}
		catch(ConnectException e) {
			//cloud controller is not online, therefore this client will not even start.
			System.err.println("Cloud controller is offline");
		}
		catch (UnknownHostException e) {} 
		catch (IOException e) {}
	}

	/**
	 * Registers to the shell the interactive commands that the client can perform and then starts the shell.
	 */
	private void startShell(){
		shell.register(this);
		new Thread(shell).start();
	}

	/**
	 * Sends a request to the cloud controller, first it writes to the outputstream of the socket and then reads from its inputstream.
	 * @param request
	 * 		the request which has to be sent to the cloud controller
	 * @return
	 * 		the response from the cloud controller
	 * @throws IOException
	 */
	
	private String sendRequest(String request) throws IOException{
		
		if(!authenticated){
			return "You are not authenticated!";
		}
		
		try {
			aesEncryptedChannel.send(request);
		} catch (BadPaddingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String response = "";
		try{
			response = aesEncryptedChannel.receive();
			if(response == null){
				//this check was necessary when running the code on a linux machine
				close();
				return "Cloud controller suddenly went offline. Shutting down " +componentName + " now";
			}
		}
		catch (SocketException e){
			//cloud controller suddenly went offline. make sure to close all the resources in order to exit this client.
			close();
			return "Cloud controller suddenly went offline. Shutting down " +componentName + " now";
		}
		return response;
	}
	
//	private String sendRequest(String request) throws IOException{
//		((TcpChannel) tcpChannel).writeString(request);
//		String response = "";
//		try{
//			response = ((TcpChannel) tcpChannel).readString();
//			if(response == null){
//				//this check was necessary when running the code on a linux machine
//				close();
//				return "Cloud controller suddenly went offline. Shutting down " +componentName + " now";
//			}
//		}
//		catch (SocketException e){
//			//cloud controller suddenly went offline. make sure to close all the resources in order to exit this client.
//			close();
//			return "Cloud controller suddenly went offline. Shutting down " +componentName + " now";
//		}
//		return response;
//	}
	
	/**
	 * Releases all resources, stops all threads and closes the socket.
	 * @throws IOException
	 */
	private void close() throws IOException{
		shell.close();
		if(socket != null) socket.close();
		tcpChannel.close();
	}
	
	/**
	 * Starts the client.
	 */
	@Override
	public void run() {
		readClientProperties();
		connectToCloudControllerAndStartShell();
	}

	@Command(value="login")
	@Override
	public String login(String username, String password) throws IOException {
		return "This command is not supported anymore. Please use the !authenticate command instead.";
	}

	@Command(value="logout")
	@Override
	public String logout() throws IOException {
		return sendRequest("!logout");
	}

	@Command(value="credits")
	@Override
	public String credits() throws IOException {
		return sendRequest("!credits");
	}

	@Command(value="buy")
	@Override
	public String buy(long credits) throws IOException {
		return sendRequest("!buy "+credits);
	}

	@Command(value="list")
	@Override
	public String list() throws IOException {
		return sendRequest("!list");
	}

	@Command(value="compute")
	@Override
	public String compute(String term) throws IOException {
		return sendRequest("!compute "+term);
	}

	@Command(value="exit")
	@Override
	public String exit() throws IOException {
		logout();
		close();
		return "Shutting down "+componentName+" now.";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		Client client = new Client(args[0], new Config("client"), System.in, System.out);
		client.run();
	}
	
	@Command(value="authenticate")
	@Override
	public String authenticate(String username) throws IOException {
		sendKey = Keys.readPublicPEM(new File(config.getString("controller.key")));
	    receiveKey = Keys.readPrivatePEM(new File("keys/client/"+username+".pem"));
	    clientAuthenticator = new ClientAuthenticator(username);
		RSAEncryptedChannel rsaEncryptedChannel = new RSAEncryptedChannel((TcpChannel)tcpChannel, sendKey, receiveKey);
	
		try {
			aesEncryptedChannel = clientAuthenticator.rsaAuthenticate(rsaEncryptedChannel,sendKey, receiveKey);
		
		} catch (BadPaddingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		authenticated = true;
		return aesEncryptedChannel.receive(); //sendRequest("!authenticate "+ username);
	}

}