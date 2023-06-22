package net.sf.l2j.gameserver.model.entity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.data.manager.InstanceManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/** 
 * @author evill33t, GodKratos
 * 
 */
public class Instance
{
	private final static Logger _log = Logger.getLogger(Instance.class.getName());

	private static final boolean DEBUG = false;

	private int _id;
	private String _name;
	
	//private TIntHashSet _players = new TIntHashSet();
	private final List<Integer> _players = new ArrayList<>();//new CopyOnWriteArrayList<>();
	
	private /*Fast*/List<Npc> _npcs = new ArrayList<>();//new FastList<Npc>();
	private /*Fast*/List<Door> _doors = new ArrayList<>();//new FastList<Door>();
	private int[] _spawnLoc = new int[3];
	private boolean _allowSummon = true;
	private long _emptyDestroyTime = -1;
	private long _lastLeft = -1;
	private long _instanceEndTime = -1;
	private boolean _isPvPInstance = false;

	protected ScheduledFuture<?> _CheckTimeUpTask = null;

	public Instance(int id)
	{
		_id = id;
	}

	/**
	 *  Returns the ID of this instance.
	 * @return 
	 */
	public int getId()
	{
		return _id;
	}

	/**
	 *  Returns the name of this instance
	 * @return 
	 */
	public String getName()
	{
		return _name;
	}

	public void setName(String name)
	{
		_name = name;
	}
	
	/**
	 * Returns whether summon friend type skills are allowed for this instance
	 * @return 
	 */
	public boolean isSummonAllowed()
	{
		return _allowSummon;
	}
	
	/**
	 * Sets the status for the instance for summon friend type skills
	 * @param b 
	 */
	public void setAllowSummon(boolean b)
	{
		_allowSummon = b;
	}

	/*
	 * Returns true if entire instance is PvP zone
	 */
	public boolean isPvPInstance()
	{
		return _isPvPInstance;
	}
	
	/*
	 * Sets PvP zone status of the instance 
	 */
	public void setPvPInstance(boolean b)
	{
		_isPvPInstance = b;
	}
	
	/**
	 * Set the instance duration task
	 * @param duration in milliseconds
	 */
	public void setDuration(int duration)
	{
		if (_CheckTimeUpTask != null)
			_CheckTimeUpTask.cancel(true);

		_CheckTimeUpTask = ThreadPool.schedule(new CheckTimeUp(duration), 500);
		_instanceEndTime = System.currentTimeMillis() + duration + 500;
	}

	/**
	 * Set time before empty instance will be removed
	 * @param time in milliseconds
	 */
	public void setEmptyDestroyTime(long time)
	{
		_emptyDestroyTime = time;
	}

	/**
	 * Checks if the player exists within this instance
	 * @param objectId
	 * @return true if player exists in instance
	 */
	public boolean containsPlayer(int objectId)
	{
		return _players.contains(objectId);
	}

	/**
	 * Adds the specified player to the instance
	 * @param objectId Players object ID
	 */
	public void addPlayer(int objectId)
	{
		synchronized(_players)
		{
			_players.add(objectId);
		}
	}
	
	/**
	 * Removes the specified player from the instance list
	 * @param objectId Players object ID
	 */
	public void removePlayer(int objectId)
	{
		synchronized(_players)
		{
			_players.remove(objectId);
		}
		
		if (_players.isEmpty() && _emptyDestroyTime >= 0)
		{
			_lastLeft = System.currentTimeMillis();
			setDuration((int) (_instanceEndTime - System.currentTimeMillis() - 1000));
		}
	}

	/**
	 * Removes the player from the instance by setting InstanceId to 0 and teleporting to nearest town.
	 * @param objectId
	 */
	public void ejectPlayer(int objectId)
	{
		Player player = (Player) World.getInstance().getObject(objectId);
		if (player != null && player.getInstanceId() == this.getId())
		{
			player.setInstanceId(0);
			player.sendMessage("You were removed from the instance");
			if (getSpawnLoc()[0] != 0 && getSpawnLoc()[1] != 0 && getSpawnLoc()[2] != 0)
				player.teleportTo(getSpawnLoc()[0], getSpawnLoc()[1], getSpawnLoc()[2], 0);
			else
				player.teleportTo(TeleportType.TOWN);
		}
	}

