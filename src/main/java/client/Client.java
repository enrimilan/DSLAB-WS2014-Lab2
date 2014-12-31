package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.util.encoders.Base64;
import util.Config;
import channel.AESChannel;
import channel.Base64Channel;
import channel.Channel;
import channel.RSAChannel;
import channel.TcpChannel;
import cli.Command;
import cli.Shell;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private String controllerHost;
	private int controllerTcpPort;
	private String keysDir;
	private String controllerKey;
	private Shell shell;
	private Socket socket;
	private Channel tcpChannel;
	private AESChannel aesChannel;
	private boolean authenticated = false;

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
		keysDir = config.getString("keys.dir");
		controllerKey = config.getString("controller.key");
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
	 * Sends a request to the cloud controller and then gets the response, outgoing and ingoing messages are encrypted using the AES algorithm.
	 * @param request the request which has to be sent to the cloud controller
	 * @return the response from the cloud controller
	 * @throws IOException
	 */
	private String sendRequest(String request) throws IOException{
		if(!authenticated){
			return "You are not authenticated!";
		}
		aesChannel.write(request.getBytes());

		String response = "";
		try{
			response = new String(aesChannel.read());
			/*if(response == null){
				//this check was necessary when running the code on a linux machine
				close();
				return "Cloud controller suddenly went offline. Shutting down " +componentName + " now";
			}*/
		}
		catch (SocketException e){
			//cloud controller suddenly went offline. make sure to close all the resources in order to exit this client.
			close();
			return "Cloud controller suddenly went offline. Shutting down " +componentName + " now";
		}
		return response;
	}

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
		String response = sendRequest("!logout");
		authenticated = false;
		return response;
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
		if(authenticated){
			return "You are already authenticated";
		}
		try {
			RSAChannel rsaChannel = new RSAChannel(new Base64Channel(tcpChannel), new File(keysDir+"/"+username+".pem"));
			rsaChannel.sendFirstMessage(("!authenticate "+username+" ").getBytes(), controllerKey);
			String[] okMessageParts = (new String(rsaChannel.read())).split("\\s+");
			if(!Arrays.equals(Base64.decode(okMessageParts[1]),rsaChannel.getChallenge())){
				return "Challenges not equal!";
			}
			byte[] cloudControllerChallenge = Base64.decode(okMessageParts[2].getBytes());
			byte[] secretKey = Base64.decode(okMessageParts[3].getBytes());
			byte[] initializationVector = Base64.decode(okMessageParts[4].getBytes());
			SecretKey key = new SecretKeySpec(secretKey, 0, secretKey.length, "AES");
			
			aesChannel = new AESChannel(new Base64Channel(tcpChannel),key,initializationVector);
			aesChannel.write(cloudControllerChallenge); //first message encrypted in AES
		} 
		catch(FileNotFoundException e){
			return "User not found!";
		}
		String response = new String(aesChannel.read());
		if(!response.contains("already") && !response.contains("not equal")){
			authenticated = true;
		}
		return response;
	}

}