package channel;

import java.io.IOException;

public class IntegrityException extends IOException {

	private static final long serialVersionUID = -2599400546553186034L;

	public IntegrityException() {
		super();
	}

	public IntegrityException(String message) {
		super(message);
	}

	public IntegrityException(Throwable cause) {
		super(cause);
	}

	public IntegrityException(String message, Throwable cause) {
		super(message, cause);
	}

}
