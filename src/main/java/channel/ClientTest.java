package channel;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import util.Config;

public class ClientTest implements Runnable {

	@Override
	public void run() {
		try {
			Socket socket = new Socket("localhost",5678);
			Channel c = new HmacChannel(new TcpChannel(socket),new Config("controller").getString("hmac.key"));
			String request = "!compute 2 * 2";
			c.write(request.getBytes());
			socket.close();

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		ClientTest test = new ClientTest();
		test.run();
	}

}
