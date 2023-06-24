package net.sf.l2j.gameserver.model.entity;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.l2j.commons.data.StatSet;
import net.sf.l2j.commons.pool.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.manager.InstanceManager;
import net.sf.l2j.gameserver.data.manager.InstanceManager.InstanceWorld;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.WorldRegion;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Door;
import net.sf.l2j.gameserver.model.actor.template.DoorTemplate;
import net.sf.l2j.gameserver.model.holder.InstanceReenterTimeHolder;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Main class for game instances.
 * @author evill33t
 * @author GodKratos
 */
public final class Instance
{
	private static final Logger _log = Logger.getLogger(Instance.class.getName());
	
	private final int _id;
	private String _name;
	private int _ejectTime = 60000;
	/** Allow random walk for NPCs, global parameter. */
	private boolean _allowRandomWalk = true;
	private final List<Integer> _players = new CopyOnWriteArrayList<>();
	private final List<Npc> _npcs = new CopyOnWriteArrayList<>();
	private final Map<Integer, Door> _doors = new ConcurrentHashMap<>();
	private final Map<String, List<Spawn>> _manualSpawn = new HashMap<>();
	// private StartPosType _enterLocationOrder; TODO implement me
	private List<Location> _enterLocations = null;
	private Location _exitLocation = null;
	private boolean _allowSummon = true;
	private long _emptyDestroyTime = -1;
	private long _lastLeft = -1;
	private final long _instanceStartTime;
	private long _instanceEndTime = -1;
	private boolean _isPvPInstance = false;
	private boolean _showTimer = false;
	private boolean _isTimerIncrease = true;
	private String _timerText = "";
	// Instance reset data
	private InstanceReenterType _type = InstanceReenterType.NONE;
	private final List<InstanceReenterTimeHolder> _resetData = new ArrayList<>();
	// Instance remove buffs data
	private InstanceRemoveBuffType _removeBuffType = InstanceRemoveBuffType.NONE;
	private final List<Integer> _exceptionList = new ArrayList<>();
	
	protected ScheduledFuture<?> _checkTimeUpTask = null;
	protected final Map<Integer, ScheduledFuture<?>> _ejectDeadTasks = new ConcurrentHashMap<>();
	
	public Instance(int id)
	{
		_id = id;
		_instanceStartTime = System.currentTimeMillis();
	}
	
	public Instance(int id, String name)
	{
		_id = id;
		_name = name;
		_instanceStartTime = System.currentTimeMillis();
	}
	
	/**
	 * @return the ID of this instance.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * @return the name of this instance
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
	 * @return the eject time
	 */
	public int getEjectTime()
	{
		return _ejectTime;
	}
	
	/**
	 * @param ejectTime the player eject time upon death
	 */
	public void setEjectTime(int ejectTime)
	{
		_ejectTime = ejectTime;
	}
	
	/**
	 * @return whether summon friend type skills are allowed for this instance
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
	
	/**
	 * Returns true if entire instance is PvP zone
	 * @return
	 */
	public boolean isPvPInstance()
	{
		return _isPvPInstance;
	}
	
	/**
	 * Sets PvP zone status of the instance
	 * @param b
	 */
	public void setPvPInstance(boolean b)
	{
		_isPvPInstance = b;
	}
	
	/**
	 * Set the instance duration task
	 * @param duration in milliseconds
	 */
	public void setDuration(long duration)
	{
		if (_checkTimeUpTask != null)
		{
			_checkTimeUpTask.cancel(true);
		}
		
		_checkTimeUpTask = ThreadPool.schedule(new CheckTimeUp(duration), 500);
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
		_players.add(objectId);
	}
	
	/**
	 * Removes the specified player from the instance list.
	 * @param objectId the player's object Id
	 */
	public void removePlayer(Integer objectId)
	{
		_players.remove(objectId);
		if (_players.isEmpty() && (_emptyDestroyTime >= 0))
		{
			_lastLeft = System.currentTimeMillis();
			setDuration((int) (_instanceEndTime - System.currentTimeMillis() - 500));
		}
	}
	
	public void addNpc(Npc npc)
	{
		_npcs.add(npc);
	}
	
	public void removeNpc(Npc npc)
	{
		if (npc.getSpawn() != null)
		{
			npc.getSpawn().setRespawnState(false);// .stopRespawn();
		}
		_npcs.remove(npc);
	}
	
