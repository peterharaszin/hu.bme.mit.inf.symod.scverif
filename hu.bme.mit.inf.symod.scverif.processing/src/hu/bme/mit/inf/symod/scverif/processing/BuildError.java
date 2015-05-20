package hu.bme.mit.inf.symod.scverif.processing;

/**
 * Exception to throw when there was any kind of build error on a project  
 * 
 * @author Peter Haraszin
 *
 */
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
