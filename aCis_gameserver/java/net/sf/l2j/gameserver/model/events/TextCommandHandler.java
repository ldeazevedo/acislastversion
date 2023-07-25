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
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminEditChar;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.pledge.Clan;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.GMHennaInfo;
import net.sf.l2j.gameserver.network.serverpackets.GMViewItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class TextCommandHandler
{
	protected static final Logger log = Logger.getLogger(TextCommandHandler.class.getName());
	
	private static Player pShift;
	
	public static boolean process(String text, Player player)
	{
	//	final Player target = player.getTarget().getActingPlayer();
		switch (text.toLowerCase()){
			case EventConstants.REGISTER:
			case EventConstants.UNREGISTER:
			case EventConstants.WATCH:
			case EventConstants.EXIT:
				log.info("Event command is being processed");
				RandomFightEngine.getInstance().processCommand(text, player);
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
			player.setSwitchHair(!player.getHair());
			return true;
		}
		if (text.equalsIgnoreCase(".register") || text.equalsIgnoreCase(".unregister") || text.equalsIgnoreCase(".ver") || text.equalsIgnoreCase(".salir"))
		{
		//	EventManager.getInstance().checkEvents(text, player);
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
				player.setEffectVita();
				player.stopAbnormalEffect(AbnormalEffect.VITALITY);
				return true;
			}
			if (text.equalsIgnoreCase(".heading"))
			{
				player.sendMessage("GetHeading: "+player.getHeading());
				return true;
			}
			if (text.equalsIgnoreCase(".test"))
			{
				int[] midpoint = calculateMidpoint(player.getPosition().toString(), player.getTarget().getPosition().toString());

				player.sendMessage("Midpoint: (" + midpoint[0] + ", " + midpoint[1] + ", " + midpoint[2] + ")");
		        log.info("Midpoint: (" + midpoint[0] + ", " + midpoint[1] + ", " + midpoint[2] + ")");
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
			switch (text) {
				case ".tvt_add":
					if (!(player.getTarget() instanceof Player)) {
						player.sendMessage("You should select a player!");
						return true;
					}

					add(player, player.getTarget().getActingPlayer());
					break;
				case ".tvt_start":
					TvTManager.getInstance().startTvT();
					return true;
				case ".tvt_remove":
					if (!(player.getTarget() instanceof Player)) {
						player.sendMessage("You should select a player!");
						return true;
					}

					remove(player, player.getTarget().getActingPlayer());
					break;
				case ".tvt_advance":
					TvTManager.getInstance().skipDelay();
					return true;
				case ".frintezza":
					ScriptData.getInstance().getQuest("Frintezza").startQuestTimer("start", null, null, 1000);
					return true;
				case ".read":
					player.setReadChat(!player.getReadChat());
					player.sendMessage("Read chats " + (!player.getReadChat() ? "off" : "on"));
					return true;
			}

			if (text.startsWith(".clanchat"))
			{
				StringTokenizer st = new StringTokenizer(text);
				text = st.nextToken();
				try
				{
					final String clanName = st.nextToken();
					StringBuilder message = new StringBuilder();
					while (st.hasMoreTokens())
						message.append(st.nextToken()).append(" ");
					Clan receiverClan = null;
					for (Clan clan : ClanTable.getInstance().getClans())
						if (clan.getName().equalsIgnoreCase(clanName))
						{
							receiverClan = clan;
							break;
						}
					if (receiverClan != null)
					{
						receiverClan.broadcastToMembers(new CreatureSay(player.getObjectId(), SayType.CLAN, player.getName(), message.toString()));
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
					StringBuilder message = new StringBuilder();
					while (st.hasMoreTokens())
						message.append(st.nextToken()).append(" ");
					Player victim = World.getInstance().getPlayer(charName);
					if (victim != null)
					{
						if (!clan)
						{
							final CreatureSay cs = new CreatureSay(victim.getObjectId(), !global ? SayType.ALL : SayType.TRADE, victim.getName(), message.toString());
							if (global)
								for (Player p : World.getInstance().getPlayers())
									p.sendPacket(cs);
							else
								for (Player p : victim.getKnownTypeInRadius(Player.class, 1250))
								{
									if (p != null)
									{
										p.sendPacket(cs);
										victim.sendPacket(cs);
									}
								}
						}
						else if (victim.getClan() != null)
						{
							AdminData.getInstance().broadcastToGMs(new CreatureSay(victim.getObjectId(), SayType.ALLIANCE, victim.getName(), "[" + victim.getClan().getName() + "]:" + message));
							victim.getClan().broadcastToMembers(new CreatureSay(victim.getObjectId(), SayType.CLAN, victim.getName(), message.toString()));
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
		if (player.isGM())
		{
				AdminEditChar.gatherPlayerInfo(player, shift, html);
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
		}
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
    
	public static TextCommandHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TextCommandHandler INSTANCE = new TextCommandHandler();
	}
	
	public static class MidPointCalculator
	{
	    public static void main(String[] args)
	    {
	        String object1 = "1, 2, 3";
	        String object2 = "-1, 4, 5";

	        int[] midpoint = calculateMidpoint(object1, object2);

	        System.out.println("Midpoint: (" + midpoint[0] + ", " + midpoint[1] + ", " + midpoint[2] + ")");
	    }
	}
	
    public static int[] calculateMidpoint(String object1, String object2)
    {
        String[] coordinates1 = object1.split(", ");
        String[] coordinates2 = object2.split(", ");

        int x1 = Integer.parseInt(coordinates1[0]);
        int y1 = Integer.parseInt(coordinates1[1]);
        int z1 = Integer.parseInt(coordinates1[2]);

        int x2 = Integer.parseInt(coordinates2[0]);
        int y2 = Integer.parseInt(coordinates2[1]);
        int z2 = Integer.parseInt(coordinates2[2]);

        int midpointX = (x1 + x2) / 2;
        int midpointY = (y1 + y2) / 2;
        int midpointZ = (z1 + z2) / 2;

        return new int[] { midpointX, midpointY, midpointZ };
    }
}
