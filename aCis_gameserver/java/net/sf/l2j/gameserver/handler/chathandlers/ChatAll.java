package net.sf.l2j.gameserver.handler.chathandlers;

import java.util.StringTokenizer;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.FloodProtector;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.events.EventManager;
import net.sf.l2j.gameserver.model.events.TvTEvent;
import net.sf.l2j.gameserver.model.events.TvTEventTeleporter;
import net.sf.l2j.gameserver.model.events.TvTManager;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

public class ChatAll implements IChatHandler
{
	private static final SayType[] COMMAND_IDS =
	{
		SayType.ALL
	};
	
	@Override
	public void handleChat(SayType type, Player player, String target, String text)
	{
		if (!player.getClient().performAction(FloodProtector.GLOBAL_CHAT))
			return;
		
		if (!TvTEvent.isInactive())
		{
			if (text.equalsIgnoreCase(".register"))
			{
				TvTEvent.onBypass("tvt_event_participation", player);
				return;
			}
		}
		if (text.equalsIgnoreCase(".register") || text.equalsIgnoreCase(".unregister") || text.equalsIgnoreCase(".ver") || text.equalsIgnoreCase(".salir"))
		{
			EventManager.getInstance().checkEvents(text, player);
			return;
		}
			
		if (player.isGM())
		{
			if (text.startsWith(".setinstance"))
			{
				final StringTokenizer st = new StringTokenizer(text, " ");
				st.nextToken();
		
				if (!st.hasMoreTokens())
				{
					player.sendMessage("Usage: .setinstance <id>");
					return;
				}
			
				final WorldObject targetWorldObject = player.getTarget();

				final String param = st.nextToken();
				if (StringUtil.isDigit(param))
				{
					final int id = Integer.parseInt(param);
						//	if (!(targetWorldObject instanceof Player))
						//		return;
					if (targetWorldObject != null)
					{
						targetWorldObject.setInstanceId(id);
						player.sendMessage("You successfully set in Instance " + id);
					}
				}
				return;
			}
		/*	Player target = player.getTarget() instanceof Player ? (Player) player.getTarget() : null;
			if (text.equals(".pc"))
			{
				if (target != null)
					player.sendMessage(target.getName() + " tiene [" + target.getPcBangScore() + "] PC BANG SCORE");
				return;
			}
			else */if (text.equals(".tvt_add"))
			{
				if (!(player.getTarget() instanceof Player))
				{
					player.sendMessage("You should select a player!");
					return;
				}
				
				add(player, player.getTarget().getActingPlayer());
			}
			else if (text.equals(".tvt_start"))
			{
				TvTManager.getInstance().startTvT();
				return;
			}
			else if (text.equals(".tvt_remove"))
			{
				if (!(player.getTarget() instanceof Player))
				{
					player.sendMessage("You should select a player!");
					return;
				}
				
				remove(player, player.getTarget().getActingPlayer());
			}
			if (text.equalsIgnoreCase(".tvt_advance"))
			{
				TvTManager.getInstance().skipDelay();
				return;
			}
			if (text.equalsIgnoreCase(".frintezza"))
			{
				ScriptData.getInstance().getQuest("Frintezza").startQuestTimer("start", null, null, 1000);
				return;
			}
			if (text.startsWith(".read"))
			{
				player.setReadChat(!player.getReadChat() ? true : false);
				player.sendMessage("Read chats "+ (!player.getReadChat() ? "off" : "on"));
				return;
			}
			
			if (text.startsWith(".clanchat"))
			{
				StringTokenizer st = new StringTokenizer(text);
				text = st.nextToken();
				try
				{
					final String clanName = st.nextToken();
					String message = "";
					while (st.hasMoreTokens())
						message += st.nextToken() + " ";
					Clan receiverClan = null;
					for (Clan clan : ClanTable.getInstance().getClans())
						if (clan.getName().equalsIgnoreCase(clanName))
						{
							receiverClan = clan;
							break;
						}
					if (receiverClan != null)
					{
						receiverClan.broadcastToMembers(new CreatureSay(player.getObjectId(), SayType.CLAN, player.getName(), message));
						player.sendPacket(new CreatureSay(player.getObjectId(), SayType.ALLIANCE, player.getName(), "[" + receiverClan.getName() + "]:" + message));
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					player.sendMessage("Usage: .clanchat <clanname> [text]");
				}
				return;
			}
			if (text.startsWith(".chat") || text.startsWith(".all") || text.startsWith(".clan"))
			{
				boolean global = text.startsWith(".all");
				boolean clan = text.startsWith(".clan");
				StringTokenizer st = new StringTokenizer(text);
				text = st.nextToken();
				try
				{
					final String charName = st.nextToken();
					String message = "";
					while (st.hasMoreTokens())
						message += st.nextToken() + " ";
					Player victima = null;
					victima = World.getInstance().getPlayer(charName);
					if (victima != null)
					{
						if (!clan)
						{
							final CreatureSay cs = new CreatureSay(victima.getObjectId(), !global ? type : SayType.TRADE, victima.getName(), message);
							if (global)
								for (Player p : World.getInstance().getPlayers())
									p.sendPacket(cs);
							else
								for (Player p : victima.getKnownTypeInRadius(Player.class, 1250))
								{
									if (p != null)
									{
										p.sendPacket(cs);
										victima.sendPacket(cs);
									}
								}
						}
						else if (victima.getClan() != null)
						{
							AdminData.getInstance().broadcastToGMs(new CreatureSay(victima.getObjectId(), SayType.ALLIANCE, victima.getName(), "[" + victima.getClan().getName() + "]:" + message));
							victima.getClan().broadcastToMembers(new CreatureSay(victima.getObjectId(), SayType.CLAN, victima.getName(), message));
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					player.sendMessage("Usage: .clanchat <clanname> [text]");
				}
				return;
			}
			
		}
		if (!TvTEvent.isInactive())
		{
			if (text.equalsIgnoreCase(".tvt"))
			{
			/*	ScriptData.getInstance().getQuest("EventsTask").startQuestTimer("tvt_event_participation", null, player, 1000);
				if (!TvTEvent.isPlayerParticipant(player.getObjectId()))
					TvTEvent.onBypass("tvt_event_participation", player);
				else */TvTEvent.addParticipant(player);
				return;
			}
		}
		if (text.equalsIgnoreCase(".expoff"))
		{
		//	player.invertExpOff();
		//	return;
		}
		
		
		final CreatureSay cs = new CreatureSay(player, type, text);
		for (Player knownPlayer : player.getKnownTypeInRadius(Player.class, 1250))
			knownPlayer.sendPacket(cs);
		
		player.sendPacket(cs);
	}
	
	private static void add(Player activeChar, Player playerInstance)
	{
		if (TvTEvent.isPlayerParticipant(playerInstance.getObjectId()))
		{
			activeChar.sendMessage("Player already participated in the event!");
			return;
		}
		
		if (!TvTEvent.addParticipant(playerInstance))
		{
			activeChar.sendMessage("Player instance could not be added, it seems to be null!");
			return;
		}
		
		if (TvTEvent.isStarted())
		{
			new TvTEventTeleporter(playerInstance, TvTEvent.getParticipantTeamCoordinates(playerInstance.getObjectId()), true, false);
		}
	}
	
	private static void remove(Player activeChar, Player playerInstance)
	{
		if (!TvTEvent.removeParticipant(playerInstance.getObjectId()))
		{
			activeChar.sendMessage("Player is not part of the event!");
			return;
		}
		
		new TvTEventTeleporter(playerInstance, Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
	}
	
	@Override
	public SayType[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}