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

/**
 * Messages that are sent are encrypted with the RSA algorithm, messages that are received are decrypted back.
 */
public class RSAChannel extends ChannelDecorator {

	private final String algorithm = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";
	private Cipher encryption;
	private Cipher decryption;
	private byte[] challenge;
	private SecretKey key;
	private byte[] initializationVector;
	
	/**
	 * 
	 * @param channel
	 * @param privateKeyPath the path of the user's or controller's private key
	 * @throws IOException
	 */
	public RSAChannel(Channel channel, File privateKeyPath) throws IOException {
		super(channel);
		try {
			PrivateKey privateKey = Keys.readPrivatePEM(privateKeyPath);
			decryption = Cipher.getInstance(algorithm);
			decryption.init(Cipher.DECRYPT_MODE, privateKey);
		} catch (InvalidKeyException e) {
			System.err.println("Invalid Key: please check the length and encoding of the private key.");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Algorithm" + algorithm + "not found.");
		} catch (NoSuchPaddingException e) {
			System.err.println("Padding mechanism not available in the environment.");
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
			System.err.println("Input data is not a multiple of the block-size.");
		} catch (BadPaddingException e) {
			System.err.println("Bad padding.");
		}
		return channel.read();
	}

	@Override
	public void close() throws IOException {
		channel.close();

	}
	
	/**
	 * The first message is a !authenticate request (analogous to the !login command from Lab 1). 
	 * The syntax of the message is: !authenticate username client-challenge . 
	 * This message is sent by the client and is encrypted using RSA initialized with the cloud controller's public key.
	 * @param firstPartOfMessage the part !authenticate username of the message
	 * @param controllerKey the cloud controller's public key
	 */
	public void sendFirstMessage(byte[] firstPartOfMessage, String controllerKey) {
		try {
			challenge = generateSecureRandomNumber(32);
			byte[] base64EncryptedSecureRandomNumber = Base64.encode(challenge);
			PublicKey publicKey = Keys.readPublicPEM(new File(controllerKey));
			initializeCipher(publicKey);
			byte[] message = new byte[firstPartOfMessage.length + base64EncryptedSecureRandomNumber.length];
			System.arraycopy(firstPartOfMessage, 0, message, 0, firstPartOfMessage.length);
			System.arraycopy(base64EncryptedSecureRandomNumber, 0, message, firstPartOfMessage.length, base64EncryptedSecureRandomNumber.length);
			write(Base64.encode(encryption.doFinal(message)));
		} catch (InvalidKeyException e) {
			System.err.println("Invalid Key: please check the length and encoding of the public controller key.");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Algorithm" + algorithm + "not found.");
		} catch (NoSuchPaddingException e) {
			System.err.println("Padding mechanism not available in the environment.");
		} catch (IllegalBlockSizeException e) {
			System.err.println("Input data is not a multiple of the block-size.");
		} catch (BadPaddingException e) {
			System.err.println("Bad padding.");
		} catch (IOException e) {
			System.err.println("IOException: maybe the file does not exist.");
		}
	}
	
	/**
	 * The second message is sent by the cloud controller and is encrypted using RSA initialized with the user's public key. 
	 * Its syntax is: !ok client-challenge controller-challenge secret-key iv-parameter
	 * @param clientChallenge the challenge that the client sent in the first message.
	 * @param userKey the directory where the user's public key is located.
	 */
	public void sendSecondMessage(byte[] clientChallenge, String userKey) {
		try {
			byte[] space = " ".getBytes();
			byte[] ok = "!ok".getBytes();
			challenge = Base64.encode(generateSecureRandomNumber(32));
			byte[] secretKey = Base64.encode(generateSecretAESKey().getEncoded());
			initializationVector = Base64.encode(generateSecureRandomNumber(16));
			PublicKey publicKey = Keys.readPublicPEM(new File(userKey));
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
			outputStream.close();
			write(Base64.encode(encryption.doFinal(message)));
		} 
		catch (InvalidKeyException e) {
			System.err.println("Invalid Key: please check the length and encoding of the user's key.");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Algorithm" + algorithm + "not found.");
		} catch (NoSuchPaddingException e) {
			System.err.println("Padding mechanism not available in the environment.");
		} catch (IllegalBlockSizeException e) {
			System.err.println("Input data is not a multiple of the block-size.");
		} catch (BadPaddingException e) {
			System.err.println("Bad padding.");
		}
		catch (IOException e) {
			System.err.println("IOException: maybe the file does not exist.");
		}
	}
	
	/**
	 * Generates a random secure number.
	 * @param length the size in bytes of that number.
	 * @return the secure random number.
	 */
	private byte[] generateSecureRandomNumber(int length){
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[length];
		secureRandom.nextBytes(number);
		return number;
	}
	
	/**
	 * Initializes the cipher used for encryption.
	 * @param publicKey the user's or controller's public key.
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	private void initializeCipher(PublicKey publicKey) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException{
		encryption = Cipher.getInstance(algorithm);
		encryption.init(Cipher.ENCRYPT_MODE, publicKey);
	}
	
	/**
	 * Generates a secret AES key for the second message, which is sent by the cloud controller back to the client.
	 * @return the generated secret AES key
	 */
	private SecretKey generateSecretAESKey(){
		KeyGenerator generator = null;
		try {
			generator = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Algorithm AES not found.");
		}
		// KEYSIZE is in bits
		generator.init(256);
		key = generator.generateKey();
		return key;
	}
	
	/**
	 * Getter used by the client side
	 * @return the generated challenge for the client
	 */
	public byte[] getChallenge() {
		return challenge;
	}
	
	/**
	 * Getter used by cloud controller side
	 * @return the generated AES key for the cloud controller
	 */
	public SecretKey getKey(){
		return key;
	}
	
	/**
	 * Getter used by cloud controller side
	 * @return he generated initialization vector for the cloud controller
	 */
	public byte[] getInitializationVector(){
		return Base64.decode(initializationVector);
	}
}