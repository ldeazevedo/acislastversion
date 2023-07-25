package net.sf.l2j.gameserver.session;

import java.util.HashMap;


public class HtmSession
{
	public enum CachePage
	{
		EVENT_MASTER,
		ADD_INSTANT_EVENT,
		INSTANT_EVENTS,
		PLAYERS_COMMUNITY,
		FORUM,
		SCHEME_BUFFER,
		SEARCH_BOARD,
		AUCTION_HOUSE,
		EMAIL,
		BUFFER_NPC
	}
	
	private HashMap<CachePage, ParamsCache> _pageCache;
	
	public HtmSession()
	{
		_pageCache = new HashMap<>();
	}
	
	public ParamsCache get(CachePage page)
	{
		if (!_pageCache.containsKey(page)) 
		{
			_pageCache.put(page, new ParamsCache());
		}
		
		return _pageCache.get(page);
	}
}
