package net.sf.l2j.gameserver.session;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import net.sf.l2j.commons.util.SortType;


public class ParamsCache
{
	private HashMap<String, Integer> _intParameters;
	private HashMap<String, ArrayList<Integer>> _intArrayParameters;
	private HashMap<String, String> _stringParameters;
	private HashMap<String, SortType> _sortingParameters;
	private HashMap<String, Boolean> _booleanParameters;
	private HashMap<String, HashMap<Integer,Integer>> _dictionaryIntParameters;

	public ParamsCache()
	{
		_intParameters = new HashMap<>();
		_stringParameters = new HashMap<>();
		_intArrayParameters = new HashMap<>();
		_sortingParameters = new HashMap<>();
		_booleanParameters = new HashMap<>();
		_dictionaryIntParameters = new HashMap<>();
	}
	
	
	public  HashMap<Integer,Integer> getIntDictionary(String param, boolean initWhenNull)
	{
		if (initWhenNull && !_dictionaryIntParameters.containsKey(param))
		{
			_dictionaryIntParameters.put(param, new HashMap<>());
		}
		return _dictionaryIntParameters.get(param);
	}
	
	public  HashMap<Integer,Integer> getIntDictionary(String param)
	{
		return _dictionaryIntParameters.get(param);
	}
	
	public void addToIntDictionary(String param, int key, int value)
	{
		if (!_dictionaryIntParameters.containsKey(param))
		{
			_dictionaryIntParameters.put(param, new HashMap<>());
		}
		_dictionaryIntParameters.get(param).put(key, value);
	}
	
	public void addSortOrder(String param, SortType value) {
		_sortingParameters.put(param, value);
	}
	
	public SortType getSortOrder(String param)
	{
		if (!_sortingParameters.containsKey(param))
		{
			return SortType.NONE;
		}
		var val = _sortingParameters.get(param);
		
		return val == null ? SortType.NONE : val;
	}
	
	public SortType getSortOrder(String param, SortType defaultValue)
	{
		if (!_sortingParameters.containsKey(param))
		{
			return defaultValue;
		}
		var val = _sortingParameters.get(param);
		
		return val == null ?  defaultValue : val;
	}
	
	public void switchSortOrder(String param)
	{
		var sortOrder = getSortOrder(param);

		if (sortOrder == SortType.NONE)
		{
			sortOrder = SortType.ASC;
		}
		else if(sortOrder == SortType.ASC)
		{
			sortOrder = SortType.DESC;
		}
		else
		{
			sortOrder = SortType.NONE;
		}
		addSortOrder(param, sortOrder);
	}
	
	
	public void addInt(String param, Integer value) {
		_intParameters.put(param, value);
	}
	
	//if same value is added then set null
	public void swapInt(String param, Integer value) {
		if (!_intParameters.containsKey(param) || !Objects.equals(_intParameters.get(param), value))
		{
			_intParameters.put(param, value);
		}
		else
		{
			_intParameters.put(param, null);
		}
	}
	
	public void addBoolean(String param, boolean value) {
		_booleanParameters.put(param, value);
	}
	
	public boolean getBoolean(String param) {
		if (!_booleanParameters.containsKey(param))
		{
			_booleanParameters.put(param,false);
		}
		return _booleanParameters.get(param);
	}
	
	public boolean getBoolean(String param, boolean defaultValue) {
		if (!_booleanParameters.containsKey(param))
		{
			return defaultValue;
		}
		return _booleanParameters.get(param);
	}
	
	public void addString(String param, String value) {
		_stringParameters.put(param, value);
	}
	
	public void addIntArray(String param, Integer[] value) {
		_intArrayParameters.put(param, new ArrayList<>(Arrays.asList(value)));
	}
	
	public void addToIntArray(String param, Integer value) {
		if (!_intArrayParameters.containsKey(param))
		{
			_intArrayParameters.put(param, new ArrayList<>());
		}
		
		_intArrayParameters.get(param).add(value);
	}
	
	public void swapInIntArray(String param, Integer value) {
		if (!_intArrayParameters.containsKey(param))
		{
			_intArrayParameters.put(param, new ArrayList<>());
		}
		
		if (_intArrayParameters.get(param).contains(value))
		{
			_intArrayParameters.get(param).remove(_intArrayParameters.get(param).indexOf(value));
		}
		else
		{
			_intArrayParameters.get(param).add(value);
		}
	}
	
	public void clampInt(String param, int min, int max)
	{
		if (!_intParameters.containsKey(param)) {
			_intParameters.put(param, min);
		}
		else
		{
			var val = _intParameters.get(param);
			if (val > max)
			{
				_intParameters.put(param, max);
			}
			else if (val < min)
			{
				_intParameters.put(param, min);
			}
		}
	}
	
	public Integer getInt(String param, int defaultValue) {
		if (!_intParameters.containsKey(param)) {
			return defaultValue;
		}
		var val = _intParameters.get(param);
		
		return val == null ? defaultValue : val;
	}
	
	public Integer getInt(String param) {
		if (!_intParameters.containsKey(param)) {
			return null;
		}
		return _intParameters.get(param);
	}
	
	public Integer[] getIntArray(String param) {
		if (!_intArrayParameters.containsKey(param)) {
			return null;
		}
		return _intArrayParameters.get(param).toArray(new Integer[0]);
	}
	
	public String getString(String param, String defaultValue) {
		if (!_stringParameters.containsKey(param)) {
			return defaultValue;
		}
		var val = _stringParameters.get(param);
		
		return val == null ? defaultValue : val;
	}
	
	public String getString(String param) {
		if (!_stringParameters.containsKey(param)) {
			return null;
		}
		return _stringParameters.get(param);
	}
	
	public void clear()
	{
		_intArrayParameters.clear();
		_stringParameters.clear();
		_intParameters.clear();
	}
}