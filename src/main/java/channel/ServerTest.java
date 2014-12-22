package channel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTest implements Runnable {

	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(5678);
			Socket clientSocket = serverSocket.accept();
			Channel c = new TcpChannel(clientSocket);
			String request = new String(c.read());
			System.out.println(request);
			String response = "söwa pappnase. Des haßt owa ruamzuzla hier, mia san in wien!";
			c.write(response.getBytes());
			clientSocket.close();
			
			//now with datastreams
			clientSocket = serverSocket.accept();
			c = new TcpChannelDataStreams(clientSocket);
			request = new String(c.read());
			System.out.println(request);
			response = "söwa pappnase. Des haßt owa ruamzuzla hier, mia san in wien!";
			c.write(response.getBytes());
			clientSocket.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public static void main(String[] args) {
		ServerTest test = new ServerTest();
		test.run();
	}

}
