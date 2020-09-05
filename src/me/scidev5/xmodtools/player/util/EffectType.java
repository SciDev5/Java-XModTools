/*
 * XModTools  (A set of Java tools for using extended module song files.)
 * Copyright (C) 2020  SciDev5 (https://github.com/SciDev5)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * License file should be called "COPYING" which is in the root of the
 * master branch of the GitHub repositiory (https://github.com/SciDev5/Java-XModTools).
 */

package me.scidev5.xmodtools.player.util;

public enum EffectType {
	// Arpeggio
	ARPEGGIO(0),
	
	// Portamento
	PORTA_UP(1),
	PORTA_DOWN(2),
	PORTA_UP_FINE(14,1),
	PORTA_DOWN_FINE(14,2),
	PORTA_EXTRA_FINE_UP(33,1),
	PORTA_EXTRA_FINE_DOWN(33,1),
	PORTA_NOTE(3),
	
	// LFO
	VIBRATO(4),
	TREMOLO(7),
	
	// Panning
	SET_PANNING(8),
	PANNING_SLIDE(25),
	SET_PANNING_EXTCMD(14,8),
	
	// Volume
	SET_VOLUME(12),
	VOLUME_SLIDE(10),
	VOLUME_SLIDE_FINE_UP(14,10),
	VOLUME_SLIDE_FINE_DOWN(14,11),
	
	// Volume slide combos
	VOLUME_SLIDE_CONT_PORTA(5),
	VOLUME_SLIDE_CONT_VIBRATO(6),
	VOLUME_SLIDE_AND_RETRIGGER_NOTE(27),
	
	// Note Things
	CUT(14,12),
	DELAY_NOTE(14,13),
	KEY_OFF(20),
	
	RETRIGGER_NOTE(14,9),
	SAMPLE_OFFSET(9),
	SET_ENVELOPE_FRAME(21),
	SET_FINETUNE(14,5),
	
	// Other
	GLISSANDO_CONTROL(14,3),
	VIBRATO_CONTROL(14,4),
	TREMOLO_CONTROL(14,7),
	TREMOR(29),
	
	// Global
	SET_TEMPO(15),
	JUMP_TO_ORDER(11),
	DELAY_ROW(14,14),
	PATTERN_LOOP(14,6),
	PATTERN_BREAK(13),
	SET_GLOBAL_VOLUME(16),
	GLOBAL_VOLUME_SLIDE(17);
	
	public final int id;
	public final int dataId;
	private EffectType(int id) {
		this.id = id;
		this.dataId = -1;
	}
	private EffectType(int id, int dataId) {
		this.id = id;
		this.dataId = dataId;
	}
	
	public static EffectType get(byte type, byte data) {
		EffectType[] all = values();
		for (EffectType effectType : all)
			if (effectType.id == (type & 0xff) && (effectType.dataId == -1 || effectType.dataId == ((data >> 4) & 0xf)))
				return effectType;
		return null;
	}
}
