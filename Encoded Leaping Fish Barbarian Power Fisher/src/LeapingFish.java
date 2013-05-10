package scripts;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Arrays;

import javax.swing.JOptionPane;

import org.tribot.api.DynamicClicking;
import org.tribot.api.General;
import org.tribot.api.Timing;
import org.tribot.api.input.Mouse;
import org.tribot.api2007.Camera;
import org.tribot.api2007.GameTab;
import org.tribot.api2007.GameTab.TABS;
import org.tribot.api2007.ChooseOption;
import org.tribot.api2007.GroundItems;
import org.tribot.api2007.Interfaces;
import org.tribot.api2007.Inventory;
import org.tribot.api2007.NPCChat;
import org.tribot.api2007.NPCs;
import org.tribot.api2007.Player;
import org.tribot.api2007.Skills;
import org.tribot.api2007.Walking;
import org.tribot.api2007.types.RSGroundItem;
import org.tribot.api2007.types.RSItem;
import org.tribot.api2007.types.RSNPC;
import org.tribot.api2007.types.RSTile;
import org.tribot.script.Script;
import org.tribot.script.ScriptManifest;
import org.tribot.script.interfaces.Painting;

@ScriptManifest(authors = { "Encoded" }, category = "Fishing", name = "Encoded Leaping Fish", description= "<font face=\"Georgia\" color=\"orange\" size=\"5\"><b>Encoded Leaping Fish</b></font><br><font size=\"3\"><i>Leaping Fish Barbarian Power Fisher</i><br><br><b>Minimum Requirements:</b><br>48 Fishing, 15 Agility, 15 Strength<br><br>For more information about this script, please refer to the official thread.<br>https://tribot.org/forums/showthread.php?tid=7827")

public class LeapingFish extends Script implements Painting {

	private final int FISH_SPOT_ID = 2722,
			WHIRLPOOL_ID = 2723,
			EVIL_FISH_ID = 390,
			BARBARIAN_ROD_ID = 11323,
			KNIFE_ID = 946,
			FISHING_BAIT_ID = 313,
			FEATHER_ID = 314,
			OFFCUTS_ID = 11334,
			ROE_ID = 11324,
			CAVIAR_ID = 11326,
			FISHING_START_XP = Skills.getXP("Fishing"),
			STRENGTH_START_XP = Skills.getXP("Strength"),
			AGILITY_START_XP = Skills.getXP("Agility"),
			COOKING_START_XP = Skills.getXP("Cooking"),
			FISHING_START_LEVEL = Skills.getCurrentLevel("Fishing"),
			STRENGTH_START_LEVEL = Skills.getCurrentLevel("Strength"),
			AGILITY_START_LEVEL = Skills.getCurrentLevel("Agility"),
			COOKING_START_LEVEL = Skills.getCurrentLevel("Cooking");
	private final int[] LEAPING_FISH_ID =  { 11328, 11330, 11332 },
			FEATHER_BAIT = { 313, 314 },
			BAIT = { 313, 314, 11334 },
			ROE_CAVIAR = { 11324, 11326 },
			ITEMS_TO_KEEP = { 11323, 985, 987, 946 },
			JUNK = { 995, 117, 464, 6183, 1454, 1604, 1606, 1608, 1603, 1605, 1607, 1619, 1621, 1623, 3050, 3052, 1971, 1917, 1969, 1973, 6963, 6965, 6961, 6962, 9003, 2327, 1886, 1617, 1618, 1601, 1602 },
			ITEMS_TO_DROP = { 11328, 11330, 11332, 995, 117, 464, 6183, 1454, 1604, 1606, 1608, 1603, 1605, 1607, 1619, 1621, 1623, 3050, 3052, 1971, 1917, 1969, 1973, 6963, 6965, 6961, 6962, 9003, 2327, 11326, 11324, 1886, 1617, 1618, 1601, 1602 };
	private final long START_TIME = System.currentTimeMillis();
	private final RSTile MIDDLE_TILE = new RSTile(2509, 3538, 0),
			OTTO_HOUSE_TILE = new RSTile(2503, 3488, 0),
			BARB_BED = new RSTile(2500, 3490, 0),
			RESET_TILE = new RSTile(2501, 3501, 0),
			FISH_TILE = new RSTile(2504, 3518, 0);
	private int fCurrentXp = FISHING_START_XP,
			sCurrentXp = STRENGTH_START_XP,
			aCurrentXp = AGILITY_START_XP,
			cCurrentXp = COOKING_START_XP,
			escapeTileX, 
			escapeTileY,
			fishTileX,
			fishTileY,
			mouseX,
			mouseY,
			idle,
			teleportFailSafe = 0,
			fishCount = 0,
			fishXp,
			invi = 28 - Inventory.find(ITEMS_TO_KEEP).length,
			bait = Inventory.getCount(FISHING_BAIT_ID) + Inventory.getCount(FEATHER_ID) + Inventory.getCount(OFFCUTS_ID),
			fishoffcuts = Inventory.getCount(OFFCUTS_ID),
			roeAndCaviar = Inventory.getCount(ROE_ID) + Inventory.getCount(CAVIAR_ID);
	private State scriptState;
	private RSItem dropFish;
	private RSTile escapeTile,
	fishTile;
	private boolean hover = false,
			mouseMove = false,
			mouseClick = false,
			fishSpotClick,
			guiWait = true,
			F1D1 = false,
			dropWhenFull = false,
			cutFish = false,
			teleport = false,
			rodClick = false;