	public void addNpc(Npc npc)
	{
		_npcs.add(npc);
	}

	public void removeNpc(Npc npc)
	{
		if (npc != null)
			_npcs.remove(npc.getNpcId());
	}
	
	/**
	 * Adds a door into the instance
	 * @param doorId - from doors.csv
	 * @param open - initial state of the door 
	 */
	public void addDoor(int doorId, boolean open)
	{
		for (Door door: _doors)
		{
			if (door.getDoorId() == doorId)
			{
				_log.warning("Door ID " + doorId + " already exists in instance " + this.getId());
				return;
			}
		}

		Door door = new Door(IdFactory.getInstance().getNextId(), DoorData.getInstance().getDoor(doorId).getTemplate()); //Door newdoor = new Door(IdFactory.getInstance().getNextId(), temp.getTemplate(), temp.getDoorId(), temp.getName(), temp.isUnlockable());
		door.setInstanceId(getId());
		door.getStatus().setMaxHpMp();
		door.getPosition().set(door.getX(), door.getY(), door.getZ());
		
		//newdoor.spawnMe(door.getX(), door.getY(), door.getZ());//.setRange(temp.getX(), temp.getY(), temp.getZ());
		try
		{
		//	newdoor.setRegion(MapRegionData.getInstance().getMapRegion(door.getX(), door.getY()));
		}
		catch (Exception e)
		{
			_log.severe("Error in door data, ID:" + door.getDoorId());
		}
		
	//	door.setXYZInvisible(door.getX(), door.getY(), door.getZ());
		door.spawnMe(door.getX(), door.getY(), door.getZ());
		if (open)
			door.openMe();
		else door.closeMe();

		_doors.add(door);
	}
	
	//public TIntHashSet getPlayers()
	public List<Integer> getPlayers()
	{
		return _players;
	}

	public List<Npc> getNpcs()
	{
		return _npcs;
	}

	public List<Door> getDoors()
	{
		return _doors;
	}
	
	public Door getDoor(int id)
	{
		for (Door temp: getDoors())
		{
			if (temp.getDoorId() == id)
				return temp;
		}
		return null;
	}
	
	/**
	 * Returns the spawn location for this instance to be used when leaving the instance
	 * @return int[3]
	 */
	public int[] getSpawnLoc()
	{
		return _spawnLoc;
	}

	/**
	 * Sets the spawn location for this instance to be used when leaving the instance
	 * @param loc 
	 */
	public void setSpawnLoc(int[] loc)
	{
		if (loc == null || loc.length < 3)
			return;
		System.arraycopy(loc, 0, _spawnLoc, 0, 3);
	}
	
	public void removePlayers()
	{
		//_players.forEach(_ejectProc);
		synchronized (_players)
		{
			_players.clear();
		}
	}

	public void removeNpcs()
	{
		for (Npc mob : _npcs)
		{
			if (mob != null)
			{
				if (mob.getSpawn() != null)
					mob.getSpawn().setRespawnState(false);//mob.getSpawn().stopRespawn();
				mob.deleteMe();
			}
		}
		_npcs.clear();
	}
	
	public void removeDoors()
	{
		for (Door door: _doors)
		{
			if (door != null)
			{
				WorldRegion region = door.getRegion();//.getWorldRegion();
				door.decayMe();
				
				if (region != null)
					region.removeVisibleObject(door);
				
				//door.getKnownList().removeAllKnownObjects(); // TODO ??
				World.getInstance().removeObject(door);
			}
		}
		_doors.clear();
	}

