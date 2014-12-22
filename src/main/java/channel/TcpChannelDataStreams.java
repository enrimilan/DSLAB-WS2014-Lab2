package channel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TcpChannelDataStreams implements Channel {
	
	private Channel iochannel;

	public TcpChannelDataStreams(Socket socket) throws IOException {
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
		DataInputStream in = new DataInputStream(socket.getInputStream());
		
		this.iochannel = new DataStreamChannel(out, in);
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