	public State getState() {
		RSNPC[] fishSpot = NPCs.findNearest(new int[] { FISH_SPOT_ID });
		RSNPC[] whirlpool = NPCs.findNearest(new int[] { WHIRLPOOL_ID });
		RSNPC[] evilFish = NPCs.findNearest(new int[] { EVIL_FISH_ID });
		if (evilFish.length > 0 && evilFish[0].getPosition().distanceTo(Player.getPosition()) < 2) {
			return State.WAIT_FOR_ROD;
		}
		if (Player.getRSPlayer().isInCombat()) {
			return State.COMBAT;
		}
		if (Inventory.find(BARBARIAN_ROD_ID).length < 1 && GroundItems.findNearest(BARBARIAN_ROD_ID).length > 0) {
			return State.LOST_ROD;
		}
		if (fishSpot.length > 0 && whirlpool.length > 0 && Player.getRSPlayer().getInteractingCharacter() != null && Player.getRSPlayer().getInteractingCharacter().getIndex() == whirlpool[0].getIndex()) {
			return State.WHIRLPOOL;
		}	
		if (Inventory.find(BARBARIAN_ROD_ID).length < 1 && GroundItems.findNearest(BARBARIAN_ROD_ID).length < 1 && Player.getPosition().distanceTo(MIDDLE_TILE) < 60) {
			return State.NEW_ROD;
		}
		if ((GroundItems.findNearest(FEATHER_BAIT).length > 0 || GroundItems.findNearest(KNIFE_ID).length > 0) && !Inventory.isFull()) {
			return State.PICK_UP_SUPPLIES;
		}
		if (Inventory.find(BAIT).length < 1 && GroundItems.findNearest(BAIT).length < 1 && cutFish == false || Inventory.find(BAIT).length < 1 && GroundItems.findNearest(BAIT).length < 1 && Inventory.find(ROE_CAVIAR).length < 1 && Inventory.find(LEAPING_FISH_ID).length < 1 && cutFish == true) {
			return State.NO_BAIT;
		}
		if (NPCs.findNearest(2481).length > 0 && NPCs.findNearest(2479).length > 0) {
			return State.EVIL_BOB;
		}
		if (fishSpot.length > 0 && fishSpot != null && whirlpool.length < 1 && !fishSpot[0].isOnScreen() && Player.getPosition().distanceTo(MIDDLE_TILE) < 60 || fishSpot.length > 0 && fishSpot != null && whirlpool.length > 0 && !fishSpot[0].isOnScreen() && Player.getPosition().distanceTo(MIDDLE_TILE) < 60 && fishSpot[0].getPosition().distanceTo(whirlpool[0].getPosition()) < 1) {
			return State.FIND_FISH;
		}
		if (Player.getPosition().distanceTo(MIDDLE_TILE) > 60) {
			return State.TELEPORT;
		}
		if (F1D1) {
			if (Inventory.find(BARBARIAN_ROD_ID).length > 0 && Inventory.find(BAIT).length > 0 && fishSpot.length > 0 && fishSpot != null && fishSpot[0].isOnScreen()) {
				if (Player.getAnimation() == -1 && Inventory.find(LEAPING_FISH_ID).length < 2) {
					return State.CLICK_FISH;
				}
				if (Inventory.find(LEAPING_FISH_ID).length > 1 || Inventory.find(JUNK).length > 0) {
					return State.DROP_FISH;
				}
				if (Player.getAnimation() != -1 && Inventory.find(LEAPING_FISH_ID).length > 0) {
					return State.HOVER_FISH;
				}
			}
		}
		if (dropWhenFull) {
			if (Inventory.find(BARBARIAN_ROD_ID).length > 0 && Inventory.find(BAIT).length > 0 && fishSpot.length > 0 && fishSpot != null && fishSpot[0].isOnScreen()) {
				if (Player.getAnimation() == -1 && !Inventory.isFull()) {
					return State.CLICK_FISH;
				}
				if (Inventory.isFull()) {
					return State.DROP_FISH;
				}
				if ((Player.getAnimation() == 622 && !Player.getRSPlayer().isInCombat()) || (Player.getAnimation() == 623 && !Player.getRSPlayer().isInCombat())) {
					return State.FISHING;
				}
			}
		}
		if (cutFish) {
			if (Inventory.find(KNIFE_ID).length < 1 && GroundItems.findNearest(KNIFE_ID).length < 1) {
				return State.NO_KNIFE;
			}
			if (Inventory.find(BARBARIAN_ROD_ID).length > 0 && (Inventory.find(BAIT).length > 0 || Inventory.find(ROE_CAVIAR).length > 0 || Inventory.find(LEAPING_FISH_ID).length > 0) && fishSpot.length > 0 && fishSpot != null && fishSpot[0].isOnScreen()) {
				if (Inventory.isFull() || Inventory.find(JUNK).length > 0 || Inventory.find(LEAPING_FISH_ID).length > 0 && Inventory.find(BAIT).length < 1 && Inventory.find(ROE_CAVIAR).length < 1) {
					return State.CUT_FISH;
				}
				if (bait > invi && Inventory.find(LEAPING_FISH_ID).length < 1 && Inventory.find(ROE_CAVIAR).length > 0) {
					return State.DROP_FISH;
				}
				if ((Player.getAnimation() == -1 || Player.getAnimation() == 6702) && !Inventory.isFull() && bait <= invi && bait + roeAndCaviar > 0 || Player.getAnimation() == -1 && !Inventory.isFull() && bait > invi && Inventory.find(ROE_CAVIAR).length < 1) {
					return State.CLICK_FISH;
				}
				if ((Player.getAnimation() == 622 && !Player.getRSPlayer().isInCombat()) || (Player.getAnimation() == 623 && !Player.getRSPlayer().isInCombat())) {
					return State.FISHING;
				}
			}
		}
		if (NPCChat.getClickContinueInterface() != null) {
			return State.CLICK_CONTINUE;
		}
		if (Camera.getCameraAngle() < 85) {
			return State.CAMERA_ADJUSTMENT;
		}
		return State.SLEEP;
	}

