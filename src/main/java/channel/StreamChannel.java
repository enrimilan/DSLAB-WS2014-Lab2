package channel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class StreamChannel implements Channel {
	
	private PrintWriter out;
	private BufferedReader in;
	
	public StreamChannel(PrintWriter out, BufferedReader in){
		this.out = out;
		this.in = in;
	}
	
	@Override
	public void write(byte[] message) {
		out.println(new String(message));
	}

	@Override
	public byte[] read() throws IOException {
		return in.readLine().getBytes();
	}
	
	@Override
	public void close() throws IOException{
		if(out != null) out.close();
		if(in != null) in.close();
	}
	
	public void writeString(String message) {
		out.println(message);
	}
	
	public String readString() throws IOException {
		return in.readLine();
	}
}