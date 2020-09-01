package me.scidev5.xmodtools.data;

public class Sample {
	protected long sampleLength = 0;
	protected short[] sampleData16 = null;
	protected byte[] sampleData8 = null;

	protected boolean is16Bit;
	
	protected long sampleLoopStart = 0;
	protected long sampleLoopLength = 0;
	protected LoopType sampleLoop = LoopType.NONE;

	protected int volume = 0x40;
	protected int panning = 0x80;
	protected byte fineTune = 0;
	protected byte relativeNote = 0;

	protected String name = "";

	/**
	 * Create a new 8-bit sample
	 * @param data The PCM samples.
	 */
	public Sample(byte[] data) {
		assert data != null : "sample data was null";
		
		this.sampleData8 = data;
		this.is16Bit = false;
	}
	/**
	 * Create a new 16-bit sample
	 * @param data The PCM samples.
	 */
	public Sample(short[] data) {
		assert data != null : "sample data was null";
		
		this.sampleData16 = data;
		this.is16Bit = true;
	}
	
	
	// GETTERS
	
	/** Get the name of the sample. */
	public String getName()       { return this.name;         }
	/** Get the default volume of the sample. (Range: $00 - $40 (0 - 64))*/
	public int getVolume()        { return this.volume;       }
	/** Get the default panning of the sample. (0 -> left; 255 -> 127/128 right)*/
	public int getPanning()       { return this.panning;      }
	/** Get the fineTune of the sample (-128 -> 1 half-step down; 127 -> 127/128 half-steps up). */
	public byte getFineTune()     { return this.fineTune;     }
	/** Get the transposition of the sample. */
	public byte getRelativeNote() { return this.relativeNote; }
	/** Get if the sample is 16 bit. */
	public boolean get16Bit()     { return this.is16Bit;      }
	/** Get the 8-bit PCM data (null if 16-bit). */
	public byte[] getData8()      { if (!this.is16Bit) return this.sampleData8; else return null;  }
	/** Get the 16-bit PCM data (null if 8-bit). */
	public short[] getData16()    { if (this.is16Bit) return this.sampleData16; else return null; }
	
	/**
	 * Get a sample at a given index (accounting for loops)
	 * @param i The index.
	 * @return The value at the given index.
	 */
	public byte getSample8(long i) {
		if (this.is16Bit) return 0;
		
		this.forceValidRange();
		
		if (i < 0) return this.sampleData8[0];
		
		int len = this.sampleData8.length;
		int loopStart = (int) this.sampleLoopStart;
		int loopLen = (int) this.sampleLoopLength;
		
		boolean loop = this.sampleLoop != LoopType.NONE && loopLen > 0;
		
		if (loop && i > loopStart) {
			long samplesAfterLoopStart = i - loopStart;
			long loops = samplesAfterLoopStart / loopLen;
			boolean reflectDirection = this.sampleLoop == LoopType.PING_PONG ? loops % 2 == 1 : false;
			
			int trueI = (int) (loopStart + (reflectDirection ? loopLen - 1 - samplesAfterLoopStart % loopLen : samplesAfterLoopStart % loopLen));
			return this.sampleData8[trueI];
		} else if (i < len) {
			return this.sampleData8[(int) i];
		} else {
			return (byte) (Math.signum(this.sampleData8[len-1]) * Math.max(0,Math.abs(this.sampleData8[len-1])-(i-len)/10f));
		}
	}
	/**
	 * Get a sample at a given index (accounting for loops)
	 * @param i The index.
	 * @return The value at the given index.
	 */
	public short getSample16(long i) {
		if (!this.is16Bit) return 0;
		
		this.forceValidRange();
		
		if (i < 0) return this.sampleData16[0];
		
		int len = this.sampleData16.length;
		int loopStart = (int) this.sampleLoopStart;
		int loopLen = (int) this.sampleLoopLength;
		
		boolean loop = this.sampleLoop != LoopType.NONE && loopLen > 0;
		
		if (loop && i > loopStart) {
			long samplesAfterLoopStart = i - loopStart;
			long loops = samplesAfterLoopStart / loopLen;
			boolean reflectDirection = this.sampleLoop == LoopType.PING_PONG ? loops % 2 == 1 : false;
			
			int trueI = (int) (loopStart + (reflectDirection ? loopLen - 1 - samplesAfterLoopStart % loopLen : samplesAfterLoopStart % loopLen));
			return this.sampleData16[trueI];
		} else if (i < len) {
			return this.sampleData16[(int) i];
		} else {
			return (byte) (Math.signum(this.sampleData16[len-1]) * Math.max(0,Math.abs(this.sampleData16[len-1])-(i-len)/10f));
		}
	}

