package net.sf.l2j.gameserver.model.actor.instance;

import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import net.sf.l2j.commons.data.Pagination;
import net.sf.l2j.commons.util.HtmHelper;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.cache.HtmCache;
import net.sf.l2j.gameserver.data.xml.SchemeBufferData;
import net.sf.l2j.gameserver.data.xml.SkillsIconsData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.buffer.BuffCategory;
import net.sf.l2j.gameserver.model.buffer.ParamValidation;
import net.sf.l2j.gameserver.model.buffer.RestoreType;
import net.sf.l2j.gameserver.model.buffer.SchemeBufferManager;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.session.HtmSession.CachePage;
import net.sf.l2j.gameserver.session.ParamsCache;
import net.sf.l2j.gameserver.session.SessionManager;

public class BuffsMaster extends Folk
{
	private static final String Param_TargetIsPlayer = "Param_TargetIsPlayer";
	private static final String Param_IsEditingName = "Param_IsEditingName";
	
	private static final String Param_SelectedScheme = "Param_SelectedScheme";
	private static final String Param_RequestRemoveScheme = "Param_RequestRemoveScheme";

	private static final String Param_BuffsPage = "Param_BuffsPage";
	private static final String Param_BuffsOrderPage = "Param_BuffsOrderPage";
	
	private static final String Param_NewSchemeIcon = "Param_NewSchemeIcon";
	private static final String Param_NewSchemeName = "Param_NewSchemeName";
	
	
	private static final String Param_CopySchemeBuffs = "Param_CopySchemeBuffs";
	private static final int Param_CopyWarriorBuffs = 1;
	private static final int Param_CopyMysticBuffs = 2;
	private static final int Param_CopyHealerBuffs = 3;
	private static final int Param_CopyTankerBuffs = 4;
	
	
	private static final String Param_SelectedBuff = "Param_SelectedBuff";
	private static final String Param_SelectedOrderBuff = "Param_SelectedOrderBuff";
	private static final String Param_SelectedBuffsTab = "Param_SelectedBuffsTab";
	
