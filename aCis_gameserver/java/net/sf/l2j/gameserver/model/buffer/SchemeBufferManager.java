package net.sf.l2j.gameserver.model.buffer;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.pool.ConnectionPool;
import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.data.xml.SchemeBufferData;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.Summon;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;

public class SchemeBufferManager
{
	private static final CLogger LOGGER = new CLogger(SchemeBufferManager.class.getName());
	
	private static final String DELETE_PLAYER_SCHEMES = "DELETE FROM ex_buffer_schemes WHERE PlayerID=? ";
	private static final String CREATE_SCHEME = "INSERT INTO ex_buffer_schemes (PlayerID,Description,IconID,Name,CreatedDate,Skills,RestoreCP,RestoreHP,RestoreMP,ShareCode,ShareCodeLocked) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
	private static final String LOAD_ALL_SCHEMES = "SELECT * FROM ex_buffer_schemes";
	
	private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	public static final int MAX_SCHEMES = 4;
	private int SAVE_INTERVAL = 600 * 1000; //600 seconds = 10 minutes
	private SaveTask _saveTask = null;
	private String _paymentItemName = "Free";
	
	public static SchemeBufferManager getInstance() {
		return SingletonHolder._instance;
	}
	
	public SchemeBufferManager()
	{
		_saveTask = new SaveTask();
		loadSchemesFromDB();
		ThreadPool.schedule(_saveTask, SAVE_INTERVAL);
		loadPaymentItemName();
	}
	
	private Map<String, Integer> _shareCodesPlayers = new HashMap<>();
	private Map<Integer, PlayerSchemesEntity> _playersData = new HashMap<>();
	
	public Map<Integer, PlayerSchemesEntity> getPlayersSchemesData()
	{
		return _playersData;
	}
	
	public List<Integer> getAvailableBuffsByCategorySorted(BuffCategory category)
	{
		return SchemeBufferData.getInstance().getAvailableBuffsByCategory(category);
	}
	
	public Map <Integer, SchemeEntity> getSchemesForPlayer(int playerId)
	{
		return getPlayerSchemesData(playerId).getSchemes();
	}
	
	private void loadPaymentItemName()
	{
		if (!Config.BUFFS_MASTER_PAYMENT_ITEM_NAME.isBlank())
		{
			_paymentItemName = Config.BUFFS_MASTER_PAYMENT_ITEM_NAME;
		}
		else
		{
			var paymentItem = ItemData.getInstance().getTemplate(Config.BUFFS_MASTER_PAYMENT_ITEM);
			if (paymentItem != null)
			{
				_paymentItemName = paymentItem.getName();
			}
		}
	}
		
	public String getPaymentItemName()
	{
		return _paymentItemName;
	}
	
	private void setSchemesModifiedFlag(int playerId)
	{
		getPlayerSchemesData(playerId).setHasChanges(true);
	} 
	
	public boolean hasSchemeBuff(int playerId, int schemeId, int buffId)
	{
		var playerSchemes = getPlayerSchemesData(playerId).getSchemes();
		if (!playerSchemes.containsKey(schemeId))
		{
			return false;
		}
		return playerSchemes.get(schemeId).Buffs.containsKey(buffId);
	}
	
	public Map<Integer, SchemeBuffEntity> getSchemeBuffs(int playerId, int schemeId)
	{
		var playerSchemes = getPlayerSchemesData(playerId).getSchemes();
		if (!playerSchemes.containsKey(schemeId))
		{
			return new HashMap<>();
		}
		return playerSchemes.get(schemeId).Buffs;
	}
	
	private void generateNewShareCode(SchemeEntity scheme)
	{
		var currentCode = scheme.getShareCode();
		_shareCodesPlayers.remove(currentCode);
		
		String newCode = null;
		while (true)
		{
			newCode = randomAlphaNumeric(6);
			if (!_shareCodesPlayers.containsKey(newCode))
			{
				break;
			}	
		}
		scheme.setShareCode(newCode);
		_shareCodesPlayers.put(newCode, scheme.getPlayerId());
	}
	
	public void addSchemeAndGenerateID(SchemeEntity scheme)
	{
		int schemesAmount = getPlayerSchemesData(scheme.getPlayerId()).getSchemes().size();
		if (schemesAmount >= 10)
		{
			return;
		}
		
		scheme.setId(getPlayerSchemesData(scheme.getPlayerId()).getSchemes().size() + 1);
		generateNewShareCode(scheme);
		getSchemesForPlayer(scheme.getPlayerId()).put(scheme.getId() , scheme);
	}
	
