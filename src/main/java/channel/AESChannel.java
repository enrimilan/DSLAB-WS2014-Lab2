package channel;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class AESChannel extends ChannelDecorator {
	
	private final String algorithm = "AES/CTR/NoPadding";
	private Cipher encryption;
	private Cipher decryption;
	
	public AESChannel(Channel channel,  SecretKey secretKey, byte[] ivParameter) {
		super(channel);

		IvParameterSpec ivParameterSpec = new IvParameterSpec(ivParameter);

		try {
		
			encryption = Cipher.getInstance(algorithm);
			encryption.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
			
			decryption = Cipher.getInstance(algorithm);
			decryption.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
			
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			}
		
	
	}

	@Override
	public void write(byte[] message) {
		try {
			channel.write(encryption.doFinal(message));
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public byte[] read() throws IOException {
		try {
			return decryption.doFinal(channel.read());
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void close() throws IOException {
		channel.close();
	}


}
