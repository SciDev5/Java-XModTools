package me.scidev5.xmodtools.data;

import java.util.Arrays;

import me.scidev5.xmodtools.Constants;
import me.scidev5.xmodtools.player.automation.Envelope;
import me.scidev5.xmodtools.player.automation.LFO;

public class Instrument {
	protected String name = "";
	protected byte[] sampleNoteMap = new byte[Constants.NOTE_COUNT];
	protected Envelope volumeEnv = null;
	protected Envelope panningEnv = null;
	protected LFO autoVibrato = null;
	protected int volumeFadeout = 0xffff;
	protected final Sample[] samples;
	
	protected int numSamples = 0;
	
	public Instrument() {
		this.sampleNoteMap = new byte[Constants.NOTE_COUNT];
		this.samples = new Sample[256];
		
		Arrays.fill(this.samples, null);
		Arrays.fill(this.sampleNoteMap,(byte) 0);
	}
	
	/**
	 * Get the sample corresponding to a given note.
	 * @param note The note to look for a sample by.
	 * @return The sample at the given note.
	 */
	public Sample getNoteSample(int note) {
		// TODO error check
		return this.samples[this.sampleNoteMap[note]];
	}
	
	// SAMPLES

	/**
	 * Get a sample at a given index. If the index is out of range, the sample will be null.
	 * @param index The index to look at.
	 * @return The sample corresponding to the index provided.
	 */
	public Sample getSample(int index) {
		if (index < 0) return null;
		return index<this.numSamples?this.samples[index]:null;
	}
	/**
	 * Add a new sample.
	 * @param sample The sample to add.
	 * @throws IllegalStateException If the sample array is already full.
	 */
	public void addSample(Sample sample) throws IllegalStateException {
		if (this.numSamples >= 256) throw new IllegalStateException("Can only have at most 256 samples.");
		
		this.samples[this.numSamples] = sample;
		this.numSamples ++;
	}
	/**
	 * Set a sample at an index.
	 * @param index The index to set at.
	 * @param sample The sample to set.
	 * @throws IndexOutOfBoundsException If the index was less than 0 or greater than or equal to the number of samples.
	 */
	public void setSample(int index, Sample sample) throws IndexOutOfBoundsException {
		if (index < 0) throw new IndexOutOfBoundsException("Index was less than 0.");
		if (index >= this.numSamples) throw new IndexOutOfBoundsException("Index was greater or equal to numSamples.");

		this.samples[index] = sample;
	}
	/**
	 * Remove a sample at an index.
	 * @param index The index.
	 * @throws IndexOutOfBoundsException If the index was less than 0 or greater than or equal to the number of samples.
	 * @throws IllegalStateException If there are no samples to remove.
	 */
	public void removeSample(int index) throws IndexOutOfBoundsException, IllegalStateException {
		if (this.numSamples <= 0) throw new IllegalStateException("Cannot remove samples because there are none.");
		if (index < 0) throw new IndexOutOfBoundsException("Index was less than 0.");
		if (index >= this.numSamples) throw new IndexOutOfBoundsException("Index was greater or equal to numSamples.");
		
		this.numSamples--;
		for (int i = index; i < this.numSamples; i++)
			this.samples[i] = this.samples[i+1];
	}

	// GETTERS
	
	/**
	 * Get the number of samples added to the instrument.
	 * @return The number of samples.
	 */
	public int getNumSamples()    { return this.numSamples;  }
	/**
	 * Get the integer value for the amount of fadeout to apply to a released note.
	 * @return The instrument's fadeout. ($0 -> none; $ffff -> instant cut).
	 */
	public int getFadeout()    { return this.volumeFadeout;  }
	/**
	 * Get a copy of the volume envelope.
	 * @return A copy of the instrument's volume envelope.
	 */
	public Envelope getVolumeEnv()    { if (this.volumeEnv != null) return this.volumeEnv.copy(); else return null; }
	/**
	 * Get a copy of the panning envelope.
	 * @return A copy of the instrument's panning envelope.
	 */
	public Envelope getPanningEnv()    { if (this.panningEnv != null) return this.panningEnv.copy(); else return null; }
	
	// SETTERS
	
