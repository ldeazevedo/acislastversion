package net.sf.l2j.commons.util;


public enum SortType {
    NONE(0),
    ASC(1),
    DESC(2);
    
    private final int id;
    SortType(int id) { this.id = id; }
    public int getValue() { return id; }
    
	public int GetId()
	{
		return id;
	}
	
	public static SortType GetById(int id) {
		  for(SortType e: SortType.values()) {
		    if(e.GetId()==id) {
		      return e;
		    }
		  }
		 return null;// not found
	}
	
	public SortType switchSorting()
	{
		if (this==SortType.NONE)
		{
			return SortType.ASC;
		}
		else if(this==SortType.ASC)
		{
			return SortType.DESC;
		}
		else
		{
			return SortType.NONE;
		}
	}
}