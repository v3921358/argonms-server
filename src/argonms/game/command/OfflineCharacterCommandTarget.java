/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.game.command;

import argonms.common.GlobalConstants;
import argonms.common.character.Player;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.Pet;
import argonms.common.util.DatabaseManager;
import argonms.game.character.ExpTables;
import argonms.game.loading.map.MapDataLoader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class OfflineCharacterCommandTarget implements CommandTarget {
	private static final Logger LOG = Logger.getLogger(OfflineCharacterCommandTarget.class.getName());

	private final String target;
	private Connection con;
	private PreparedStatement ps;
	private ResultSet rs;

	public OfflineCharacterCommandTarget(String name) {
		target = name;
	}

	private void setValueInCharactersTable(String column, short value) throws SQLException {
		ps = con.prepareStatement("UPDATE `characters` SET `" + column + "` = ? WHERE `name` = ?");
		ps.setShort(1, value);
		ps.setString(2, target);
		ps.executeUpdate();
		ps.close();
	}

	private void addValueInCharactersTable(String column, short value, short max) throws SQLException {
		ps = con.prepareStatement("UPDATE `characters` SET `" + column + "` = LEAST(CAST(`" + column + "` AS UNSIGNED) + ?, ?) WHERE `name` = ?");
		ps.setShort(1, value);
		ps.setShort(2, max);
		ps.setString(3, target);
		ps.executeUpdate();
		ps.close();
	}

	private void setValueInCharactersTable(String column, int value) throws SQLException {
		ps = con.prepareStatement("UPDATE `characters` SET `" + column + "` = ? WHERE `name` = ?");
		ps.setInt(1, value);
		ps.setString(2, target);
		ps.executeUpdate();
		ps.close();
	}

	private void addValueInCharactersTable(String column, int value, int max) throws SQLException {
		ps = con.prepareStatement("UPDATE `characters` SET `" + column + "` = LEAST(CAST(`" + column + "` AS UNSIGNED) + ?, ?) WHERE `name` = ?");
		ps.setInt(1, value);
		ps.setInt(2, max);
		ps.setString(3, target);
		ps.executeUpdate();
		ps.close();
	}

	private byte getByteValueInCharactersTable(String column) throws SQLException {
		byte val;

		ps = con.prepareStatement("SELECT `" + column + "` FROM `characters` WHERE `name` = ?");
		ps.setString(1, target);
		rs = ps.executeQuery();
		rs.next(); //assert this is true
		val = rs.getByte(1);
		rs.close();
		ps.close();

		return val;
	}

	private int getIntValueInCharactersTable(String column) throws SQLException {
		int val;

		ps = con.prepareStatement("SELECT `" + column + "` FROM `characters` WHERE `name` = ?");
		ps.setString(1, target);
		rs = ps.executeQuery();
		rs.next(); //assert this is true
		val = rs.getInt(1);
		rs.close();
		ps.close();

		return val;
	}

	private short getShortValueInCharactersTable(String column) throws SQLException {
		short val;

		ps = con.prepareStatement("SELECT `" + column + "` FROM `characters` WHERE `name` = ?");
		ps.setString(1, target);
		rs = ps.executeQuery();
		rs.next(); //assert this is true
		val = rs.getShort(1);
		rs.close();
		ps.close();

		return val;
	}

	private short getTotalEquipBonus(String column) throws SQLException {
		short val;

		ps = con.prepareStatement("SELECT LEAST(SUM(`e`.`" + column + "`), " + Short.MAX_VALUE + ") FROM `inventoryequipment` `e` "
				+ "LEFT JOIN `inventoryitems` `i` ON `i`.`inventoryitemid` = `e`.`inventoryitemid` "
				+ "LEFT JOIN `characters` `c` ON `i`.`characterid` = `c`.`id` "
				+ "WHERE `c`.`name` = ? AND `i`.`inventorytype` = " + Inventory.InventoryType.EQUIPPED.byteValue());
		ps.setString(1, target);
		rs = ps.executeQuery();
		rs.next(); //assert this is true
		val = rs.getShort(1);
		rs.close();
		ps.close();

		return val;
	}

	private int getInt(CharacterManipulation update) {
		return ((Integer) update.getValue()).intValue();
	}

	private short getShort(CharacterManipulation update) {
		return ((Short) update.getValue()).shortValue();
	}

	@Override
	public void mutate(List<CharacterManipulation> updates) {
		int prevTransactionIsolation = Connection.TRANSACTION_REPEATABLE_READ;
		boolean prevAutoCommit = true;
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
			prevTransactionIsolation = con.getTransactionIsolation();
			prevAutoCommit = con.getAutoCommit();
			con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
			con.setAutoCommit(false);

			for (CharacterManipulation update : updates) {
				switch (update.getKey()) {
					case CHANGE_MAP: {
						MapValue value = (MapValue) update.getValue();
						ps = con.prepareStatement("UPDATE `characters` SET `map` = ?, `spawnpoint` = ? WHERE `name` = ?");
						ps.setInt(1, value.mapId);
						ps.setByte(2, value.spawnPoint);
						ps.setString(3, target);
						ps.executeUpdate();
						ps.close();
						break;
					}
					case CHANGE_CHANNEL:
						throw new UnsupportedOperationException("Cannot change channel of offline player");
					case ADD_LEVEL: {
						addValueInCharactersTable("level", getShort(update), GlobalConstants.MAX_LEVEL);
						short level = getShortValueInCharactersTable("level");
						//clip exp in case we subtracted levels or reached max level
						addValueInCharactersTable("exp", 0, level < GlobalConstants.MAX_LEVEL ? ExpTables.getForLevel(level) - 1 : 0);
						break;
					}
					case SET_LEVEL: {
						short level = getShort(update);
						setValueInCharactersTable("level", level);
						//clip exp in case we subtracted levels or reached max level
						addValueInCharactersTable("exp", 0, level < GlobalConstants.MAX_LEVEL ? ExpTables.getForLevel(level) - 1 : 0);
						break;
					}
					case SET_JOB:
						setValueInCharactersTable("job", getShort(update));
						break;
					case ADD_STR:
						addValueInCharactersTable("str", getShort(update), Short.MAX_VALUE);
						break;
					case SET_STR:
						setValueInCharactersTable("str", getShort(update));
						break;
					case ADD_DEX:
						addValueInCharactersTable("dex", getShort(update), Short.MAX_VALUE);
						break;
					case SET_DEX:
						setValueInCharactersTable("dex", getShort(update));
						break;
					case ADD_INT:
						addValueInCharactersTable("int", getShort(update), Short.MAX_VALUE);
						break;
					case SET_INT:
						setValueInCharactersTable("int", getShort(update));
						break;
					case ADD_LUK:
						addValueInCharactersTable("luk", getShort(update), Short.MAX_VALUE);
						break;
					case SET_LUK:
						setValueInCharactersTable("luk", getShort(update));
						break;
					case ADD_AP:
						addValueInCharactersTable("ap", getShort(update), Short.MAX_VALUE);
						break;
					case SET_AP:
						setValueInCharactersTable("ap", getShort(update));
						break;
					case ADD_SP:
						addValueInCharactersTable("sp", getShort(update), Short.MAX_VALUE);
						break;
					case SET_SP:
						setValueInCharactersTable("sp", getShort(update));
						break;
					case ADD_MAX_HP:
						addValueInCharactersTable("maxhp", getShort(update), 30000);
						break;
					case SET_MAX_HP:
						setValueInCharactersTable("maxhp", getShort(update));
						break;
					case ADD_MAX_MP:
						addValueInCharactersTable("maxmp", getShort(update), 30000);
						break;
					case SET_MAX_MP:
						setValueInCharactersTable("maxmp", getShort(update));
						break;
					case ADD_HP: {
						short maxHp = (short) Math.min(getShortValueInCharactersTable("maxhp") + getTotalEquipBonus("hp"), 30000);
						addValueInCharactersTable("hp", getShort(update), maxHp);
						break;
					}
					case SET_HP: {
						short maxHp = (short) Math.min(getShortValueInCharactersTable("maxhp") + getTotalEquipBonus("hp"), 30000);
						setValueInCharactersTable("hp", (short) Math.min(getShort(update), maxHp));
						break;
					}
					case ADD_MP: {
						short maxMp = (short) Math.min(getShortValueInCharactersTable("maxmp") + getTotalEquipBonus("mp"), 30000);
						addValueInCharactersTable("mp", getShort(update), maxMp);
						break;
					}
					case SET_MP: {
						short maxMp = (short) Math.min(getShortValueInCharactersTable("maxmp") + getTotalEquipBonus("mp"), 30000);
						setValueInCharactersTable("mp", (short) Math.min(getShort(update), maxMp));
						break;
					}
					case ADD_FAME:
						addValueInCharactersTable("fame", getShort(update), Short.MAX_VALUE);
						break;
					case SET_FAME:
						setValueInCharactersTable("fame", getShort(update));
						break;
					case ADD_EXP: {
						short level = getShortValueInCharactersTable("level");
						addValueInCharactersTable("exp", getInt(update), level < GlobalConstants.MAX_LEVEL ? ExpTables.getForLevel(level) - 1 : 0);
						break;
					}
					case SET_EXP: {
						setValueInCharactersTable("exp", getInt(update));
						short level = getShortValueInCharactersTable("level");
						addValueInCharactersTable("exp", 0, level < GlobalConstants.MAX_LEVEL ? ExpTables.getForLevel(level) - 1 : 0);
						break;
					}
					case ADD_MESO:
						addValueInCharactersTable("mesos", getInt(update), Integer.MAX_VALUE);
						break;
					case SET_MESO:
						setValueInCharactersTable("mesos", getInt(update));
						break;
					case SET_SKILL_LEVEL: {
						SkillValue value = (SkillValue) update.getValue();
						int characterId = Player.getIdFromName(target);

						ps = con.prepareStatement("DELETE FROM `skills` WHERE `characterid` = ? AND `skillid` = ?");
						ps.setInt(1, characterId);
						ps.setInt(2, value.skillId);
						ps.executeUpdate();
						ps.close();

						ps = con.prepareStatement("INSERT INTO `skills` (`characterid`,`skillid`,`level`,`mastery`) VALUES (?,?,?,?)");
						ps.setInt(1, characterId);
						ps.setInt(2, value.skillId);
						ps.setByte(3, value.skillLevel);
						ps.setByte(4, value.skillMasterLevel);
						ps.executeUpdate();
						ps.close();
						break;
					}
					case ADD_ITEM: {
						ItemValue value = (ItemValue) update.getValue();
						Inventory.InventoryType type = InventoryTools.getCategory(value.itemId);
						Pet[] pets = new Pet[3];

						ps = con.prepareStatement("SELECT `accountid`,`id`,`" + type.toString().toLowerCase() + "slots` FROM `characters` WHERE `name` = ?");
						ps.setString(1, target);
						rs = ps.executeQuery();
						rs.next(); //assert this is true
						int accountId = rs.getInt(1);
						int characterId = rs.getInt(2);
						Map<Inventory.InventoryType, Inventory> inventories = Collections.singletonMap(type, new Inventory(rs.getShort(2)));
						rs.close();
						ps.close();

						ps = con.prepareStatement("SELECT * FROM `inventoryitems` WHERE "
								+ "`characterid` = ? AND `inventorytype` = ?");
						ps.setInt(1, characterId);
						ps.setByte(2, type.byteValue());
						rs = ps.executeQuery();
						Player.loadInventory(pets, con, rs, inventories);
						rs.close();
						ps.close();

						InventoryTools.addToInventory(inventories.get(type), value.itemId, value.quantity);

						Player.commitInventory(characterId, accountId, pets, con, inventories);
						break;
					}
					case CANCEL_DEBUFFS:
						//offline characters don't have any active status effects
						break;
					case MAX_ALL_EQUIP_STATS:
						ps = con.prepareStatement("UPDATE `inventoryequipment` `e` "
								+ "LEFT JOIN `inventoryitems` `i` ON `i`.`inventoryitemid` = `e`.`inventoryitemid` "
								+ "LEFT JOIN `characters` `c` ON `i`.`characterid` = `c`.`id` "
								+ "SET "
								+ "`e`.`str` = " + Short.MAX_VALUE + ", "
								+ "`e`.`dex` = " + Short.MAX_VALUE + ", "
								+ "`e`.`int` = " + Short.MAX_VALUE + ", "
								+ "`e`.`luk` = " + Short.MAX_VALUE + ", "
								+ "`e`.`hp` = 30000, "
								+ "`e`.`mp` = 30000, "
								+ "`e`.`watk` = " + Short.MAX_VALUE + ", "
								+ "`e`.`matk` = " + Short.MAX_VALUE + ", "
								+ "`e`.`wdef` = " + Short.MAX_VALUE + ", "
								+ "`e`.`mdef` = " + Short.MAX_VALUE + ", "
								+ "`e`.`acc` = " + Short.MAX_VALUE + ", "
								+ "`e`.`avoid` = " + Short.MAX_VALUE + ", "
								+ "`e`.`hands` = " + Short.MAX_VALUE + ", "
								+ "`e`.`speed` = 40, "
								+ "`e`.`jump` = 23 "
								+ "WHERE `c`.`name` = ? AND `i`.`inventorytype` = " + Inventory.InventoryType.EQUIPPED.byteValue());
						ps.setString(1, target);
						ps.executeUpdate();
						ps.close();
						break;
					case MAX_INVENTORY_SLOTS:
						ps = con.prepareStatement("UPDATE `characters` SET "
								+ "`equipslots` = 255, `useslots` = 255, `setupslots` = 255, `etcslots` = 255, `cashslots` = 255 "
								+ "WHERE `name` = ?");
						ps.setString(1, target);
						ps.executeUpdate();
						ps.close();

						ps = con.prepareStatement("UPDATE `accounts` SET "
								+ "`storageslots` = 255 "
								+ "WHERE `id` = (SELECT `accountid` FROM `characters` WHERE `name` = ?)");
						ps.setString(1, target);
						ps.executeUpdate();
						ps.close();
						break;
					case MAX_BUDDY_LIST_SLOTS:
						ps = con.prepareStatement("UPDATE `characters` SET "
								+ "`buddyslots` = 255 WHERE `name` = ?");
						ps.setString(1, target);
						ps.executeUpdate();
						ps.close();
						break;
				}
			}

			con.commit();
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not manipulate stat of offline character. Rolling back all changes...", e);
			try {
				con.rollback();
			} catch (SQLException ex2) {
				LOG.log(Level.WARNING, "Error rolling back stat manipulations of offline character.", ex2);
			}
		} finally {
			try {
				con.setAutoCommit(prevAutoCommit);
				con.setTransactionIsolation(prevTransactionIsolation);
			} catch (SQLException ex) {
				LOG.log(Level.WARNING, "Could not reset Connection config after manipulating offline character " + target, ex);
			}
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
		}
	}

	@Override
	public Object access(CharacterProperty key) {
		try {
			con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
			switch (key) {
				case MAP:
					return new MapValue(getIntValueInCharactersTable("map"), getByteValueInCharactersTable("spawnpoint"));
				case CHANNEL:
					return Byte.valueOf((byte) 0);
				case POSITION:
					return MapDataLoader.getInstance().getMapStats(getIntValueInCharactersTable("map")).getPortals().get(Byte.valueOf(getByteValueInCharactersTable("spawnpoint"))).getPosition();
				case PLAYER_ID:
					return Integer.valueOf(Player.getIdFromName(target));
				default:
					return null;
			}
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Could not retrieve stat of offline character", e);
			return null;
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
		}
	}
}
