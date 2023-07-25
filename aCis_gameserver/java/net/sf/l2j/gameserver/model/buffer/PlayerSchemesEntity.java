package net.sf.l2j.gameserver.model.buffer;

import java.util.HashMap;
import java.util.Map;

public final class PlayerSchemesEntity
{
	private boolean _hasChanges;
	private Map<Integer, SchemeEntity> _schemes = new HashMap<>();
	
	public Map<Integer, SchemeEntity> getSchemes()
	{
		return _schemes;
	}
	
	public void setHasChanges(boolean hasChanges)
	{
		_hasChanges = hasChanges;
	}
	
	public boolean hasChanges()
	{
		return _hasChanges;
	}
}