	/**
	 * Adds a door into the instance
	 * @param doorId - from doors.xml
	 * @param set - StatSet for initializing door
	 */
	public void addDoor(int doorId, StatSet set)
	{
		if (_doors.containsKey(doorId))
		{
			_log.warning("Door ID " + doorId + " already exists in instance " + getId());
			return;
		}
		
		final Door newdoor = new Door(doorId, new DoorTemplate(set));
		newdoor.setInstanceId(getId());
		newdoor.getStatus().setHp(newdoor.getStatus().getMaxHp());
		newdoor.spawnMe(newdoor.getTemplate().getPosX(), newdoor.getTemplate().getPosY(), newdoor.getTemplate().getPosZ());
		_doors.put(doorId, newdoor);
	}
	
	public List<Integer> getPlayers()
	{
		return _players;
	}
	
	public List<Npc> getNpcs()
	{
		return _npcs;
	}
	
	public Collection<Door> getDoors()
	{
		return _doors.values();
	}
	
	public Door getDoor(int id)
	{
		return _doors.get(id);
	}
	
	public long getInstanceEndTime()
	{
		return _instanceEndTime;
	}
	
	public long getInstanceStartTime()
	{
		return _instanceStartTime;
	}
	
	public boolean isShowTimer()
	{
		return _showTimer;
	}
	
	public boolean isTimerIncrease()
	{
		return _isTimerIncrease;
	}
	
	public String getTimerText()
	{
		return _timerText;
	}
	
	/**
	 * @return the spawn location for this instance to be used when enter in instance
	 */
	public List<Location> getEnterLocs()
	{
		return _enterLocations;
	}
	
	/**
	 * Sets the spawn location for this instance to be used when enter in instance
	 * @param loc
	 */
	public void addEnterLoc(Location loc)
	{
		_enterLocations.add(loc);
	}
	
	/**
	 * @return the spawn location for this instance to be used when leaving the instance
	 */
	public Location getExitLoc()
	{
		return _exitLocation;
	}
	
	/**
	 * Sets the spawn location for this instance to be used when leaving the instance
	 * @param loc
	 */
	public void setExitLoc(Location loc)
	{
		_exitLocation = loc;
	}
	
	public void removePlayers()
	{
		for (Integer objectId : _players)
		{
			final Player player = World.getInstance().getPlayer(objectId);
			if ((player != null) && (player.getInstanceId() == getId()))
			{
				player.setInstanceId(0);
				if (getExitLoc() != null)
				{
					player.teleportTo(getExitLoc()/* , true */, 20);
				}
				else
				{
					player.teleportTo(TeleportType.TOWN);
				}
			}
		}
		_players.clear();
	}
	
	public void removeNpcs()
	{
		for (Npc mob : _npcs)
		{
			if (mob != null)
			{
				if (mob.getSpawn() != null)
				{
					mob.getSpawn().setRespawnState(false);// .stopRespawn();
				}
				mob.deleteMe();
			}
		}
		_npcs.clear();
		_manualSpawn.clear();
	}
	
	public void removeDoors()
	{
		for (Door door : _doors.values())
		{
			if (door != null)
			{
				WorldRegion region = door.getRegion();
				door.decayMe();
				
				if (region != null)
				{
					region.removeVisibleObject(door);
				}
				if (door.getInstanceId() == 0)
					continue;
				
				// door.getKnownList().removeAllKnownObjects();
				World.getInstance().removeObject(door);
			}
		}
		_doors.clear();
	}
	
	/**
	 * Spawns group of instance NPC's
	 * @param groupName - name of group from XML definition to spawn
	 * @return list of spawned NPC's
	 */
	public List<Npc> spawnGroup(String groupName)
	{
		List<Npc> ret = null;
		if (_manualSpawn.containsKey(groupName))
		{
			final List<Spawn> manualSpawn = _manualSpawn.get(groupName);
			ret = new ArrayList<>(manualSpawn.size());
			
			for (Spawn spawnDat : manualSpawn)
			{
				ret.add(spawnDat.doSpawn(true));
			}
		}
		else
		{
			_log.warning(getName() + " instance: cannot spawn NPC's, wrong group name: " + groupName);
		}
		
		return ret;
	}
	
