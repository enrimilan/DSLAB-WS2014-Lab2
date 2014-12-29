package channel;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bouncycastle.util.encoders.Base64;

public class ServerAuthenticator extends Challenge {

	private String username;

	SecretKey secretKey;
	byte[] ivParameter;
	String serverChallenge;

	public ServerAuthenticator() {
		serverChallenge = new String(Base64.encode(getRandom(32)));
		ivParameter = getRandom(16);
	}

	public AESEncryptedChannel rsaAuthenticate(EDChannel channel,
			PublicKey sendKey, PrivateKey receiveKey) throws IOException,
			BadPaddingException {

		((RSAEncryptedChannel) channel).setUpReceiveKey(receiveKey);
		String input = channel.receive();
		//System.out.println("FirstMsg" + input);
		if (input == null) {
			channel.close();
			throw new IntegrityException("Channel closed");
		}

		String[] clientRequest = input.split(" ");
		if (clientRequest.length != 3) {
			channel.close();
			throw new IntegrityException("Invalid request from client.");
		}
		if (!clientRequest[0].equals("!authenticate")) {
			channel.close();
			throw new IntegrityException("Invalid request from client.");
		}

		username = clientRequest[1];
		String clientChallenge = clientRequest[2];

		String serverChallenge = new String(Base64.encode(getRandom(32)));

		KeyGenerator generator;
		try {
			generator = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			channel.close();
			throw new IntegrityException(e.getMessage());
		}
		generator.init(256);
		SecretKey secretKey = generator.generateKey();

		byte[] ivParameter = getRandom(16);

		String response = "!ok " + clientChallenge + " " + serverChallenge
				+ " " + new String(Base64.encode(secretKey.getEncoded())) + " "
				+ new String(Base64.encode(ivParameter));

		((RSAEncryptedChannel) channel).setUpSendKey(sendKey);
		channel.send(response);
		//System.out.println("SecondMsg" + response);
		AESEncryptedChannel aesEncryptedChannel = new AESEncryptedChannel(
				channel.getPlainChannel(), secretKey, ivParameter);
		String clientResponse = aesEncryptedChannel.receive();
		if (!clientResponse.equals(serverChallenge)) {
			channel.close();
			throw new IntegrityException(
					"Invalid response from client (wrong server challenge).");
		}

		//System.out.println("ThirdMsg" + clientResponse);

		return aesEncryptedChannel;
	}

	public String getUsername() {
		return username;
	}
}
