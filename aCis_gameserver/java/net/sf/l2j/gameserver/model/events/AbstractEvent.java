package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.events.util.EventConstants;
import net.sf.l2j.gameserver.model.events.util.EventUtil;
import net.sf.l2j.gameserver.model.events.util.State;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractEvent implements IEvent
{
	protected final Logger log = Logger.getLogger(getClassName());
	protected final List<Player> registeredPlayers = new ArrayList<>();
	protected final Location defaultLocation = new Location(82698, 148638, -3473);
	protected State currentState = State.INACTIVE;
	private static Npc npc;

	protected boolean isInProgress()
	{
		return currentState != State.INACTIVE;
	}

	protected void getBuffs(Player killer)
	{
		killer.getSkill(1204, 2); // Wind Walk
		if (killer.isMageClass())
			killer.getSkill(1085, 3);// Acumen
		else
			killer.getSkill(1086, 2); // haste
	}

	public boolean isInEvent(Player player)
	{
		return currentState == State.FIGHT && registeredPlayers.contains(player);
	}

	public boolean areRequiredPlayersRegistered()
	{
		var requiredPlayers = !registeredPlayers.isEmpty() && registeredPlayers.size() >= 2;
		if (!requiredPlayers)
			announceNpc(EventConstants.INSUFFICIENT);
		return requiredPlayers;
	}

	protected void clean(String questName)
	{
		registeredPlayers.clear();
		currentState = State.INACTIVE;
		ScriptData.getInstance().getQuest(questName).startQuestTimer("cancelQuestTimers", null, null, 1000);
	}

	public static void announceNpc(String msg)
	{
		if (npc == null)
		{
			for (Player players : World.getInstance().getPlayers())
				if (players.isOnline())
					players.sendMessage(msg);
			return;
		}
		final CreatureSay cs = new CreatureSay(npc, SayType.SHOUT, msg);
		final int region = MapRegionData.getInstance().getMapRegion(npc.getX(), npc.getY());

		for (Player worldPlayer : World.getInstance().getPlayers())
		{
			if (region == MapRegionData.getInstance().getMapRegion(worldPlayer.getX(), worldPlayer.getY()))
				worldPlayer.sendPacket(cs);
		}
	}

	protected boolean validateCommand(String text, Player player)
	{
		if (!isInProgress())
		{
			log.info("The event is inactive");
			return false;
		}
		if (player.isInObserverMode() || player.isInOlympiadMode() || player.isFestivalParticipant() || player.isInJail() || player.isCursedWeaponEquipped() || player.getKarma() > 0 || net.sf.l2j.gameserver.model.events.tvt.TvTEvent.isInProgress() && net.sf.l2j.gameserver.model.events.tvt.TvTEvent.isPlayerParticipant(player.getObjectId()))
		{
			player.sendMessage("You do not meet the conditions to participate.");
			return false;
		}
		if (OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage("No puedes participar ni ver el evento mientras estas registrado en oly.");
			return false;
		}

		if (text.equalsIgnoreCase(EventConstants.EXIT))
		{
			if (!registeredPlayers.contains(player) || currentState != State.FIGHT)
				return false;
			if (player.isDead())
			{
				registeredPlayers.remove(player);
				EventUtil.revertPlayer(player);
			}
			return false;
		}
		return true;
	}

	protected void validateRegister(String text, Player player)
	{
		if (text.equalsIgnoreCase(EventConstants.REGISTER) && currentState == State.REGISTER)
		{
			if (player.isDead())
				return;
			if (player.isInObserverMode())
			{
				player.sendMessage("No te podes anotar si estas mirando el evento.");
				return;
			}
			if (registeredPlayers.contains(player))
			{
				player.sendMessage("Ya estas registrado en el evento.");
				return;
			}
			registeredPlayers.add(player);
			player.sendMessage("Te registraste al evento!");
			return;
		} else if (text.equalsIgnoreCase(EventConstants.UNREGISTER) && currentState == State.REGISTER)
		{
			if (!registeredPlayers.contains(player))
			{
				player.sendMessage("No te registraste al evento.");
				return;
			}
			registeredPlayers.remove(player);
			player.sendMessage("Saliste del evento.");
			return;
		}

		if (currentState != State.REGISTER)
			player.sendMessage("El evento ya comenzo.");
	}

	protected abstract String getClassName();
}