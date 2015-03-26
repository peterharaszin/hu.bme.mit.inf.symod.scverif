package hu.bme.mit.remo.scverif.ui;

public class BuildError extends Exception {

	public BuildError() {
		super();
	}

	public BuildError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public BuildError(String message, Throwable cause) {
		super(message, cause);
	}

	public BuildError(String message) {
		super(message);
	}

	public BuildError(Throwable cause) {
		super(cause);
	}

	private static final long serialVersionUID = 1L;

}
