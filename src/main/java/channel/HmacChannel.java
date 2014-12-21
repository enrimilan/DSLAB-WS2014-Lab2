package channel;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

public class HmacChannel extends ChannelDecorator {
	
	private Mac hMac;

	public HmacChannel(Channel channel, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
		super(channel);
		
		Key secretKey = new SecretKeySpec(secret.getBytes(),"HmacSHA256");
		hMac = Mac.getInstance("HmacSHA256");
		hMac.init(secretKey);
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
		
		System.out.println(new String(hmacmsg)); // TODO: remove
		
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

}