	private PlayerSchemesEntity getPlayerSchemesData(int playerId)
	{
		if (_playersData.containsKey(playerId))
		{
			return _playersData.get(playerId);
		}
		
		PlayerSchemesEntity data = new PlayerSchemesEntity(); 
		_playersData.put(playerId, data);
		return data;
	}
	
	
	public ParamValidation validateIcon(Player player, Integer icon)
	{
		var validResult = new ParamValidation();

		if (icon == null)
		{
			validResult.setError("Incorrect icon.");
			return validResult;
		}
		
		if (!SchemeBufferData.getInstance().getIcons().containsKey(icon))
		{
			validResult.setError("Incorrect icon.");
			return validResult;
		}
		
		return validResult;
	}
	
	
	public ParamValidation useShareCode(Player player, String code)
	{
		var validResult = new ParamValidation();
		
		if (code == null || code.isEmpty())
		{
			validResult.setError("Please, enter valid code.");
			return validResult;
		}
		
		code = code.trim();
		
		code = code.toUpperCase();
		
		if (!_shareCodesPlayers.containsKey(code))
		{
			validResult.setError("You are not authorized to use this code.");
			return validResult;
		}
		
		int playerId = _shareCodesPlayers.get(code);
		var playerSchemes = getSchemesForPlayer(playerId);
		
		if (getSchemesForPlayer(player.getObjectId()).size() >= MAX_SCHEMES)
		{
			 validResult.setError("You cannot create more than " + MAX_SCHEMES + " schemes.");
			 return validResult;
		}
		
		for (var schemeId : playerSchemes.keySet())
		{
			if (playerSchemes.get(schemeId).getShareCode().equals(code))
			{
				var scheme = playerSchemes.get(schemeId);
				
				if (scheme.isShareCodeLocked())
				{
					validResult.setError("You are not authorized to use this code.");
					return validResult;
				}
				
				copySheme(player, scheme);
				break;
			}
		}
		setSchemesModifiedFlag(player.getObjectId());
		return validResult;
	}
	
	private ParamValidation copySheme(Player player, SchemeEntity scheme)
	{
		var validResult = new ParamValidation();
		
		var playerSchemes = getSchemesForPlayer(player.getObjectId());
		if (playerSchemes.size() >= MAX_SCHEMES)
		{
			 validResult.setError("You cannot create more than " + MAX_SCHEMES + " schemes.");
			 return validResult;
		}
		
		var newScheme = createNewScheme(player, scheme.getName(), scheme.getIconId());
		//copy buffs also
		for (var buffId : scheme.Buffs.keySet())
		{
			newScheme.addBuffOnLoad(buffId);
		}
		newScheme.recalculatePrice();
		return validResult;
	}
	
	public ParamValidation resetShareCode(Player player, int schemeId)
	{
		var validResult = new ParamValidation();
		Map<Integer, SchemeEntity> schemes = getSchemesForPlayer(player.getObjectId());
		SchemeEntity scheme = schemes.get(schemeId);
		
		if (scheme == null)
		{
			validResult.setError("Can't change code, because scheme doesn't exist.");
			return validResult;
		}
		
		//Finally change code
		generateNewShareCode(scheme);
		setSchemesModifiedFlag(player.getObjectId());
		return validResult;
	} 
	
	public ParamValidation swapSchemeLockShareCode(Player player, int schemeId)
	{
		var validResult = new ParamValidation();
		Map<Integer, SchemeEntity> schemes = getSchemesForPlayer(player.getObjectId());
		SchemeEntity scheme = schemes.get(schemeId);
		
		if (scheme == null)
		{
			validResult.setError("Can't lock/unlock code usage, because scheme doesn't exist.");
			return validResult;
		}
		
		scheme.setShareCodeLocked(!scheme.isShareCodeLocked());
		setSchemesModifiedFlag(player.getObjectId());
		return validResult;
	}
	
	public ParamValidation autoOrder(Player player, int schemeId)
	{
		var validResult = new ParamValidation();
		
		Map<Integer, SchemeEntity> schemes = getSchemesForPlayer(player.getObjectId());
		SchemeEntity scheme = schemes.get(schemeId);
		if (scheme == null)
		{
			validResult.setError("Can't sort effects, because scheme doesn't exist.");
			return validResult;
		}
		
		var order = SchemeBufferData.getInstance().getBuffsOrders();
		
        Comparator<Integer> byOrder = (Integer obj1, Integer obj2) -> order.get(obj1).compareTo(order.get(obj2));
		var result = scheme.Buffs.keySet().stream().sorted(byOrder).collect(Collectors.toList());
		
		for (int i = 0; i < result.size(); i++)
		{
			var buffId = result.get(i);
			scheme.Buffs.get(buffId).setOrder(i);
		}
		setSchemesModifiedFlag(player.getObjectId());
		return validResult;
	}
	
	public ParamValidation canAddScheme(Player player)
	{
		var validResult = new ParamValidation();
		
		var playerSchemes = getSchemesForPlayer(player.getObjectId());
		if (playerSchemes.size() >= MAX_SCHEMES)
		{
			validResult.setError("You cannot create more schemes.");
			return validResult;
		}
		
		return validResult;
	}
	
