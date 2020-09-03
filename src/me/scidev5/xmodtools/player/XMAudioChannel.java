package me.scidev5.xmodtools.player;

import me.scidev5.xmodtools.Constants;
import me.scidev5.xmodtools.data.Instrument;
import me.scidev5.xmodtools.data.Pattern.PatternData;
import me.scidev5.xmodtools.data.Sample;
import me.scidev5.xmodtools.player.automation.Envelope;

public class XMAudioChannel {

	private XMAudioController controller;

	private Sample sample;
	private Instrument instrument;

	private Envelope volumeEnv;
	private Envelope panningEnv;

	private float sampleFraction = 0f;
	private long sampleNumber = 0l;

	private byte fineTune = 0;
	private int note = 0;
	private int pitchBend = 0;
	private boolean isHeld = false;

	private float lastVolume = 0;
	private float lastPanning = 0;

	private int channelVolume = 0;
	private int fadeoutVolume = 0;
	
	private int channelPanning = 0;
	
	private double currentSampleValue = 0;
	private double lastSampleValue = 0;
	private boolean justCutIn = false;
	
	public XMAudioChannel(XMAudioController controller) {
		this.controller = controller;
	}
	
	public float getPanning() {
		float value = this.channelPanning;
		
		if (this.panningEnv != null)
			Math.min(1, Math.max(-1, this.channelPanning + (1 - Math.abs(this.channelPanning)) * this.panningEnv.get()));
		
		float deltaPanning = value-this.lastPanning;
		this.lastPanning += Math.signum(deltaPanning) * (this.justCutIn?Math.abs(deltaPanning):Math.min(2*Constants.VOLUME_SMOOTHING_FACTOR, Math.abs(deltaPanning)));
		return this.lastPanning;
	}
	private float getVolume() {
		
		float value = ((float)channelVolume / (float)0x40) * ((float)fadeoutVolume / (float)0xffff);
		
		if (this.volumeEnv != null)
			value *= this.volumeEnv.get() / (float)0x40;
		
		float deltaVolume = value-this.lastVolume;
		this.lastVolume += Math.signum(deltaVolume) * Math.min(this.justCutIn?Constants.VOLUME_CUT_FACTOR:Constants.VOLUME_SMOOTHING_FACTOR, Math.abs(deltaVolume));
		
		return this.lastVolume;
	}
	
	public double sample() {
		if (this.sample == null) {
			if (this.lastSampleValue != 0)
				this.lastSampleValue -= Math.signum(this.lastSampleValue) * Math.min(Constants.VOLUME_SMOOTHING_FACTOR, Math.abs(this.lastSampleValue));
			return this.lastSampleValue;
		}
		float volume = this.getVolume();
		
		double sampleDelay = FrequencyTable.LINEAR.apply(this.note, this.fineTune)/this.controller.format.getSampleRate();
		
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
		
		if (this.lastSampleValue != 0)
			this.lastSampleValue -= Math.signum(this.lastSampleValue) * Math.min(Constants.VOLUME_SMOOTHING_FACTOR, Math.abs(this.lastSampleValue));

		this.currentSampleValue = data * volume + this.lastSampleValue;
		return this.currentSampleValue;
	}
	
	public void preTick(int n) {
		this.justCutIn = false;
	}
	
	public void tick(int tick) {
		this.tickVolumeColumn(tick);
		this.tickEffects(tick);
		
		this.tickEnvelopes();
		this.tickFadeout();
	}
	public void lazyTick(int tick) {
		this.tickEnvelopes();
		this.tickFadeout();
	}
	private void tickFadeout() {
		if (this.isHeld)
			this.fadeoutVolume = 0xffff;
		else if (this.volumeEnv != null)
			this.fadeoutVolume = Math.max(0, this.fadeoutVolume - 2*this.instrument.getFadeout());
	}
	private void tickEnvelopes() {
		if (this.volumeEnv != null)
			this.volumeEnv.calculate(this.isHeld);
		if (this.panningEnv != null)
			this.panningEnv.calculate(this.isHeld);
		
	}
	private void tickEffects(int tick) {
		// TODO
	}
	private void tickVolumeColumn(int tick) {
		// TODO
	}

	public void runEffect(byte effectType, byte effectData) {
		// TODO
	}
	public void runVolumeColumn(byte volumeByte) {
		if (volumeByte >= 0x10 && volumeByte < 0x20)
			this.channelVolume = volumeByte - 0x10;
		// TODO
	}

	/**
	 * Start a note playing on a certain instrument.
	 * @param note The note to play.
	 * @param instrument The instrument to play on.
	 */
	public void playNote(int note, Instrument instrument) {
		this.hardCut();
		this.sample = instrument.getNoteSample(note);
		this.instrument = instrument;
		this.volumeEnv = instrument.getVolumeEnv();
		this.panningEnv = instrument.getPanningEnv();
		this.switchNote(note);
		this.resetNote();
	}
	/**
	 * Switch the note out while playing.
	 * @param note The note to switch to.
	 */
	public void switchNote(int note) {
		if (this.sample == null) return;

		this.note = note + this.sample.getRelativeNote();
		this.fineTune = this.sample.getFineTune();
		this.sampleNumber = 0;
		this.sampleFraction = 0f;
		this.lastSampleValue = this.currentSampleValue;
		this.lastVolume = 0f;
		this.justCutIn = true;
	}
	/**
	 * Reset the envelopes and volume of a note while it is playing.
	 */
	public void resetNote() {
		if (this.sample == null) return;
		
		this.channelVolume = this.sample.getVolume();
		this.channelPanning = this.sample.getPanning();
		
		if (this.volumeEnv != null)
			this.volumeEnv.retrigger();
		if (this.panningEnv != null)
			this.panningEnv.retrigger();
		
		this.isHeld = true;
	}
	/**
	 * Release the playing key.
	 */
	public void noteOff() {
		this.isHeld = false;
		if (this.volumeEnv == null)
			this.cut();
	}
	/**
	 * Mute the channel.
	 */
	public void cut() {
		this.channelVolume = 0;
	}
	/**
	 * Mute the channel by nullifying the sample.
	 */
	public void hardCut() {
		this.lastSampleValue = this.currentSampleValue;
		this.lastVolume = 0f;
		this.sample = null;
		this.isHeld = false;
	}
	
	/**
	 * Returns true if the PatternData provided will result in a note playing on the first tick.
	 * @param data The PatternData to examine.
	 * @return If a note will play immediately given the PatternData data.
	 */
	public boolean canNotePlayImmediately(PatternData data) {
		return data.note >= Constants.NOTE_FIRST && data.note < Constants.NOTE_FIRST+Constants.NOTE_COUNT && 
			data.effectType != 3 && data.effectType != 5 && 
			!(data.effectType == 20 && data.effectData == 0) && 
			!(data.effectType == 14 && (data.effectData == 0xC0 || (data.effectData & 0xF0) == 0xD0 && (data.effectData & 0xF) > 0)) && 
			(data.volume & 0xF0) != 0xF0;
	}
	/**
	 * Returns True if the note playing will result in note to note portamento.
	 * @param data The PatternData to examine.
	 * @return If the note playing will result in note to note portamento.
	 */
	public boolean isNotePorta(PatternData data) {
		return data.note > 0 && data.note < 97 && 
	    	(data.effectType == 3 || data.effectType == 5 || (data.volume & 0xF0) == 0xF0);
	}
	
	/**
	 * Returns if the channel is currently playing a sample.
	 * @return If a sample is playing.
	 */
	public boolean hasSample() {
		return this.sample != null;
	}
}
