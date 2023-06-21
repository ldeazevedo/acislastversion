package net.sf.l2j.gameserver.communitybbs.module;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.sf.l2j.commons.pool.ConnectionPool;

public class CastleStatus
{
	private StringBuilder CastleStatus = new StringBuilder();
	
	public CastleStatus()
	{
		loadFromDB();
	}
	
	@SuppressWarnings("resource")
	private void loadFromDB()
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			for (int i = 1; i < 9; i++)
			{
				PreparedStatement statement = con.prepareStatement("SELECT clan_name, clan_level FROM clan_data WHERE hasCastle=" + i + ";");
				ResultSet result = statement.executeQuery();
				
				PreparedStatement statement2 = con.prepareStatement("SELECT name, siegeDate, taxPercent FROM castle WHERE id=" + i + ";");
				ResultSet result2 = statement2.executeQuery();
				
				while (result.next())
				{
					String owner = result.getString("clan_name");
					int level = result.getInt("clan_level");
					
					while (result2.next())
					{
						String name = result2.getString("name");
						long someLong = result2.getLong("siegeDate");
						int tax = result2.getInt("taxPercent");
						Date anotherDate = new Date(someLong);
						String DATE_FORMAT = "dd-MMM-yyyy HH:mm";
						SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
						
						addCastleToList(name, owner, level, tax, sdf.format(anotherDate));
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void addCastleToList(String name, String owner, int level, int tax, String siegeDate)
	{
		this.CastleStatus.append("<table border=0 cellspacing=0 cellpadding=2 width=750>");
		this.CastleStatus.append("<tr>");
		this.CastleStatus.append("<td FIXWIDTH=10></td>");
		this.CastleStatus.append("<td FIXWIDTH=100>" + name + ".</td>");
		this.CastleStatus.append("<td FIXWIDTH=100>" + owner + ".</td>");
		this.CastleStatus.append("<td FIXWIDTH=80>" + level + ".</td>");
		this.CastleStatus.append("<td FIXWIDTH=40>" + tax + "</td>");
		this.CastleStatus.append("<td FIXWIDTH=180>" + siegeDate + "</td>");
		this.CastleStatus.append("<td FIXWIDTH=5></td>");
		this.CastleStatus.append("</tr>");
		this.CastleStatus.append("</table>");
		this.CastleStatus.append("<img src=\"L2UI.Squaregray\" width=\"740\" height=\"1\">");
	}
	
	public String loadCastleList()
	{
		return this.CastleStatus.toString();
	}
}