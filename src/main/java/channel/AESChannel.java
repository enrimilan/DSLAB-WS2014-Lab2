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
				System.err.println("Invalid Key: please check the length and encoding of the key.");
			} catch (InvalidAlgorithmParameterException e) {
				System.err.println("Invalid or inappropriate algorithm parameters.");
			} catch (NoSuchAlgorithmException e) {
				System.err.println("Algorithm" + algorithm + "not found.");
			} catch (NoSuchPaddingException e) {
				System.err.println("Padding mechanism not available in the environment.");
			}
	}

	@Override
	public void write(byte[] message) {
		try {
			channel.write(encryption.doFinal(message));
		} catch (IllegalBlockSizeException e) {
			System.err.println("Input data is not a multiple of the block-size.");
		} catch (BadPaddingException e) {
			System.err.println("Bad padding.");
		}
	}

	@Override
	public byte[] read() throws IOException {
		try {
			return decryption.doFinal(channel.read());
		} catch (IllegalBlockSizeException e) {
			System.err.println("Input data is not a multiple of the block-size.");
		} catch (BadPaddingException e) {
			System.err.println("Bad padding.");
		}
		return null;
	}
	
	@Override
	public void close() throws IOException {
		channel.close();
	}
}