	private void doF1D1Fish() {
		RSNPC[] fishSpot = NPCs.findNearest(new int[] { FISH_SPOT_ID });
		RSNPC[] whirlpool = NPCs.findNearest(new int[] { WHIRLPOOL_ID });
		if (fishSpot != null && fishSpot.length > 0 && whirlpool.length > 0 && fishSpot[0].getPosition().distanceTo(whirlpool[0].getPosition()) > 0 && fishSpot[0].isOnScreen()) {
			fishSpotClick = fishSpot[0].click("Use-rod");
			if (fishSpotClick == true && fishSpot[0].isOnScreen()) {
				int i = 0;
				while (i < 500 && Player.getAnimation() == -1) {
					sleep(1);
					i++;
				}
				hover = false;
				mouseMove = false;
				mouseClick = false;
				rodClick = false;
				while (Player.isMoving()) {
					sleep(5, 10);
				}
			}
		}
		if (fishSpot != null && fishSpot.length > 0 && whirlpool.length < 1 && fishSpot[0].isOnScreen()) {
			fishSpotClick = fishSpot[0].click("Use-rod");
			if (fishSpotClick == true && fishSpot[0].isOnScreen()) {
				int i = 0;
				while (i < 500 && Player.getAnimation() == -1) {
					sleep(1);
					i++;
				}
				hover = false;
				mouseMove = false;
				mouseClick = false;
				rodClick = false;
				while (Player.isMoving()) {
					sleep(5, 10);
				}
			}
		}
	}

	private void doHover() {
		RSItem[] fish = Inventory.find(LEAPING_FISH_ID);
		if (GameTab.getOpen() != GameTab.TABS.INVENTORY) {
			GameTab.open(TABS.INVENTORY);
		}
		dropFish = fish[0];
		if (fish != null && fish.length > 0 && GameTab.getOpen() == GameTab.TABS.INVENTORY) {
			if (hover == false) {
				fish[0].hover();
				hover = true;
			}
			if (hover == true && mouseMove == false) { 
				Mouse.click(3);
				mouseX = (int) Mouse.getPos().getX() + General.random(8, -8);
				mouseY = (int) Mouse.getPos().getY() + General.random(38, 46);
				Mouse.move(mouseX, mouseY); //hovers mouse over drop option when only 1 fish is in inventory
				mouseX = (int) Mouse.getPos().getX();
				mouseY = (int) Mouse.getPos().getY();
				mouseMove = true;
			}
		}
	}

