package net.sf.l2j.commons.util.htm;

public class StringToken
{
	private String _value;
	private String _error; 
	
	public void addError(String error)
	{
		_error = error;
	}
	
	public void setValue(String value)
	{
		_value = value;
	}
	
	public boolean isValid()
	{
		return _error == null || _error.isEmpty();
	}
	
	public String getValue()
	{
		return _value;
	}
}