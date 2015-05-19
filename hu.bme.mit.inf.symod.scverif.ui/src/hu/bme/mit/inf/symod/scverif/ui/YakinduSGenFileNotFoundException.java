package hu.bme.mit.inf.symod.scverif.ui;

import java.io.FileNotFoundException;

/**
 * Exception to throw when there was no Yakindu .sgen file in the project
 * 
 * @author Peter Haraszin
 *
 */
public class YakinduSGenFileNotFoundException extends FileNotFoundException {
	public YakinduSGenFileNotFoundException() {
	
	}
	
	public YakinduSGenFileNotFoundException(String message) {
		super(message);
	}

	private static final long serialVersionUID = 1L;
}