	private void doF1D1Drop() {
		RSItem[] fish = Inventory.find(LEAPING_FISH_ID);
		if (GameTab.getOpen() != GameTab.TABS.INVENTORY) {
			GameTab.open(TABS.INVENTORY);
		}
		if (dropFish != null  && GameTab.getOpen() == GameTab.TABS.INVENTORY) {
			if (mouseX != (int) Mouse.getPos().getX() || mouseY != (int) Mouse.getPos().getY()) {
				mouseMove = false; //fail safe in case mouse moves between doHover() and doF1D1Drop(), normally due to user input or random event
			}
			if (fish != null && fish.length > 2 && mouseMove ==  true) {
				mouseMove = false; // fail safe to reset dropping if misclick Drop when using hover
			}
			if (mouseMove == false) {
				sleep(1000, 1200);
				dropAll(ITEMS_TO_DROP);
			}
			if (mouseMove == true && mouseClick == false) {
				Mouse.click(1); // works better than ChooseOption.select("Drop");
				mouseClick = true; // fail safe to prevent clicking more than once
			}
			if (mouseMove == true && mouseClick == true && dropFish == null) {
				mouseMove = false; // fail safe in case doesn't click Drop
			}
		}
		if (mouseMove == false  && GameTab.getOpen() == GameTab.TABS.INVENTORY) {
			if (fish != null && fish.length > 2) { //fail safe in case more than 2 fish in inventory
				sleep(1000, 1200);
				dropAll(ITEMS_TO_DROP);
			}
		}
		if (dropFish == null  && GameTab.getOpen() == GameTab.TABS.INVENTORY) { 
			if (fish != null && fish.length > 0) { //fail safe in case inventory contained fish before starting
				sleep(1000, 1200);
				dropAll(ITEMS_TO_DROP);
			}
		}
		if (Inventory.find(JUNK).length > 0 && GameTab.getOpen() == GameTab.TABS.INVENTORY) {
			sleep(1000, 1200);
			dropAll(ITEMS_TO_DROP);
		}
	}

	private void doDWFFish() {
		RSNPC[] fishSpot = NPCs.findNearest(new int[] { FISH_SPOT_ID });
		RSNPC[] whirlpool = NPCs.findNearest(new int[] { WHIRLPOOL_ID });
		if (fishSpot != null && fishSpot.length > 0 && whirlpool.length > 0 && fishSpot[0].getPosition().distanceTo(whirlpool[0].getPosition()) > 0 && fishSpot[0].isOnScreen()) {
			fishSpotClick = fishSpot[0].click("Use-rod");
			if (fishSpotClick == true && fishSpot[0].isOnScreen()) {
				int i = 0;
				while (i < 500 && Player.getAnimation() == -1) {
					sleep(1);
					i++;
				}
				rodClick = false;
				while (Player.isMoving()) {
					sleep(5, 10);
				}
			}
		}
		if (fishSpot != null && fishSpot.length > 0 && whirlpool.length < 1 && fishSpot[0].isOnScreen()) {
			fishSpotClick = fishSpot[0].click("Use-rod");
			if (fishSpotClick == true && fishSpot[0].isOnScreen()) {
				int i = 0;
				while (i < 500 && Player.getAnimation() == -1) {
					sleep(1);
					i++;
				}
				rodClick = false;
				while (Player.isMoving()) {
					sleep(5, 10);
				}
			}
		}
	}

	private void doDropWhenFull() {
		if (GameTab.getOpen() != GameTab.TABS.INVENTORY) {
			GameTab.open(TABS.INVENTORY);
		}
		fishSpotClick = false;
		if (GameTab.getOpen() == GameTab.TABS.INVENTORY) {
			sleep(1000, 1200); //sleep so inventory is full notification doesn't mess up dropping
			dropAll(ITEMS_TO_DROP);
		}
	}

	private void doCFFish() {
		RSNPC[] fishSpot = NPCs.findNearest(new int[] { FISH_SPOT_ID });
		RSNPC[] whirlpool = NPCs.findNearest(new int[] { WHIRLPOOL_ID });
		if (fishSpot != null && fishSpot.length > 0 && whirlpool.length > 0 && fishSpot[0].getPosition().distanceTo(whirlpool[0].getPosition()) > 0 && fishSpot[0].isOnScreen()) {
			fishSpotClick = fishSpot[0].click("Use-rod");
			if (fishSpotClick == true && fishSpot[0].isOnScreen()) {
				int i = 0;
				while (i < 500 && Player.getAnimation() == -1) {
					sleep(1);
					i++;
				}
				rodClick = false;
				while (Player.isMoving()) {
					sleep(5, 10);
				}
			}
		}
		if (fishSpot != null && fishSpot.length > 0 && whirlpool.length < 1 && fishSpot[0].isOnScreen()) {
			fishSpotClick = fishSpot[0].click("Use-rod");
			if (fishSpotClick == true && fishSpot[0].isOnScreen()) {
				int i = 0;
				while (i < 500 && Player.getAnimation() == -1) {
					sleep(1);
					i++;
				}
				rodClick = false;
				while (Player.isMoving()) {
					sleep(5, 10);
				}
			}
		}
	}

