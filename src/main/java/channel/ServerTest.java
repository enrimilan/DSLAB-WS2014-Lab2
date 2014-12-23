package channel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

import util.Config;

public class ServerTest implements Runnable {

	@Override
	public void run() {
		try {
			String secret = new Config("controller").getString("hmac.key");
			Key secretKey = new SecretKeySpec(secret.getBytes(),"HmacSHA256");
			Mac hMac = Mac.getInstance("HmacSHA256");
			hMac.init(secretKey);
			ServerSocket serverSocket = new ServerSocket(5678);
			Socket clientSocket = serverSocket.accept();
			TcpChannel c = new TcpChannel(clientSocket);
			String request = new String(c.readString());
			System.out.println(request);
			String[] splittedResult = new String (request).split("\\s+");
			hMac.update("!compute 2 * 2 ".getBytes());
			byte[] computedHash = hMac.doFinal();
			System.out.println(new String(Base64.encode(computedHash)));
			byte[] receivedHash = Base64.decode(splittedResult[0].getBytes());
			System.out.println(MessageDigest.isEqual(computedHash, receivedHash));
			clientSocket.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void main(String[] args) {
		ServerTest test = new ServerTest();
		test.run();
	}

}
