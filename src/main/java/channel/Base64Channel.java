package channel;

import java.io.IOException;

import org.bouncycastle.util.encoders.Base64;

public class Base64Channel extends ChannelDecorator {

	public Base64Channel(Channel channel) {
		super(channel);
	}
	
	public void write(byte[] message) {
		write(Base64.encode(message));
	}

	public byte[] read() throws IOException {
		return Base64.decode(read());
	}

}
