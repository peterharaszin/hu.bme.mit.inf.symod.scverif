package hu.bme.mit.inf.symod.scverif.processing.sct;

import java.io.FileNotFoundException;

/**
 * Exception to throw when there was no SCT file in the project
 * 
 * @author Peter Haraszin
 *
 */
public class YakinduSCTFileNotFoundException extends FileNotFoundException {
	public YakinduSCTFileNotFoundException() {
		
	}

	public YakinduSCTFileNotFoundException(String message) {
		super(message);
	}
	
	private static final long serialVersionUID = 1L;
}