	public ParamValidation changeBuffOrder(Player player, int schemeId, int buffId, Boolean moveBack)
	{
		var validResult = new ParamValidation();
		
		Map<Integer, SchemeEntity> schemes = getSchemesForPlayer(player.getObjectId());
		SchemeEntity scheme = schemes.get(schemeId);
		if (scheme == null)
		{
			validResult.setError("Can't sort effects, because scheme doesn't exist.");
			return validResult;
		}
		
		if (!scheme.Buffs.containsKey(buffId))
		{
			validResult.setError("Effect doesn't exist in scheme.");
			return validResult;
		}
		
		int newOrder = 0;
		int currentOrder = scheme.Buffs.get(buffId).getOrder();
		
		List<Integer> sordedKeys = scheme.getBuffsSorted();
		int currentIndex = sordedKeys.indexOf(buffId);
		
		if ((currentIndex <= 0 && moveBack) || (!moveBack && currentIndex>=sordedKeys.size()-1))
		{
			validResult.setError("You trying to move effect in wrong direction.");
			return validResult;
		}
		
		if (moveBack)
		{
			SchemeBuffEntity buffBefore = scheme.Buffs.get(sordedKeys.get(currentIndex-1));
			newOrder = buffBefore.getOrder();
			if (newOrder == currentOrder)
			{
				newOrder = sordedKeys.size();
			}
			
			buffBefore.setOrder(currentOrder);
		}
		else
		{
			SchemeBuffEntity buffAfter = scheme.Buffs.get(sordedKeys.get(currentIndex+1));
			newOrder = buffAfter.getOrder();
			if (newOrder == currentOrder)
			{
				newOrder = sordedKeys.size();
			}
			
			buffAfter.setOrder(currentOrder);
		}
			
		scheme.Buffs.get(buffId).setOrder(newOrder);
		setSchemesModifiedFlag(player.getObjectId());
		
		return validResult;
	}
	
	
	public void addNewScheme(Player player, String name)
	{
		addNewScheme(player, name, null, 0);
	}
	
	public ParamValidation addNewScheme(Player player, String name, Integer iconId, int copyFrom)
	{
		var result = new ParamValidation();
		
		if (getSchemesForPlayer(player.getObjectId()).size() >= MAX_SCHEMES)
		{
			result.setError("You cannot create more than " + MAX_SCHEMES + " schemes.");
			return result;
		}
		
		if (name == null || name.isBlank())
		{
			result.setError("Please, specify scheme name.");
			return result;
		}
		
		if (iconId != null && !SchemeBufferData.getInstance().getIcons().containsKey(iconId))
		{
			result.setError("Selected icon is incorrect.");
			return result;
		}
		
		//Finally create scheme
		var newScheme = createNewScheme(player, name.trim(), iconId);
		
		//copy buffs
		if (copyFrom == 1)
		{
			//copy buffs also
			for (var buffId : Config.BUFFS_MASTER_WARRIOR_SCHEME)
			{
				newScheme.addBuffOnLoad(buffId);
			}
			newScheme.recalculatePrice();
		}
		else if (copyFrom == 2)
		{
			//copy buffs also
			for (var buffId : Config.BUFFS_MASTER_MYSTIC_SCHEME)
			{
				newScheme.addBuffOnLoad(buffId);
			}
			newScheme.recalculatePrice();
		}
		else if (copyFrom == 3)
		{
			//copy buffs also
			for (var buffId : Config.BUFFS_MASTER_HEALER_SCHEME)
			{
				newScheme.addBuffOnLoad(buffId);
			}
			newScheme.recalculatePrice();
		}
		else if (copyFrom == 4)
		{
			//copy buffs also
			for (var buffId : Config.BUFFS_MASTER_TANKER_SCHEME)
			{
				newScheme.addBuffOnLoad(buffId);
			}
			newScheme.recalculatePrice();
		}
		setSchemesModifiedFlag(player.getObjectId());
		return result;
	}
	
	private SchemeEntity createNewScheme(Player player, String name,Integer iconId)
	{
		//1. Create Entity
		SchemeEntity scheme = new SchemeEntity();
		scheme.setPlayerId(player.getObjectId());
		scheme.setName(name);
		scheme.setIconId(iconId);
		scheme.setCreationDate(System.currentTimeMillis());
		scheme.setShareCodeLocked(true);

		//2. Add to cache data
		addSchemeAndGenerateID(scheme);
		return scheme;
	}
	
	// return string as Add result - empty = no errors
	public ParamValidation addOrRemoveBuffInScheme(Player player, int schemeId, int buffId)
	{
		var result = new ParamValidation();
		
		Map<Integer, SchemeEntity> schemes = getSchemesForPlayer(player.getObjectId());
		SchemeEntity scheme = schemes.get(schemeId);
		
		if (scheme == null)
		{
			result.setError("Wrong scheme.");
			return result;
		}
		
		if (!SchemeBufferData.getInstance().hasBuff(buffId))
		{
			result.setError("You trying to add unlisted effects.");
			return result;
		}
		
		if (scheme.Buffs.containsKey(buffId))
		{
			//when buff is removed decrease buffs order
			//after that all buffs have to be saved ( Save button)
			scheme.Buffs.remove(buffId);
		}
		else
		{
			//before adding check if there is already buff with same stackType, if there is remove it
			SchemeBufferData.getInstance().getBuff(buffId);
			
			var buffsInScheme = scheme.Buffs.keySet().toArray(Integer[]::new);
			for (int checkBuffId : buffsInScheme)
			{
				if (buffId  == checkBuffId)
					continue;
				
				if (SchemeBufferData.getInstance().getBuff(checkBuffId).isStacking(buffId))
				{
					scheme.Buffs.remove(checkBuffId);
				}
			}
			
			
			// if player is trying add, check if still has place for buff
			if (scheme.Buffs.size() >= Config.BUFFS_MASTER_MAX_SKILLS_PER_SCHEME)
			{
				result.setError("You cannot add more effects to this scheme.");
				return result;
			}
		
			scheme.addBuffOnLastPosition(buffId);
		}
		
		scheme.recalculatePrice();
		setSchemesModifiedFlag(player.getObjectId());
		return result;
	}
	
	
	public ParamValidation addOrRemoveRestoreInScheme(Player player, int schemeId, RestoreType restore)
	{
		var result = new ParamValidation();
		
		Map<Integer, SchemeEntity> schemes = getSchemesForPlayer(player.getObjectId());
		SchemeEntity scheme = schemes.get(schemeId);
		
		if (scheme == null)
		{
			result.setError("Wrong scheme.");
			return result;
		}
		
		if (restore == RestoreType.HP)
		{
			scheme.setRestoreHp(!scheme.getRestoreHp());
		}
		else if(restore == RestoreType.MP)
		{
			scheme.setRestoreMp(!scheme.getRestoreMp());
		}
		else if(restore == RestoreType.CP)
		{
			scheme.setRestoreCp(!scheme.getRestoreCp());
		}
		
		//add restores to price
		scheme.recalculatePrice();
		setSchemesModifiedFlag(player.getObjectId());
		return result;
	}
	
