package channel;

import java.io.IOException;

public class HmacChannel extends ChannelDecorator {

	public HmacChannel(Channel channel) {
		super(channel);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void write(byte[] message) {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] read() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
