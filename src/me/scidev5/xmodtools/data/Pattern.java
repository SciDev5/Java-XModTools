package me.scidev5.xmodtools.data;

public class Pattern {
	
	private PatternData[][] data;
	private final Song song;
	private int length;
	private int channels;
	
	public Pattern(Song song, int length) throws IllegalArgumentException {
		if (song == null) throw new IllegalArgumentException("Song was null.");
		if (length < 0 || length > 256) throw new IllegalArgumentException("Length not in range (1-256)");
		this.song = song;
		this.length = length;
		this.channels = song.getNumChannels();
		this.data = new PatternData[this.length][this.channels];
		for (int i = 0; i < this.length; i++) {
			for (int j = 0; j < this.channels; j++) {
				this.data[i][j] = new PatternData(0, 0, 0, 0, 0);
			}
		}
	}
	public void setData(PatternData data, int row, int channel) throws IndexOutOfBoundsException {
		if (row >= this.length) throw new IndexOutOfBoundsException("Row number out of bounds.");
		if (channel >= this.channels) throw new IndexOutOfBoundsException("Channel number out of bounds.");
		
		this.data[row][channel] = data;
	}
	public PatternData getData(int row, int channel) throws IndexOutOfBoundsException {
		if (row >= this.length) throw new IndexOutOfBoundsException("Row number out of bounds.");
		if (channel >= this.channels) throw new IndexOutOfBoundsException("Channel number out of bounds.");
		
		return this.data[row][channel];
	}

	public int getNRows() {
		return this.length;
	}
	public void setNRows(short len) throws IllegalArgumentException {
		if (len < 0)
		this.length = len;
	}
	
	public void updateChannels() {
		this.channels = song.getNumChannels();
		this.resize();
	}
	public void resize() {
		if (this.length == this.data.length)
			if (this.channels == this.data[0].length) 
				return; // Skip if no change has occurred.
		
		PatternData[][] oldData = this.data;
		this.data = new PatternData[this.length][this.channels];
		for (int i = 0; i < this.data.length; i++) {
			for (int j = 0; j < this.data[i].length; j++) {
				if (i >= oldData.length) {
					this.data[i][j] = new PatternData(0,0,0,0,0);
				} else if (j >= oldData[i].length) {
					this.data[i][j] = new PatternData(0,0,0,0,0);
				} else {
					this.data[i][j] = oldData[i][j];
				}
			}
		}
	}
	
	public static class PatternData {
		public byte note;
		public byte instrument;
		public byte volume;
		public byte effectType;
		public byte effectData;
		public PatternData(byte note, byte instrument, byte volume, byte effectType, byte effectData) {
			this.note = note;
			this.instrument = instrument;
			this.volume = volume;
			this.effectType = effectType;
			this.effectData = effectData;
		}
		public PatternData(int note, int instrument, int volume, int effectType, int effectData) {
			this((byte) note, (byte) instrument, (byte) volume, (byte) effectType, (byte) effectData);
		}
	}
}
