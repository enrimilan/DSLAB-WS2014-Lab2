package channel;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;

import util.Config;
import util.Keys;

public class ClientAuthenticator extends Challenge {
	private String username;

	String clientChallenge;
	SecretKey secretKey;
	byte[] ivParameter;
	PublicKey publicKey;
	PrivateKey privateKey;
	Keys key;
	Config config;

	public ClientAuthenticator(String username) {
		this.username = username;
		clientChallenge = new String(Base64.encode(getRandom(32)));
	}

	public AESEncryptedChannel rsaAuthenticate(EDChannel channel,
			PublicKey sendKey, PrivateKey receiveKey) throws IOException,
			BadPaddingException {

		String clientChallenge = new String(Base64.encode(getRandom(32)));


		((RSAEncryptedChannel) channel).setUpSendKey(sendKey);
		channel.send("!authenticate " + username + " " + clientChallenge);

		((RSAEncryptedChannel) channel).setUpReceiveKey(receiveKey);
		String response = channel.receive();

		if (response == null) {
			channel.close();
			throw new IntegrityException("Connection closed");
		}

		String[] serverResponse = response.split(" ");

		if (serverResponse.length != 5) {
			throw new IntegrityException("Invalid response from server.");
		}
		if (!serverResponse[0].equals("!ok")) {
			throw new IntegrityException("Invalid response from server.");
		}
		if (!serverResponse[1].equals(clientChallenge)) {
			throw new IntegrityException(
					"Invalid response from server (client challenge wrong).");
		}

		String serverChallenge = serverResponse[2];
		byte[] secretKey = Base64.decode(serverResponse[3].getBytes());
		byte[] ivParameter = Base64.decode(serverResponse[4].getBytes());

		AESEncryptedChannel aesEncryptedChannel = new AESEncryptedChannel(
				channel.getPlainChannel(), new SecretKeySpec(secretKey, "AES"),
				ivParameter);

		aesEncryptedChannel.send(serverChallenge);

		return aesEncryptedChannel;
	}
}