	private void doCutFish() {
		fishSpotClick = false;
		RSItem[] offcuts = Inventory.find(OFFCUTS_ID);
		RSItem[] fish = Inventory.find(LEAPING_FISH_ID);
		if (GameTab.getOpen() != GameTab.TABS.INVENTORY) {
			GameTab.open(TABS.INVENTORY);
		}
		if (GameTab.getOpen() == GameTab.TABS.INVENTORY) {
			if (Inventory.isFull() && Inventory.find(OFFCUTS_ID).length < 1 && fish.length > 0) {
				sleep(1000, 1200);
				fish[0].hover();
				Mouse.click(3);
				Timing.waitMenuOpen(2000);
				if (ChooseOption.isOpen()) {
					if (ChooseOption.isOptionValid("Drop")) {
						ChooseOption.select("Drop");
						sleep(300, 400);
					}
					if (!ChooseOption.isOptionValid("Drop")) {
						ChooseOption.select("Cancel");
						sleep(300, 400);
					}
				}
			}
			if (Inventory.find(JUNK).length > 0) {
				sleep(1000, 1200);
				dropAll(JUNK);
			}
			if (Inventory.find(LEAPING_FISH_ID).length > 0 && Inventory.find(KNIFE_ID).length > 0) {
				sleep(1000, 1200);
				cut(LEAPING_FISH_ID);
			}
			if (bait > invi && Inventory.find(LEAPING_FISH_ID).length < 1) {
				sleep(1000, 1200);
				dropAll(ITEMS_TO_DROP);
			}
			if (roeAndCaviar == invi - 1 && fishoffcuts > 1 && Inventory.find(LEAPING_FISH_ID).length < 1 && offcuts.length > 0) {
				sleep(1000, 1200);
				offcuts[0].hover();
				Mouse.click(3);
				Timing.waitMenuOpen(2000);
				if (ChooseOption.isOpen()) {
					if (ChooseOption.isOptionValid("Drop")) {
						ChooseOption.select("Drop");
						sleep(300, 400);
					}
					if (!ChooseOption.isOptionValid("Drop")) {
						ChooseOption.select("Cancel");
						sleep(300, 400);
					}
				}
			}
		}
	}

	private void dropAll(final int... ids) { // custom, more accurate dropping
		Arrays.sort(ids);
		for (RSItem i : Inventory.getAll()) {
			if (Arrays.binarySearch(ids, i.getID()) > -1) {
				if (Inventory.find(ids).length > 0) {
					i.hover();
					Mouse.click(3);
					Timing.waitMenuOpen(2000);
					if (ChooseOption.isOpen()) {
						if (ChooseOption.isOptionValid("Drop")) {
							ChooseOption.select("Drop");
							sleep(195, 215);
						}
						if (!ChooseOption.isOptionValid("Drop")) {
							ChooseOption.select("Cancel");
							sleep(195, 215);
						}
					}
				}
			}
		}
	}

	private void cut(final int... ids) {
		RSItem[] knife = Inventory.find(946);
		Arrays.sort(ids);
		for (RSItem i : Inventory.getAll()) {
			if (Arrays.binarySearch(ids, i.getID()) > -1) {
				if (Inventory.find(ids).length > 0) {
					if (i.click("Use")) {
						knife[0].click("->");
						sleep(195, 215);
					}
				}
			}
		}
	}

	private void findFish() {
		RSNPC[] fishSpot = NPCs.findNearest(new int[] { FISH_SPOT_ID });
		RSNPC[] whirlpool = NPCs.findNearest(new int[] { WHIRLPOOL_ID });
		if (fishSpot != null && fishSpot.length > 0 && whirlpool.length < 1 && !fishSpot[0].isOnScreen() || fishSpot != null && fishSpot.length > 0 && !fishSpot[0].isOnScreen() && whirlpool.length > 0 && fishSpot[0].getPosition().distanceTo(whirlpool[0].getPosition()) > 0) {
			fishTileX = fishSpot[0].getPosition().getX() + General.random(-1, 1);
			fishTileY = fishSpot[0].getPosition().getY() + General.random(-1, 1);
			fishTile = new RSTile(fishTileX, fishTileY, 0);
			Walking.walkPath(Walking.generateStraightPath(fishTile));
			while (Player.isMoving() && Inventory.find(BARBARIAN_ROD_ID).length > 0 && GroundItems.findNearest(BARBARIAN_ROD_ID).length < 1) {
				sleep(5, 10);
			}
		}
	}