	public BuffsMaster(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	private static ParamsCache getSession(Player activeChar)
	{
		return SessionManager.getInstance().get(activeChar.getObjectId()).get(CachePage.BUFFER_NPC);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		StringTokenizer st = new StringTokenizer(command, ";");
		String currentCommand = st.nextToken();
		
		
		if (currentCommand.startsWith("main"))
		{
			getSession(player).addInt(Param_SelectedOrderBuff, 0);
			showMainWindow(player);
		}
		else if (currentCommand.startsWith("info"))
		{
			showInfoWindow(player);
		}
		else if (currentCommand.startsWith("givenoblesse"))
		{
			var isBuffingPlayer = getSession(player).getBoolean(Param_TargetIsPlayer, true);
			var result = SchemeBufferManager.getInstance().useBuff(player, isBuffingPlayer, 1323);
			if (result.hasError())
			{
				player.sendMessage(result.getError()); 
			}
			showMainWindow(player);
		}
		else if (currentCommand.startsWith("giveberserk"))
		{
			var isBuffingPlayer = getSession(player).getBoolean(Param_TargetIsPlayer, true);
			var result = SchemeBufferManager.getInstance().useBuff(player, isBuffingPlayer, 1062);
			if (result.hasError())
			{
				player.sendMessage(result.getError()); 
			}
			showMainWindow(player);
		}
		else if (currentCommand.startsWith("editscheme2"))
		{
			var selectedSchemeResult = HtmHelper.getNextInt(st);
			if (selectedSchemeResult.isValid())
			{
				getSession(player).addInt(Param_SelectedScheme, selectedSchemeResult.getValue());
			}
			editSchemeWizard2(player);
		}		
		else if (currentCommand.startsWith("editscheme"))
		{
			var selectedSchemeResult = HtmHelper.getNextInt(st);
			if (selectedSchemeResult.isValid())
			{
				getSession(player).addInt(Param_SelectedScheme, selectedSchemeResult.getValue());
			}
			editSchemeWizard1(player);
		}
		else if (currentCommand.startsWith("acceptcode"))
		{
			var codeResult = HtmHelper.getNextString(st);
			if (codeResult.isValid())
			{
				var result = SchemeBufferManager.getInstance().useShareCode(player, codeResult.getValue());
				if (result.hasError())
				{
					player.sendMessage(result.getError()); 
				}
			}
			showManageWindow(player);
		}
		else if (currentCommand.startsWith("resetcode")) 
		{
			var schemeId = getSession(player).getInt(Param_SelectedScheme,-1);
			var result = SchemeBufferManager.getInstance().resetShareCode(player, schemeId);
			if (result.hasError())
			{
				player.sendMessage(result.getError()); 
			}
			shareSchemeWindow(player);
		}
		else if (currentCommand.startsWith("swaplockcode"))
		{
			var schemeId = getSession(player).getInt(Param_SelectedScheme,-1);
			var result = SchemeBufferManager.getInstance().swapSchemeLockShareCode(player, schemeId);
			if (result.hasError())
			{
				player.sendMessage(result.getError()); 
			}
			shareSchemeWindow(player);
		}
		else if (currentCommand.startsWith("manage"))
		{
			getSession(player).addInt(Param_SelectedOrderBuff, 0);
			showManageWindow(player);
		}
		else if (currentCommand.startsWith("newscheme3"))
		{
			var result = SchemeBufferManager.getInstance().validateIcon(player, getSession(player).getInt(Param_NewSchemeIcon));
			if (result.hasError()) 
			{
				player.sendMessage(result.getError());
				addSchemeWizard2(player);
			}	
			else
			{
				addSchemeWizard3(player);
			}
		}
		else if (currentCommand.startsWith("newscheme2"))
		{
			var schemeNameResult = HtmHelper.getNextString(st);
			if (schemeNameResult.isValid())
			{
				
				getSession(player).addString(Param_NewSchemeName, schemeNameResult.getValue());
				addSchemeWizard2(player);
			}
			else 
			{
				player.sendMessage("Scheme name is incorrect.");
				addSchemeWizard1(player);
			}
		}
		else if (currentCommand.startsWith("newscheme"))
		{
			getSession(player).addInt(Param_SelectedScheme,-1);
			getSession(player).addInt(Param_NewSchemeIcon,-1);
			getSession(player).addString(Param_NewSchemeName, null);
			getSession(player).addInt(Param_CopySchemeBuffs, 0);
			
			var result = SchemeBufferManager.getInstance().canAddScheme(player);
			if (result.hasError())
			{
				player.sendMessage(result.getError());
				showManageWindow(player);
			}
			else
			{
				addSchemeWizard1(player);
			}
		}
		else if (currentCommand.startsWith("switchtarget"))
		{
			var isPlayer = getSession(player).getBoolean(Param_TargetIsPlayer, true);
			
			var result = SchemeBufferManager.getInstance().switchTarget(player, isPlayer);
			if (result.hasError())
			{
				player.sendMessage(result.getError());
			}
			else
			{
				getSession(player).addBoolean(Param_TargetIsPlayer, !isPlayer);	
				player.sendMessage(result.getInfo());
			}
			
			showMainWindow(player);
		}
		else if (currentCommand.startsWith("restore"))
		{
			var actionRestult = HtmHelper.getNextString(st);
			if (actionRestult.isValid())
			{
				var result = performRestoring(player, RestoreType.GetByName(actionRestult.getValue()));
				if (result.hasError())
				{
					player.sendMessage(result.getError());
				}
			}
			showMainWindow(player);
		}
		else if (currentCommand.startsWith("autoorder")) 
		{
			var schemeId = getSession(player).getInt(Param_SelectedScheme,-1);
			SchemeBufferManager.getInstance().autoOrder(player, schemeId);
			showSchemeOrderBuffsWindow(player);
		}
		else if (currentCommand.startsWith("copyscheme"))
		{
			var actionRestult = HtmHelper.getNextInt(st);
			if (actionRestult.isValid())
			{
				var copyFrom = actionRestult.getValue();
				if (getSession(player).getInt(Param_CopySchemeBuffs,0) == copyFrom)
					copyFrom = 0;
				
				getSession(player).addInt(Param_CopySchemeBuffs, copyFrom);
			}
			
			addSchemeWizard3(player);
		}
		else if (currentCommand.startsWith("sharescheme"))
		{
			var selectedSchemeResult = HtmHelper.getNextInt(st);
			if (selectedSchemeResult.isValid())
			{
				getSession(player).addInt(Param_SelectedScheme, selectedSchemeResult.getValue());
			}
			shareSchemeWindow(player);
		}		
		else if (currentCommand.startsWith("removebuff"))
		{
			var selectedBuff = getSession(player).getInt(Param_SelectedOrderBuff, -1);
			var schemeId = getSession(player).getInt(Param_SelectedScheme, -1);
			SchemeBufferManager.getInstance().addOrRemoveBuffInScheme(player, schemeId, selectedBuff);
			showSchemeOrderBuffsWindow(player);
		}		
		
		else if (currentCommand.startsWith("selecticon")) 
		{
			int schemeId = 0;
			var selectedIconResult = HtmHelper.getNextInt(st);
			if (selectedIconResult.isValid())
			{
				schemeId = getSession(player).getInt(Param_SelectedScheme,-1);
				if (schemeId > 0)
				{
					var result = SchemeBufferManager.getInstance().changeSchemeIcon(player, schemeId, selectedIconResult.getValue());
					if (result.hasError())
					{
						player.sendMessage(result.getError());
					}
					
				}
				//no schemeId = we are creating new scheme
				else 
				{
					getSession(player).addInt(Param_NewSchemeIcon, selectedIconResult.getValue());	
				}
			}
			
			if (schemeId > 0)
			{
				editSchemeWizard2(player);
			}
			else
			{
				addSchemeWizard2(player);
			}
		}
		else if (currentCommand.startsWith("selectbuff")) 
		{
			var selectedBuffResult = HtmHelper.getNextInt(st);
			if (selectedBuffResult.isValid())
			{
				getSession(player).addInt(Param_SelectedBuff, selectedBuffResult.getValue());
				var result = addOrRemoveBuffInSchme(player);
				if (result.hasError())
				{
					player.sendMessage(result.getError());
				}
			}
			showSchemeBuffsWindow(player);
		}
		else if (currentCommand.startsWith("selectorderbuff")) 
		{
			var selectedIconResult = HtmHelper.getNextInt(st);
			if (selectedIconResult.isValid())
			{
				getSession(player).addInt(Param_SelectedOrderBuff, selectedIconResult.getValue());
			}
			showSchemeOrderBuffsWindow(player);
		}
		else if (currentCommand.startsWith("addrestore"))
		{
			var selectedRestoreRes = HtmHelper.getNextString(st);
			if (selectedRestoreRes.isValid())
			{
				var schemeId = getSession(player).getInt(Param_SelectedScheme);
				var restoreType = RestoreType.GetByName(selectedRestoreRes.getValue());
				if (restoreType != null)
				{
					var result = SchemeBufferManager.getInstance().addOrRemoveRestoreInScheme(player, schemeId, restoreType);
					if (result.hasError())
					{
						player.sendMessage(result.getError());
					}
				}
			}
			showSchemeBuffsWindow(player);
		}
		else if (currentCommand.startsWith("buffspage"))
		{
			var selectedPageResult = HtmHelper.getNextInt(st);
			if (selectedPageResult.isValid())
			{
				getSession(player).addInt(Param_BuffsPage, selectedPageResult.getValue());
			}
			showSchemeBuffsWindow(player);
		}
		else if (currentCommand.startsWith("buffsorderpage"))
		{
			var selectedPageResult = HtmHelper.getNextInt(st);
			if (selectedPageResult.isValid())
			{
				getSession(player).addInt(Param_BuffsOrderPage, selectedPageResult.getValue());
			}
			showSchemeOrderBuffsWindow(player);
		}
		else if (currentCommand.startsWith("buffstab"))
		{
			var selectedTabResult = HtmHelper.getNextInt(st);
			if (selectedTabResult.isValid())
			{
				getSession(player).addInt(Param_SelectedBuffsTab, selectedTabResult.getValue());
				getSession(player).addInt(Param_SelectedBuff, 0);
				getSession(player).addInt(Param_BuffsPage, 1);
				
			}
			showSchemeBuffsWindow(player);
		}
		else if (currentCommand.startsWith("orderback"))
		{
			var selectedScheme = getSession(player).getInt(Param_SelectedScheme, 0);
			var selectedBuff = getSession(player).getInt(Param_SelectedOrderBuff, 0);
			var result = SchemeBufferManager.getInstance().changeBuffOrder(player, selectedScheme, selectedBuff, true);
			if (result.hasError())
			{
				player.sendMessage(result.getError());
			}
			
			showSchemeOrderBuffsWindow(player);
		}
		else if (currentCommand.startsWith("orderfore"))
		{
			var selectedScheme = getSession(player).getInt(Param_SelectedScheme, 0);
			var selectedBuff = getSession(player).getInt(Param_SelectedOrderBuff, 0);
			var result = SchemeBufferManager.getInstance().changeBuffOrder(player, selectedScheme, selectedBuff, false);
			if (result.hasError())
			{
				player.sendMessage(result.getError());
			}
			showSchemeOrderBuffsWindow(player);
		}
		
		else if (currentCommand.startsWith("addscheme"))
		{
			var selectedIcon = getSession(player).getInt(Param_NewSchemeIcon);
			var schemeName = getSession(player).getString(Param_NewSchemeName);
			var copyFrom = getSession(player).getInt(Param_CopySchemeBuffs, 0);
			
			var result = SchemeBufferManager.getInstance().addNewScheme(player, schemeName, selectedIcon, copyFrom);
			if (result.hasError())
			{
				player.sendMessage(result.getError());
				addSchemeWizard3(player);
			}
			else
			{
				showManageWindow(player);
			}
		}
		else if (currentCommand.startsWith("modifyscheme"))
		{
			var selectedSchemeResult = HtmHelper.getNextInt(st);
			if (selectedSchemeResult.isValid())
			{
				getSession(player).addInt(Param_SelectedScheme, selectedSchemeResult.getValue());
			}
			
			showSchemeBuffsWindow(player);
		}
		else if (currentCommand.startsWith("orderbuffs"))
		{
			var selectedSchemeResult = HtmHelper.getNextInt(st);
			if (selectedSchemeResult.isValid())
			{
				getSession(player).addInt(Param_SelectedScheme, selectedSchemeResult.getValue());
			}
			
			showSchemeOrderBuffsWindow(player);
		}
		
		else if (currentCommand.startsWith("askremovescheme"))
		{
			var selectedSchemeResult = HtmHelper.getNextInt(st);
			if (selectedSchemeResult.isValid())
			{
				getSession(player).addInt(Param_RequestRemoveScheme, selectedSchemeResult.getValue());
			}
			
			showManageWindow(player);
		}
		
		else if (currentCommand.startsWith("cancelremovescheme"))
		{
			getSession(player).addInt(Param_RequestRemoveScheme, -1);
			showManageWindow(player);
		}
		else if (currentCommand.startsWith("removescheme"))
		{
			var schemeToRemove = getSession(player).getInt(Param_RequestRemoveScheme, -1);
			var result = SchemeBufferManager.getInstance().deleteScheme(player, schemeToRemove);
			if (result.hasError())
			{
				player.sendMessage(result.getError());
			}
			getSession(player).addInt(Param_RequestRemoveScheme, -1);
			showManageWindow(player);
		}
		else if (currentCommand.startsWith("editname"))
		{
			getSession(player).addBoolean(Param_IsEditingName, true);
			
			var selectedSchemeId = getSession(player).getInt(Param_SelectedScheme, 0);
			if (selectedSchemeId > 0)
			{
				editSchemeWizard1(player);
			}
		}
		else if (currentCommand.startsWith("acceptname"))
		{
			int selectedSchemeId  =  getSession(player).getInt(Param_SelectedScheme, 0);
			getSession(player).addBoolean(Param_IsEditingName, false);
			
			var schemeNameResult = HtmHelper.getNextString(st);
			if (schemeNameResult.isValid())
			{
				if (selectedSchemeId > 0)
				{
					var result = SchemeBufferManager.getInstance().changeSchemeName(player, selectedSchemeId, schemeNameResult.getValue());
					if (result.hasError())
					{
						player.sendMessage(result.getError());
					}
				}
				else
				{
					getSession(player).addString(Param_NewSchemeName, schemeNameResult.getValue());
				}
			}
			
			if (selectedSchemeId > 0)
			{
				editSchemeWizard1(player);
			}
		}
		else if (currentCommand.startsWith("usescheme"))
		{
			var selectedSchemeResult = HtmHelper.getNextInt(st);
			if (selectedSchemeResult.isValid())
			{
				var isBuffingPlayer = getSession(player).getBoolean(Param_TargetIsPlayer, true);
				var result = SchemeBufferManager.getInstance().useScheme(player, isBuffingPlayer, selectedSchemeResult.getValue());
				if (result.hasError())
				{
					player.sendMessage(result.getError());
				}
			}
			showMainWindow(player);
		}
		else if (currentCommand.startsWith("use_warrior"))
		{
			var isBuffingPlayer = getSession(player).getBoolean(Param_TargetIsPlayer, true);
			var result = SchemeBufferManager.getInstance().useWarriorScheme(player, isBuffingPlayer);
			if (result.hasError())
			{
				player.sendMessage(result.getError());
			}
			showMainWindow(player);
		}
		else if (currentCommand.startsWith("use_mystic"))
		{
			var isBuffingPlayer = getSession(player).getBoolean(Param_TargetIsPlayer, true);
			var result = SchemeBufferManager.getInstance().useMysticScheme(player, isBuffingPlayer);
			if (result.hasError())
			{
				player.sendMessage(result.getError());
			}
			showMainWindow(player);
		}
		else if (currentCommand.startsWith("clearbuffs"))
		{
			var isBuffingPlayer = getSession(player).getBoolean(Param_TargetIsPlayer, true);
			var result = SchemeBufferManager.getInstance().cancelBuffs(player, isBuffingPlayer);
			if (result.hasError())
			{
				player.sendMessage(result.getError());
			}
			showMainWindow(player);
		}
		
		else 
		{
			super.onBypassFeedback(player, command);	
		}
	}
	
	private static ParamValidation performRestoring(Player player, RestoreType restore)
	{
		var result = new ParamValidation();
		
		if (restore == null) 
		{
			result.setError("Incorrect action.");
			return result;
		}
		
		var isPlayer = getSession(player).getBoolean(Param_TargetIsPlayer, true);
		
		if (restore == RestoreType.HP)
			return SchemeBufferManager.getInstance().restoreHP(player, isPlayer);
		
		if (restore == RestoreType.MP)
			return SchemeBufferManager.getInstance().restoreMP(player, isPlayer);
		
		if (restore == RestoreType.CP)
			return SchemeBufferManager.getInstance().restoreCP(player, isPlayer);
		
		if (restore == RestoreType.ALL)
			return SchemeBufferManager.getInstance().restoreAll(player, isPlayer);
		
		return result;		
	}
	
	private static ParamValidation addOrRemoveBuffInSchme(Player player)
	{
		var buffId = getSession(player).getInt(Param_SelectedBuff);
		var schemeId = getSession(player).getInt(Param_SelectedScheme, 0);
		return SchemeBufferManager.getInstance().addOrRemoveBuffInScheme(player, schemeId, buffId);
	}
	
	@Override
	public void showChatWindow(Player player, int val)
	{
		showMainWindow(player);
	}
	
	private void showInfoWindow(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath("page-info"));
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	private void showMainWindow(Player player)
	{
		
		//player.sendPacket(new TutorialShowHtml(HtmCache.getInstance().getHtmForce("data/html/mods/buffs_master/tutorial_09.htm")));
		
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath("page"));
		
		
		var schemes = SchemeBufferManager.getInstance().getSchemesForPlayer(player.getObjectId());
		
		var schemeTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-scheme_main"));
		var schemeEmptyTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-scheme_main-empty"));
		var sb = new StringBuilder();
		var schemesIds = schemes.keySet().stream().collect(Collectors.toList());
		
		var paymentItemName = SchemeBufferManager.getInstance().getPaymentItemName();
		
		for (int i = 0; i < SchemeBufferManager.MAX_SCHEMES ; i ++)
		{
			if (schemesIds.size() > i)
			{
				var schemeId = schemesIds.get(i);
				var scheme = schemes.get(schemeId);
				
				var icon = "Icon.NOIMAGE";
				
				if (scheme.getIconId() > 0)
				{
					icon = SchemeBufferData.getInstance().getIcons().get(scheme.getIconId());
				}
				
				var schemeHtml = schemeTemplate;
				schemeHtml = schemeHtml.replace("%name%", scheme.getNameForUI());
				schemeHtml = schemeHtml.replace("%schemeId%", Integer.toString(schemeId));
				schemeHtml = schemeHtml.replace("%icon%", icon);
				schemeHtml = schemeHtml.replace("%price%", scheme.getPrice() + " " + paymentItemName +"(s)");
				
				sb.append(schemeHtml);
			}
			else
			{
				var schemeHtml = schemeEmptyTemplate;
				sb.append(schemeHtml);
			}
		}
		
		var isPlayer = getSession(player).getBoolean(Param_TargetIsPlayer, true);
		html.replace("%target%", isPlayer ? player.getName() : "summon");
		
		html.replace("%warrior_price%", SchemeBufferData.getInstance().getWarriorSchemePrice() + " " + paymentItemName + "(s)");
		html.replace("%mystic_price%", SchemeBufferData.getInstance().getMysticSchemePrice() + " " + paymentItemName + "(s)");
		
		html.replace("%schemes%", sb.toString());
		html.replace("%objectId%", getObjectId());
		showMessage(player,html.getLength());
		player.sendPacket(html);
	}
	
