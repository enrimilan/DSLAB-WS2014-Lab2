package channel;

import java.security.SecureRandom;

public abstract class Challenge {

	protected final byte[] getRandom(int length) {
		SecureRandom random = new SecureRandom();
		final byte[] number = new byte[length];
		random.nextBytes(number);
		
		return number;
	}
	
}