	public ParamValidation changeSchemeName(Player player, int schemeId, String name)
	{
		var result = new ParamValidation();
		
		Map<Integer, SchemeEntity> schemes = getSchemesForPlayer(player.getObjectId());
		SchemeEntity scheme = schemes.get(schemeId);
		
		if (scheme == null)
		{
			result.setError("Cannot change name, because scheme doesn't exist.");
			return result;
		}
		
		if (name == null || name.isBlank())
		{
			result.setError("Please, specify scheme name.");
			return result;
		}
		
		name = name.trim();
		
		scheme.setName(name);
		setSchemesModifiedFlag(player.getObjectId());
		return result;
	}
	
	public ParamValidation changeSchemeIcon(Player player, int schemeId, Integer iconId)
	{
		var result = new ParamValidation();
		
		Map<Integer, SchemeEntity> schemes = getSchemesForPlayer(player.getObjectId());
		SchemeEntity scheme = schemes.get(schemeId);
		
		if (scheme == null)
		{
			result.setError("Cannot change icon, because scheme doesn't exist.");
			return result;
		}
		
		if (iconId == null || !SchemeBufferData.getInstance().getIcons().containsKey(iconId))
		{
			result.setError("Wrong icon.");
			return result;
		}
		
		scheme.setIconId(iconId);
		setSchemesModifiedFlag(player.getObjectId());
		return result;
	}
	
	public ParamValidation deleteScheme(Player player, int schemeId)
	{
		var validResult = new ParamValidation();
		Map<Integer, SchemeEntity> schemes = getSchemesForPlayer(player.getObjectId());
		SchemeEntity scheme = schemes.get(schemeId);
		
		if (scheme == null)
		{
			validResult.setError("Cannot remove scheme, because it doesn't exist.");
			return validResult;

		}
		
		//1. Remove from cache
		schemes.remove(schemeId);
		setSchemesModifiedFlag(player.getObjectId());
		return validResult;
	}
	
	public ParamValidation restoreCP(Player player, boolean isBuffingPlayer)
	{
		var result = checkPlayerConditions(player);
		
		if (result.hasError()) {
			return result;
		}
		
		var validResult = new ParamValidation();
		
		if (!isBuffingPlayer)
		{
			validResult.setError("You cannot restore combat points on summon.");
			return validResult;
		}
		
		if (Config.BUFFS_MASTER_PAYMENT_ITEM > 0)
		{
			int payItemsCount = player.getInventory().getItemCount(Config.BUFFS_MASTER_PAYMENT_ITEM, -1);
			
			if (payItemsCount < Config.BUFFS_MASTER_CP_RESTORE_PRICE)
			{
				validResult.setError("You don't have enough " + _paymentItemName + " to restore combat points.");
				return validResult;
			}
						
			var destroyResult = player.destroyItemByItemId("SchemeBuffer", Config.BUFFS_MASTER_PAYMENT_ITEM, Config.BUFFS_MASTER_CP_RESTORE_PRICE, player, true);
					
			if (!destroyResult)
			{
				validResult.setError("Ups, something goes wrong.");
				return validResult;
			}
		}

		if (isBuffingPlayer)
		{
			validResult.setInfo("Your combat points has been restored.");
			player.getStatus().setCp(player.getStatus().getMaxCp());
		}
		
		return validResult;
	}

