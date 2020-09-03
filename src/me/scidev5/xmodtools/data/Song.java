package me.scidev5.xmodtools.data;

import java.util.Arrays;

public class Song {
	protected final Instrument[] instruments;
	protected final Pattern[] patterns;
	protected final int[] patternOrderTable;

	protected int songLength = 1;
	protected int songLoopPos = 0;
	protected int numChannels = 6;
	protected int defaultSpeed = 6;
	protected int defaultBPM = 125;
	protected String moduleName = "";
	protected String trackerName = "XModTools";
	
	protected int numInstruments = 0;
	protected int numPatterns = 0;
	
	public Song() {
		this.instruments = new Instrument[128];
		this.patterns = new Pattern[256];
		this.patternOrderTable = new int[256];

		Arrays.fill(this.instruments, null);
		Arrays.fill(this.patterns, null);
		Arrays.fill(this.patternOrderTable, 0);
	}

	// Instruments
	
	/**
	 * Get an instrument at a given index.
	 * @param index The index to look at.
	 * @return The instrument. (or null if the index was invalid).
	 */
	public Instrument getInstrument(int index) {
		if (index < 0) return null;
		return index<this.instruments.length?this.instruments[index]:null;
	}
	/**
	 * Add an instrument to the song.
	 * @param instrument The instrument to add.
	 * @throws IndexOutOfBoundsException If too many instruments were added.
	 */
	public void addInstrument(Instrument instrument) throws IndexOutOfBoundsException {
		if (this.numInstruments >= 128) throw new IndexOutOfBoundsException("Can only have at most 128 instruments.");
		
		this.instruments[this.numInstruments] = instrument;
		this.numInstruments ++;
	}
	/**
	 * Set an instrument at a given index.
	 * @param index The index to set at.
	 * @param instrument The instrument to set.
	 * @throws IndexOutOfBoundsException
	 */
	public void setInstrument(int index, Instrument instrument) throws IndexOutOfBoundsException {
		if (index < 0) throw new IndexOutOfBoundsException("Index was less than 0.");
		if (index >= this.numInstruments) throw new IndexOutOfBoundsException("Index was greater or equal to numInstruments.");

		this.instruments[index] = instrument;
	}

	
	// Patterns
	
	/**
	 * Get a pattern in the song by its index.
	 * @param index The index to get from;
	 * @return The pattern.
	 */
	public Pattern getPattern(int index) {
		if (index < 0) return null;
		return index<patterns.length?patterns[index]:null;
	}
	/**
	 * Add a new pattern to the song.
	 * @param pattern The pattern to add.
	 * @throws IndexOutOfBoundsException If too many patterns were added.
	 */
	public void addPattern(Pattern pattern) throws IndexOutOfBoundsException {
		if (this.numPatterns >= 255) throw new IndexOutOfBoundsException("Can only have at most 255 patterns.");
		
		this.patterns[this.numPatterns] = pattern;
		this.numPatterns ++;
	}
	/**
	 * Set a pattern at a given index.
	 * @param index The index to set at.
	 * @param pattern The pattern to set.
	 * @throws IndexOutOfBoundsException If the index provided was out of bounds.
	 */
	public void setPattern(int index, Pattern pattern) throws IndexOutOfBoundsException {
		if (index < 0) throw new IndexOutOfBoundsException("Index was less than 0.");
		if (index >= this.numPatterns) throw new IndexOutOfBoundsException("Index was greater or equal to numPatterns.");

		this.patterns[index] = pattern;
	}
	/**
	 * Set an element of the pattern order table.
	 * @param index The index. (in order of song play)
	 * @param patternIndex The index of the pattern to set. (the pattern index to set)
	 * @throws IndexOutOfBoundsException If the index provided was out of bounds.
	 */
	public void setPatternOrder(int index, int patternIndex) throws IndexOutOfBoundsException {
		if (index >= this.patternOrderTable.length || index < 0) 
			throw new IndexOutOfBoundsException("Index in patternOrderTable was out of bounds.");
		this.patternOrderTable[index] = patternIndex;
	}
	/**
	 * Set an element of the pattern order table.
	 * @param patternOrderTable The value to set in the patternOrderTable.
	 */
	public void setPatternOrder(byte[] patternOrderTable) {
		for (int i = 0; i < this.patternOrderTable.length && i < patternOrderTable.length; i++)
			this.patternOrderTable[i] = patternOrderTable[i] & 0xff;
	}
	/**
	 * Get an element of the pattern order table.
	 * @param index The index. (in order of song play)
	 * @return The index of the pattern.
	 * @throws IndexOutOfBoundsException
	 */
	public int getPatternIndexByOrder(int index) throws IndexOutOfBoundsException {
		if (index >= this.patternOrderTable.length || index < 0) 
			throw new IndexOutOfBoundsException("Index in patternOrderTable was out of bounds.");
		return this.patternOrderTable[index];
	}
	
