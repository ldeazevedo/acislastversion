package net.sf.l2j.gameserver.communitybbs.manager;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.communitybbs.module.CastleStatus;
import net.sf.l2j.gameserver.communitybbs.module.StatusEpicBoss;
import net.sf.l2j.gameserver.communitybbs.module.TopPvpPk;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.model.actor.Player;

public class TopBBSManager extends BaseBBSManager
{
	protected TopBBSManager()
	{
	}
	
	@Override
	public void parseCmd(String command, Player player)
	{
		if (command.equals("_bbshome"))
			loadStaticHtm("index.htm", player);
		// CastleStatus, StatusEpicBoss, TopPvpPk
		else if(command.startsWith("_bbshome;"))
		{
			CastleStatus statuscastle = new CastleStatus();
			StatusEpicBoss epicboss = new StatusEpicBoss();
			TopPvpPk pvppk = new TopPvpPk();
			
			StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			int idp = Integer.parseInt(st.nextToken());
			String content = HtmCache.getInstance().getHtm("data/html/CommunityBoard/"+idp+".htm");
			if (content == null)
			{
				content = "<html><body><br><br><center>404 :File Not foud: 'data/html/CommunityBoard/"+idp+".htm' </center></body></html>";
			}
			
			content = content.replaceAll("%CastleStatus%", statuscastle.loadCastleList());
			content = content.replaceAll("%StatusEpicBoss%", epicboss.loadStatusEpicBossList());
			content = content.replaceAll("%TopPvpPk%", pvppk.loadTopPvpPk());
			
			separateAndSend(content, player);
		}
		else if (command.startsWith("_bbshome;"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			
			loadStaticHtm(st.nextToken(), player);
		}
		else
			super.parseCmd(command, player);
	}
	
	@Override
	protected String getFolder()
	{
		return "top/";
	}
	
	public static TopBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TopBBSManager INSTANCE = new TopBBSManager();
	}
}