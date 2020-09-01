package me.scidev5.xmodtools.player;

import me.scidev5.xmodtools.Constants;
import me.scidev5.xmodtools.data.Instrument;
import me.scidev5.xmodtools.data.Sample;
import me.scidev5.xmodtools.player.automation.Envelope;

public class PlayerChannel {

	private PlayerThread player;
	
	private Sample sample;

	private Envelope volumeEnv;// TODO
	private Envelope panningEnv;// TODO

	private float sampleFraction = 0f;
	private long sampleNumber = 0l;
	
	private byte fineTune = 0;
	private int note = 0;
	private int pitchBend = 0;
	private boolean isHeld = false;
	
	private float lastVolume = 0;

	private int channelVolume = 0;
	private int fadeoutVolume = 0;
	
	private int channelPanning = 0; // TODO
	
	
	public PlayerChannel(PlayerThread player) {
		this.player = player;
	}
	
	public float getPanning() {
		return 0;
	}
	private float getVolume() {
		
		float value = ((float)channelVolume / (float)0x40) * ((float)fadeoutVolume / (float)0xffff); // TODO envelope here
		
		float deltaVolume = value-this.lastVolume;
		this.lastVolume += Math.signum(deltaVolume) * Math.min(Constants.VOLUME_SMOOTHING_FACTOR, Math.abs(deltaVolume));
		
		return this.lastVolume;
	}
	public double sample() {
		if (this.sample == null) return 0;
		float volume = this.getVolume();
		
		double sampleDelay = FrequencyTable.LINEAR.apply(this.note, this.fineTune)/this.player.getSampleRate();
		
		SampleInterpolation interpolation = SampleInterpolation.LINEAR;
		
		double data = 0;
		if (volume > 0) {
			data = this.sample.get16Bit() ? 
				interpolation.apply(
					this.sample.getSample16(this.sampleNumber), 
					this.sample.getSample16(this.sampleNumber+1), 
					this.sampleFraction) :
				interpolation.apply(
					this.sample.getSample8(this.sampleNumber),
					this.sample.getSample8(this.sampleNumber+1),
					this.sampleFraction);
		}
		
		this.sampleFraction += sampleDelay % 1;
		this.sampleNumber += (long) sampleDelay + (long) this.sampleFraction;
		this.sampleFraction %= 1;
		
		return data * volume;
	}

	public void playNote(int note, Instrument instrument) {
		this.note = note;
		this.sample = instrument.getNoteSample(note);
		this.sampleNumber = 0;
		this.sampleFraction = 0f;
		this.isHeld = true;
	}
	public void switchNote(int note) {
		this.note = note;
		this.sampleNumber = 0;
		this.sampleFraction = 0f;
	}
	public void resetNote() {
		this.channelVolume = this.sample.getVolume();
		this.channelPanning = this.sample.getPanning();
		// TODO reset envelopes
		this.isHeld = true;
	}
	public void noteOff() {
		this.isHeld = false;
	}
	public void cut() {
		this.channelVolume = 0;
	}
	public void hardCut() {
		this.sample = null;
	}
}
