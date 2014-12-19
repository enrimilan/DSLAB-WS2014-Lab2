package admin;

import cli.Command;
import cli.Shell;
import controller.IAdminConsole;
import model.ComputationRequestInfo;
import util.Config;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.Key;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class AdminConsole implements IAdminConsole, Runnable {

	private Config config;
	private String bindingName;
	private String controllerHost;
	private int controllerRmiPort;
	private String keysDir;
	private Shell shell;
	private IAdminConsole adminService;

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
	public AdminConsole(String componentName, Config config, InputStream userRequestStream, PrintStream userResponseStream) {
		this.config = config;
		this.shell = new Shell(componentName, userRequestStream, userResponseStream);
	}

	/**
	 * Reads all the parameters from the admin's properties file.
	 */
	private void readAdminProperties(){
		bindingName = config.getString("binding.name");
		controllerHost = config.getString("controller.host");
		controllerRmiPort = config.getInt("controller.rmi.port");
		keysDir = config.getString("keys.dir");
	}

	/**
	 * Gets the reference to the AdminService component.
	 */
	private void getReferenceToTheRemoteObject(){
		try{
			// obtain registry that was created by the server
			Registry registry = LocateRegistry.getRegistry(controllerHost,controllerRmiPort);
			// look for the bound server remote-object implementing the IServer interface
			adminService = (IAdminConsole) registry.lookup(bindingName);
		} catch (RemoteException e) {
			throw new RuntimeException("Error while obtaining registry/server-remote-object.", e);
		} catch (NotBoundException e) {
			throw new RuntimeException("Error while looking for server-remote-object.", e);
		}
	}

	/**
	 * Registers to the shell the interactive commands that the admin can perform and then starts the shell.
	 */
	private void startShell(){
		shell.register(this);
		getReferenceToTheRemoteObject();
		new Thread(shell).start();
	}

	/**
	 * Starts the AdminConsole.
	 */
	@Override
	public void run() {
		readAdminProperties();
		startShell();
	}
	
	@Command(value="subscribe")
	public String subscribe(String username, int credits) throws RemoteException{
		boolean subscribed = subscribe(username, credits, new NotificationCallback());
		if(subscribed){
			return "Successfully subscribed for user " + username + ".";
		}
		else{
			return "Could not subscribe: you have already subsribed or this user does not exist or credits are less than 1";
		}
	}
	
	
	@Override
	public boolean subscribe(String username, int credits, INotificationCallback callback) throws RemoteException {
		return adminService.subscribe(username, credits, callback);
	}

	@Command(value="getLogs")
	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {
		return adminService.getLogs();
	}

	@Command(value="statistics")
	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
		return adminService.statistics();
	}

	@Override
	public Key getControllerPublicKey() throws RemoteException {
		// We don't have to implement this method.
		return null;
	}

	@Override
	public void setUserPublicKey(String username, byte[] key) throws RemoteException {
		// We don't have to implement this method.
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link AdminConsole}
	 *            component
	 */
	public static void main(String[] args) {
		AdminConsole adminConsole = new AdminConsole(args[0], new Config("admin"), System.in, System.out);
		adminConsole.run();
	}
}