	private int clamp(int value, int lower, int upper) { return value>upper?upper:value<lower?lower:value; }
	
	/**
	 * Clamp all values in the builder back into their intended range.
	 * (For example if reflection is used to modify values beyond their normal range.)
	 */
	public void forceValidRange() {
		if (this.name == null) this.name = "";
		this.name = this.name.trim();
		if (this.name.length() > 22) this.name.substring(0, 22);
		
		if (this.sampleNoteMap.length != Constants.NOTE_COUNT) {
			byte[] data = this.sampleNoteMap;
			this.sampleNoteMap = new byte[Constants.NOTE_COUNT];
			for (int i = 0; i < Constants.NOTE_COUNT; i++) {
				if (i >= data.length)
					this.sampleNoteMap[i] = 0;
				else
					this.sampleNoteMap[i] = data[i];
			}
		}
		
		this.volumeFadeout = clamp(this.volumeFadeout,0,0xffff);
	}
	

	/**
	 * Set the name of the instrument.
	 * @param name The name of the instrument.
	 * @return this instrument.
	 */
	public Instrument setName(String name) { 
		this.name = name;

		if (this.name == null) this.name = "";
		this.name = this.name.trim();
		if (this.name.length() > 22) this.name.substring(0, 22);
		return this;
	}

	/**
	 * Set the map from notes to samples
	 * @param data The data for the map.
	 * @return this instrument.
	 */
	public Instrument setSampleNoteMap(byte[] data) {
		this.sampleNoteMap = new byte[Constants.NOTE_COUNT];
		for (int i = 0; i < Constants.NOTE_COUNT; i++) {
			if (i >= data.length)
				this.sampleNoteMap[i] = 0;
			else
				this.sampleNoteMap[i] = data[i];
		}
		return this;
	}
	/**
	 * Set an element map from notes to samples
	 * @param i The index to set at. (note)
	 * @param data The data for the map at index i. (sample index)
	 * @return this instrument.
	 */
	public Instrument setSampleNoteMapValue(int i, byte data) {
		if (this.sampleNoteMap.length != Constants.NOTE_COUNT) {
			byte[] olddata = this.sampleNoteMap;
			this.sampleNoteMap = new byte[Constants.NOTE_COUNT];
			for (int j = 0; j < Constants.NOTE_COUNT; j++) {
				if (i >= olddata.length)
					this.sampleNoteMap[j] = 0;
				else
					this.sampleNoteMap[j] = olddata[j];
			}
		}
		if (i >= 0 && i < Constants.NOTE_COUNT)
			this.sampleNoteMap[i] = data;
		return this;
	}

	/**
	 * Set the envelope responsible for volume.
	 * @param volumeEnvelope The envelope to set.
	 * @return this instrument.
	 */
	public Instrument setVolumeEnv(Envelope volumeEnvelope) {
		this.volumeEnv = volumeEnvelope;
		return this;
	}
	/**
	 * Set the envelope responsible for panning.
	 * @param panningEnvelope The envelope to set.
	 * @return this instrument.
	 */
	public Instrument setPanningEnv(Envelope panningEnvelope) {
		this.panningEnv = panningEnvelope;
		return this;
	}
	/**
	 * Set the LFO (low frequency oscillator) responsible for auto-vibrato.
	 * @param autoVibratoLFO The lfo to use for auto-vibrato.
	 * @return this instrument.
	 */
	public Instrument setAutoVibratoLFO(LFO autoVibratoLFO) {
		this.autoVibrato = autoVibratoLFO;
		return this;
	}

	/**
	 * Set the fadeout amount of the instrument. 
	 * @param fadeout The fadeout amount. ($ffff -> instant cut; $0000 -> no fadeout)
	 * @return this instrument.
	 */
	public Instrument setFadeout(int fadeout) {
		this.volumeFadeout = clamp(fadeout,0,0xffff);
		return this;
	}
	/**
	 * Set the fadeout amount of the instrument. (parameter will be made unsigned)
	 * @param fadeout The fadeout amount. ($ffff -> instant cut; $0000 -> no fadeout)
	 * @return this instrument.
	 */
	public Instrument setFadeout(short fadeout) {
		this.volumeFadeout = fadeout & 0xffff;
		return this;
	}

}
