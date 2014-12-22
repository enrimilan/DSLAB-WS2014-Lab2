package channel;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientTest implements Runnable {

	@Override
	public void run() {
		try {
			Socket socket = new Socket("localhost",5678);
			Channel c = new TcpChannel(socket);
			String request = "haha du pappnase!";
			c.write(request.getBytes());
			String response = new String(c.read());
			System.out.println(response);
			socket.close();

			Thread.sleep(3000);
			//and now with datastreams
			socket = new Socket("localhost",5678);
			c = new TcpChannelDataStreams(socket);
			request = "haha du pappnase!";
			c.write(request.getBytes());
			response = new String(c.read());
			System.out.println(response);
			socket.close();

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		ClientTest test = new ClientTest();
		test.run();
	}

}
