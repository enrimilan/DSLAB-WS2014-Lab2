package channel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DataStreamChannel implements Channel {

	private DataOutputStream out;
	private DataInputStream in;

	public DataStreamChannel(DataOutputStream out, DataInputStream in){
		this.out = out;
		this.in = in;
	}

	@Override
	public void write(byte[] message) {
		try {
			out.writeInt(message.length); // write length of the message
			out.write(message); // write the message
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}           
	}

	@Override
	public byte[] read() throws IOException {
		int length = in.readInt();                    // read length of incoming message
		byte[] message = new byte[length];
		in.readFully(message, 0, message.length); // read the message
		return message;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}
}