	private void avoidWhirlpool() {
		Walking.walkTo(Player.getPosition());
		sleep(500, 650);
		println("Whirlpool avoided.");
	}

	private void avoidCombat() {
		escapeTileX = Player.getPosition().getX() + General.random(-2, 2);
		escapeTileY = Player.getPosition().getY() + General.random(12, 14);
		escapeTile = new RSTile(escapeTileX, escapeTileY, 0);
		Walking.walkPath(Walking.generateStraightPath(escapeTile));
		println("Getting out of combat.");
		int i = 0;
		while (i < 9000 && Player.getRSPlayer().isInCombat()) {
			sleep(1);
			i++;
		}
	}

	private void waitForRod() {
		int i = 0;
		while (i < 6000 && NPCs.findNearest(390).length > 0) {
			sleep(1);
			i++;
		}
		sleep(1650, 1750);
		Walking.walkTo(Player.getPosition()); // attempts to stay at current position and not run off without the rod in inventory
		sleep(100, 200);
	}

	private void getNewRod() {
		if (!OTTO_HOUSE_TILE.isOnScreen() && !BARB_BED.isOnScreen()) {
			Walking.walkPath(Walking.generateStraightScreenPath(OTTO_HOUSE_TILE));
		}
		if (BARB_BED.isOnScreen()) {
			rodClick = DynamicClicking.clickRSTile(BARB_BED, "Search");
			if (rodClick == true) {
				println("Getting new rod.");
				sleep(200, 400);
			}
		}
		while (Player.isMoving()) {
			sleep(5, 10);
		}
	}

	private void pickUpSupplies() {
		RSGroundItem[] rod = GroundItems.findNearest(BARBARIAN_ROD_ID);
		RSGroundItem[] feather = GroundItems.findNearest(FEATHER_ID);
		RSGroundItem[] fishingBait = GroundItems.findNearest(FISHING_BAIT_ID);
		RSGroundItem[] knife = GroundItems.findNearest(KNIFE_ID);
		if (rod != null && rod.length > 0) {
			if (!rod[0].isOnScreen()) {
				Walking.walkPath(Walking.generateStraightScreenPath(rod[0].getPosition()));
			} else {
				if (DynamicClicking.clickRSTile(rod[0].getPosition(), "Take Barbarian rod")) {
					sleep(200, 400);
					println("Picked up lost rod.");
				}
			}
		}
		if (feather != null && feather.length > 0) {
			if (!feather[0].isOnScreen()) {
				Walking.walkPath(Walking.generateStraightScreenPath(feather[0].getPosition()));
			} else {
				if (DynamicClicking.clickRSTile(feather[0].getPosition(), "Take Feather")) {
					sleep(200, 400);
				}
			}
		}
		if (fishingBait != null && fishingBait.length > 0) {
			if (!fishingBait[0].isOnScreen()) {
				Walking.walkPath(Walking.generateStraightScreenPath(fishingBait[0].getPosition()));
			} else {
				if (DynamicClicking.clickRSTile(fishingBait[0].getPosition(), "Take Fishing bait")) {
					sleep(200, 400);
				}
			}
		}
		if (knife != null && knife.length > 0) {
			if (!knife[0].isOnScreen()) {
				Walking.walkPath(Walking.generateStraightScreenPath(knife[0].getPosition()));
			} else {
				if (DynamicClicking.clickRSTile(knife[0].getPosition(), "Take Knife")) {
					sleep(200, 400);
				}
			}
		}
		while (Player.isMoving()) {
			sleep(5, 10);
		}
	}

	private void gamesNecklaceTeleport() {
		if (GameTab.getOpen() != TABS.EQUIPMENT) {
			GameTab.open(TABS.EQUIPMENT);
			sleep(500, 1000);
		}
		if (GameTab.getOpen() == TABS.EQUIPMENT) {
			if (teleportFailSafe >= 3) {
				println("Failed to teleport via games necklace. Stopping.");
				stopScript();
			}
			if (Interfaces.get(387, 14) != null && teleportFailSafe < 3) {
				Interfaces.get(387, 14).click(new String[] { "Operate" });
				sleep(3500, 4000);
				teleport = false;
				teleportFailSafe++;
			}
			if (Interfaces.get(230, 2) != null) {
				Interfaces.get(230, 2).click(new String[] { "" });
				println("Teleporting to Barbarian Outpost.");
				teleportFailSafe = 0;
				sleep(500, 1000);
				GameTab.open(TABS.INVENTORY);
				sleep(7000, 8000);
				teleport = true;
			}
			if (teleport == true) {
				Walking.walkPath(Walking.generateStraightPath(FISH_TILE));
				sleep(50, 100);
				while (Player.isMoving()) {
					sleep(25, 50);
				}
			}
		}
	}

