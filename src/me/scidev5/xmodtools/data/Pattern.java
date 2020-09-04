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
	/**
	 * Set the pattern data at a given row and channel.
	 * @param data The PatternData to set.
	 * @param row The row to set at.
	 * @param channel The channel to set at.
	 * @throws IndexOutOfBoundsException If the row or channel is out of bounds.
	 */
	public void setData(PatternData data, int row, int channel) throws IndexOutOfBoundsException {
		if (row >= this.length) throw new IndexOutOfBoundsException("Row number out of bounds.");
		if (channel >= this.channels) throw new IndexOutOfBoundsException("Channel number out of bounds.");
		
		this.data[row][channel] = data;
	}
	/**
	 * Get the pattern data at a given row and channel.
	 * @param row The row to look at.
	 * @param channel The channel to look at.
	 * @return The pattern data at the row and channel.
	 * @throws IndexOutOfBoundsException If the row or channel is out of bounds.
	 */
	public PatternData getData(int row, int channel) throws IndexOutOfBoundsException {
		if (row >= this.length) throw new IndexOutOfBoundsException("Row number out of bounds.");
		if (channel >= this.channels) throw new IndexOutOfBoundsException("Channel number out of bounds.");
		
		return this.data[row][channel];
	}

	/**
	 * Get the number of rows in this pattern.
	 * @return The number of rows in the pattern.
	 */
	public int getNRows() {
		return this.length;
	}
	/**
	 * Get the number of rows in this pattern.
	 * @param len The new number of rows in the pattern.
	 * @throws IllegalArgumentException If the new length was less than or equal to 0.
	 */
	public void setNRows(short len) throws IllegalArgumentException {
		if (len <= 0) throw new IllegalArgumentException("New pattern length was less than or equal to 0");
		this.length = len;
		this.resize();
	}
	
	/**
	 * Update the number of channels in the pattern based on the number of channels in the song.
	 */
	public void updateChannels() {
		this.channels = song.getNumChannels();
		this.resize();
	}
	/**
	 * Make sure the size of the data array matches the pattern dimensions (rows * channels)
	 */
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
