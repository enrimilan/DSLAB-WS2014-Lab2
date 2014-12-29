package channel;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class AESEncryptedChannel extends EDChannel{
	
	private final String algorithm = "AES/CTR/NoPadding";
	
	public AESEncryptedChannel(TcpChannel tcpChannel,  SecretKey secretKey, byte[] ivParameter) {
		super(tcpChannel);

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
	
}
