package net.sf.l2j.gameserver.model.events;

import net.sf.l2j.gameserver.model.events.util.Tuple;

import java.util.ArrayList;
import java.util.List;

public class EventConstants {

	public static final String REGISTER = ".reg1";
	public static final String UNREGISTER = ".unreg1";
	public static final String WATCH = ".ver";
	public static final String EXIT = ".salir";

	public static final int LOADING_START_TIME = 1000 * 60; // 1 minute
	public static final int PREPARING_START_TIME = 1000 * 70; // 1 minute 10 seconds
	public static final int PREPARING_START_TIME2 = 1000 * 85; // 1 minute 25 seconds
	public static final int FIGHT_START_TIME = 1000 * 115; // 1 minute 55 seconds
	public static final int ENDING_START_TIME = 1000 * 295; // 4 minutes 55 seconds
	public static final String STAGE_1 = "STG01";
	public static final String STAGE_2 = "STG02";
	public static final String STAGE_3 = "STG03";
	public static final String STAGE_4 = "STG04";
	public static final String STAGE_5 = "STG05";

	public static List<Tuple<String, Integer>> RANDOM_FIGHT_TIMER_CONFIG = new ArrayList<>();

	static {
		RANDOM_FIGHT_TIMER_CONFIG.add(new Tuple<>(STAGE_1, LOADING_START_TIME));
		RANDOM_FIGHT_TIMER_CONFIG.add(new Tuple<>(STAGE_2, PREPARING_START_TIME));
		RANDOM_FIGHT_TIMER_CONFIG.add(new Tuple<>(STAGE_3, PREPARING_START_TIME2));
		RANDOM_FIGHT_TIMER_CONFIG.add(new Tuple<>(STAGE_4, FIGHT_START_TIME));
		RANDOM_FIGHT_TIMER_CONFIG.add(new Tuple<>(STAGE_5, ENDING_START_TIME));
	}

	public static final String QUERY_EVENT_INFO = "select * from rf where char_name=?";
	public static final String UPDATE_EVENT_INFO = "update rf set count=count+1 where char_name=?";
	public static final String INSERT_EVENT_INFO = "insert rf set count=1,char_name=?";
}