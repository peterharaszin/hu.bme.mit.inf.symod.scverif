package hu.bme.mit.remo.scverif.ui;

import java.io.FileNotFoundException;

public class YakinduSCTFileNotFoundException extends FileNotFoundException {
	public YakinduSCTFileNotFoundException() {
		
	}

	public YakinduSCTFileNotFoundException(String message) {
		super(message);
	}
	
	private static final long serialVersionUID = 1L;
}