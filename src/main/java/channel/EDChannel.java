package channel;

import java.io.IOException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import org.bouncycastle.util.encoders.Base64;

public class EDChannel implements Channel {

	protected Cipher encryption;
	protected Cipher decryption;

	private TcpChannel tcpChannel;

	public EDChannel(TcpChannel tcpChannel) {
		this.tcpChannel = tcpChannel;
	}

	@Override
	public void write(byte[] message) {
		tcpChannel.write(message);
	}

	@Override
	public byte[] read() throws IOException {
		return tcpChannel.read();
	}

	@Override
	public void close() throws IOException {
		tcpChannel.close();
	}
	
	public void send(String message) throws IntegrityException,
			BadPaddingException {
		try {

			tcpChannel.writeString(new String(Base64.encode(encryption
					.doFinal(message.getBytes()))));

		} catch (IllegalBlockSizeException e) {
			throw new IntegrityException(e.getMessage());
		}
	}


	public String receive() throws IOException {
		try {

			String message = this.tcpChannel.readString();
			if (message == null) {
				return null;
			}

			return new String(decryption.doFinal(Base64.decode(message.getBytes())));

		} catch (IllegalBlockSizeException e) {
			throw new IntegrityException(e.getMessage());
		} catch (BadPaddingException e) {
			throw new IntegrityException(e.getMessage());
		}
	}

	public TcpChannel getPlainChannel() {
		return tcpChannel;
	}

}
