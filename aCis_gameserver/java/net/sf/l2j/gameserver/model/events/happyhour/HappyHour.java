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
package net.sf.l2j.gameserver.model.events.happyhour;

/**
 * @author Emmanuel
 */
public class HappyHour
{
	private int _day;
	private int _startHour;
	private int _endHour;
	
	/**
	 * @param day Dia en el cual funciona
	 * @param startHour Hora en el cual empieza
	 * @param endHour Hora en el cual termina
	 */
	public HappyHour(int day, int startHour, int endHour)
	{
		setDay(day);
		setStartHour(startHour);
		setEndHour(endHour);
	}
	
	/**
	 * Constructor que por defecto pone todos los dias, de 20 a 21 horas.
	 */
	public HappyHour()
	{
		setDay(-1);
		setStartHour(20);
		setEndHour(21);
	}
	
	public void setDay(int day)
	{
		_day = day;
	}
	
	public int getDay()
	{
		return _day;
	}
	
	public void setStartHour(int startHour)
	{
		_startHour = startHour;
	}
	
	public int getStartHour()
	{
		return _startHour;
	}
	
	public void setEndHour(int endHour)
	{
		_endHour = endHour;
	}
	
	public int getEndHour()
	{
		return _endHour;
	}
}
