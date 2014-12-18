package controller;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.Key;
import java.util.LinkedHashMap;
import java.util.List;

import util.Config;
import model.ComputationRequestInfo;
import admin.INotificationCallback;

public class AdminService extends UnicastRemoteObject implements IAdminConsole {
	
	private CloudController cloudController;
	private Registry registry;
	private String bindingName;
	private String controllerHost;
	private int controllerRmiPort;
	private String keysDir;
	
	public AdminService(CloudController cloudController) throws RemoteException{
		this.cloudController = cloudController;
		readAdminProperties();
		try {
			registerRemoteObject();
		} 
		catch (RemoteException e) {
			throw new RuntimeException("Error while starting server.", e);
		} catch (AlreadyBoundException e) {
			throw new RuntimeException("Error while binding remote object to registry.", e);
		}
	}
	
	/**
	 * Reads all the parameters from the admin's properties file.
	 */
	private void readAdminProperties(){
		Config config = new Config("admin");
		bindingName = config.getString("binding.name");
		controllerHost = config.getString("controller.host");
		controllerRmiPort = config.getInt("controller.rmi.port");
		keysDir = config.getString("keys.dir");
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
		return cloudController.getStatistics();
	}

	@Override
	public Key getControllerPublicKey() throws RemoteException {
		// // We don't have to implement this method.
		return null;
	}

	@Override
	public void setUserPublicKey(String username, byte[] key)
			throws RemoteException {
		// // We don't have to implement this method.
		
	}

}
