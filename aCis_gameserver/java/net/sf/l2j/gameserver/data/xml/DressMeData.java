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
package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.data.xml.IXmlReader;

import net.sf.l2j.gameserver.model.DressMe;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

/**
 * @author ldeazevedo
 */
public class DressMeData implements IXmlReader
{
	private final List<DressMe> _entries = new ArrayList<>();
	
	public DressMeData()
	{
		load();
	}
	
	public void reload()
	{
		_entries.clear();
		load();
	}
	
	@Override
	public void load()
	{
		parseFile("./data/xml/skins/skins.xml");

		LOGGER.info("Loaded Skins templates.", _entries.size());
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{	
			forEach(listNode, "dressme", enchantNode ->
			{
				NamedNodeMap attrs = enchantNode.getAttributes();
				final int itemId = Integer.valueOf(attrs.getNamedItem("itemId").getNodeValue());
				final int chest = Integer.valueOf(attrs.getNamedItem("chest").getNodeValue());
				final int legs = Integer.valueOf(attrs.getNamedItem("legs").getNodeValue());
				final int hair = Integer.valueOf(attrs.getNamedItem("hair").getNodeValue());
				final int gloves = Integer.valueOf(attrs.getNamedItem("gloves").getNodeValue());
				final int feet = Integer.valueOf(attrs.getNamedItem("feet").getNodeValue());
				final boolean hairOn = Boolean.valueOf(attrs.getNamedItem("hairon").getNodeValue());
			
				_entries.add(new DressMe(itemId, chest, legs, hair, gloves, feet, hairOn));
			});
			
		});
	}
	
	public DressMe getItemId(int itemId)
	{
		return _entries.stream().filter(x -> x.getItemId() == itemId).findFirst().orElse(null);
	}
	
	public static DressMeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DressMeData INSTANCE = new DressMeData();
	}
}