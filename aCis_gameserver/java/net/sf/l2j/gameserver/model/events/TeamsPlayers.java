package net.sf.l2j.gameserver.model.events;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import net.sf.l2j.gameserver.model.actor.Player;

public class TeamsPlayers
{
	private static List<Player> registersPlayers = new ArrayList<>();
	private static Map<Integer, TeamsPlayers> teamsPlayers = new HashMap<>();
	private Player player1;
	private Player player2;
	private int idTeam;
	
	public int getIdTeam()
	{
		return idTeam;
	}
	
	public Player getPlayer1()
	{
		return player1;
	}
	
	public Player getPlayer2()
	{
		return player2;
	}
	
	public TeamsPlayers(int id, Player p1, Player p2)
	{
		idTeam = id;
		this.player1 = p1;
		this.player2 = p2;
	}
	
	public static void addIdTeams(int id, Player p1, Player p2)
	{
		TeamsPlayers tp = new TeamsPlayers(id, p1, p2);
		teamsPlayers.put(id, tp);
	}
	
	public static void addPlayer(Player player)
	{
		if (player != null)
			registersPlayers.add(player);
	}
	
	public static void removePlayer(Player player)
	{
		if (player != null)
			registersPlayers.remove(player);
	}
	
	public void cleanMe()
	{
		registersPlayers.clear();
		registersPlayers = new ArrayList<>();
	}
	
	public static boolean containsPlayer(Player player)
	{
		return player != null ? registersPlayers.contains(player) : false;
	}
	
	public static List<Player> registersPlayers()
	{
		return registersPlayers;
	}
	
	public int getRegPlayersCount()
	{
		return registersPlayers.size();
	}
	
	public int getTeamsCount()
	{
		return teamsPlayers.size();
	}
	
	public static Map<Integer, TeamsPlayers> getTeamsPlayers()
	{
		return teamsPlayers;
	}
}