	private void doReset() {
		idle++;
		if (Player.getAnimation() != -1) {
			idle = 0;
		}
		if (idle >= 1500) {
			Walking.walkPath(Walking.generateStraightPath(RESET_TILE));
			idle = 0;
			sleep(50, 100);
			while (Player.isMoving()) {
				sleep(5, 10);
			}
		}
	}

	private void antiBan() {
		int random = General.random(1, 30000);
		switch (random) {
		case 1:
			Camera.setCameraRotation(General.random(1, 360));
			break;
		case 2:
			Mouse.move(General.random(1, 300), General.random(1, 300));
			break;
		case 3:
			GameTab.open(GameTab.TABS.STATS);
			sleep(200, 500);
			Mouse.move(General.random(679, 732), General.random(275, 294));
			sleep(3400, 6400);
			GameTab.open(GameTab.TABS.INVENTORY);
			sleep(200, 500);
			Mouse.move(General.random(1, 300), General.random(1, 300));
			break;
		default:
			sleep(50, 100);
		}
	}

	private void onStart() {
		Object[] options = {"Drop When Full", "F1D1", "Cut Fish"};
		String method = (String)JOptionPane.showInputDialog(null, "Choose drop method:", "Encoded Leaping Fish", JOptionPane.INFORMATION_MESSAGE, null, options, "Drop When Full"); {
			if (method == "F1D1") {
				println("Drop method: F1D1.");
				F1D1 = true;
				guiWait = false;
			}
			if (method == "Drop When Full") {
				println("Drop method: Drop When Full.");
				dropWhenFull = true;
				guiWait = false;
			}
			if (method == "Cut Fish") {
				println("Drop method: Cut Fish.");
				cutFish = true;
				guiWait = false;
			}
		} 
	}

	@Override
	public void run() {
		Walking.control_click = true;
		Mouse.setSpeed(General.random(168,  177));
		fishXp = Skills.getXP("Fishing");
		onStart();
		while (guiWait) {
			sleep(500);
		}
		while (true) {
			scriptState = getState();
			fCurrentXp = Skills.getXP("Fishing");
			sCurrentXp = Skills.getXP("Strength");
			aCurrentXp = Skills.getXP("Agility");
			cCurrentXp = Skills.getXP("Cooking");
			if (fishXp != fCurrentXp && fCurrentXp - fishXp < 480) {
				fishXp = fCurrentXp;
				fishCount++;
			}

			switch (scriptState) {
			case CLICK_FISH:
				if (F1D1) {
					doF1D1Fish();
				}
				if (dropWhenFull) {
					doDWFFish();
				}
				if (cutFish) {
					doCFFish();
				}
				break;
			case DROP_FISH:
				if (F1D1) {
					doF1D1Drop();
				}
				if (dropWhenFull) {
					doDropWhenFull();
				}
				if (cutFish) {
					doDropWhenFull();
				}
				break;
			case HOVER_FISH:
				doHover();
				break;
			case CUT_FISH:
				doCutFish();
				break;
			case FIND_FISH:
				findFish();
				break;
			case COMBAT:
				avoidCombat();
				break;
			case WHIRLPOOL:
				avoidWhirlpool();
				break;
			case LOST_ROD:
				pickUpSupplies();
				break;
			case WAIT_FOR_ROD:
				waitForRod();
				break;
			case NEW_ROD:
				getNewRod();
				break;
			case NO_BAIT:
				println("Out of bait.");
				stopScript();
				break;
			case SLEEP:
				doReset();
				sleep(1);
				break;
			case CAMERA_ADJUSTMENT:
				Camera.setCameraAngle(General.random(85, 100));
				break;
			case CLICK_CONTINUE:
				NPCChat.clickContinue(true);
				break;
			case EVIL_BOB:
				println("Evil Bob random! Stopping.");
				stopScript();
				break;
			case TELEPORT:
				gamesNecklaceTeleport();
				break;
			case FISHING:
				antiBan();
				break;
			case PICK_UP_SUPPLIES:
				pickUpSupplies();
				break;
			case NO_KNIFE:
				println("Can not find knife. Stopping.");
				stopScript();
				break;
			}
		}
	}

