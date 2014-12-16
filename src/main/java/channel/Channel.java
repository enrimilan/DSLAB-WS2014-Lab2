package channel;

import java.io.IOException;

public interface Channel {
	
	public void write(byte[] message);
	public byte[] read() throws IOException;
	
}
