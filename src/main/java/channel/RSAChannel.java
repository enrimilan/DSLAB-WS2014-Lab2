package channel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.bouncycastle.util.encoders.Base64;

import util.Keys;


public class RSAChannel extends ChannelDecorator {
	
	private final String algorithm = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";
	private PublicKey publicKey;
	private PrivateKey privateKey;
	private Cipher encryption;
	private Cipher decryption;
	private byte[] challenge;
	private SecretKey key;
	private byte[] initializationVector;
	
	public RSAChannel(Channel channel, File privateKeyPath) throws IOException {
		super(channel);
		
		try {
			privateKey = Keys.readPrivatePEM(privateKeyPath);
			initializeCipher(privateKey);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void write(byte[] message) {
		channel.write(message);
		
	}

	@Override
	public byte[] read() throws IOException {
		try {
			return decryption.doFinal(Base64.decode(channel.read()));
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return channel.read();
	}
	
	@Override
	public void close() throws IOException {
		channel.close();
		
	}
	
	public void sendFirstMessage(byte[] firstPartOfMessage, String controllerKey) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
		challenge = generateSecureRandomNumber(32);
		byte[] base64EncryptedSecureRandomNumber = Base64.encode(challenge);
		publicKey = Keys.readPublicPEM(new File(controllerKey));
		initializeCipher(publicKey);
		byte[] message = new byte[firstPartOfMessage.length + base64EncryptedSecureRandomNumber.length];
		System.arraycopy(firstPartOfMessage, 0, message, 0, firstPartOfMessage.length);
		System.arraycopy(base64EncryptedSecureRandomNumber, 0, message, firstPartOfMessage.length, base64EncryptedSecureRandomNumber.length);
		write(Base64.encode(encryption.doFinal(message)));
	}
	
	public void sendSecondMessage(byte[] clientChallenge, String userKey) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
		byte[] space = " ".getBytes();
		byte[] ok = "!ok".getBytes();
		challenge = Base64.encode(generateSecureRandomNumber(32));
		byte[] secretKey = Base64.encode(generateSecretAESKey().getEncoded());
		initializationVector = Base64.encode(generateSecureRandomNumber(16));
		publicKey = Keys.readPublicPEM(new File(userKey));
		initializeCipher(publicKey);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(ok);
		outputStream.write(space);
		outputStream.write(clientChallenge);
		outputStream.write(space);
		outputStream.write(challenge);
		outputStream.write(space);
		outputStream.write(secretKey);
		outputStream.write(space);
		outputStream.write(initializationVector);
		byte message[] = outputStream.toByteArray();
		write(Base64.encode(encryption.doFinal(message)));
		
	}
	
	private byte[] generateSecureRandomNumber(int length){
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[length];
		secureRandom.nextBytes(number);
		return number;
	}
	
	private void initializeCipher(PublicKey publicKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException{
		encryption = Cipher.getInstance(algorithm);
		encryption.init(Cipher.ENCRYPT_MODE, publicKey);
	}
	
	private void initializeCipher(PrivateKey privateKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException{
		decryption = Cipher.getInstance(algorithm);
		decryption.init(Cipher.DECRYPT_MODE, privateKey);
	}
	
	private SecretKey generateSecretAESKey(){
		KeyGenerator generator = null;
		try {
			generator = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// KEYSIZE is in bits
		generator.init(256);
		key = generator.generateKey();
		return key;
	}

	public byte[] getChallenge() {
		return challenge;
	}
	
	public SecretKey getKey(){
		return key;
	}
	
	public byte[] getInitializationVector(){
		return Base64.decode(initializationVector);
	}

}
