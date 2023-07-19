package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.commons.logging.CLogger;

import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.events.EventManager;
import net.sf.l2j.gameserver.model.events.TvTEvent;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;

public class AdminTimeus implements IAdminCommandHandler
{
	final CLogger LOG = new CLogger(AdminTimeus.class.getName());
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_rf",
		"admin_clanchat",
		"admin_survival",
		"admin_add_player",
		"admin_remove_player",
		"admin_clear_players",
		"admin_clear",
		"admin_frintezza",
		"admin_loc"
	};
	
	@Override
	public void useAdminCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command);
		command = st.nextToken();

		if (command.equals("admin_loc"))
			LOG.info(activeChar.getPosition());
		if (command.equals("admin_add_player"))
			addTargetPlayer(true, activeChar);
		if (command.equals("admin_remove_player"))
			addTargetPlayer(false, activeChar);
		if (command.startsWith("admin_add_player"))
		{
			if (st.countTokens() > 1)
			{
				st.nextToken();
				String player = st.nextToken();
				Player plyr = World.getInstance().getPlayer(player);
				if (plyr != null)
					register(true, plyr, activeChar);
			}
		}
		if (command.startsWith("admin_remove_player"))
		{
			if (st.countTokens() > 1)
			{
				st.nextToken();
				Player player = World.getInstance().getPlayer(st.nextToken());
				if (player != null)
					register(false, player, activeChar);
			}
		}
		else if (command.startsWith("admin_clear_players"))
			EventManager.getInstance().getPlayers().clear();
		else if (command.startsWith("admin_clear"))
			EventManager.getInstance().clear();
		else if (command.equals("admin_rf"))
			ScriptData.getInstance().getQuest("EventsEngineTask").startQuestTimer("doItJustOnceRF", null, null, 1000);
			//ScriptData.getInstance().getQuest("EventsTask").startQuestTimer("doItJustOnceRF", null, null, 1000);
			//EventManager.getInstance().doItJustOnceRF();
		else if (command.equals("admin_survival"))
			ScriptData.getInstance().getQuest("EventsTask").startQuestTimer("doItJustOnceSurvivalbeginning", null, null, 1000);
			//EventManager.getInstance().doItJustOnceSurvival();
		else if (command.equals("admin_clanchat"))
		{
			try
			{
				final String clanName = st.nextToken();
				StringBuilder message = new StringBuilder();
				while (st.hasMoreTokens())
					message.append(st.nextToken()).append(" ");
				
				for (Clan clan : ClanTable.getInstance().getClans())
					if (clan.getName().equalsIgnoreCase(clanName))
					{
						activeChar.sendMessage("[" + clan.getName() + "]->" + message);
						clan.broadcastToMembers(new ExShowScreenMessage(message.toString(), 3500, SMPOS.MIDDLE_RIGHT, false));
						break;
					}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				activeChar.sendMessage("Usage: //clanchat <clanname> [text]");
			}
		}
	}
	
	private static void addTargetPlayer(boolean register, Player activeChar)
	{
		WorldObject target = activeChar.getTarget();
		if (target instanceof Player)
		{
			Player player = (Player) target;
			if (player != activeChar)
				register(register, player, activeChar);
		}
	}
	
	private static void register(boolean register, Player player, Player activeChar)
	{
		if (EventManager.getInstance().isInProgress() || player.isInOlympiadMode() || player.isFestivalParticipant() || /*player.isInSiege() || */player.isInJail() || player.isFestivalParticipant() || player.isDead() || player.getKarma() > 0 || player.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(player.getObjectId()))
			return;
		
		if (OlympiadManager.getInstance().isRegistered(player))
		{
			activeChar.sendMessage("No puede participar ni ver el evento mientras esta registrado en oly.");
			return;
		}
		if (register)
		{
			if (player.isInObserverMode())
			{
				activeChar.sendMessage("No puedes anotar al player si esta mirando el evento.");
				return;
			}
			if (EventManager.getInstance().containsPlayer(player))
			{
				activeChar.sendMessage("Ya esta registrado en el evento.");
				return;
			}
			EventManager.getInstance().addPlayer(player);
			activeChar.sendMessage("Player registrado al evento.");
		}
		else
		{
			if (!EventManager.getInstance().containsPlayer(player))
			{
				activeChar.sendMessage("No esta registrado en el evento.");
				return;
			}
			EventManager.getInstance().removePlayer(player);
			activeChar.sendMessage("Player removido del evento.");
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}