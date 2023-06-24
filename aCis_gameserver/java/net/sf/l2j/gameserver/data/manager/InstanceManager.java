/*
 * Copyright © 2004-2023 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.data.manager;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.pool.ConnectionPool;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.entity.Instance;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Instance Manager.
 * @author evill33t
 * @author GodKratos
 */
public final class InstanceManager implements IXmlReader
{
	private final static Logger LOG = Logger.getLogger(InstanceManager.class.getName());
	
	private static final Map<Integer, Instance> INSTANCES = new ConcurrentHashMap<>();
	
	private final Map<Integer, InstanceWorld> _instanceWorlds = new ConcurrentHashMap<>();
	
	private int _dynamic = 300000;
	
	private static final Map<Integer, String> _instanceIdNames = new HashMap<>();
	
	private final Map<Integer, Map<Integer, Long>> _playerInstanceTimes = new ConcurrentHashMap<>();
	
	private static final String ADD_INSTANCE_TIME = "INSERT INTO character_instance_time (charId,instanceId,time) values (?,?,?) ON DUPLICATE KEY UPDATE time=?";
	
	private static final String RESTORE_INSTANCE_TIMES = "SELECT instanceId,time FROM character_instance_time WHERE charId=?";
	
	private static final String DELETE_INSTANCE_TIME = "DELETE FROM character_instance_time WHERE charId=? AND instanceId=?";
	
	protected InstanceManager()
	{
		// Creates the multiverse.
		INSTANCES.put(-1, new Instance(-1, "multiverse"));
		LOG.info("Multiverse Instance created.");
		// Creates the universe.
		INSTANCES.put(0, new Instance(0, "universe"));
		LOG.info("Universe Instance created.");
		load();
	}
	
	@Override
	public void load()
	{
		_instanceIdNames.clear();
		parseFile("data/instancenames.xml");
		LOG.info("Loaded {} instance names. "+ _instanceIdNames.size());
	}
	
	public long getInstanceTime(int playerObjId, int id)
	{
		if (!_playerInstanceTimes.containsKey(playerObjId))
		{
			restoreInstanceTimes(playerObjId);
		}
		if (_playerInstanceTimes.get(playerObjId).containsKey(id))
		{
			return _playerInstanceTimes.get(playerObjId).get(id);
		}
		return -1;
	}
	
	public Map<Integer, Long> getAllInstanceTimes(int playerObjId)
	{
		if (!_playerInstanceTimes.containsKey(playerObjId))
		{
			restoreInstanceTimes(playerObjId);
		}
		return _playerInstanceTimes.get(playerObjId);
	}
	
	public void setInstanceTime(int playerObjId, int id, long time)
	{
		if (!_playerInstanceTimes.containsKey(playerObjId))
		{
			restoreInstanceTimes(playerObjId);
		}

		try (Connection con = ConnectionPool.getConnection();
			var ps = con.prepareStatement(ADD_INSTANCE_TIME))
		{
			ps.setInt(1, playerObjId);
			ps.setInt(2, id);
			ps.setLong(3, time);
			ps.setLong(4, time);
			ps.execute();
			_playerInstanceTimes.get(playerObjId).put(id, time);
		}
		catch (Exception ex)
		{
			LOG.warning("Could not insert character instance time data!"+ ex);
		}
	}
	
	public void deleteInstanceTime(int playerObjId, int id)
	{
		try (Connection con = ConnectionPool.getConnection();
			var ps = con.prepareStatement(DELETE_INSTANCE_TIME))
		{
			ps.setInt(1, playerObjId);
			ps.setInt(2, id);
			ps.execute();
			_playerInstanceTimes.get(playerObjId).remove(id);
		}
		catch (Exception ex)
		{
			LOG.warning("Could not delete character instance time data!" + ex);
		}
	}
	
	public void restoreInstanceTimes(int playerObjId)
	{
		if (_playerInstanceTimes.containsKey(playerObjId))
		{
			return; // already restored
		}
		_playerInstanceTimes.put(playerObjId, new ConcurrentHashMap<>());
		
		try (Connection con = ConnectionPool.getConnection();
			// try (var con = ConnectionFactory.getInstance().getConnection();
			var ps = con.prepareStatement(RESTORE_INSTANCE_TIMES))
		{
			ps.setInt(1, playerObjId);
			try (var rs = ps.executeQuery())
			{
				while (rs.next())
				{
					int id = rs.getInt("instanceId");
					long time = rs.getLong("time");
					if (time < System.currentTimeMillis())
						deleteInstanceTime(playerObjId, id);
					else
						_playerInstanceTimes.get(playerObjId).put(id, time);
				}
			}
		}
		catch (Exception ex)
		{
			LOG.warning("Could not delete character instance time data!" + ex);
		}
	}
	
