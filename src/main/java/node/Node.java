package node;

import util.Config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import model.ComputationRequestInfo;
import cli.Command;
import cli.Shell;

public class Node implements INodeCli, Runnable {

	private String componentName;
	private Config config;
	private String logDir;
	private int tcpPort;
	private String controllerHost;
	private int controllerUdpPort;
	private int nodeAlive;
	private String nodeOperators;
	private int nodeRmin;
	private int resourceLevel;
	private int newResourceLevel;
	private Mac hMac;
	private Shell shell;
	private AlivePacketSender alivePacketSender;
	private Listener listener;
	private ExecutorService executor;
	private final static ThreadLocal<SimpleDateFormat> threadLocal = new ThreadLocal<SimpleDateFormat>() {
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
		};
	};

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
	public Node(String componentName, Config config, InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.shell = new Shell(componentName, userRequestStream, userResponseStream);
		executor = Executors.newCachedThreadPool();
	}

	/**
	 * Reads all the parameters from the node's properties file.
	 */
	private void readNodeProperties(){
		logDir = config.getString("log.dir");
		tcpPort = config.getInt("tcp.port");
		controllerHost = config.getString("controller.host");
		controllerUdpPort = config.getInt("controller.udp.port");
		nodeAlive = config.getInt("node.alive");
		nodeOperators = config.getString("node.operators");
		nodeRmin = config.getInt("node.rmin");
		resourceLevel = 0;
		newResourceLevel = 0;
		try {
			generateHMAC(config.getString("hmac.key"));
		} 
		catch (InvalidKeyException e) {} 
		catch (NoSuchAlgorithmException e) {} 
		catch (IOException e) {}
	}

	/**
	 * Registers to the shell the interactive commands that the node can perform and then starts the shell.
	 */
	private void startShell(){
		shell.register(this);
		executor.submit(shell);
	}

	/**
	 * Tells the cloud controller if this node is still alive. See {@link AlivePacketSender} for more details.
	 */
	private void startAlivePacketSender(){
		alivePacketSender = new AlivePacketSender(tcpPort, controllerHost, controllerUdpPort, nodeAlive, nodeOperators,this);
		executor.submit(alivePacketSender);
	}

	/**
	 * Listens for requests from the cloud controller and other nodes. See {@link Listener} for more details.
	 */
	private void startListener(){
		listener = new Listener(tcpPort,this,nodeRmin);
		executor.submit(listener);
	}

	/**
	 * Creates a log file containing the request and the result of an operation
	 * @param request
	 * 		The computation request.
	 * @param result
	 * 		The result of the computation request.
	 */
	public void createLogFile(String request, String result){
		BufferedWriter writer = null;
		SimpleDateFormat sdf = threadLocal.get();
		File file = new File(logDir+"/"+sdf.format(new Date())+"_"+componentName+".log");
		file.getParentFile().mkdirs();
		try {
			writer = new BufferedWriter(new PrintWriter(file));
			writer.write(request);
			writer.newLine();
			writer.write(result);
			writer.close();
		} 
		catch (FileNotFoundException e) {} 
		catch (UnsupportedEncodingException e) {} 
		catch (IOException e) {}
	}
	
	/**
	 * Sets a new temporary resource level, which will possibly be the true resource level in case of a successful Two-Phase commit.
	 * @param resourceLevel the new resource level
	 */
	public void setNewResourceLevel(int resourceLevel) {
		this.newResourceLevel = resourceLevel;
	}

	/**
	 * Sets the new resource level for this node(the Two-Phase commit was successful).
	 * @param resourceLevel the new resource level
	 */
	public void commit(int resourceLevel){
		this.resourceLevel = resourceLevel;
	}

	/**
	 * Sets the temporary resource level back to the old resource level.
	 */
	public void rollback(){
		this.newResourceLevel = resourceLevel;
	}

	/**
	 * @return the minimum resource level of this node.
	 */
	public int getNodeRmin(){
		return nodeRmin;
	}

	/**
	 * Generates the HMAC for this node
	 * @param hMacKeyDir the directory of hmac.key
	 * @throws IOException
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 */
	private void generateHMAC(String hMacKeyDir) throws IOException, InvalidKeyException, NoSuchAlgorithmException{
		FileInputStream fis = new FileInputStream(hMacKeyDir);
		byte[] key = new byte[2048];
		fis.read(key);
		fis.close();
		Key secretKey = new SecretKeySpec(key,"HmacSHA256");
		hMac = Mac.getInstance("HmacSHA256");
		hMac.init(secretKey);
	}

	public Mac getHMAC(){
		return hMac;
	}
	
	public String getSecret() {
		return config.getString("hmac.key");
	}
	
	/**
	 * @return all the log files of this node as a list of DTOs
	 * @throws IOException
	 */
	public ArrayList<ComputationRequestInfo> getLogs() throws IOException{
		ArrayList<ComputationRequestInfo> logs = new ArrayList<ComputationRequestInfo>();
		BufferedReader br = null;
		File folder = new File(logDir);
		if(folder.exists()){
			File[] files = folder.listFiles();
			for (int i = 0; i<files.length; i++){
				br = new BufferedReader(new FileReader(logDir+"/"+files[i].getName()));
				String request = br.readLine();
				String response = br.readLine();
				logs.add(new ComputationRequestInfo(files[i].getName(),componentName,request,response));
			}
			if(br != null){
				br.close();
			}
		}
		return logs;
	}

	/**
	 * Starts the node.
	 */
	@Override
	public void run() {
		readNodeProperties();
		startShell();
		startAlivePacketSender();
		startListener();
	}

	@Command(value="exit")
	@Override
	public String exit() throws IOException {
		shell.close();
		alivePacketSender.stopRunning();
		listener.stopRunning();
		executor.shutdown();
		return "Shuting down "+ componentName+" now.";
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Node} component,
	 *            which also represents the name of the configuration
	 */
	public static void main(String[] args) {
		Node node = new Node(args[0], new Config(args[0]), System.in, System.out);
		node.run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String history(int numberOfRequests) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Command(value="resources")
	@Override
	public String resources() throws IOException {
		return resourceLevel+"";
	}
}