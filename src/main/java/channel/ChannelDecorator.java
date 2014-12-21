package channel;

import java.io.IOException;

public abstract class ChannelDecorator implements Channel {
	
	protected Channel channel;
	
	protected ChannelDecorator(Channel channel){
		this.channel = channel;
	}
	
	public void write(byte[] message) {
		channel.write(message);
	}

	public byte[] read() throws IOException {
		return channel.read();
	}
}
