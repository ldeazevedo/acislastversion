package net.sf.l2j.gameserver.session;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.gameserver.model.actor.Player;

public class SessionManager
{
	private Map<Integer, HtmSession> _playersSessions;
	
	protected SessionManager()
	{
		_playersSessions =  new HashMap<>();
	}
	
	public HtmSession get(int playerId)
	{
		if (!_playersSessions.containsKey(playerId) || _playersSessions.get(playerId) == null)
		{
			_playersSessions.put(playerId, new HtmSession());
		}
		return _playersSessions.get(playerId);
	}
	
	public HtmSession get(Player player)
	{
		return get(player.getObjectId());
	}
	
	
	public static SessionManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final SessionManager _instance = new SessionManager();
	}
}