	@SuppressWarnings("null")
	public ParamValidation restoreHP(Player player, boolean isBuffingPlayer)
	{
		var result = checkPlayerConditions(player);
	
		if (result.hasError()) {
			return result;
		}
		
		var validResult = new ParamValidation();
		Summon summon = null;
		
		if (!isBuffingPlayer)
		{
			summon = player.getSummon();
			if (summon == null)
			{
				validResult.setError("You don't have any summon at this time.");
				return validResult;
			}
		}
		
		if (Config.BUFFS_MASTER_PAYMENT_ITEM > 0)
		{
			int payItemsCount = player.getInventory().getItemCount(Config.BUFFS_MASTER_PAYMENT_ITEM, -1);
			
			if (payItemsCount < Config.BUFFS_MASTER_HP_RESTORE_PRICE)
			{
				validResult.setError("You don't have enough " + _paymentItemName + " to restore health points.");
				return validResult;
			}
						
			var destroyResult = player.destroyItemByItemId("SchemeBuffer", Config.BUFFS_MASTER_PAYMENT_ITEM, Config.BUFFS_MASTER_HP_RESTORE_PRICE, player, true);
					
			if (!destroyResult)
			{
				validResult.setError("Ups, something went wrong.");
				return validResult;
			}
		}

		if (isBuffingPlayer)
		{
			validResult.setInfo("Your health points has been restored.");
			player.getStatus().setHp(player.getStatus().getMaxHp());
		}
		else 
		{
			validResult.setInfo("Your summon health points has been restored.");
			summon.getStatus().setHp(summon.getStatus().getMaxHp());
		}
		
		return validResult;
	}
	
	@SuppressWarnings("null")
	public ParamValidation restoreMP(Player player, boolean isBuffingPlayer)
	{
		var result = checkPlayerConditions(player);
		
		if (result.hasError()) {
			return result;
		}
		
		var validResult = new ParamValidation();
		Summon summon = null;
		
		if (!isBuffingPlayer)
		{
			summon = player.getSummon();
			if (summon == null)
			{
				validResult.setError("You don't have any summon at this time.");
				return validResult;
			}
		}
		
		if (Config.BUFFS_MASTER_PAYMENT_ITEM > 0)
		{
			int payItemsCount = player.getInventory().getItemCount(Config.BUFFS_MASTER_PAYMENT_ITEM, -1);
			
			if (payItemsCount < Config.BUFFS_MASTER_MP_RESTORE_PRICE)
			{
				validResult.setError("You don't have enough " + _paymentItemName + " to restore mana points.");
				return validResult;
			}
						
			var destroyResult = player.destroyItemByItemId("SchemeBuffer", Config.BUFFS_MASTER_PAYMENT_ITEM, Config.BUFFS_MASTER_MP_RESTORE_PRICE, player, true);
					
			if (!destroyResult)
			{
				validResult.setError("Ups, something goes wrong.");
				return validResult;
			}
		}

		if (isBuffingPlayer)
		{
			validResult.setInfo("Your mana points has been restored.");
			player.getStatus().setMp(player.getStatus().getMaxMp());
		}
		else 
		{
			validResult.setInfo("Your summon mana points has been restored.");
			summon.getStatus().setMp(summon.getStatus().getMaxMp());
		}
		
		return validResult;
	}
	
	@SuppressWarnings("null")
	public ParamValidation restoreAll(Player player, boolean isBuffingPlayer)
	{
		var result = checkPlayerConditions(player);
		
		if (result.hasError()) {
			return result;
		}
		
		var validResult = new ParamValidation();
		Summon summon = null;
		
		if (!isBuffingPlayer)
		{
			summon = player.getSummon();
			if (summon == null)
			{
				validResult.setError("You don't have any summon at this time.");
				return validResult;
			}
		}
		
		if (Config.BUFFS_MASTER_PAYMENT_ITEM > 0)
		{
			int payItemsCount = player.getInventory().getItemCount(Config.BUFFS_MASTER_PAYMENT_ITEM, -1);
			var price = Config.BUFFS_MASTER_CP_RESTORE_PRICE + Config.BUFFS_MASTER_HP_RESTORE_PRICE + Config.BUFFS_MASTER_MP_RESTORE_PRICE;
			
			if (payItemsCount < price)
			{
				validResult.setError("You don't have enough " + _paymentItemName + " to use restoration.");
				return validResult;
			}
						
			var destroyResult = player.destroyItemByItemId("SchemeBuffer", Config.BUFFS_MASTER_PAYMENT_ITEM, price, player, true);
					
			if (!destroyResult)
			{
				validResult.setError("Ups, something goes wrong.");
				return validResult;
			}
		}

		if (isBuffingPlayer)
		{
			validResult.setInfo("Your CP/HP/MP has been restored.");
			player.getStatus().setMp(player.getStatus().getMaxMp());
			player.getStatus().setHp(player.getStatus().getMaxHp());
			player.getStatus().setCp(player.getStatus().getMaxCp());
		}
		else 
		{
			validResult.setInfo("Your summon CP/HP/MP has been restored.");
			summon.getStatus().setMp(summon.getStatus().getMaxMp());
			summon.getStatus().setHp(summon.getStatus().getMaxHp());
		}
		
		return validResult;
	}	
	
