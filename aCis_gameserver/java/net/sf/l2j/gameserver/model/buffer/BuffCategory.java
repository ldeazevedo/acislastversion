package net.sf.l2j.gameserver.model.buffer;

public enum BuffCategory 
{
	All(0,"All"),
	Buffs(1,"Buffs"),
	Dances(2,"Dances"),
	Songs(3,"Songs"),
	Chants(4,"Chants"),
	Other(5,"Other");
	
	BuffCategory(int id, String name)	
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
    
    public static BuffCategory GetById(int id) {
		  for(BuffCategory e: BuffCategory.values()) {
		    if(e.GetId()==id) {
		      return e;
		    }
		  }
		 return null;// not found
	}
    
    public static BuffCategory GetByName(String name) {
		  for(BuffCategory e: BuffCategory.values()) {
		    if(e.GetName().equals(name)) {
		      return e;
		    }
		  }
		 return null;// not found
	}
}