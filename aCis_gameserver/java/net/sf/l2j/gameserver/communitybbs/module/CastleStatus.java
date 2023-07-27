package net.sf.l2j.gameserver.communitybbs.module;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import net.sf.l2j.commons.pool.ConnectionPool;

public class CastleStatus
{
	protected static final Logger log = Logger.getLogger(CastleStatus.class.getName());

	private final StringBuilder html = new StringBuilder();

	public CastleStatus()
	{
		loadFromDB();
	}

	private void loadFromDB()
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			for (int i = 1; i < 9; i++)
			{
				try (PreparedStatement statement = con.prepareStatement("SELECT clan_name, clan_level FROM clan_data WHERE hasCastle=?"))
				{
					statement.setInt(1, i);
					ResultSet result = statement.executeQuery();
					try (PreparedStatement statement2 = con.prepareStatement("SELECT name, siegeDate, taxPercent FROM castle WHERE id=?"))
					{
						statement2.setInt(1, i);
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

								populateCastleHTML(name, owner, level, tax, sdf.format(anotherDate));
							}
						}
					}
				}
			}
		} catch (Exception e)
		{
			log.severe("There was an error when loading castle status: " + e.getMessage());
		}
	}

	private void populateCastleHTML(String name, String owner, int level, int tax, String siegeDate)
	{
		html.append("<table border=0 cellspacing=0 cellpadding=2 width=750>");
		html.append("<tr>");
		html.append("<td FIXWIDTH=10></td>");
		html.append("<td FIXWIDTH=100>").append(name).append(".</td>");
		html.append("<td FIXWIDTH=100>").append(owner).append(".</td>");
		html.append("<td FIXWIDTH=80>").append(level).append(".</td>");
		html.append("<td FIXWIDTH=40>").append(tax).append("</td>");
		html.append("<td FIXWIDTH=180>").append(siegeDate).append("</td>");
		html.append("<td FIXWIDTH=5></td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("<img src=\"L2UI.Squaregray\" width=\"740\" height=\"1\">");
	}

	public String getCastleListAsHTML()
	{
		return html.toString();
	}
}