	@Override
	public void onPaint(Graphics g) {
		int f = Skills.getPercentToNextLevel("FISHING") * 2,
				s = Skills.getPercentToNextLevel("STRENGTH") * 2,
				a = Skills.getPercentToNextLevel("AGILITY") * 2,
				c = Skills.getPercentToNextLevel("Cooking") *2,
				fLvlGain = Skills.getCurrentLevel("Fishing") - FISHING_START_LEVEL,
				sLvlGain = Skills.getCurrentLevel("Strength") - STRENGTH_START_LEVEL,
				aLvlGain = Skills.getCurrentLevel("Agility") - AGILITY_START_LEVEL,
				cLvlGain = Skills.getCurrentLevel("Cooking") - COOKING_START_LEVEL,
				fXpGain = fCurrentXp - FISHING_START_XP,
				sXpGain = sCurrentXp - STRENGTH_START_XP,
				aXpGain = aCurrentXp - AGILITY_START_XP,
				cXpGain = cCurrentXp - COOKING_START_XP;
		if (F1D1 || dropWhenFull || cutFish) {
			g.setColor(new Color(100, 89, 74));
			if (F1D1 || dropWhenFull) {
				g.fillRect(207, 345, 290, 42);
			}
			if (cutFish) {
				g.fillRect(207, 345, 290, 56);
			}
			g.fillRect(7, 345, 200, 14);
			g.fillRect(7, 359, 200, 14);
			g.fillRect(7, 373, 200, 14);
			g.setColor(new Color(102, 153, 204));
			g.fillRect(7, 345, f, 14);
			g.setColor(new Color(14, 121, 79));
			g.fillRect(7, 359, s, 14);
			g.setColor(new Color(47, 49, 129));
			g.fillRect(7, 373, a, 14);
			g.setColor(Color.black);
			if (F1D1 || dropWhenFull) {
				g.drawRect(207, 345, 290, 42);
			}
			if (cutFish) {
				g.drawRect(207, 345, 290, 56);
			}
			g.drawRect(7, 345, 200, 14);
			g.drawRect(7, 345, f, 14);
			g.drawRect(7, 359, 200, 14);
			g.drawRect(7, 359, s, 14);
			g.drawRect(7, 373, 200, 14);
			g.drawRect(7, 373, a, 14);
			g.setColor(Color.white);
			g.setFont(new Font("", 0, 11));
			g.drawString("Run Time: " + format(System.currentTimeMillis() - START_TIME), 210, 356);
			g.drawString("Fish Caught: " + fishCount + " (" + getPerHour(fishCount) + " /h)" , 210, 370);
			g.drawString("State: " + scriptState, 210, 384);
			g.drawString("Fishing: " + Skills.getCurrentLevel("Fishing") + " (+" + fLvlGain + ") " + fXpGain + " (" + getPerHour(fXpGain) + " xp/h)", 10, 356);
			g.drawString("Strength: " + Skills.getCurrentLevel("Strength") + " (+" + sLvlGain + ") " + sXpGain + " (" + getPerHour(sXpGain) + " xp/h)", 10, 370);
			g.drawString("Agility: " + Skills.getCurrentLevel("Agility") + " (+" + aLvlGain + ") " + aXpGain + " (" + getPerHour(aXpGain) + " xp/h)", 10, 384);
			g.setFont(new Font("Palatino Linotype", 0, 11));
			g.drawString("1.07", 471, 372);
			g.setColor(Color.orange);
			g.setFont(new Font("Georgia", 1, 12));
			g.drawString("Encoded Leaping Fish", 354, 358);
			g.setColor(Color.white);
			g.setFont(new Font("", 0, 11));
			if (F1D1) {
				g.drawString("F1D1", 470, 384);
			}
			if (dropWhenFull) {
				g.drawString("Drop When Full", 421, 384);
			}
			if (cutFish) {
				g.setColor(new Color(100, 89, 74));
				g.fillRect(7, 387, 200, 14);
				g.setColor(new Color(77, 36, 90));
				g.fillRect(7, 387, c, 14);
				g.setColor(Color.black);
				g.drawRect(7, 387, 200, 14);
				g.drawRect(7, 387, c, 14);
				g.setColor(Color.white);
				g.setFont(new Font("", 0, 11));
				g.drawString("Cooking: " + Skills.getCurrentLevel("Cooking") + " (+" + cLvlGain + ") " + cXpGain + " (" + getPerHour(cXpGain) + " xp/h)", 10, 398);
				g.drawString("Cut Fish", 454, 398);
			}
		}
	}

	public String format(final long ms) {
		int seconds = (int) ms / 1000;
		int minutes = seconds / 60;
		seconds -= minutes * 60;
		int hours = minutes / 60;
		minutes -= hours * 60;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	private int getPerHour(final int value) {
		return (int) (value * 3600000D / (System.currentTimeMillis() - START_TIME));
	} 

	public static enum State {
		CLICK_FISH, DROP_FISH, FIND_FISH, HOVER_FISH, CUT_FISH, COMBAT, WHIRLPOOL, LOST_ROD, NEW_ROD, WAIT_FOR_ROD, NO_BAIT, CAMERA_ADJUSTMENT, CLICK_CONTINUE, EVIL_BOB, TELEPORT, SLEEP, FISHING, PICK_UP_SUPPLIES, NO_KNIFE;
	}
}