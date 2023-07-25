package net.sf.l2j.gameserver.custom.fakeplayer;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.Collectors;

public class FakePlayerManager
{
	
	private static final CLogger LOGGER = new CLogger(GameServer.class.getName());
	private static final String SELECT_TOP_LEVEL = "SELECT MAX(level) from characters";
	private static FakePlayerManager instance;
	private boolean already10 = false;
	private boolean already25 = false;
	private boolean already35 = false;
	private boolean already45 = false;
	private boolean already60 = false;
	
	public static FakePlayerManager getInstance()
	{
		if (instance == null)
			instance = new FakePlayerManager();
		return instance;
	}
	
	public void init()
	{
		if (Config.FAKE_PLAYER_ENABLED)
		{
			LOGGER.info("Initializing FakePlayerManger...");
			
			ThreadPool.scheduleAtFixedRate(() ->
			{
				try (Connection conn = ConnectionPool.getConnection();
					PreparedStatement statement = conn.prepareStatement(SELECT_TOP_LEVEL);
					ResultSet rs = statement.executeQuery())
				{
					int maxLevel = 1;
					if (rs.next())
						maxLevel = rs.getInt(1);
					addDwarfs(maxLevel);
				}
				catch (Exception ex)
				{
					LOGGER.error("Error when getting max level from DB", ex);
				}
			}, 5000, 1000 * 60 * 60);
			LOGGER.info("Done");
		}
	}
	
	@SuppressWarnings("unused")
	private int getFakePlayersCount()
	{
		return getFakePlayers().size();
	}
	
	@SuppressWarnings("static-method")
	private List<FakePlayer> getFakePlayers()
	{
		return World.getInstance().getPlayers().stream().filter(x -> x instanceof FakePlayer).map(x -> (FakePlayer) x).collect(Collectors.toList());
	}
	
	private void addDwarfs(int maxLevel)
	{
		if (maxLevel >= 60 && !already60)
		{
			FakeHelper.createFakePlayers(1, FakeHelper.City.GIRAN, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.GIRAN, FakeHelper.FakePlayerType.BUYER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.OREN, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.OREN, FakeHelper.FakePlayerType.BUYER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.HUNTER, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.HUNTER, FakeHelper.FakePlayerType.BUYER);
			already60 = true;
		}
		if (maxLevel >= 45 && !already45)
		{
			FakeHelper.createFakePlayers(1, FakeHelper.City.GIRAN, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.GIRAN, FakeHelper.FakePlayerType.BUYER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.GLUDIO, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.GLUDIO, FakeHelper.FakePlayerType.BUYER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.DION, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.DION, FakeHelper.FakePlayerType.BUYER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.OREN, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.OREN, FakeHelper.FakePlayerType.BUYER);
			already45 = true;
		}
		if (maxLevel >= 35 && !already35)
		{
			FakeHelper.createFakePlayers(1, FakeHelper.City.GLUDIN, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.GLUDIN, FakeHelper.FakePlayerType.BUYER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.GIRAN, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.GIRAN, FakeHelper.FakePlayerType.BUYER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.GLUDIO, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.GLUDIO, FakeHelper.FakePlayerType.BUYER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.DION, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.DION, FakeHelper.FakePlayerType.BUYER);
			already35 = true;
		}
		if (maxLevel >= 25 && !already25)
		{
			FakeHelper.createFakePlayers(1, FakeHelper.City.GLUDIN, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.GLUDIO, FakeHelper.FakePlayerType.SELLER);
			FakeHelper.createFakePlayers(1, FakeHelper.City.DION, FakeHelper.FakePlayerType.SELLER);
			already25 = true;
		}
		if (maxLevel > 20 && !already10)
		{
			FakeHelper.createFakePlayers(5, FakeHelper.City.GLUDIN, FakeHelper.FakePlayerType.SELLER);
			already10 = true;
		}
		else
			FakeHelper.createFakePlayers(5, FakeHelper.City.GLUDIN, FakeHelper.FakePlayerType.SELLER);
	}
}
