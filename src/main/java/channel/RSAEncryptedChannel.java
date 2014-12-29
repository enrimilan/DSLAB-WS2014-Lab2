package channel;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class RSAEncryptedChannel extends EDChannel {
	
	private final String algorithm = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";
	
	public RSAEncryptedChannel(TcpChannel channel, PublicKey sendKey, PrivateKey receiveKey) throws IOException {
		
		super(channel);
		
		if(sendKey != null) {
			setUpSendKey(sendKey);
		}
		if(receiveKey != null) {
			setUpReceiveKey(receiveKey);
		}
	}
	
	public RSAEncryptedChannel(TcpChannel channel){
		super(channel);
	}
	
	public void setUpSendKey(PublicKey sendKey) throws IntegrityException {
		try {
			
			encryption = Cipher.getInstance(algorithm);
			encryption.init(Cipher.ENCRYPT_MODE, sendKey);
			
			
		} catch (NoSuchAlgorithmException e) {
			throw new IntegrityException(e.getMessage(), e);
		} catch (NoSuchPaddingException e) {
			throw new IntegrityException(e.getMessage(), e);
		} catch (InvalidKeyException e) {
			throw new IntegrityException(e.getMessage(), e);
		}
	}

	public void setUpReceiveKey(PrivateKey receiveKey) throws IntegrityException {
		try {
			
			
			decryption = Cipher.getInstance(algorithm);
			decryption.init(Cipher.DECRYPT_MODE, receiveKey);
			
			
		} catch (NoSuchAlgorithmException e) {
			throw new IntegrityException(e.getMessage(), e);
		} catch (NoSuchPaddingException e) {
			throw new IntegrityException(e.getMessage(), e);
		} catch (InvalidKeyException e) {
			throw new IntegrityException(e.getMessage(), e);
		}
	}
}