	// GETTERS

	/** Get the number of channels in the song. */
	public int getNumChannels()    { return this.numChannels;  }
	/** Get the length of the song in patterns. */
	public int getSongLength()     { return this.songLength;   }
	/** Get the patternOrderTable index that is set when the song loops. */
	public int getSongLoopPos()    { return this.songLoopPos;  }
	/** Get the default number of ticks per row. */
	public int getDefaultSpeed()   { return this.defaultSpeed; }
	/** Get the default tempo of the song. (tick duration (ms) = 2500 * bpm) */
	public int getDefaultBPM()     { return this.defaultBPM;   }
	/** Get the name of the song. */
	public String getModuleName()  { return this.moduleName;   }
	/** Get the name of the tracker which created / edited the song. */
	public String getTrackerName() { return this.trackerName;  }
	
	// SETTERS
	
	private int clamp(int value, int lower, int upper) { return value>upper?upper:value<lower?lower:value; }
	
	/**
	 * Clamp all values in the builder back into their intended range.
	 * (For example if reflection is used to modify values beyond their normal range.)
	 */
	public void forceValidRange() {
		this.songLength = clamp(this.songLength & 0xffff, 1, 256);
		this.songLoopPos = clamp(this.songLoopPos & 0xffff,0,this.songLength-1);
		this.numChannels = this.numChannels < 0 ? 32 : clamp(this.numChannels/2,1,0x20)*2;
		this.defaultSpeed = this.defaultSpeed < 0 ? 0x1f : clamp(this.defaultSpeed,1,0x1f);
		this.defaultBPM = this.defaultBPM < 0 ? 0xff : clamp(this.defaultBPM,0x20,0xff);
		
		if (this.trackerName == null) this.trackerName = "";
		this.trackerName = this.trackerName.trim();
		if (this.trackerName.length() > 20) this.trackerName.substring(0, 20);
		
		if (this.moduleName == null) this.moduleName = "";
		this.moduleName = this.moduleName.trim();
		if (this.moduleName.length() > 20) this.moduleName.substring(0, 20);
	}

	/**
	 * Set the length of the song to build (in pattern table)
	 * @param length The length of the song. Range (1-256)
	 * @return this builder.
	 */
	public Song setSongLength(short length) {
		this.songLength = clamp(length & 0xffff, 1, 256);
		return this;
	}
	/**
	 * Set the songs restart position when it loops.
	 * @param loopPos The restart position to set
	 * @return this builder.
	 */
	public Song setSongLoopPos(short loopPos) {
		this.songLoopPos = clamp(loopPos & 0xffff,0,this.songLength-1);
		return this;
	}
	/**
	 * Set the number of channels in the song.
	 * @param numChannels The number of channels for the song.
	 * @return this builder.
	 */
	public Song setNumChannels(int numChannels) {
		this.numChannels = numChannels < 0 ? 32 : clamp(numChannels/2,1,0x20)*2;
		return this;
	}
	/**
	 * Set the default tempo (speed and bpm) of the song.
	 * @param speed The default speed (number of ticks per row).
	 * @param bpm The default bpm (tick duration = 2500ms / bpm).
	 * @return this builder.
	 */
	public Song setDefaultTempo(int speed, int bpm) {
		this.defaultSpeed = speed < 0 ? 0x1f : clamp(speed,1,0x1f);
		this.defaultBPM = bpm < 0 ? 0xff : clamp(bpm,0x20,0xff);
		return this;
	}
	/**
	 * Set the name of the tracker used to create the song.
	 * @param name The name of the tracker.
	 * @return this builder.
	 */
	public Song setTrackerName(String name) { 
		this.trackerName = name;

		if (this.trackerName == null) this.trackerName = "";
		this.trackerName = this.trackerName.trim();
		if (this.trackerName.length() > 20) this.trackerName.substring(0, 20);
		return this;
	}
	/**
	 * Set the name of the module (eg. the name of the song).
	 * @param name The name of the module (song).
	 * @return this builder.
	 */
	public Song setModuleName(String name) { 
		this.moduleName = name;

		if (this.moduleName == null) this.moduleName = "";
		this.moduleName = this.moduleName.trim();
		if (this.moduleName.length() > 20) this.moduleName.substring(0, 20);
		return this;
	}
	
	
}
