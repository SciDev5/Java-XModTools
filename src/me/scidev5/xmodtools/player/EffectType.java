package me.scidev5.xmodtools.player;

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
	
	private final int id;
	private final int dataId;
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