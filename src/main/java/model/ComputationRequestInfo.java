package model;

import java.io.Serializable;

/**
 * Represents a Data Transfer Object (DTO), contains all the relevant information of a log file.
 */
public class ComputationRequestInfo implements Serializable {

	private static final long serialVersionUID = 8097915840693170234L;
	private String timestamp;
	private String stringRepresentation;

	public ComputationRequestInfo(String filename, String nodeComponentName, String request, String result){
		this.timestamp = filename.substring(0,19);
		this.stringRepresentation = timestamp + " ["+nodeComponentName+"]: "+request+" = "+result;
	}
	
	public ComputationRequestInfo(String message){
		stringRepresentation = message;
	}

	public String getTimestamp(){
		return timestamp;
	}

	@Override
	public String toString(){
		return stringRepresentation;
	}
}