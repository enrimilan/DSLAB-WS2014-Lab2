package channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class TcpChannel implements Channel {
	
	private PrintWriter out;
	private BufferedReader in;
	
	public TcpChannel(PrintWriter out, BufferedReader in){
		this.out = out;
		this.in = in;
	}
	
	@Override
	public void write(byte[] message) {
		writeString(new String(message));
	}

	@Override
	public byte[] read() throws IOException {
		return readString().getBytes();
	}
	
	public void writeString(String message) {
		out.write(message);
	}
	
	public String readString() throws IOException {
		return in.readLine();
	}
}