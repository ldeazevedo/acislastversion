/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.model.events;

import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.sql.ClanTable;
import net.sf.l2j.gameserver.data.xml.AdminData;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.GMHennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.GMViewItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class EventHandlers
{
	protected static final Logger _log = Logger.getLogger(EventHandlers.class.getName());
	
	private static Player pShift;
	
	public static boolean check(String text, Player player)
	{
	//	final Player target = player.getTarget().getActingPlayer();
		if (text.equalsIgnoreCase(".reg1") || text.equalsIgnoreCase(".unreg1") || text.equalsIgnoreCase(".ver1") || text.equalsIgnoreCase(".salir1"))
		{
			EventEngine.getInstance().checkEvents(text, player);
			if (TeamsPlayers.containsPlayer(player))
			{
				player.sendMessage("Ya estas registrado en el evento.");
				return true;
			}
			TeamsPlayers.addPlayer(player);
			player.sendMessage("Te registraste al evento!");
            return true;
		}
		if (!TvTEvent.isInactive())
		{
			if (text.equalsIgnoreCase(".register"))
			{
				TvTEvent.onBypass("tvt_event_participation", player);
				return true;
			}
			else if (text.equalsIgnoreCase(".tvt"))
			{
				TvTEvent.addParticipant(player);
				return true;
			}
		}
		if (text.equalsIgnoreCase(".hair"))
		{
			player.setSwitchHair(player.getHair() ? false : true);
			return true;
		}
		if (text.equalsIgnoreCase(".register") || text.equalsIgnoreCase(".unregister") || text.equalsIgnoreCase(".ver") || text.equalsIgnoreCase(".salir"))
		{
		//	EventManager.getInstance().checkEvents(text, player);
            return true;
		}
		
        if(text.equalsIgnoreCase(".expoff"))
        {
        	player.invertExpOff();
            return true;
        }
		if (text.equalsIgnoreCase(".expoff"))
		{
			player.invertExpOff();
			return true;
		}
		
		if (player.isGM())
		{
			if (text.equalsIgnoreCase(".stopvita"))
			{
				player.stopAbnormalEffect(AbnormalEffect.VITALITY);
				return true;
			}
			if (text.startsWith(".setinstance"))
			{
				final StringTokenizer st = new StringTokenizer(text, " ");
				st.nextToken();
		
				if (!st.hasMoreTokens())
				{
					player.sendMessage("Usage: .setinstance <id>");
					return true;
				}
			
				final WorldObject targetWorldObject = player.getTarget();

				final String param = st.nextToken();
				if (StringUtil.isDigit(param))
				{
					final int id = Integer.parseInt(param);
					if (targetWorldObject != null)
					{
						targetWorldObject.setInstanceId(id);
						targetWorldObject.decayMe();
						targetWorldObject.spawnMe();
						player.sendMessage("You successfully set in Instance " + id);
					}
				}
				return true;
			}
		/*	Player target = player.getTarget() instanceof Player ? (Player) player.getTarget() : null;
			if (text.equals(".pc"))
			{
				if (target != null)
					player.sendMessage(target.getName() + " tiene [" + target.getPcBangScore() + "] PC BANG SCORE");
				return;
			}
			else */
			if (text.equals(".tvt_add"))
			{
				if (!(player.getTarget() instanceof Player))
				{
					player.sendMessage("You should select a player!");
					return true;
				}
				
				add(player, player.getTarget().getActingPlayer());
			}
			else if (text.equals(".tvt_start"))
			{
				TvTManager.getInstance().startTvT();
				return true;
			}
			else if (text.equals(".tvt_remove"))
			{
				if (!(player.getTarget() instanceof Player))
				{
					player.sendMessage("You should select a player!");
					return true;
				}
				
				remove(player, player.getTarget().getActingPlayer());
			}
			if (text.equalsIgnoreCase(".tvt_advance"))
			{
				TvTManager.getInstance().skipDelay();
				return true;
			}
			if (text.equalsIgnoreCase(".frintezza"))
			{
				ScriptData.getInstance().getQuest("Frintezza").startQuestTimer("start", null, null, 1000);
				return true;
			}
			if (text.equalsIgnoreCase(".read"))
			{
				player.setReadChat(!player.getReadChat() ? true : false);
				player.sendMessage("Read chats "+ (!player.getReadChat() ? "off" : "on"));
				return true;
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
				return true;
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
							final CreatureSay cs = new CreatureSay(victima.getObjectId(), !global ? SayType.ALL : SayType.TRADE, victima.getName(), message);
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
				return true;
			}
		}
		return false;
	}
	
	public void bypass(Player client, String command)
	{
		final Player player = client.getActingPlayer();
		if (player == null)
			return;
		NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		final Player shift = player.getShiftTarget();
		if (command.equalsIgnoreCase("shift_clan"))
		{
			html.setFile("data/html/mods/shift/clan.htm");
			player.sendPacket(html);
		}
		else if (command.equalsIgnoreCase("shift_stats"))
		{
			html.setFile("data/html/mods/shift/stats.htm");
			html.replace("%class%", shift.getClass().getSimpleName());
			html.replace("%name%", shift.getName());
			html.replace("%lvl%", shift.getStatus().getLevel());
			player.sendPacket(html);
		}
		else if (command.equalsIgnoreCase("shift_equipped"))
		{
			if (!player.isGM())
			{
				if (!shift.isGM())
					player.sendPacket(new GMViewItemList(shift, true));
				else
					player.sendMessage("You can't use it on GMs!");
			}
			else
				player.sendPacket(new GMViewItemList(shift));
			player.sendPacket(new GMHennaInfo(shift));
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		else if (command.equalsIgnoreCase("home"))
		{
			html.setFile("data/html/mods/shift/initial.htm");
			player.sendPacket(html);
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public static void showHtml(Player player, Player shift)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(player.getObjectId());
		html.setFile("data/html/mods/shift/initial.htm");
		pShift = shift;
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

	private static void add(Player player, Player playerInstance)
	{
		if (TvTEvent.isPlayerParticipant(playerInstance.getObjectId()))
		{
			player.sendMessage("Player already participated in the event!");
			return;
		}
		
		if (!TvTEvent.addParticipant(playerInstance))
		{
			player.sendMessage("Player instance could not be added, it seems to be null!");
			return;
		}
		
		if (TvTEvent.isStarted())
			new TvTEventTeleporter(playerInstance, TvTEvent.getParticipantTeamCoordinates(playerInstance.getObjectId()), true, false);
	}
	
	private static void remove(Player player, Player playerInstance)
	{
		if (!TvTEvent.removeParticipant(playerInstance.getObjectId()))
		{
			player.sendMessage("Player is not part of the event!");
			return;
		}
		new TvTEventTeleporter(playerInstance, Config.TVT_EVENT_PARTICIPATION_NPC_COORDINATES, true, true);
	}
	
    public static Player getShiftTarget()
    {
    	return pShift;
    }
    
	public static final EventHandlers getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EventHandlers INSTANCE = new EventHandlers();
	}
}
