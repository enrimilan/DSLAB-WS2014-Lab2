package admin;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class NotificationCallback extends UnicastRemoteObject implements INotificationCallback,Serializable {
	
	private static final long serialVersionUID = -6309051212252537075L;

	public NotificationCallback() throws RemoteException {}
	
	@Override
	public void notify(String username, int credits) throws RemoteException {
		System.out.println("Notification: " + username + " has less than " + credits + " credits.");
	}
}