	private void showSchemeOrderBuffsWindow(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath("page-buffs_order"));
		
		int buffsPerPage = 18;
		
		var page = getSession(player).getInt(Param_BuffsOrderPage, 1);
		
		var selectedScheme = getSession(player).getInt(Param_SelectedScheme, 0);
		var schemeEntity = SchemeBufferManager.getInstance().getSchemesForPlayer(player.getObjectId()).get(selectedScheme);
		List<Integer> sortedBuffs = schemeEntity.getBuffsSorted(); 
		
		final Pagination<Integer> pageItems = new Pagination<>(sortedBuffs.stream(), page, buffsPerPage);
		if (pageItems.getTotal() < page)
			page = 1;
		
		var buffTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-buff_reorder"));
		var buffTemplateSelected = HtmCache.getInstance().getHtm(getHtmlPath("template-buff_selected"));
		var buffEmptyTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-buff_reorder-empty"));

		
		var selectedBuff = getSession(player).getInt(Param_SelectedOrderBuff,0);
		
		var sb = new StringBuilder();
			
		int index = 0;
		
		for (int i = 0; i < buffsPerPage; i++)
		{
			if (index % 6 == 0)
			{
				sb.append("</tr><tr>");
			}

			
			if (pageItems.size() > i)
			{
				var buffId = pageItems.get(i);
				var bufIcon = SkillsIconsData.getInstance().getIcon(buffId);
				var iconHtml =  Integer.compare(selectedBuff, buffId) == 0 ? buffTemplateSelected : buffTemplate;
				iconHtml = iconHtml.replace("%icon%", bufIcon);
				iconHtml = iconHtml.replace("%buffId%", Integer.toString(buffId));
				sb.append(iconHtml);
			}
			else
			{
				sb.append(buffEmptyTemplate);
			}
			index++;
		}
		
		
		var pager = generatePager(page, pageItems.getTotal(), "bypass npc_" + getObjectId() + "_buffsorderpage", true);
		html.replace("%pager%", pager);
		

