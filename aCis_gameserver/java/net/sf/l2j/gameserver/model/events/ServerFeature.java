package net.sf.l2j.gameserver.model.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Logger;

import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;

public class ServerFeature
{
	protected static final Logger _log = Logger.getLogger(ServerFeature.class.getName());

	protected ServerFeature()
	{
	}
	public static void saveExp(Player player)
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(""))
		{
			ps.setInt(1, player.getObjectId());
			ps.setLong(2, player.getRestantVitalityExp());
			ps.executeUpdate();

		}
		catch (Exception e)
		{
			_log.warning("Error: " + e);
		}
	}

	public static void checkEnterWorld(Player player)
	{
		player.showPcBangWindow();
		if (!player.getInVitality()) //Temporal arreglo para verlo en funcionamiento
			player.setVitalityExp();
		else
			ThreadPool.schedule(new updateVitalityEffect(player), 15000);
	}

	public static long getRateVitalityRateXpSp(int expsp)
	{
		return expsp == 1 ? 2 : 2;
	}

	private static class updateVitalityEffect implements Runnable
	{
		Player player = null;
		updateVitalityEffect(Player p)
		{
			player = p;
		}
		
		@Override
		public void run()
		{
			player.updateVitalityEffect();
		}
	}

	/**
	 * @param attacker
	 * @param exp
	 * @param sp
	 * @param pcbandpoints 
	 */
	public static void onCalculateRewards(Player attacker, long exp, int sp, int pcbandpoints)
	{
		attacker.setReduceVitalityExp(exp);
		if (pcbandpoints > 0)
			attacker.updatePcBangScore(pcbandpoints);
	}

	public static void readChats(Player player, String text, L2GameServerPacket packet)
	{
		for (Player gm : AdminData.getInstance().getAllGms(true))
			if (gm.getReadChat())
				gm.sendPacket(new CreatureSay(player.getObjectId(), SayType.ALLIANCE, player.getName(), "[" + player.getClan().getName() + "]:" + text));
	}
}