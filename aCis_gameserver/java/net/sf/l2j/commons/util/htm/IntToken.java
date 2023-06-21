package net.sf.l2j.commons.util.htm;

public class IntToken
{
	private Integer _value;
	private String _error; 
	
	public void addError(String error)
	{
		_error = error;
	}
	
	public void setValue(Integer value)
	{
		_value = value;
	}
	
	public boolean isValid()
	{
		return _error == null || _error.isEmpty();
	}
	
	public Integer getValue()
	{
		return _value;
	}
}