	@SuppressWarnings("null")
	public ParamValidation cancelBuffs(Player player, boolean isBuffingPlayer)
	{
		var validResult = checkPlayerConditions(player);
		
		if (validResult.hasError()) {
			return validResult;
		}
		
		Summon summon = null;
		
		if (!isBuffingPlayer)
		{
			summon = player.getSummon();
			if (summon == null)
			{
				validResult.setError("You don't have any summon at this time.");
				return validResult;
			}
		}
		
		if (isBuffingPlayer)
		{
			validResult.setInfo("You have been cleared of the effects.");
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		else 
		{
			validResult.setInfo("Your summon have been cleared of the effects.");
			summon.stopAllEffectsExceptThoseThatLastThroughDeath();
		}
		return validResult;
	}
	
	public ParamValidation useMysticScheme(Player player, boolean isBuffingPlayer)
	{
		var result = checkPlayerConditions(player);
		
		if (result.hasError()) {
			return result;
		}
		
		var validResult = new ParamValidation();
	
		Summon summon = null;
		
		if (!isBuffingPlayer)
		{
			summon = player.getSummon();
			if (summon == null)
			{
				validResult.setError("You don't have any summon at this time.");
				return validResult;
			}
		}
		
		if (Config.BUFFS_MASTER_PAYMENT_ITEM > 0)
		{
			int payItemsCount = player.getInventory().getItemCount(Config.BUFFS_MASTER_PAYMENT_ITEM, -1);
			
			var schemePrice = SchemeBufferData.getInstance().getMysticSchemePrice();
			if (payItemsCount < schemePrice)
			{
				validResult.setError("You don't have enough " + _paymentItemName + " to use scheme.");
				return validResult;
			}
			
			var destroyResult = player.destroyItemByItemId("SchemeBuffer", Config.BUFFS_MASTER_PAYMENT_ITEM, schemePrice, player, true);
			
			if (!destroyResult)
			{
				validResult.setError("Ups, something goes wrong.");
				return validResult;
			}
		}		
		
		for (int skillId : Config.BUFFS_MASTER_MYSTIC_SCHEME)
		{
			var buffData = SchemeBufferData.getInstance().getBuff(skillId);
			if (buffData != null)
			{
				var buffLevel = buffData.getLevel();
				var buff = SkillTable.getInstance().getInfo(skillId, buffLevel);
				buff.getEffects(isBuffingPlayer ? player : summon, isBuffingPlayer ? player : summon);
			}
		}
		
		validResult.setInfo("You used mystic sheme on " + (isBuffingPlayer ? "yourself." : "your summon."));		
		return validResult;
	}
	
	public ParamValidation useWarriorScheme(Player player, boolean isBuffingPlayer)
	{
		var result = checkPlayerConditions(player);
		
		if (result.hasError()) {
			return result;
		}
		
		var validResult = new ParamValidation();
		
		Summon summon = null;
		if (!isBuffingPlayer)
		{
			summon = player.getSummon();
			if (summon == null)
			{
				validResult.setError("You don't have any summon at this time.");
				return validResult;
			}
		}
				
		if (Config.BUFFS_MASTER_PAYMENT_ITEM > 0)
		{
			int payItemsCount = player.getInventory().getItemCount(Config.BUFFS_MASTER_PAYMENT_ITEM, -1);
			
			var schemePrice = SchemeBufferData.getInstance().getWarriorSchemePrice();
			
			if (payItemsCount < schemePrice)
			{
				validResult.setError("You don't have enough " + _paymentItemName + " to use scheme.");
				return validResult;
			}
			
			var destroyResult = player.destroyItemByItemId("SchemeBuffer", Config.BUFFS_MASTER_PAYMENT_ITEM, schemePrice, player, true);

			if (!destroyResult)
			{
				validResult.setError("Ups, something went wrong.");
				return validResult;
			}
		}
		
		for (int skillId : Config.BUFFS_MASTER_WARRIOR_SCHEME)
		{
			var buffData = SchemeBufferData.getInstance().getBuff(skillId);
			
			if(buffData != null)
			{
				var buffLevel = buffData.getLevel();
				var buff = SkillTable.getInstance().getInfo(skillId, buffLevel);
				buff.getEffects(isBuffingPlayer ? player : summon, isBuffingPlayer ? player : summon);
			}
		}
		
		validResult.setInfo("You used warrior sheme on " + (isBuffingPlayer ? "yourself." : "your summon."));		
		return validResult;
	}
	
	@SuppressWarnings("null")
	public ParamValidation useScheme(Player player, boolean isBuffingPlayer, int schemeId)
	{
		var result = checkPlayerConditions(player);
		
		if (result.hasError()) {
			return result;
		}
		
		var validResult = new ParamValidation();
		
		Map<Integer, SchemeEntity> schemes = getSchemesForPlayer(player.getObjectId());
		SchemeEntity scheme = schemes.get(schemeId);
		if (scheme == null)
		{
			validResult.setError("Wrong scheme.");
			return validResult;
		}
		
		Summon summon = null;
		if (!isBuffingPlayer)
		{
			summon = player.getSummon();
			if (summon == null)
			{
				validResult.setError("You don't have any summon at this time.");
				return validResult;
			}
		}
		
		var schemePrice = scheme.getPrice();
		if (Config.BUFFS_MASTER_PAYMENT_ITEM > 0 && schemePrice > 0)
		{
			int payItemsCount = player.getInventory().getItemCount(Config.BUFFS_MASTER_PAYMENT_ITEM, -1);
			
			if (payItemsCount < schemePrice)
			{
				validResult.setError("You don't have enough " + _paymentItemName + " to use scheme.");
				return validResult;
			}
			
			var destroyResult = player.destroyItemByItemId("SchemeBuffer", Config.BUFFS_MASTER_PAYMENT_ITEM, schemePrice, player, true);
			if (!destroyResult)
			{
				validResult.setError("Ups, something goes wrong.");
				return validResult;
			}
			
		}
		
		for (int skillId : scheme.getBuffsSorted())
		{
			int buffLvl =  SchemeBufferData.getInstance().getBuff(skillId).getLevel();
			SkillTable.getInstance().getInfo(skillId, buffLvl).getEffects(isBuffingPlayer ? player : summon, isBuffingPlayer ? player : summon);
		}
		
		if (scheme.getRestoreCp() && isBuffingPlayer)
		{
			player.getStatus().setCp(player.getStatus().getMaxCp());
		}
		if (scheme.getRestoreHp())
		{
			(isBuffingPlayer ? player : summon).getStatus().setHp(player.getStatus().getMaxHp());
		}
		if (scheme.getRestoreMp())
		{
			(isBuffingPlayer ? player : summon).getStatus().setMp(player.getStatus().getMaxMp());
		}
		
		validResult.setInfo("You used scheme: " + scheme.getName());
		return validResult;

	}
	
	public ParamValidation switchTarget(Player player, boolean isBuffingPlayer)
	{
		var validResult = new ParamValidation();
		
		if (!isBuffingPlayer)
		{
			validResult.setInfo("You switched target to yourself.");
			return validResult;
		}
		
		if (isBuffingPlayer && player.getSummon() == null)
		{
			validResult.setError("You don't have any summon at this time.");
			return validResult;
		}
		
		validResult.setInfo("You switched target to your summon.");
		return validResult;
	}
	
	public ParamValidation useBuff(Player player, boolean isBuffingPlayer, int buffId)
	{
		var validResult = checkPlayerConditions(player);
		
		if (validResult.hasError()) {
			return validResult;
		}
		
		if (!SchemeBufferData.getInstance().hasBuff(buffId))
		{
			validResult.setError("You trying to use non-existent effect.");
			return validResult;
		}
		
		Summon summon = null;
		if (!isBuffingPlayer)
		{
			summon = player.getSummon();
			if (summon == null)
			{
				validResult.setError("You don't have any summon at this time.");
				return validResult;
			}
		}
		
		
		if (Config.BUFFS_MASTER_PAYMENT_ITEM > 0)
		{
			int payItemsCount = player.getInventory().getItemCount(Config.BUFFS_MASTER_PAYMENT_ITEM, -1);
			var buff = SchemeBufferData.getInstance().getBuff(buffId);
			
			if (buff.getPriceItemCount() > payItemsCount)
			{
				validResult.setError("You don't have enough " + _paymentItemName + " to use this effect.");
				return validResult;
			}
			
			var destroyResult = player.destroyItemByItemId("SchemeBuffer", Config.BUFFS_MASTER_PAYMENT_ITEM, buff.getPriceItemCount(), player, true);
			if (!destroyResult)
			{
				validResult.setError("Ups, something goes wrong.");
				return validResult;
			}
		}
		
		var buffLevel = SchemeBufferData.getInstance().getBuff(buffId).getLevel();
		var buff = SkillTable.getInstance().getInfo(buffId, buffLevel);
		buff.getEffects(isBuffingPlayer ? player : summon, isBuffingPlayer ? player : summon);
		
		validResult.setInfo("You used " + buff.getName());
		
		return validResult;
	}
	
	public ParamValidation checkPlayerConditions(Player player)
	{
		var validResult = new ParamValidation();
		
		if (player.getCast().isCastingNow())
		{
			validResult.setError("Cannot use buffer while casting.");
			return validResult;
		}
		
		if (player.isAlikeDead())
		{
			validResult.setError("Dead players cannot use buffer.");
			return validResult;
		}
		
		if (!Config.BUFFS_MASTER_CAN_USE_KARMA && player.isCursedWeaponEquipped())
		{
			validResult.setError("Players under possesion of cursed weapon cannot use buffer.");
			return validResult;
		}
		
		if (!Config.BUFFS_MASTER_CAN_USE_KARMA && player.getKarma() > 0)
		{
			validResult.setError("Players with karma cannot use buffer.");
			return validResult;
		}
		
		if (!Config.BUFFS_MASTER_CAN_USE_IN_COMBAT && player.getAttack().isAttackingNow())
		{
			validResult.setError("Cannot use buffer being in combat mode.");
			return validResult;
		}
		
		if ((player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player) || OlympiadManager.getInstance().isRegisteredInComp(player)))
		{
			validResult.setError("Olympiad games participants cannot use buffer.");
			return validResult;
		}
		
		if (!player.isGM() && !player.isInsideZone(ZoneId.TOWN) && !Config.BUFFS_MASTER_CAN_USE_OUTSIDE_TOWN)
		{
			validResult.setError("Cannot use buffer being outside the town.");
			return validResult;
		}
		
		if (player.isInCombat())
		{
			validResult.setError("Cannot use buffer being in combat mode.");
			return validResult;
		}
			
		return validResult;
	}
	
