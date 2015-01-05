package controller;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.Key;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import model.ComputationRequestInfo;
import model.NodeInfo;
import admin.INotificationCallback;

public class AdminService extends UnicastRemoteObject implements IAdminConsole {

	private static final long serialVersionUID = -488461154514105199L;
	private CloudController cloudController;
	private Registry registry;
	private String bindingName;
	private String controllerHost;
	private int controllerRmiPort;

	public AdminService(CloudController cloudController, String bindingName, String controllerHost, int controllerRmiPort) throws RemoteException{
		this.cloudController = cloudController;
		this.bindingName = bindingName;
		this.controllerHost = controllerHost;
		this.controllerRmiPort = controllerRmiPort;
		try {
			registerRemoteObject();
		} 
		catch (RemoteException e) {
			System.err.println("Error while starting AdminService.");
		} 
		catch (AlreadyBoundException e) {
			System.err.println("Error while binding remote object to registry. Object alreasy bound.");
		}
	}

	/**
	 * Registers this object as a remote object
	 * @throws AccessException
	 * @throws RemoteException
	 * @throws AlreadyBoundException
	 */
	private void registerRemoteObject() throws AccessException, RemoteException, AlreadyBoundException{
		registry = LocateRegistry.createRegistry(controllerRmiPort);
		registry.bind(bindingName, this);
	}

	@Override
	public boolean subscribe(String username, int credits, INotificationCallback callback) throws RemoteException {
		return cloudController.subscribe(username,credits,callback);
	}

	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {
		List<ComputationRequestInfo> logs = new ArrayList<ComputationRequestInfo>();
		for(NodeInfo node: cloudController.getOnlineNodes()){
			Socket socket = null;
			PrintWriter out = null;
			ObjectInputStream in = null;
			try {
				socket = new Socket(node.getAddress(),node.getTcpPort());
				out = new PrintWriter(socket.getOutputStream(), true);
				out.println("!getLogs");
				in = new ObjectInputStream(socket.getInputStream());
				ComputationRequestInfo c = null;
				while((c = (ComputationRequestInfo)in.readObject())!=null){
					logs.add(c);
				}
			} 
			catch (IOException e) {
				try {
					socket.close();
					in.close();
					out.close();
				} catch (IOException e1) {}
			} 
			catch (ClassNotFoundException e) {}
		}
		//sort by timestamp
		Collections.sort(logs, new Comparator<ComputationRequestInfo>() {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
			public int compare(ComputationRequestInfo one, ComputationRequestInfo other) {
				try {
					if (sdf.parse(one.getTimestamp()).after(sdf.parse(other.getTimestamp()))) {
						return 1;
					} else {
						return -1;
					}
				} catch (ParseException e) {} 
				return -1;
			}
		});
		if(logs.size()==0){
			List<ComputationRequestInfo> emptyList = new ArrayList<ComputationRequestInfo>();
			emptyList.add(new ComputationRequestInfo("No logs found!"));
			return emptyList;
		}
		return logs;
	}

	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
		LinkedHashMap<Character, Long> map = new LinkedHashMap<Character, Long>(cloudController.getStatistics());
		LinkedHashMap<Character, Long> sortedMap = new LinkedHashMap<Character, Long>();
		//sort by occurrences of the operators
		for(int i = 0; i<4; i++){
			Character maxChar = getCharWithMaxValue(map);
			sortedMap.put(maxChar, map.get(maxChar));
			map.remove(maxChar);
		}
		return sortedMap;
	}

	/**
	 * @param map
	 * @return the character(operator) with the greatest number of occurrences
	 */
	private Character getCharWithMaxValue(LinkedHashMap<Character, Long> map){
		long max = 0;
		Character maxChar = '.';
		if(map.containsKey('+') && map.get('+')>=max){
			max = map.get('+');
			maxChar = '+';
		}
		if(map.containsKey('-') && map.get('-')>=max){
			max = map.get('-');
			maxChar = '-';
		}
		if(map.containsKey('*') && map.get('*')>=max){
			max = map.get('*');
			maxChar = '*';
		}
		if(map.containsKey('/') && map.get('/')>=max){
			max = map.get('/');
			maxChar = '/';
		}
		return maxChar;
	}

	@Override
	public Key getControllerPublicKey() throws RemoteException {
		// We don't have to implement this method.
		return null;
	}

	@Override
	public void setUserPublicKey(String username, byte[] key)
			throws RemoteException {
		// We don't have to implement this method.
	}
	
	/**
	 * Unbind and exit the AdminService.
	 */
	public void close() {
		try {
			registry.unbind(bindingName);
			UnicastRemoteObject.unexportObject(this,true);
		} catch (RemoteException e) {
			System.err.println("Unbind error.");
		} catch (NotBoundException e) {
			System.err.println("Unbind error.");
		}
	}
}