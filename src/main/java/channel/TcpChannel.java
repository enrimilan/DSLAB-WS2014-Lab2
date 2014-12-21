package channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpChannel implements Channel {
	
	private Channel iochannel;

	public TcpChannel(Socket socket) throws IOException {
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		
		this.iochannel = new StreamChannel(out, in);
	}

	@Override
	public void write(byte[] message) {
		iochannel.write(message);
	}

	@Override
	public byte[] read() throws IOException {
		return iochannel.read();
	}

}
