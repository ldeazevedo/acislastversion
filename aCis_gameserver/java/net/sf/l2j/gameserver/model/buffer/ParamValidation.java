package net.sf.l2j.gameserver.model.buffer;


public class ParamValidation
{
	private String _errorMessage;
	private String _infoMessage;
	
	//those variables are used when validating text length
	private boolean _isTextTooLong = false;
	private int _maxTextLength = 0;
	
	public ParamValidation()
	{
		_errorMessage = null;
	}
	
	public void setIsTextTooLong(int maxLength)
	{
		_isTextTooLong = true;
		_maxTextLength = maxLength;
	}
	
	public int getMaxTextLength()
	{
		return _maxTextLength;
	}
	
	public boolean getIsTextTooLong()
	{
		return _isTextTooLong;
	}
	
	public void setError(String error)
	{
		_errorMessage = error;
	}
	
	public void setInfo(String info)
	{
		_infoMessage = info;
	}
	
	public boolean isValid()
	{
		return _errorMessage == null;
	}
	
	public boolean hasError()
	{
		return _errorMessage != null;
	}
	
	public boolean hasInfo()
	{
		return _infoMessage != null;
	}
	
	public String getInfo()
	{
		return _infoMessage;
	}
	
	public String getError()
	{
		return _errorMessage;
	}
	
	public boolean hasMesage()
	{
		return _errorMessage != null || _infoMessage != null;
	}
}