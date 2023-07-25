package net.sf.l2j.gameserver.communitybbs.module;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.pool.ConnectionPool;

public class TopPvpPk
{
	private StringBuilder _TopPvpPk = new StringBuilder();
	private int _posId;
	
	public TopPvpPk()
	{
		loadFromDB();
	}
	
	private void loadFromDB()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT char_name, base_class, pvpkills, pkkills, level, online, clan_name, ally_name FROM characters LEFT JOIN clan_data ON clan_id=clanid WHERE accesslevel=0 ORDER BY pvpkills DESC, char_name ASC LIMIT 10");
			ResultSet result = statement.executeQuery())
		{
			_posId = 0;
			
			while (result.next())
			{
				boolean status = false;
				
				_posId = _posId + 1;
				
				if(result.getInt("online") == 1)
					status = true;
				addTopPvpPkList(_posId, result.getString("char_name"), result.getString("clan_name") == null ? "No Clan" : result.getString("clan_name"), result.getString("ally_name") == null ? "No Ally" : result.getString("ally_name"), result.getInt("base_class"), result.getInt("level"), result.getInt("pvpkills"), result.getInt("pkkills"), status);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public String loadTopPvpPk()
	{
		return _TopPvpPk.toString();
	}
	
	private void addTopPvpPkList(int objId, String name, String clan, String alliance, int ChrClass, int level, int points, int pkkills, boolean isOnline)
	{
		_TopPvpPk.append("<table border=0 cellspacing=0 cellpadding=2 width=750>");
		_TopPvpPk.append("<tr>");
		_TopPvpPk.append("<td FIXWIDTH=2></td>");
		_TopPvpPk.append("<td FIXWIDTH=15>"+objId+".</td>");
		_TopPvpPk.append("<td FIXWIDTH=90>"+name+"</td>");
        _TopPvpPk.append("<td FIXWIDTH=20>"+level+"</td>");
        _TopPvpPk.append("<td FIXWIDTH=70>"+className(ChrClass)+"</td>");
        _TopPvpPk.append("<td FIXWIDTH=70>"+clan+"</td>");
        _TopPvpPk.append("<td FIXWIDTH=70>"+alliance+"</td>");
        _TopPvpPk.append("<td FIXWIDTH=25>"+points+"</td>");
        _TopPvpPk.append("<td FIXWIDTH=25>"+pkkills+"</td>");
        _TopPvpPk.append("<td FIXWIDTH=90>"+((isOnline) ? "<font color=00FF00>Online</font>" : "<font color=CC0000>Offline</font>")+"</td>");
        _TopPvpPk.append("<td FIXWIDTH=2></td>");
        _TopPvpPk.append("</tr>");
        _TopPvpPk.append("</table>");
        _TopPvpPk.append("<img src=\"L2UI.Squaregray\" width=\"740\" height=\"1\">");
	}
	
	private final static String className(int classId)
	{
		Map<Integer, String> classList;
		classList = new HashMap<>();
        classList.put(0, "Fighter");
        classList.put(1, "Warrior");
        classList.put(2, "Gladiator");
        classList.put(3, "Warlord");
        classList.put(4, "Knight");
        classList.put(5, "Paladin");
        classList.put(6, "Dark Avenger");
        classList.put(7, "Rogue");
        classList.put(8, "Treasure Hunter");
        classList.put(9, "Hawkeye");
        classList.put(10, "Mage");
        classList.put(11, "Wizard");
        classList.put(12, "Sorcerer");
        classList.put(13, "Necromancer");
        classList.put(14, "Warlock");
        classList.put(15, "Cleric");
        classList.put(16, "Bishop");
        classList.put(17, "Prophet");
        classList.put(18, "Elven Fighter");
        classList.put(19, "Elven Knight");
        classList.put(20, "Temple Knight");
        classList.put(21, "Swordsinger");
        classList.put(22, "Elven Scout");
        classList.put(23, "Plains Walker");
        classList.put(24, "Silver Ranger");
        classList.put(25, "Elven Mage");
        classList.put(26, "Elven Wizard");
        classList.put(27, "Spellsinger");
        classList.put(28, "Elemental Summoner");
        classList.put(29, "Oracle");
        classList.put(30, "Elder");
        classList.put(31, "Dark Fighter");
        classList.put(32, "Palus Knightr");
        classList.put(33, "Shillien Knight");
        classList.put(34, "Bladedancer");
        classList.put(35, "Assasin");
        classList.put(36, "Abyss Walker");
        classList.put(37, "Phantom Ranger");
        classList.put(38, "Dark Mage");
        classList.put(39, "Dark Wizard");
        classList.put(40, "Spellhowler");
        classList.put(41, "Phantom Summoner");
        classList.put(42, "Shillien Oracle");
        classList.put(43, "Shilien Elder");
        classList.put(44, "Orc Fighter");
        classList.put(45, "Orc Raider");
        classList.put(46, "Destroyer");
        classList.put(47, "Orc Monk");
        classList.put(48, "Tyrant");
        classList.put(49, "Orc Mage");
        classList.put(50, "Orc Shaman");
        classList.put(51, "Overlord");
        classList.put(52, "Warcryer");
        classList.put(53, "Dwarven Fighter");
        classList.put(54, "Scavenger");
        classList.put(55, "Bounty Hunter");
        classList.put(56, "Artisan");
        classList.put(57, "Warsmith");
        classList.put(88, "Duelist");
        classList.put(89, "Dreadnought");
        classList.put(90, "Phoenix Knight");
        classList.put(91, "Hell Knight");
        classList.put(92, "Sagittarius");
        classList.put(93, "Adventurer");
        classList.put(94, "Archmage");
        classList.put(95, "Soultaker");
        classList.put(96, "Arcana Lord");
        classList.put(97, "Cardinal");
        classList.put(98, "Hierophant");
        classList.put(99, "Evas Templar");
        classList.put(100, "Sword Muse");
        classList.put(101, "Wind Rider");
        classList.put(102, "Moonlight Sentinel");
        classList.put(103, "Mystic Muse");
        classList.put(104, "Elemental Master");
        classList.put(105, "Evas Saint");
        classList.put(106, "Shillien Templar");
        classList.put(107, "Spectral Dancer");
        classList.put(108, "Ghost Hunter");
        classList.put(109, "Ghost Sentinel");
        classList.put(110, "Storm Screamer");
        classList.put(111, "Spectral Master");
        classList.put(112, "Shillien Saint");
        classList.put(113, "Titan");
        classList.put(114, "Grand Khavatari");
        classList.put(115, "Dominator");
        classList.put(116, "Doomcryer");
        classList.put(117, "Fortune Seeker");
        classList.put(118, "Maestro");
      
      return classList.get(classId);
	}
}