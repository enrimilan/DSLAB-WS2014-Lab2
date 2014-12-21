package channel;

import java.io.IOException;

public abstract class ChannelDecorator implements Channel {
	
	protected Channel channel;
	
	protected ChannelDecorator(Channel channel){
		this.channel = channel;
	}
	
	public abstract void write(byte[] message);
	public abstract byte[] read() throws IOException;
}