	// SETTERS
	
	private int clamp(int value, int lower, int upper) { assert upper >= lower; return value>upper?upper:value<lower?lower:value; }
	private long clamp(long value, long lower, long upper) { assert upper >= lower; return value>upper?upper:value<lower?lower:value; }
	
	/**
	 * Clamp all values in the builder back into their intended range.
	 * (For example if reflection is used to modify values beyond their normal range.)
	 */
	public void forceValidRange() {
		this.volume = clamp(this.volume,0,0x40);
		this.panning = clamp(this.panning,0,0xff);
		
		if (this.name == null) this.name = "";
		this.name = this.name.trim();
		if (this.name.length() > 22) this.name.substring(0, 22);

		if (this.sampleLoop == null) 
			this.sampleLoop = LoopType.NONE;
		
		long dataLen = this.is16Bit?this.sampleData16.length:this.sampleData8.length;

		this.sampleLoopStart = clamp(this.sampleLoopStart,0,dataLen-1);
		this.sampleLoopLength = clamp(this.sampleLoopLength,0,dataLen-1-this.sampleLoopStart);
	}
	
	/**
	 * Set the name of the sample.
	 * @param name The name of the sample.
	 * @return this sample.
	 */
	public Sample setName(String name) { 
		this.name = name;

		if (this.name == null) this.name = "";
		this.name = this.name.trim();
		if (this.name.length() > 22) this.name.substring(0, 22);
		return this;
	}
	/**
	 * Set the default volume of the sample.
	 * @param volume The default volume to set.
	 * @return this sample.
	 */
	public Sample setDefaultVolume(byte volume) {
		this.volume = Byte.toUnsignedInt(volume) > 0x40 ? 0x40 : volume;
		return this;
	}
	/**
	 * Set the default panning of the sample. (0 -> left; 255 -> 127/128 right)
	 * @param panning The default panning to set.
	 * @return this sample.
	 */
	public Sample setDefaultPanning(byte panning) {
		this.panning = Byte.toUnsignedInt(panning);
		return this;
	}
	/**
	 * Set the default fineTune of the sample. (-128 -> 1 half-step down; 127 -> 127/128 half-steps up)
	 * @param fineTune The default fineTune to set.
	 * @return this sample.
	 */
	public Sample setDefaultFineTune(byte fineTune) {
		this.fineTune = fineTune;
		return this;
	}
	/**
	 * Set the relative note of the sample. (signed byte how many notes to transpose)
	 * @param relativeNote The default relative note to set.
	 * @return this sample.
	 */
	public Sample setRelativeNote(byte relativeNote) {
		this.relativeNote = relativeNote;
		return this;
	}
	/**
	 * Set the PCM sample data of this sample.
	 * @param data The 8-bit sample data.
	 * @return this sample.
	 */
	public Sample setData(byte[] data) {
		assert data != null : "sample data was null";
		
		this.sampleData8 = data;
		this.is16Bit = false;
		
		return this;
	}
	/**
	 * Set the PCM sample data of this sample.
	 * @param data The 16-bit sample data.
	 * @return this sample.
	 */
	public Sample setData(short[] data) {
		assert data != null : "sample data was null";
		
		this.sampleData16 = data;
		this.is16Bit = true;
		
		return this;
	}
	/**
	 * Set loop information of the sample
	 * @param data The 16-bit sample data.
	 * @return this sample.
	 */
	public Sample setLoop(LoopType type, long loopStart, long loopLength) {
		
		if (type == null) this.sampleLoop = LoopType.NONE;
		else              this.sampleLoop = type;
		
		long dataLen = this.is16Bit?this.sampleData16.length:this.sampleData8.length;

		this.sampleLoopStart = clamp(loopStart,0,dataLen-1);
		this.sampleLoopLength = clamp(loopLength,0,dataLen-1-this.sampleLoopStart);
		
		return this;
	}
	
}
