/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.common;

/**
 *
 * @author GoldenKevin
 */
public final class StatEffect {
	public static final byte //indicies for a stats array (bonus or req)
		STR = 0,
		DEX = 1,
		INT = 2,
		LUK = 3,
		PAD = 4,
		PDD = 5,
		MAD = 6,
		MDD = 7,
		ACC = 8,
		EVA = 9,
		MHP = 10,
		MMP = 11,
		Speed = 12,
		Jump = 13,
		Level = 14,
		MaxLevel = 15
	;

	private StatEffect() {
		//uninstantiable...
	}
}