	protected class SaveTask implements Runnable
	{
		private boolean _isSaving;
		private boolean _stopTask;
		
        public SaveTask()
        {
        }

        public void stopTask()
        {
        	_stopTask = true;
        }
        
        public boolean isSaving()
        {
        	return _isSaving;
        }
        
		@Override
		public void run()
		{
			 if (_stopTask) 
				 return;
			 
			_isSaving = true;
			saveSchemes();
			_isSaving = false;
			ThreadPool.schedule(_saveTask, SAVE_INTERVAL);
		}
	}
	
	
	private static String randomAlphaNumeric(int count)
	{
		StringBuilder builder = new StringBuilder();
		while (count-- != 0)
		{
			int character = (int)(Math.random() * ALPHA_NUMERIC_STRING.length());
			builder.append(ALPHA_NUMERIC_STRING.charAt(character));
		}
		return builder.toString();
	}
	
	public void cleanUp()
	{
		_saveTask.stopTask();
		
		// Start saving task if it's not already in progress
		if (!_saveTask.isSaving())
		{
			saveSchemes();
		}
	}
	
	public static String saveSchemes()
	{
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement st = con.prepareStatement(CREATE_SCHEME);
			PreparedStatement st2 = con.prepareStatement(DELETE_PLAYER_SCHEMES);)
		{
			int savedSchemes = 0;
			var allPlayersSchemes = SchemeBufferManager.getInstance().getPlayersSchemesData();

			for (int playerId : allPlayersSchemes.keySet())
			{
				var playerSchemes = allPlayersSchemes.get(playerId);
				
				// nothing has changed in schemes
				if (!playerSchemes.hasChanges())
				{
					continue;
				}
				
				// Delete all player schemes
				st2.setInt(1, playerId);
				st2.execute();
				
				
				// Save Player Schemes
				for (int schemeId : allPlayersSchemes.get(playerId).getSchemes().keySet())
				{
					SchemeEntity currentScheme = allPlayersSchemes.get(playerId).getSchemes().get(schemeId);
					// Build a String composed of skill ids separated by a ",".
					final StringBuilder sb = new StringBuilder();
					for (int skillId : currentScheme.getBuffsSorted())
						StringUtil.append(sb, skillId, ",");
					
					// Delete the last "," : must be called only if there is something to delete!
					if (sb.length() > 0)
						sb.setLength(sb.length() - 1);
					
					st.setInt(1, playerId);
					st.setString(2, currentScheme.getDescription() == null ? "" : currentScheme.getDescription());
					st.setInt(3, currentScheme.getIconId());
					st.setString(4, currentScheme.getName());
					st.setLong(5, currentScheme.getCreationDate());
					st.setString(6, sb.toString());
					st.setBoolean(7, currentScheme.getRestoreCp());
					st.setBoolean(8, currentScheme.getRestoreHp());
					st.setBoolean(9, currentScheme.getRestoreMp());
					st.setString(10, currentScheme.getShareCode());
					st.setBoolean(11, currentScheme.isShareCodeLocked());
					
					st.addBatch();
					
					savedSchemes ++;
				}
				//when changes has been stored, set flag "not changed", so it will not be stored again
				playerSchemes.setHasChanges(false);
			}

			//add new schemes
			if (savedSchemes > 0)
			{
				st.executeBatch();
			}
			LOGGER.info("{} schemes have been stored in database (next save in 10 minute(s)).", savedSchemes);
		}
		catch (Exception e)
		{
			LOGGER.error("Error occurred while saving schemes to database!", e);
		}
		