		var selBuffInfo = SchemeBufferData.getInstance().getBuff(selectedBuff);
		
		if (selBuffInfo == null)
		{
			html.replace("%details%", "<img height=21>");
		}
		else
		{
			var detailsTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-buff-order-details"));
			detailsTemplate = detailsTemplate.replace("%name%",selBuffInfo.getSkill().getName());	
			html.replace("%details%", detailsTemplate);
		}
		
		
		html.replace("%buffs%", sb.toString());
		html.replace("%objectId%", getObjectId());
		showMessage(player,html.getLength());
		player.sendPacket(html);
	}
	
	private void showSchemeBuffsWindow(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath("page-schemebuffs"));
		
		
		int buffsPerPage = 6;
		
		var intCategory = getSession(player).getInt(Param_SelectedBuffsTab, 1);
		var category = BuffCategory.GetById(intCategory);
	
		
		var selectedScheme = getSession(player).getInt(Param_SelectedScheme, 0);
		var schemeEntity = SchemeBufferManager.getInstance().getSchemesForPlayer(player.getObjectId()).get(selectedScheme);
		var schemeBuffs = SchemeBufferManager.getInstance().getSchemeBuffs(player.getObjectId(),selectedScheme);
		
		var page = getSession(player).getInt(Param_BuffsPage, 1);
		
		var buffsInCategory = SchemeBufferData.getInstance().getAvailableBuffsByCategory(category);
		final Pagination<Integer> pageItems = new Pagination<>(buffsInCategory.stream(), page, buffsPerPage);
		
		var buffTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-bufficon"));
		var buffTemplateSelected = HtmCache.getInstance().getHtm(getHtmlPath("template-bufficon_selected"));
		var buffEmptyTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-bufficon-empty"));
		
		var sb = new StringBuilder();
		
		for (int i = 0; i < buffsPerPage; i++)
		{
			if (pageItems.size() > i)
			{
				var buffId = pageItems.get(i);
				var bufIcon = SkillsIconsData.getInstance().getIcon(buffId);
				var buffInfo = SchemeBufferData.getInstance().getBuff(buffId);
				
				var iconHtml =  schemeBuffs.containsKey(buffId) ? buffTemplateSelected : buffTemplate;
				iconHtml = iconHtml.replace("%icon%", bufIcon);
				iconHtml = iconHtml.replace("%name%", buffInfo.getSkill().getName());
				iconHtml = iconHtml.replace("%desc%", buffInfo.getDescription());
				iconHtml = iconHtml.replace("%buffId%", Integer.toString(buffId));
				iconHtml = iconHtml.replace("%color%", schemeBuffs.containsKey(buffId) ? "FF0000" : "000000");
				
				sb.append(iconHtml);
			}
			else
			{
				sb.append(buffEmptyTemplate);
			}
		}

		var payItemName = SchemeBufferManager.getInstance().getPaymentItemName();
		var schemeIcon = "";
		if (SchemeBufferData.getInstance().getIcons().containsKey(schemeEntity.getIconId()))
		{
			schemeIcon = SchemeBufferData.getInstance().getIcons().get(schemeEntity.getIconId());
		}
		
		html.replace("%scheme_icon%", schemeIcon);
		
		
		var pager = generatePager(page, pageItems.getTotal(), "bypass npc_" + getObjectId() + "_buffspage", false);
		html.replace("%pager%", pager);
		
		
		html.replace("%tab_icon1%", category == BuffCategory.Buffs ? "L2UI_CH3.smallbutton1_down" : "L2UI_CH3.smallbutton1");
		html.replace("%tab_icon2%", category == BuffCategory.Dances ? "L2UI_CH3.smallbutton1_down" : "L2UI_CH3.smallbutton1");
		html.replace("%tab_icon3%", category == BuffCategory.Songs ? "L2UI_CH3.smallbutton1_down" : "L2UI_CH3.smallbutton1");
		html.replace("%tab_icon4%", category == BuffCategory.Chants ? "L2UI_CH3.smallbutton1_down" : "L2UI_CH3.smallbutton1");
		html.replace("%tab_icon5%", category == BuffCategory.Other ? "L2UI_CH3.smallbutton1_down" : "L2UI_CH3.smallbutton1");
		
		
		html.replace("%cp_checkbox%", schemeEntity.getRestoreCp() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
		html.replace("%hp_checkbox%", schemeEntity.getRestoreHp() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
		html.replace("%mp_checkbox%", schemeEntity.getRestoreMp() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
		html.replace("%total_price%", schemeEntity.getPrice() + " " + payItemName+"(s)");
		html.replace("%count%", schemeBuffs.size() + "/" + Config.BUFFS_MASTER_MAX_SKILLS_PER_SCHEME);

		
		html.replace("%buffs%", sb.toString());
		html.replace("%objectId%", getObjectId());
		showMessage(player,html.getLength());
		player.sendPacket(html);
	}
	
	private void showManageWindow(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath("page-manage"));
		
		var schemes = SchemeBufferManager.getInstance().getSchemesForPlayer(player.getObjectId());
		
		var schemeTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-scheme"));
		var schemeEmptyTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-scheme-empty"));
		var schemeDeleteTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-scheme-delete"));
		var sb = new StringBuilder();
		
		var schemeToRemove = getSession(player).getInt(Param_RequestRemoveScheme, -1);
		
		var paymentItemName = SchemeBufferManager.getInstance().getPaymentItemName();
		var schemesIds = schemes.keySet().stream().collect(Collectors.toList());
		
		
		for (int i = 0; i < SchemeBufferManager.MAX_SCHEMES; i ++)
		{
			if (schemesIds.size() > i)
			{
				var schemeId = schemesIds.get(i);
				var scheme = schemes.get(schemeId);
				
				var icon = "Icon.NOIMAGE";
				
				if (scheme.getIconId() > 0)
				{
					icon = SchemeBufferData.getInstance().getIcons().get(scheme.getIconId());
				}
				
				if (schemeToRemove == schemeId)
				{
					var schemeHtml = schemeDeleteTemplate;
					schemeHtml = schemeHtml.replace("%icon%", icon);
					sb.append(schemeHtml);
				}
				else
				{
					var schemeHtml = schemeTemplate;
					schemeHtml = schemeHtml.replace("%name%", scheme.getNameForUI());
					schemeHtml = schemeHtml.replace("%schemeId%", Integer.toString(schemeId));
					schemeHtml = schemeHtml.replace("%icon%", icon);
					schemeHtml = schemeHtml.replace("%price%", scheme.getPrice() + " " + paymentItemName + "(s)");
					
					sb.append(schemeHtml);
				}
			}
			else
			{
				var schemeHtml = schemeEmptyTemplate;
				sb.append(schemeHtml);
			}
			
		}

		html.replace("%schemes_count%", schemes.size() + "/" + SchemeBufferManager.MAX_SCHEMES);
		html.replace("%schemes%", sb.toString());
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
		
	}
	
	private void shareSchemeWindow(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath("page-display_code"));
		
		var selectedScheme = getSession(player).getInt(Param_SelectedScheme, 0);
		
		var schemeEntity = SchemeBufferManager.getInstance().getSchemesForPlayer(player.getObjectId()).get(selectedScheme);
		html.replace("%code%", schemeEntity.getShareCode());
		
		html.replace("%lock_icon%", schemeEntity.isShareCodeLocked() ? "L2UI_CH3.joypad_lock" : "L2UI_CH3.joypad_unlock");
		
		
		
		html.replace("%objectId%", getObjectId());
		player.sendPacket(html);
	}
	
	
	private void editSchemeWizard1(Player player) 
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath("page-editscheme-1"));
		
		
		var selectedScheme = getSession(player).getInt(Param_SelectedScheme, 0);
		var schemeEntity = SchemeBufferManager.getInstance().getSchemesForPlayer(player.getObjectId()).get(selectedScheme);
		
		var isEditingName = getSession(player).getBoolean(Param_IsEditingName, false);
		
		
		var nameDisplayTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-name-display"));
		var nameEditTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-name-edit"));
		
		var nameControl = "";
		if (isEditingName)
		{
			nameControl = nameEditTemplate;
		}
		else 
		{
			nameControl = nameDisplayTemplate;
			nameControl = nameControl.replace("%scheme_name%", schemeEntity.getNameForUI());
		}
		
		html.replace("%edit_name%", nameControl);
		
		html.replace("%objectId%", getObjectId());
		showMessage(player,html.getLength());
		player.sendPacket(html);
	}
	
	private void editSchemeWizard2(Player player) 
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath("page-editscheme-2"));
		
		var selectedScheme = getSession(player).getInt(Param_SelectedScheme, 0);
		var schemeEntity = SchemeBufferManager.getInstance().getSchemesForPlayer(player.getObjectId()).get(selectedScheme);
		
		var iconTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-schemeicon"));
		var iconTemplateSelected = HtmCache.getInstance().getHtm(getHtmlPath("template-schemeicon_selected"));
		//Amount of Icons is static and equals 24
		int iconsPerPage = 24;
		var availableIcons = SchemeBufferData.getInstance().getIcons();
		var sb = new StringBuilder();
		
		int index = 0;
		
		var selectedIcon = schemeEntity.getIconId();
		
		for (var iconId : availableIcons.keySet())
		{
			if (index > 0 && index < iconsPerPage && index % 6 == 0)
			{
				sb.append("</tr><tr>");
			}
			if (index % 6 == 0)
			{
				sb.append("<td><img width=1></td>");
			}
			
			String iconHtml = null;
			var schemeIcon = availableIcons.get(iconId);
			
			if (selectedIcon == iconId)
			{
				iconHtml = iconTemplateSelected;
				iconHtml = iconHtml.replace("%icon%", schemeIcon);
				iconHtml = iconHtml.replace("%iconId%", Integer.toString(iconId));
			}
			else
			{
				iconHtml = iconTemplate;
				iconHtml = iconHtml.replace("%icon%", schemeIcon);
				iconHtml = iconHtml.replace("%iconId%", Integer.toString(iconId));
			}
			
			sb.append(iconHtml);
			
			if (index % 6 != 5 && index < iconsPerPage)
			{
				sb.append("<td width=8></td>");
			}
			
			index++;
		}
		
		html.replace("%icons%", sb.toString());
		
		html.replace("%objectId%", getObjectId());
		showMessage(player,html.getLength());
		player.sendPacket(html);
	}
	
	
	private void addSchemeWizard1(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath("page-addscheme-1"));
		html.replace("%objectId%", getObjectId());
		showMessage(player,html.getLength());
		player.sendPacket(html);
	}
	
	private void addSchemeWizard2(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath("page-addscheme-2"));
		
		var iconTemplate = HtmCache.getInstance().getHtm(getHtmlPath("template-schemeicon"));
		var iconTemplateSelected = HtmCache.getInstance().getHtm(getHtmlPath("template-schemeicon_selected"));
		
		//Amount of Icons is static and equals 24
		int iconsPerPage = 24;
		var availableIcons = SchemeBufferData.getInstance().getIcons();
		var sb = new StringBuilder();
		
		int index = 0;
		
		var selectedIcon = getSession(player).getInt(Param_NewSchemeIcon);
		
		for (var iconId : availableIcons.keySet())
		{
			if (index > 0 && index < iconsPerPage && index % 6 == 0)
			{
				sb.append("</tr><tr>");
			}
			if (index % 6 == 0)
			{
				sb.append("<td><img width=1></td>");
			}
			
			String iconHtml = null;
			var schemeIcon = availableIcons.get(iconId);
			
			if (selectedIcon == iconId)
			{
				iconHtml = iconTemplateSelected;
				iconHtml = iconHtml.replace("%icon%", schemeIcon);
				iconHtml = iconHtml.replace("%iconId%", Integer.toString(iconId));
			}
			else
			{
				iconHtml = iconTemplate;
				iconHtml = iconHtml.replace("%icon%", schemeIcon);
				iconHtml = iconHtml.replace("%iconId%", Integer.toString(iconId));
			}
			
			sb.append(iconHtml);
			
			if (index % 6 != 5 && index < iconsPerPage)
			{
				sb.append("<td width=8></td>");
			}
			
			index++;
		}
		
		html.replace("%icons%", sb.toString());
		
		html.replace("%objectId%", getObjectId());
		showMessage(player,html.getLength());
		player.sendPacket(html);
	}
	
	private void addSchemeWizard3(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(getHtmlPath("page-addscheme-3"));
		
		
		var copyfrom = getSession(player).getInt(Param_CopySchemeBuffs,0);
		
		
		html.replace("%warrior_checkbox%", copyfrom == Param_CopyWarriorBuffs ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
		html.replace("%mystic_checkbox%", copyfrom == Param_CopyMysticBuffs ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
		html.replace("%healer_checkbox%", copyfrom == Param_CopyHealerBuffs ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
		html.replace("%tanker_checkbox%", copyfrom == Param_CopyTankerBuffs ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
		
		html.replace("%objectId%", getObjectId());
		showMessage(player,html.getLength());
		player.sendPacket(html);
	}
	
	//isOrderPage = true = just add few pixels so html looks better :)
	public String generatePager(int page, int maxPages, String bypass, boolean isOrderPage)
	{
		if (maxPages < 1)
			maxPages = 1;

		StringBuilder sb = new StringBuilder();
		sb.append("<table cellpadding=0 cellspacing=0><tr>");
		
		int witdth = 104 + (isOrderPage ? 3 : 0);
		
		if (page == 1)
			sb.append("<td width=" +witdth +" align=left><button back=L2UI_CH3.prev1_down fore=L2UI_CH3.prev1 width=14 height=14></td>");
		else 
			sb.append("<td width=" +witdth +" align=left><button action=\""+bypass+";" + (page-1) + "\" back=L2UI_CH3.prev1_down fore=L2UI_CH3.prev1 width=14 height=14></td>");
		
		sb.append("<td width=30 align=center>" + page + "/" + maxPages + "</td>");
		
		if (page == maxPages)
			sb.append("<td width=" +witdth +" align=right><button back=L2UI_CH3.next1_down fore=L2UI_CH3.next1 width=14 height=14></td>");
		else 
			sb.append("<td width=" +witdth +" align=right><button action=\"" + bypass + ";" + (page+1) + "\" back=L2UI_CH3.next1_down fore=L2UI_CH3.next1 width=14 height=14></td>");
		

		sb.append("</tr></table>");
		return sb.toString();
	}
	
	private void showMessage(Player player, int length) 
	{
		//player.sendMessage("Length: " + length + " / 8192");
	}


	public String getHtmlPath(String filename)
	{
		return "data/html/mods/buffs_master/" + filename + ".htm";
	}
}
