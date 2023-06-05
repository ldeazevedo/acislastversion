package net.sf.l2j.gameserver.custom.fakeplayer;

import net.sf.l2j.commons.logging.CLogger;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.custom.fakeplayer.ai.BuyerAI;
import net.sf.l2j.gameserver.custom.fakeplayer.ai.SellerAI;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.actors.Sex;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Attackable;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.container.player.Appearance;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.network.GameClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FakeHelper {

	private static final CLogger _log = new CLogger(FakeHelper.class.getName());
	public static String[] FAKE_PLAYER_NAMES = new String[]{"DraNe", "ArTAR", "DoLtHAL", "naREREs", "LeThE", "edHElrien", "neroR", "ThANeWen",
			"pHiacaROr", "gORdHRoS", "eRIaN", "uILrO", "LeGA", "vANeL", "me", "dalthIL", "gRIeLdalMaI", "AE", "sAelRa", "DeHCU",
			"UmRuN", "HLIFcloRN", "moCHRIpDUUM", "GLo", "clornrUk", "ima", "dRORNdRiL", "duhan", "rinroM", "Babthor", "Hua",
			"aA", "KUrVaRg", "GoRnUR", "ThAtHRAL", "khakjaL", "Rorri", "thaUDAgim", "IMnE", "LeOfCRO", "SIgHES", "MoRNsIr",
			"lEoFHRuN", "crOmKRum", "ANKAHU", "KroLmBEO", "wULfgund", "taRGwYn", "Firak", "Grandcaror", "Rannir", "Anvorka",
			"Ristrarg", "Axehall", "Nargu", "Huleof", "Eadkrumm", "Thukan", "Megbra", "Dakhnagbag", "Mashco", "Nazbur",
			"Zundakh", "Nargcro", "Zug", "Gruntskum", "Luga", "Gakash", "Hurtzgar", "Gaa", "Kabolg", "Dahkzock",
			"Mashthulnákh", "Argthos", "Ugbei", "Ufork", "Braco", "Lorcthe", "Thanca", "Marg", "Rymgruk", "Sothzurg",
			"Danvi", "Reontalorvall", "Vall'a", "Drasva", "Kahangrornver", "Lormalor", "Am", "Gici", "Be", "Hudbu", "Argchi",
			"Cary", "Laro", "Drohagud", "Zuhi", "Kawalamkan", "Teyao", "Cheungmipo", "Na", "Minglee", "Shilau", "Kawari",
			"Zen", "Dochoi", "Kichun", "Rami", "Jio", "Linyofu", "Makan", "Guori", "Niying", "Tsaiying", "Royi", "Baoga",
			"Wamir", "Tabeen", "Mija", "Thufou", "Rajha", "Rijthi", "Waan", "Raser", "Fisa", "Qardil", "Amsim", "Qina",
			"Sarasa", "Yatin", "Yasghayth", "Halah", "Vidmu", "Taulfa", "Shahqa"};
	private static ClassId classId;
	private static List<Player> fakePlayers = new ArrayList<>();
	private static HashMap<Integer, Location> locationsHash = new HashMap<>();
	private static List<String> usedNames = new ArrayList<>();

	public static void createFakePlayers(int count, City city, FakePlayerType fakePlayerType) {
		for (int i = 0; i < count; i++)
			createRandomFakePlayer(city, fakePlayerType);
		_log.info("Created " + count + " " + fakePlayerType.name().toUpperCase() + " in " + city);
	}

	private static void createRandomFakePlayer(City city, FakePlayerType fakePlayerType) {
		String fakePlayerName = getRandomPlayerName();

		generateRandomClass(fakePlayerType);
		PlayerTemplate playerTemplateData = PlayerData.getInstance().getTemplate(classId);
		Appearance pcAppearance = getRandomAppearance();
		FakePlayer fakePlayer = new FakePlayer(playerTemplateData, fakePlayerName, pcAppearance);

		GameClient client = new GameClient(null);
		client.setDetached(true);
		client.setPlayer(fakePlayer);
		client.setAccountName(fakePlayer.getAccountName());
		client.setState(GameClient.GameClientState.IN_GAME);
		fakePlayer.setClient(client);

		//fakePlayer.setFakePlayer(true);
		fakePlayer.setName(fakePlayerName);
		fakePlayer.setBaseClass(classId);
		fakePlayer.setRecomHave(20);
		fakePlayer.setAccessLevel(0);
		fakePlayer.getStatus().setLevel((byte) Rnd.get(21, 31));
		fakePlayer.setUptime(System.currentTimeMillis());
		fakePlayer.rewardSkills();
		fakePlayer.getStatus().setHp(fakePlayer.getStatus().getMaxHp(), true);
		World.getInstance().addPlayer(fakePlayer);

		Location randomLocation = findRandomLocation(fakePlayer, city);
		fakePlayer.setTeleporting(false);
		fakePlayer.spawnMe(randomLocation.getX(), randomLocation.getY(), randomLocation.getZ());

		fakePlayer.onPlayerEnter();

		//setAiBasedOnClass(fakePlayer);
		switch (fakePlayerType) {
			case TRAINER -> fakePlayer.setRunning(true);
			case SELLER -> fakePlayer.setFakeAi(new SellerAI(fakePlayer, city));
			case BUYER -> fakePlayer.setFakeAi(new BuyerAI(fakePlayer, city));
		}

		giveArmorsByClass(fakePlayer);
		giveWeaponsByClass(fakePlayer);

		fakePlayers.add(fakePlayer);
	}

	private static void generateRandomClass(FakePlayerType fakePlayerType) {
		if (fakePlayerType == FakePlayerType.SELLER || fakePlayerType == FakePlayerType.BUYER) {
			int random = Rnd.get(0, 3);
			switch (random) {
				case 0:
					classId = ClassId.ORC_FIGHTER;
				case 1:
					classId = ClassId.ARTISAN;
					return;
				case 2:
					classId = ClassId.ELVEN_MYSTIC;
					return;
				case 3:
					classId = ClassId.HUMAN_FIGHTER;
					return;
			}
		}
		//ClassId.sagittarius, ClassId.duelist, ClassId.cardinal
		ClassId[] classIds = new ClassId[]{ClassId.DUELIST};
		classId = classIds[Rnd.get(classIds.length)];
	}

	public static Player findFakePlayer(int objectId) {
		return fakePlayers.stream().filter(fp -> fp.getObjectId() == objectId).findFirst().orElse(null);
	}

	private static Location findRandomLocation(FakePlayer activeChar, City city) {
		Location nextLocation = getRandomLocation(city);
		for (Location location : locationsHash.values()) {
			if (nextLocation.getX() == location.getX() && nextLocation.getY() == location.getY())
				return findRandomLocation(activeChar, city);
		}
		locationsHash.put(activeChar.getObjectId(), nextLocation);
		return nextLocation;
	}

	private static String getRandomPlayerName() {
		String randomName = FAKE_PLAYER_NAMES[Rnd.get(FAKE_PLAYER_NAMES.length)];
		if (usedNames.contains(randomName)) {
			randomName = getRandomPlayerName();
		}
		usedNames.add(randomName);
		return randomName;
	}

	private static Appearance getRandomAppearance() {
		return new Appearance((byte) Rnd.get(0, 2), (byte) Rnd.get(0, 2), (byte) Rnd.get(0, 4), Rnd.get(1, 2) == 1 ? Sex.MALE : Sex.FEMALE);
	}

	private static void giveArmorsByClass(Player player) {
		Integer[] armors = new Integer[]{390, 412, 39, 50, 24, 31, 39, 50, 1101, 1104, 39, 50};
		int armor = armors[4 * Rnd.get(3)];
		int pants = armors[1 + 4 * Rnd.get(3)];
		int boots = armors[2 + 4 * Rnd.get(3)];
		int gloves = armors[3 + 4 * Rnd.get(3)];

		for (int id : Arrays.asList(armor, pants, boots, gloves)) {
			player.getInventory().addItem("Armors", id, 1, player, null);
			ItemInstance item = player.getInventory().getItemByItemId(id);
			player.getInventory().equipItemAndRecord(item);
			player.getInventory().reloadEquippedItems();
			player.broadcastCharInfo();
		}

		//Needed items for buffs and SS
		//player.getInventory().addItem("PET", petItemId, 1, player, null);
		player.getInventory().reloadEquippedItems();
		player.broadcastCharInfo();
	}

	private static void generateRandomPet() {
		int[] pets = new int[]{6650, 2375, 6648, 6649};
		int petItemId = pets[Rnd.get(pets.length)];
	}

	private static void giveWeaponsByClass(Player player) {
		Integer itemId = Arrays.asList(273, 155, 219, 87, 15, 68, 310, 100, 177).get(Rnd.get(9));
		player.getInventory().addItem("Weapon", itemId, 1, player, null);
		ItemInstance item = player.getInventory().getItemByItemId(itemId);
		player.getInventory().equipItemAndRecord(item);
		player.getInventory().reloadEquippedItems();
	}

	private static Location getRandomLocation(City city) {
		List<Location> locations = new ArrayList<>();
		switch (city) {
			case GIRAN:
				locations.add(new Location(82685, 147977, -3472));
				locations.add(new Location(82684, 148161, -3472));
				locations.add(new Location(82609, 148282, -3472));
				locations.add(new Location(82679, 148533, -3472));
				locations.add(new Location(82679, 148533, -3472));
				locations.add(new Location(82351, 148161, -3472));
				locations.add(new Location(82417, 147923, -3472));
				locations.add(new Location(83295, 148266, -3408));
				locations.add(new Location(83614, 148307, -3400));
				locations.add(new Location(83787, 148163, -3420));
				locations.add(new Location(83514, 147708, -3400));
				locations.add(new Location(83171, 147705, -3464));
				locations.add(new Location(82909, 147840, -3464));
				locations.add(new Location(83062, 148649, -3495));
				locations.add(new Location(83390, 148738, -3400));
				locations.add(new Location(83658, 148738, -3400));
				locations.add(new Location(84177, 148830, -3400));
				locations.add(new Location(84045, 149148, -3400));
				locations.add(new Location(82470, 148627, -3464));
				locations.add(new Location(82436, 148199, -3495));
				locations.add(new Location(82534, 147992, -3464));
				locations.add(new Location(82645, 147903, -3464));
				locations.add(new Location(82717, 148083, -3495));
				locations.add(new Location(82713, 148284, -3464));
				locations.add(new Location(82703, 148415, -3495));
				locations.add(new Location(82705, 148673, -3464));
				locations.add(new Location(82497, 148635, -3495));
				locations.add(new Location(82497, 148427, -3495));
				locations.add(new Location(82585, 148263, -3495));
				locations.add(new Location(82609, 148420, -3464));
				locations.add(new Location(82437, 148967, -3495));
				locations.add(new Location(82539, 149151, -3464));
				locations.add(new Location(83614, 148307, -3400));
				locations.add(new Location(82279, 149188, -3495));
				break;
			case GLUDIO:
				locations.add(new Location(-14197, 123389, -3120));
				locations.add(new Location(-14486, 123877, -3120));
				locations.add(new Location(-14723, 123541, -3120));
				locations.add(new Location(-14667, 123153, -3120));
				locations.add(new Location(-14421, 123153, -3120));
				locations.add(new Location(-14929, 123978, -3120));
				locations.add(new Location(-12663, 122678, -3112));
				locations.add(new Location(-12793, 122640, -3143));
				locations.add(new Location(-12863, 122726, -3112));
				locations.add(new Location(-12981, 122808, -3112));
				locations.add(new Location(-13082, 122835, -3143));
				locations.add(new Location(-12494, 122654, -3104));
				locations.add(new Location(-12562, 122810, -3141));
				locations.add(new Location(-12329, 122757, -3104));
				locations.add(new Location(-13149, 122328, -3015));
				locations.add(new Location(-13330, 122202, -2984));
				locations.add(new Location(-13593, 122265, -3015));
				locations.add(new Location(-13979, 122416, -3015));
				locations.add(new Location(-14032, 121849, -2984));
				locations.add(new Location(-14245, 121567, -2992));
				locations.add(new Location(-14695, 121424, -2984));
				locations.add(new Location(-13994, 121383, -2984));
				locations.add(new Location(-12816, 123716, -3143));
				locations.add(new Location(-12612, 123391, -3112));
				locations.add(new Location(-12239, 123661, -6096));
				locations.add(new Location(-13392, 124391, -3112));
				locations.add(new Location(-13115, 124569, -3149));
				locations.add(new Location(-14324, 125429, -3136));
				locations.add(new Location(-14625, 125436, -3166));
				locations.add(new Location(-16345, 124256, -3112));
				locations.add(new Location(-16073, 123920, -3143));
				locations.add(new Location(-15379, 124448, -3147));
				locations.add(new Location(-15211, 124669, -3112));
				break;
			case DION:
				locations.add(new Location(18767, 145190, -3131));
				locations.add(new Location(18617, 144828, -3099));
				locations.add(new Location(19208, 144789, -3102));
				locations.add(new Location(18767, 145190, -3131));
				locations.add(new Location(19431, 145534, -3112));
				locations.add(new Location(18767, 145190, -3131));
				locations.add(new Location(18400, 145888, -3101));
				locations.add(new Location(18767, 145190, -3131));
				locations.add(new Location(17739, 145728, -3097));
				locations.add(new Location(20243, 145804, -3164));
				locations.add(new Location(20245, 145529, -3112));
				locations.add(new Location(18994, 143758, -3064));
				locations.add(new Location(19045, 143221, -3040));
				locations.add(new Location(19164, 142773, -3064));
				locations.add(new Location(18767, 145190, -3131));
				locations.add(new Location(19343, 143826, -3056));
				locations.add(new Location(20291, 144435, -3088));
				locations.add(new Location(20452, 144825, -3088));
				locations.add(new Location(20316, 144719, -3118));
				locations.add(new Location(19975, 144722, -3104));
				locations.add(new Location(19919, 144281, -3120));
				locations.add(new Location(18755, 145837, -3180));
				locations.add(new Location(17925, 146513, -3088));
				locations.add(new Location(17503, 146957, -3112));
				locations.add(new Location(17631, 147275, -3120));
				locations.add(new Location(17688, 146360, -3088));
				locations.add(new Location(17370, 145178, -3056));
				locations.add(new Location(16647, 144493, -3000));
				locations.add(new Location(16785, 144167, -3017));
				locations.add(new Location(16147, 143636, -2864));
				locations.add(new Location(16346, 143174, -2744));
				locations.add(new Location(16151, 143020, -2720));
				locations.add(new Location(15892, 142998, -2740));
				locations.add(new Location(15576, 143023, -2704));
				locations.add(new Location(15453, 142857, -2696));
				locations.add(new Location(15212, 142923, -2680));
				locations.add(new Location(15734, 142807, -2696));
				break;
			case GLUDIN:
				locations.add(new Location(-80994, 150162, -3047));
				locations.add(new Location(-80846, 150481, -3047));
				locations.add(new Location(-80560, 149966, -3047));
				locations.add(new Location(-81687, 150401, -3132));
				locations.add(new Location(-81650, 151259, -3132));
				locations.add(new Location(-82232, 150325, -3132));
				locations.add(new Location(-80098, 150168, -3070));
				locations.add(new Location(-81577, 150797, -3155));
				locations.add(new Location(-82220, 151385, -3155));
				locations.add(new Location(-83048, 150983, -3155));
				locations.add(new Location(-83137, 150724, -3155));
				locations.add(new Location(-82842, 150664, -3120));
				locations.add(new Location(-82801, 150205, -3120));
				locations.add(new Location(-83291, 149756, -3155));
				locations.add(new Location(-83813, 149934, -3155));
				locations.add(new Location(-84448, 150618, -3120));
				locations.add(new Location(-84135, 151397, -3120));
				locations.add(new Location(-83238, 151794, -3155));
				locations.add(new Location(-83078, 152769, -3204));
				locations.add(new Location(-84054, 152809, -3204));
				locations.add(new Location(-84629, 152658, -3168));
				locations.add(new Location(-81831, 152613, -3168));
				locations.add(new Location(-80763, 152828, -3168));
				locations.add(new Location(-80642, 153281, -3204));
				locations.add(new Location(-80540, 153958, -3204));
				locations.add(new Location(-80736, 154912, -3168));
				locations.add(new Location(-83100, 155159, -3204));
				locations.add(new Location(-82968, 154989, -3204));
				locations.add(new Location(-83700, 155069, -3204));
				locations.add(new Location(-84082, 154231, -3176));
				locations.add(new Location(-83474, 153687, -3168));
				locations.add(new Location(-82293, 151235, -3120));
				locations.add(new Location(-81726, 150807, -3120));
				locations.add(new Location(-80980, 149799, -3040));
				break;
			case OREN:
				locations.add(new Location(82560, 54137, -1499));
				locations.add(new Location(82232, 53778, -1499));
				locations.add(new Location(82287, 53337, -1499));
				locations.add(new Location(82694, 53192, -1499));
				locations.add(new Location(82881, 53572, -1499));
				locations.add(new Location(82924, 54084, -1499));
				locations.add(new Location(82901, 54581, -1520));
				locations.add(new Location(83351, 54455, -1551));
				locations.add(new Location(82703, 55516, -1520));
				locations.add(new Location(83117, 55534, -1551));
				locations.add(new Location(83224, 55712, -1520));
				locations.add(new Location(83526, 56073, -1520));
				locations.add(new Location(82356, 55995, -1551));
				locations.add(new Location(81922, 55506, -1551));
				locations.add(new Location(81496, 55487, -1520));
				locations.add(new Location(81570, 55103, -1536));
				locations.add(new Location(81737, 54913, -1504));
				locations.add(new Location(80240, 56278, -1586));
				locations.add(new Location(79734, 56290, -1512));
				locations.add(new Location(79333, 56871, -1538));
				locations.add(new Location(80021, 55611, -1586));
				locations.add(new Location(80835, 55154, -1520));
				locations.add(new Location(80961, 54833, -1551));
				locations.add(new Location(80292, 54744, -1586));
				locations.add(new Location(80030, 54412, -1586));
				locations.add(new Location(80512, 53669, -1552));
				locations.add(new Location(80869, 53399, -1552));
				locations.add(new Location(81102, 53140, -1560));
				locations.add(new Location(81539, 53349, -1488));
				locations.add(new Location(81719, 53521, -1522));
				locations.add(new Location(81875, 53694, -1522));
				locations.add(new Location(82183, 53557, -1522));
				locations.add(new Location(82485, 53507, -1488));
				locations.add(new Location(82692, 53816, -1488));
				locations.add(new Location(82948, 53878, -1522));
				locations.add(new Location(82992, 53526, -1480));
				break;
			case HUNTER:
				locations.add(new Location(116683, 76811, -2731));
				locations.add(new Location(117040, 76295, -2732));
				locations.add(new Location(117283, 76495, -2705));
				locations.add(new Location(116941, 77036, -2700));
				locations.add(new Location(116602, 77307, -2721));
				locations.add(new Location(116478, 77683, -2688));
				locations.add(new Location(116177, 77704, -2710));
				locations.add(new Location(115246, 77588, -2752));
				locations.add(new Location(114792, 77981, -2616));
				locations.add(new Location(115237, 77116, -2712));
				locations.add(new Location(116157, 77234, -2720));
				locations.add(new Location(116255, 76512, -2728));
				locations.add(new Location(116114, 76209, -2728));
				locations.add(new Location(115992, 75849, -2720));
				locations.add(new Location(116459, 76169, -2755));
				locations.add(new Location(116412, 75763, -2728));
				locations.add(new Location(115780, 75353, -2592));
				locations.add(new Location(115618, 74838, -2625));
				locations.add(new Location(116847, 75697, -2728));
				locations.add(new Location(117043, 76036, -2755));
				locations.add(new Location(117435, 75791, -2694));
				locations.add(new Location(117700, 76138, -2696));
				locations.add(new Location(117548, 76620, -2720));
				locations.add(new Location(116766, 76748, -2728));
				locations.add(new Location(116060, 77028, -2694));
				locations.add(new Location(115710, 76543, -2694));
				locations.add(new Location(115590, 76001, -2592));
				locations.add(new Location(116329, 75193, -2592));
				locations.add(new Location(116747, 75134, -2592));
				locations.add(new Location(116683, 75998, -2755));
				locations.add(new Location(117206, 76768, -2688));
				break;
			case ADEN:
				break;
		}

		return locations.get(Rnd.get(locations.size()));
	}

	public static Class<? extends Attackable> getTestTargetClass() {
		return Monster.class;
	}

	public static int getTestTargetRange() {
		return 2000;
	}

	public static int[][] getFighterBuffs() {
		return new int[][]{{1204, 2}, // wind walk
				{1040, 3}, // shield
				{1035, 4}, // Mental Shield
				{1045, 6}, // Bless the Body
				{1068, 3}, // might
				{1062, 2}, // besekers
				{1086, 2}, // haste
				{1077, 3}, // focus
				{1388, 3}, // Greater Might
				{1036, 2}, // magic barrier
				{274, 1}, // dance of fire
				{273, 1}, // dance of fury
				{268, 1}, // dance of wind
				{271, 1}, // dance of warrior
				{267, 1}, // Song of Warding
				{349, 1}, // Song of Renewal
				{264, 1}, // song of earth
				{269, 1}, // song of hunter
				{364, 1}, // song of champion
				{1363, 1}, // chant of victory
				{4699, 5} // Blessing of Queen
		};
	}

	public static int[][] getMageBuffs() {
		return new int[][]{{1204, 2}, // wind walk
				{1040, 3}, // shield
				{1035, 4}, // Mental Shield
				{4351, 6}, // Concentration
				{1036, 2}, // Magic Barrier
				{1045, 6}, // Bless the Body
				{1303, 2}, // Wild Magic
				{1085, 3}, // acumen
				{1062, 2}, // besekers
				{1059, 3}, // empower
				{1389, 3}, // Greater Shield
				{273, 1}, // dance of the mystic
				{276, 1}, // dance of concentration
				{365, 1}, // Dance of Siren
				{264, 1}, // song of earth
				{268, 1}, // song of wind
				{267, 1}, // Song of Warding
				{349, 1}, // Song of Renewal
				{1413, 1}, // Magnus\' Chant
				{4703, 4} // Gift of Seraphim
		};
	}

	public enum FakePlayerType {
		SELLER, TRAINER, BUYER
	}

	public enum City {
		GIRAN, GLUDIO, DION, GLUDIN, OREN, HUNTER, ADEN;
	}
}