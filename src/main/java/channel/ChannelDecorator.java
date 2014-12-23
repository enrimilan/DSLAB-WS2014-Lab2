package channel;

import java.io.IOException;

public abstract class ChannelDecorator implements Channel {
	
	protected Channel channel;
	
	protected ChannelDecorator(Channel channel){
		this.channel = channel;
	}
	
	public void setChannel(Channel channel) {
		this.channel = channel;
	}
	
	public Channel getChannel() {
		return this.channel;
	}
	
	public void writeString(String message) {
		write(message.getBytes());
	}
	
	public String readString() throws IOException {
		return new String(read());
	}
	
	public abstract void write(byte[] message);

	public abstract byte[] read() throws IOException;
}