		return null;
	}


	public boolean loadSchemesFromDB()
	{
		int restoredSchemes = 0;
		// Load all data.
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement st = con.prepareStatement(LOAD_ALL_SCHEMES);
			ResultSet rset = st.executeQuery())
		{
			while (rset.next())
			{
				SchemeEntity loadedScheme = null;
				
				loadedScheme = new SchemeEntity();
				
				loadedScheme.setPlayerId(rset.getInt("PlayerID"));
				loadedScheme.setName(rset.getString("Name"));
				loadedScheme.setDescription(rset.getString("Description"));
				loadedScheme.setCreationDate(rset.getLong("CreatedDate"));
				loadedScheme.setIconId(rset.getInt("IconID"));
				loadedScheme.setRestoreCp(rset.getBoolean("RestoreCP"));
				loadedScheme.setRestoreHp(rset.getBoolean("RestoreHP"));
				loadedScheme.setRestoreMp(rset.getBoolean("RestoreMP"));
				loadedScheme.setShareCode(rset.getString("ShareCode"));
				loadedScheme.setShareCodeLocked(rset.getBoolean("ShareCodeLocked"));
				
				String skillsIds = rset.getString("Skills");
				
				if (skillsIds != null && skillsIds.length() > 0)
				{
					final String[] skills = skillsIds.split(",");
					for (String oneBuff : skills)
					{
						var intBuffId = Integer.parseInt(oneBuff);
						
						if (SchemeBufferData.getInstance().getBuff(intBuffId) != null)
						{
							loadedScheme.addBuffOnLoad(intBuffId);
						}
					}
				}
				loadedScheme.recalculatePrice();
				addSchemeAndGenerateID(loadedScheme);
				restoredSchemes++;
			}
			
		}
		catch (Exception e)
		{
			LOGGER.error("Error occurred while loading schemes from database!", e);
		}
		LOGGER.info("{} schemes were loaded from database.", restoredSchemes);
		
		return true;
	}
	
	
	private static class SingletonHolder {
		protected static final SchemeBufferManager _instance = new SchemeBufferManager();
	}
}