	public static String getInstanceIdName(int id)
	{
		if (_instanceIdNames.containsKey(id))
		{
			return _instanceIdNames.get(id);
		}
		return ("UnknownInstance");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		for (Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				NamedNodeMap attrs;
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("instance".equals(d.getNodeName()))
					{
						attrs = d.getAttributes();
						_instanceIdNames.put(parseInteger(attrs, "id"), attrs.getNamedItem("name").getNodeValue());
					}
				}
			}
		}
	}
	
	public void addWorld(InstanceWorld world)
	{
		_instanceWorlds.put(world.getInstanceId(), world);
	}
	
	public InstanceWorld getWorld(int instanceId)
	{
		return _instanceWorlds.get(instanceId);
	}
	
	/**
	 * Check if the player have a World Instance where it's allowed to enter.
	 * @param player the player to check
	 * @return the instance world
	 */
	public InstanceWorld getPlayerWorld(Player player)
	{
		for (InstanceWorld temp : _instanceWorlds.values())
		{
			if ((temp != null) && (temp.isAllowed(player.getObjectId())))
			{
				return temp;
			}
		}
		return null;
	}
	
	public void destroyInstance(int instanceid)
	{
		if (instanceid <= 0)
		{
			return;
		}
		final Instance temp = INSTANCES.get(instanceid);
		if (temp != null)
		{
			temp.removeNpcs();
			temp.removePlayers();
			temp.removeDoors();
			temp.cancelTimer();
			INSTANCES.remove(instanceid);
			_instanceWorlds.remove(instanceid);
		}
	}
	
	public static Instance getInstance(int instanceid)
	{
		return INSTANCES.get(instanceid);
	}
	
	public static Map<Integer, Instance> getInstances()
	{
		return INSTANCES;
	}
	
	public static int getPlayerInstance(int objectId)
	{
		for (Instance temp : INSTANCES.values())
		{
			if (temp == null)
			{
				continue;
			}
			// check if the player is in any active instance
			if (temp.containsPlayer(objectId))
			{
				return temp.getId();
			}
		}
		// 0 is default instance aka the world
		return 0;
	}
	
	public static boolean createInstance(int id)
	{
		if (getInstance(id) != null)
		{
			return false;
		}
		
		final Instance instance = new Instance(id);
		INSTANCES.put(id, instance);
		return true;
	}
	
	public static boolean createInstanceFromTemplate(int id, String template)
	{
		if (getInstance(id) != null)
		{
			return false;
		}
		
		final Instance instance = new Instance(id);
		INSTANCES.put(id, instance);
		instance.loadInstanceTemplate(template);
		return true;
	}
	
	/**
	 * Create a new instance with a dynamic instance id based on a template (or null)
	 * @param template xml file
	 * @return
	 */
	public int createDynamicInstance(String template)
	{
		while (getInstance(_dynamic) != null)
		{
			_dynamic++;
			if (_dynamic == Integer.MAX_VALUE)
			{
				LOG.warning("More then {} instances has been created! " + (Integer.MAX_VALUE - 300000));
				_dynamic = 300000;
			}
		}
		final Instance instance = new Instance(_dynamic);
		INSTANCES.put(_dynamic, instance);
		if (template != null)
		{
			instance.loadInstanceTemplate(template);
		}
		return _dynamic;
	}
	
	public class InstanceWorld
	{
		private int _instanceId;
		private int _templateId = -1;
		private final List<Integer> _allowed = new CopyOnWriteArrayList<>();
		private final AtomicInteger _status = new AtomicInteger();
		
		public List<Integer> getAllowed()
		{
			return _allowed;
		}
		
		public void removeAllowed(int id)
		{
			_allowed.remove(Integer.valueOf(id));
		}
		
		public void addAllowed(int id)
		{
			_allowed.add(id);
		}
		
		public boolean isAllowed(int id)
		{
			return _allowed.contains(id);
		}
		
		/**
		 * Gets the dynamically generated instance ID.
		 * @return the instance ID
		 */
		public int getInstanceId()
		{
			return _instanceId;
		}
		
		/**
		 * Sets the instance ID.
		 * @param instanceId the instance ID
		 */
		public void setInstanceId(int instanceId)
		{
			_instanceId = instanceId;
		}
		
		/**
		 * Gets the client's template instance ID.
		 * @return the template ID
		 */
		public int getTemplateId()
		{
			return _templateId;
		}
		
		/**
		 * Sets the template ID.
		 * @param templateId the template ID
		 */
		public void setTemplateId(int templateId)
		{
			_templateId = templateId;
		}
		
		public int getStatus()
		{
			return _status.get();
		}
		
		public boolean isStatus(int status)
		{
			return _status.get() == status;
		}
		
		public void setStatus(int status)
		{
			_status.set(status);
		}
		
		public void incStatus()
		{
			_status.incrementAndGet();
		}
		
		/**
		 * @param killer
		 * @param victim
		 */
		public void onDeath(Creature killer, Creature victim)
		{
			if ((victim != null) && victim/*.isPlayer()*/ instanceof Player)
			{
				final Instance instance = InstanceManager.getInstance(getInstanceId());
				if (instance != null)
				{
			//		final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.YOU_WILL_BE_EXPELLED_IN_S1);
			//		sm.addInt(instance.getEjectTime() / 60 / 1000);
			//		victim.getActingPlayer().sendPacket(sm);
					instance.addEjectDeadTask(victim.getActingPlayer());
				}
			}
		}
	}
	
	public static InstanceManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final InstanceManager _instance = new InstanceManager();
	}
}
