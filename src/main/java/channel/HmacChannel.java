package channel;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

import org.bouncycastle.util.encoders.Base64;

import util.Keys;

public class HmacChannel extends ChannelDecorator {
	
	private final String algorithm = "HmacSHA256";
	private Mac hMac;

	public HmacChannel(Channel channel, String hmacKey) {
		super(channel);
		
		try {
			Key secretKey = Keys.readSecretKey(new File(hmacKey));
			hMac = Mac.getInstance(algorithm);
			hMac.init(secretKey);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Algorithm" + algorithm + "not found.");
		} catch (InvalidKeyException e) {
			System.err.println("Invalid Key: please check the length and encoding of the key.");
		} catch (IOException e) {
			System.err.println("IOException: maybe the file does not exist.");
		}
		
	}

	@Override
	public void write(byte[] message) {
		byte[] tmp = message;
		hMac.update(tmp);
		byte[] hash = Base64.encode(hMac.doFinal());
		
		int ml = message.length;
		int hl = hash.length;
		int al = hash.length + message.length + 1;
		char s = ' ';
		
		byte[] hmacmsg = new byte[al];
		System.arraycopy(hash, 0, hmacmsg, 0, hl);
		hmacmsg[hl] = (byte) s;
		System.arraycopy(message, 0, hmacmsg, hl+1, ml);
		
		channel.write(hmacmsg);
	}

	@Override
	public byte[] read() throws IOException {
		
		byte[] received = channel.read();
		
		String[] splittedResult = new String (received).split("\\s+");
		byte[] receivedHash = Base64.decode(splittedResult[0].getBytes());
		String plaintext = "";
		for(int j = 1; j<splittedResult.length; j++){
			plaintext += splittedResult[j]+" ";
		}
		plaintext = plaintext.trim();
		
		hMac.update(plaintext.getBytes());
		byte[] computedHash = hMac.doFinal();
		
		if (MessageDigest.isEqual(computedHash, receivedHash))
			return plaintext.getBytes();
		else
			throw new IntegrityException("!tampered");
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

}