package net.sf.l2j.gameserver.model.buffer;

public enum RestoreType
{
	HP(0,"Hp"),
	MP(1,"Mp"),
	CP(2,"Cp"),
	ALL(3,"All");
	
	RestoreType(int id, String name)	
	{
		_id = id;
		_name = name;
	}
	
	public String GetName()
	{
		return _name;
	}
	
	public int GetId()
	{
		return _id;
	}
	
    private int _id;
    private String _name;
    
    public static RestoreType GetById(int id)
    {
    	for(RestoreType e: RestoreType.values())
    	{
    		if(e.GetId() == id)
    		{
    			return e;
		    }
    	}
		return null;// not found
	}
    
    public static RestoreType GetByName(String name)
    {
    	for(RestoreType e: RestoreType.values())
    	{
    		if(e.GetName().equalsIgnoreCase(name))
    		{
    			return e;
    		}
    	}
		return null;// not found
	}
}