	public void loadInstanceTemplate(String filename)
	{
		File xml = new File("data/instances/" + filename);
		
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			var doc = factory.newDocumentBuilder().parse(xml);
			
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
			_log.log(Level.WARNING, "Instance: can not find " + xml.getAbsolutePath() + " ! " + e.getMessage(), e);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Instance: error while loading " + xml.getAbsolutePath() + " ! " + e.getMessage(), e);
		}
	}
	
	private void parseInstance(Node n) throws Exception
	{
		_name = n.getAttributes().getNamedItem("name").getNodeValue();
		Node a = n.getAttributes().getNamedItem("ejectTime");
		if (a != null)
		{
			_ejectTime = 1000 * Integer.parseInt(a.getNodeValue());
		}
		a = n.getAttributes().getNamedItem("allowRandomWalk");
		if (a != null)
		{
			_allowRandomWalk = Boolean.parseBoolean(a.getNodeValue());
		}
		Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			switch (n.getNodeName().toLowerCase())
			{
				case "activitytime":
				{
					a = n.getAttributes().getNamedItem("val");
					if (a != null)
					{
						_checkTimeUpTask = ThreadPool.schedule(new CheckTimeUp(Integer.parseInt(a.getNodeValue()) * 60000), 15000);
						_instanceEndTime = System.currentTimeMillis() + (Long.parseLong(a.getNodeValue()) * 60000) + 15000;
					}
				}
				case "allowsummon":
				{
					a = n.getAttributes().getNamedItem("val");
					if (a != null)
					{
						setAllowSummon(Boolean.parseBoolean(a.getNodeValue()));
					}
				}
				case "emptydestroytime":
				{
					a = n.getAttributes().getNamedItem("val");
					if (a != null)
					{
						_emptyDestroyTime = Long.parseLong(a.getNodeValue()) * 1000;
					}
				}
				case "showtimer":
				{
					a = n.getAttributes().getNamedItem("val");
					if (a != null)
					{
						_showTimer = Boolean.parseBoolean(a.getNodeValue());
					}
					a = n.getAttributes().getNamedItem("increase");
					if (a != null)
					{
						_isTimerIncrease = Boolean.parseBoolean(a.getNodeValue());
					}
					a = n.getAttributes().getNamedItem("text");
					if (a != null)
					{
						_timerText = a.getNodeValue();
					}
				}
				case "pvpinstance":
				{
					a = n.getAttributes().getNamedItem("val");
					if (a != null)
					{
						setPvPInstance(Boolean.parseBoolean(a.getNodeValue()));
					}
				}
				case "doorlist":
				{
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("door".equalsIgnoreCase(d.getNodeName()))
						{
							int doorId = Integer.parseInt(d.getAttributes().getNamedItem("doorId").getNodeValue());
							final StatSet set = new StatSet();
							set.add(DoorData.getInstance().getDoorTemplate(doorId));
							for (Node bean = d.getFirstChild(); bean != null; bean = bean.getNextSibling())
							{
								if ("set".equalsIgnoreCase(bean.getNodeName()))
								{
									NamedNodeMap attrs = bean.getAttributes();
									String setname = attrs.getNamedItem("name").getNodeValue();
									String value = attrs.getNamedItem("val").getNodeValue();
									set.set(setname, value);
								}
							}
							addDoor(doorId, set);
						}
					}
				}
				case "spawnlist":
				{
					for (Node group = n.getFirstChild(); group != null; group = group.getNextSibling())
					{
						if ("group".equalsIgnoreCase(group.getNodeName()))
						{
							String spawnGroup = group.getAttributes().getNamedItem("name").getNodeValue();
							List<Spawn> manualSpawn = new ArrayList<>();
							for (Node d = group.getFirstChild(); d != null; d = d.getNextSibling())
							{
								int respawnRandom = 0, delay = -1;
							//	String areaName = null;
							//	int globalMapId = 0;
								
								if ("spawn".equalsIgnoreCase(d.getNodeName()))
								{
									int npcId = Integer.parseInt(d.getAttributes().getNamedItem("npcId").getNodeValue());
									int x = Integer.parseInt(d.getAttributes().getNamedItem("x").getNodeValue());
									int y = Integer.parseInt(d.getAttributes().getNamedItem("y").getNodeValue());
									int z = Integer.parseInt(d.getAttributes().getNamedItem("z").getNodeValue());
									int heading = Integer.parseInt(d.getAttributes().getNamedItem("heading").getNodeValue());
									int respawn = Integer.parseInt(d.getAttributes().getNamedItem("respawn").getNodeValue());
									
									Node node = d.getAttributes().getNamedItem("onKillDelay");
									if (node != null)
									{
										delay = Integer.parseInt(node.getNodeValue());
									}
									
									node = d.getAttributes().getNamedItem("respawnRandom");
									if (node != null)
									{
										respawnRandom = Integer.parseInt(node.getNodeValue());
									}
									
									node = d.getAttributes().getNamedItem("allowRandomWalk");
									if (d.getAttributes().getNamedItem("allowRandomWalk") != null)
									{
										_allowRandomWalk = Boolean.valueOf(node.getNodeValue());
									}
									
									node = d.getAttributes().getNamedItem("areaName");
									if (d.getAttributes().getNamedItem("areaName") != null)
									{
									//	areaName = node.getNodeValue();
									}
									
									node = d.getAttributes().getNamedItem("globalMapId");
									if (node != null)
									{
									//	globalMapId = Integer.parseInt(node.getNodeValue());
									}
									
									final Spawn spawnDat = new Spawn(npcId);
									spawnDat.setLoc(x, y, z, heading);
									if (Rnd.get(100) < 20)
										respawn += respawnRandom;
									else if (Rnd.get(100) > 80)
										respawn -= respawnRandom;
									spawnDat.setRespawnDelay(respawn);
									if (respawn == 0)
									{
										spawnDat.setRespawnState(false);// .stopRespawn();
									}
									else
									{
										spawnDat.setRespawnState(true);// .startRespawn();
									}
									spawnDat.setInstanceId(getId());
									spawnDat.getRandomWalkLocation(spawnDat.getNpc(), !_allowRandomWalk ? 0 : 50);
									// if (allowRandomWalk == null) spawnDat.setIsNoRndWalk(!_allowRandomWalk); else spawnDat.setIsNoRndWalk(!allowRandomWalk);
									
									// spawnDat.setAreaName(areaName);
									// spawnDat.setGlobalMapId(globalMapId);
									
									if (spawnGroup.equals("general"))
									{
										final Npc spawned = spawnDat.doSpawn(true);
										if ((delay >= 0) && (spawned instanceof Attackable))
										{
											// ((Attackable) spawned).setOnKillDelay(delay);
										}
									}
									else
									{
										manualSpawn.add(spawnDat);
									}
								}
							}
							
							if (!manualSpawn.isEmpty())
							{
								_manualSpawn.put(spawnGroup, manualSpawn);
							}
						}
					}
				}
				case "exitpoint":
				{
					int x = Integer.parseInt(n.getAttributes().getNamedItem("x").getNodeValue());
					int y = Integer.parseInt(n.getAttributes().getNamedItem("y").getNodeValue());
					int z = Integer.parseInt(n.getAttributes().getNamedItem("z").getNodeValue());
					_exitLocation = new Location(x, y, z);
				}
				case "spawnpoints":
				{
					_enterLocations = new ArrayList<>();
					for (Node loc = n.getFirstChild(); loc != null; loc = loc.getNextSibling())
					{
						if (loc.getNodeName().equals("Location"))
						{
							try
							{
								int x = Integer.parseInt(loc.getAttributes().getNamedItem("x").getNodeValue());
								int y = Integer.parseInt(loc.getAttributes().getNamedItem("y").getNodeValue());
								int z = Integer.parseInt(loc.getAttributes().getNamedItem("z").getNodeValue());
								_enterLocations.add(new Location(x, y, z));
							}
							catch (Exception e)
							{
								_log.log(Level.WARNING, "Error parsing instance xml: " + e.getMessage(), e);
							}
						}
					}
				}
				case "reenter":
				{
					a = n.getAttributes().getNamedItem("additionStyle");
					if (a != null)
					{
						_type = InstanceReenterType.valueOf(a.getNodeValue());
					}
					
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						DayOfWeek day = null;
						int hour = -1;
						int minute = -1;
						
						if ("reset".equalsIgnoreCase(d.getNodeName()))
						{
							a = d.getAttributes().getNamedItem("time");
							if (a != null)
							{
								long time = Long.parseLong(a.getNodeValue());
								if (time > 0)
								{
									_resetData.add(new InstanceReenterTimeHolder(time));
									break;
								}
							}
							else
							{
								a = d.getAttributes().getNamedItem("day");
								if (a != null)
								{
									day = DayOfWeek.valueOf(a.getNodeValue().toUpperCase());
								}
								
								a = d.getAttributes().getNamedItem("hour");
								if (a != null)
								{
									hour = Integer.parseInt(a.getNodeValue());
								}
								
								a = d.getAttributes().getNamedItem("minute");
								if (a != null)
								{
									minute = Integer.parseInt(a.getNodeValue());
								}
								_resetData.add(new InstanceReenterTimeHolder(day, hour, minute));
							}
						}
					}
				}
				case "removebuffs":
				{
					a = n.getAttributes().getNamedItem("type");
					if (a != null)
					{
						_removeBuffType = InstanceRemoveBuffType.valueOf(a.getNodeValue().toUpperCase());
					}
					
					for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					{
						if ("skill".equalsIgnoreCase(d.getNodeName()))
						{
							a = d.getAttributes().getNamedItem("id");
							if (a != null)
							{
								_exceptionList.add(Integer.parseInt(a.getNodeValue()));
							}
						}
					}
				}
			}
		}
	}
	
	protected void doCheckTimeUp(long remaining)
	{
		CreatureSay cs = null;
		long timeLeft;
		int interval;
		
		if (_players.isEmpty() && (_emptyDestroyTime == 0))
		{
			remaining = 0;
			interval = 500;
		}
		else if (_players.isEmpty() && (_emptyDestroyTime > 0))
		{
			
			long emptyTimeLeft = (_lastLeft + _emptyDestroyTime) - System.currentTimeMillis();
			if (emptyTimeLeft <= 0)
			{
				interval = 0;
				remaining = 0;
			}
			else if ((remaining > 300000) && (emptyTimeLeft > 300000))
			{
				interval = 300000;
				remaining = remaining - 300000;
			}
			else if ((remaining > 60000) && (emptyTimeLeft > 60000))
			{
				interval = 60000;
				remaining = remaining - 60000;
			}
			else if ((remaining > 30000) && (emptyTimeLeft > 30000))
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
			// SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DUNGEON_EXPIRES_IN_S1_MINUTES);
			// sm.addString(Long.toString(timeLeft));
			// toPlayersInInstance(sm, getId());
			remaining = remaining - 300000;
		}
		else if (remaining > 60000)
		{
			timeLeft = remaining / 60000;
			interval = 60000;
			// SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.DUNGEON_EXPIRES_IN_S1_MINUTES);
			// sm.addString(Long.toString(timeLeft));
			// toPlayersInInstance(sm, getId());
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
		if (cs != null)
		{
			for (Integer objectId : _players)
			{
				final Player player = World.getInstance().getPlayer(objectId);
				if ((player != null) && (player.getInstanceId() == getId()))
				{
					player.sendPacket(cs);
				}
			}
		}
		cancelTimer();
		if (remaining >= 10000)
		{
			_checkTimeUpTask = ThreadPool.schedule(new CheckTimeUp(remaining), interval);
		}
		else
		{
			_checkTimeUpTask = ThreadPool.schedule(new TimeUp(), interval);
		}
	}
	
	public void cancelTimer()
	{
		if (_checkTimeUpTask != null)
		{
			_checkTimeUpTask.cancel(true);
		}
	}
	
	public void cancelEjectDeadPlayer(Player player)
	{
		final ScheduledFuture<?> task = _ejectDeadTasks.remove(player.getObjectId());
		if (task != null)
		{
			task.cancel(true);
		}
	}
	
	public void addEjectDeadTask(Player player)
	{
		if ((player != null))
		{
			_ejectDeadTasks.put(player.getObjectId(), ThreadPool.schedule(() ->
			{
				if (player.isDead() && (player.getInstanceId() == getId()))
				{
					player.setInstanceId(0);
					if (getExitLoc() != null)
					{
						player.teleportTo(getExitLoc(), /*true*/20);
					}
					else
					{
						player.teleportTo(TeleportType.TOWN);
					}
				}
			}, _ejectTime));
		}
	}
	
	/**
	 * @param killer the character that killed the {@code victim}
	 * @param victim the character that was killed by the {@code killer}
	 */
	public static void notifyDeath(Creature killer, Creature victim)
	{
		final InstanceWorld instance = InstanceManager.getInstance().getPlayerWorld(victim.getActingPlayer());
		if (instance != null)
		{
			instance.onDeath(killer, victim);
		}
	}
	
	public class CheckTimeUp implements Runnable
	{
		private final long _remaining;
		
		public CheckTimeUp(long remaining)
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
	
	public InstanceReenterType getReenterType()
	{
		return _type;
	}
	
	public void setReenterType(InstanceReenterType type)
	{
		_type = type;
	}
	
	public List<InstanceReenterTimeHolder> getReenterData()
	{
		return _resetData;
	}
	
	public boolean isRemoveBuffEnabled()
	{
		return getRemoveBuffType() != InstanceRemoveBuffType.NONE;
	}
	
	public InstanceRemoveBuffType getRemoveBuffType()
	{
		return _removeBuffType;
	}
	
	public List<Integer> getBuffExceptionList()
	{
		return _exceptionList;
	}
	
	public enum InstanceRemoveBuffType
	{
		NONE,
		ALL,
		WHITELIST,
		BLACKLIST
	}
	
	public enum InstanceReenterType
	{
		NONE,
		ON_INSTANCE_ENTER,
		ON_INSTANCE_FINISH
	}
}