	public void loadInstanceTemplate(String filename)
	{
		Document doc = null;
		File xml = new File(/*Config.DATAPACK_ROOT, */"data/instances/" + filename);

		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(xml);

			for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if ("instance".equalsIgnoreCase(n.getNodeName()))
				{
					parseInstance(n);
				}
			}
		}
		catch (IOException e)
		{
			_log.warning("Instance: can not find " + xml.getAbsolutePath() + " ! " + e);
		}
		catch (Exception e)
		{
			_log.warning("Instance: error while loading " + xml.getAbsolutePath() + " ! " + e);
		}
	}

	private void parseInstance(Node n) throws Exception
	{
		Spawn spawnDat;
		NpcTemplate npcTemplate;
		String name = null;
		name = n.getAttributes().getNamedItem("name").getNodeValue();
		setName(name);

		Node a;
		Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			if ("activityTime".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
				{
					_CheckTimeUpTask = ThreadPool.schedule(new CheckTimeUp(Integer.parseInt(a.getNodeValue()) * 60000), 15000);
					_instanceEndTime = System.currentTimeMillis() + Long.parseLong(a.getNodeValue()) * 60000 + 15000;
				}
			}
			/*			else if ("timeDelay".equalsIgnoreCase(n.getNodeName()))
						{
							a = n.getAttributes().getNamedItem("val");
							if (a != null)
								instance.setTimeDelay(Integer.parseInt(a.getNodeValue()));
						}*/
			else if ("allowSummon".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
					setAllowSummon(Boolean.parseBoolean(a.getNodeValue()));
			}
			else if ("emptyDestroyTime".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
					_emptyDestroyTime = Long.parseLong(a.getNodeValue()) * 1000;
			}
			else if ("PvPInstance".equalsIgnoreCase(n.getNodeName()))
			{
				a = n.getAttributes().getNamedItem("val");
				if (a != null)
					setPvPInstance(Boolean.parseBoolean(a.getNodeValue()));
			}
			else if ("doorlist".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					int doorId = 0;
					boolean doorState = false;
					if ("door".equalsIgnoreCase(d.getNodeName()))
					{
						doorId = Integer.parseInt(d.getAttributes().getNamedItem("doorId").getNodeValue());
						if (d.getAttributes().getNamedItem("open") != null)
							doorState = Boolean.parseBoolean(d.getAttributes().getNamedItem("open").getNodeValue());
						addDoor(doorId, doorState);
					}
				}
			}
			else if ("spawnlist".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					int npcId = 0, x = 0, y = 0, z = 0, respawn = 0, heading = 0;

					if ("spawn".equalsIgnoreCase(d.getNodeName()))
					{

						npcId = Integer.parseInt(d.getAttributes().getNamedItem("npcId").getNodeValue());
						x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
						y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
						z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
						heading = Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue());
						respawn = Integer.parseInt(d.getAttributes().getNamedItem("respawn").getNodeValue());

						npcTemplate = NpcData.getInstance().getTemplate(npcId);
						if (npcTemplate != null)
						{
							spawnDat = new Spawn(npcTemplate);
							spawnDat.setLoc(x,y,z,heading);
							spawnDat.setRespawnDelay(respawn);
						/*	if (respawn == 0)
								spawnDat.stopRespawn();
							else
								spawnDat.startRespawn();*/
							spawnDat.setInstanceId(getId());
							spawnDat.doSpawn(_allowSummon);
						}
						else
						{
							_log.warning("Instance: Data missing in NPC table for ID: " + npcTemplate + " in Instance " + getId());
						}
					}
				}
			}
			else if ("spawnpoint".equalsIgnoreCase(n.getNodeName()))
			{
				try
				{
					_spawnLoc[0] = Integer.parseInt(n.getAttributes().getNamedItem("spawnX").getNodeValue());
					_spawnLoc[1] = Integer.parseInt(n.getAttributes().getNamedItem("spawnY").getNodeValue());
					_spawnLoc[2] = Integer.parseInt(n.getAttributes().getNamedItem("spawnZ").getNodeValue());
				}
				catch (Exception e)
				{
					_log.warning("Error parsing instance xml: " + e);
					_spawnLoc = new int[3];
				}
			}
		}
		if (DEBUG)
			_log.info(name + " Instance Template for Instance " + getId() + " loaded");
	}

	protected void doCheckTimeUp(int remaining)
	{
		CreatureSay cs = null;
		int timeLeft;
		int interval;

		if (_players.isEmpty() && _emptyDestroyTime == 0)
		{
			remaining = 0;
			interval = 500;
		}
		else if (_players.isEmpty() && _emptyDestroyTime > 0)
		{
			
			Long emptyTimeLeft = _lastLeft + _emptyDestroyTime - System.currentTimeMillis();
			if (emptyTimeLeft <= 0)
			{
				interval = 0;
				remaining = 0;
			}
			else if (remaining > 300000 && emptyTimeLeft > 300000)
			{
				interval = 300000;
				remaining = remaining - 300000;
			}
			else if (remaining > 60000 && emptyTimeLeft > 60000)
			{
				interval = 60000;
				remaining = remaining - 60000;
			}
			else if (remaining > 30000 && emptyTimeLeft > 30000)
			{
				interval = 30000;
				remaining = remaining - 30000;
			}
			else
			{
				interval = 10000;
				remaining = remaining - 10000;
			}
		}
		else if (remaining > 300000)
		{
			timeLeft = remaining / 60000;
			interval = 300000;
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.TIME_FOR_S1_IS_S2_MINUTES_REMAINING/*DUNGEON_EXPIRES_IN_S1_MINUTES*/);
			sm.addString(Integer.toString(timeLeft));
			announceToPlayersInInstance(sm, getId());
			remaining = remaining - 300000;
		}
		else if (remaining > 60000)
		{
			timeLeft = remaining / 60000;
			interval = 60000;
		//	SystemMessage sm = new SystemMessage(SystemMessageId.DUNGEON_EXPIRES_IN_S1_MINUTES);
			SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.TIME_FOR_S1_IS_S2_MINUTES_REMAINING/*DUNGEON_EXPIRES_IN_S1_MINUTES*/);
			sm.addString(Integer.toString(timeLeft));
			announceToPlayersInInstance(sm, getId());
			remaining = remaining - 60000;
		}
		else if (remaining > 30000)
		{
			timeLeft = remaining / 1000;
			interval = 30000;
			cs = new CreatureSay(0, SayType.ALLIANCE, "Notice", timeLeft + " seconds left.");
			remaining = remaining - 30000;
		}
		else
		{
			timeLeft = remaining / 1000;
			interval = 10000;
			cs = new CreatureSay(0, SayType.ALLIANCE, "Notice", timeLeft + " seconds left.");
			remaining = remaining - 10000;
		}
		if (cs != null) //_players.forEach(new SendPacketToPlayerProcedure(cs));
		{
			for (int objectId : _players)
			{
				Player player = (Player) World.getInstance().getObject(objectId);
				if (player != null && player.getInstanceId() == getId())
					player.sendPacket(cs);
			}
		}
			
		
		cancelTimer();
		if (remaining >= 10000)
			_CheckTimeUpTask = ThreadPool.schedule(new CheckTimeUp(remaining), interval);
		else
			_CheckTimeUpTask = ThreadPool.schedule(new TimeUp(), interval);
	}

	public void cancelTimer()
	{
		if (_CheckTimeUpTask != null)
			_CheckTimeUpTask.cancel(true);
	}

	public class CheckTimeUp implements Runnable
	{
		private int	_remaining;

		public CheckTimeUp(int remaining)
		{
			_remaining = remaining;
		}

		@Override
		public void run()
		{
			doCheckTimeUp(_remaining);
		}
	}

	public class TimeUp implements Runnable
	{
		@Override
		public void run()
		{
			InstanceManager.getInstance().destroyInstance(getId());
		}
	}
	
	public static void announceToPlayersInInstance(L2GameServerPacket mov, int instanceId)
	{
		Collection<Player> pls = World.getInstance().getPlayers();// Collection<Player> pls = World.getInstance().getAllPlayers().values();
		//synchronized (character.getKnownList().getKnownPlayers())
		{
			for (Player onPlayer : pls)
				if (onPlayer.isOnline() && onPlayer.getInstanceId() == instanceId)
					onPlayer.sendPacket(mov);
		}
	}
}