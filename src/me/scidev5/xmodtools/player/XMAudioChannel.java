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

package me.scidev5.xmodtools.player;

import java.nio.DoubleBuffer;
import java.util.Arrays;

import me.scidev5.xmodtools.Constants;
import me.scidev5.xmodtools.data.Instrument;
import me.scidev5.xmodtools.data.Pattern.PatternData;
import me.scidev5.xmodtools.data.Sample;
import me.scidev5.xmodtools.player.automation.Envelope;
import me.scidev5.xmodtools.player.automation.LFO;
import me.scidev5.xmodtools.player.util.EffectType;
import me.scidev5.xmodtools.player.util.SampleInterpolation;

public class XMAudioChannel {

	private XMAudioController controller;

	private Sample sample;
	private Instrument instrument;

	private Envelope volumeEnv;
	private Envelope panningEnv;

	private LFO vibratoLFO;
	private LFO tremoloLFO;
	private LFO autoVibratoLFO;

	private float sampleFraction = 0f;
	private long sampleNumber = 0l;

	private byte fineTune = 0;
	private int note = 0;
	private double pitchBend = 0;
	private boolean isHeld = false;

	private float lastVolume = 0;
	private float lastPanning = 0;

	private int channelVolume = 0;
	private int fadeoutVolume = 0;
	
	private int channelPanning = 0;
	
	private DoubleBuffer fadeoutSamples = null;
	private boolean justCut = false;
	
	// EFFECT DATA

	private int memory_portaUp = 0;
	private int memory_portaDown = 0;
	private int memory_portaUpFine = 0;
	private int memory_portaDownFine = 0;
	private int memory_portaUpExtraFine = 0;
	private int memory_portaDownExtraFine = 0;
	private int memory_tonePorta = 0;
	
	private int memory_vibratoSpeed = 0;
	private int memory_vibratoDepth = 0;
	
	private int memory_tremoloSpeed = 0;
	private int memory_tremoloDepth = 0;

	private int memory_volumeSlide = 0;
	private int memory_volumeSlideFine = 0;

	private int memory_panningSlide = 0;
	
	private int memory_sampleOffset = 0;

	private int memory_retriggerDelay = 0;
	private int memory_retriggerMode = 8;

	private int memory_tremor = 0;

	private int portaNoteAmount = 0;
	private int portaNotePower = 0;
	private int portaNoteTarget = -1;
	private int portaTickAmount = 0;
	
	private int volumeSlideAmount = 0;
	private int panningSlideAmount = 0;
	
	private int cutTick = -1;
	private int keyOffTick = -1;
	private int playTick = -1;
	private PatternData playRow = null;

	private int retriggerCounter = -1;
	private int retriggerMode = -1;
	private boolean retriggerEnabled = false;

	private int tremorOnTicks = 1;
	private int tremorOffTicks = 0;
	private boolean tremorMute = false;
	private boolean tremorMuteLast = false;
	private int tremorCounter = 0;

	private boolean vibratoEnabled = false;
	private boolean vibratoEnabledVolumeColumn = false;
	private boolean tremoloEnabled = false;
	private boolean tremoloOverwritten = true;
	
	private int arpIndex = 0;
	private final int[] arpNotes;
	
	
	public XMAudioChannel(XMAudioController controller) {
		this.controller = controller;
		
		this.arpNotes = new int[3];
		Arrays.fill(this.arpNotes, 0);
		
		this.vibratoLFO = new LFO();
		this.tremoloLFO = new LFO();
		this.autoVibratoLFO = null;
	}
	
	public float getPanning() {
		float value = this.channelPanning / 128.0f - 1f;
		
		if (this.panningEnv != null)
			Math.min(1, Math.max(-1, value + (1 - Math.abs(value)) * (this.panningEnv.get() / (float)0x20 - 1)));
		
		float deltaPanning = value-this.lastPanning;
		this.lastPanning += Math.signum(deltaPanning) * (this.justCut?Math.abs(deltaPanning):Math.min(1f/(8*Constants.FADEOUT_SAMPLES), Math.abs(deltaPanning)));
		return this.lastPanning;
	}
	private float getVolume() {
		
		float value = ((float)channelVolume / (float)0x40) * ((float)fadeoutVolume / (float)0xffff);
		
		if (this.volumeEnv != null)
			value *= this.volumeEnv.get() / (float)0x40;
		
		if (!this.tremoloOverwritten)
		value += this.tremoloLFO.get();
		
		if (this.tremorMute)
			value = 0;
		
		float deltaVolume = value-this.lastVolume;
		this.lastVolume += Math.signum(deltaVolume) * Math.min((this.justCut || this.tremorMute != this.tremorMuteLast ? 1f : 1/8f) / Constants.FADEOUT_SAMPLES, Math.abs(deltaVolume));
		
		return this.lastVolume;
	}
	
	public double sample() {
		double lastSample = 0;
		if (this.fadeoutSamples != null) {
			if (this.fadeoutSamples.hasRemaining())
				lastSample = this.fadeoutSamples.get();
			else
				this.fadeoutSamples = null;
		}
		if (this.sample == null) {
			return lastSample;
		}
		float volume = this.getVolume();
		
		int arpNote = 0;
		if (arpIndex < arpNotes.length && arpIndex > 0)
			arpNote = arpNotes[arpIndex];
		
		FrequencyTable table = this.controller.song.getFrequencyTable();
		double sampleDelay = (this.controller.getState().doQuantizePorta()?
				table.calculateSampleRateGlissando(
					this.note + arpNote, 
					this.fineTune, 
					this.pitchBend, 
					(this.vibratoEnabled ? 127.5f*this.vibratoLFO.get()-0.5f : 0) + 
					(this.autoVibratoLFO != null ? 127.5f*this.autoVibratoLFO.get()-0.5f : 0)
				):
				table.calculateSampleRate(
					this.note + arpNote, 
					this.fineTune, 
					this.pitchBend + 
						(this.vibratoEnabled ? 127.5f*this.vibratoLFO.get()-0.5f : 0) + 
						(this.autoVibratoLFO != null ? 127.5f*this.autoVibratoLFO.get()-0.5f : 0)
				)
			)/this.controller.format.getSampleRate();
		
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
		
		
		return data * volume + lastSample;
	}
	
	public void preTick() {
		this.justCut = false;
	}
	
	public void tick(int tick) {
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
		if (this.sample == null) return;
		
		FrequencyTable frequencyTable = this.controller.song.getFrequencyTable();
		
		if (tick != 0) {
			this.pitchBend -= this.portaTickAmount;
			
			double currentPeriod = this.pitchBend + frequencyTable.calculatePeriod(this.note, this.fineTune);
			double deltaPitchBend = frequencyTable.calculatePeriod(this.portaNoteTarget, this.sample.getFineTune()) - currentPeriod;
			this.pitchBend += Math.signum(deltaPitchBend) * Math.min(Math.abs(deltaPitchBend), this.portaNoteAmount * this.portaNotePower);

			this.channelVolume = Math.max(0, Math.min(0x40, this.channelVolume + this.volumeSlideAmount));
			this.channelPanning = Math.max(0, Math.min(0xff, this.channelPanning + this.panningSlideAmount));

			this.tremorMuteLast = this.tremorMute;
			if (this.tremorOffTicks == 0) {
				this.channelVolume = this.tremorMute ? 0 : this.channelVolume;
				this.tremorMute = false;
			} else {
				this.tremorMute = this.tremorCounter >= this.tremorOnTicks;
				this.tremoloOverwritten = true;
			}

			if (this.vibratoEnabled) this.vibratoLFO.calculate();
			if (this.vibratoEnabledVolumeColumn) this.vibratoLFO.calculate();
			if (this.tremoloEnabled) {
				this.tremoloLFO.calculate();
				this.tremoloOverwritten = false;
			}
			
			if (this.autoVibratoLFO != null)
				this.autoVibratoLFO.calculate();
			
			this.tremorCounter = (this.tremorCounter + 1) % (this.tremorOnTicks+this.tremorOffTicks);
		}
		
		if (tick == this.cutTick) this.cut();
		if (tick == this.keyOffTick) this.noteOff();
		if (tick == this.playTick && this.playRow != null) this.runRow(this.playRow, true);
		
		this.arpIndex = (this.arpIndex + 1) % this.arpNotes.length;
		
		if (this.retriggerEnabled) {
			this.retriggerCounter ++;
			if (this.retriggerCounter == this.memory_retriggerDelay) {
				int volume = this.channelVolume;
				int pan = this.channelPanning;
				if (this.retriggerMode == -1)
					this.resetNote();
				this.channelPanning = pan;
				this.resetSample();
				
				switch (retriggerMode) {
				case 1: volume -= 1; break;
				case 2: volume -= 2; break;
				case 3: volume -= 4; break;
				case 4: volume -= 8; break;
				case 5: volume -= 16; break;
				case 6: volume *= 0.66666666667f; break;
				case 7: volume *= 0.5f; break;
				case 8: break;
				case 9: volume += 1; break;
				case 10: volume += 2; break;
				case 11: volume += 4; break;
				case 12: volume += 8; break;
				case 13: volume += 16; break;
				case 14: volume *= 1.5; break;
				case 15: volume *= 2; break;
				}
				this.channelVolume = Math.max(0, Math.min(0x40, volume));
				this.tremoloOverwritten = true;
				
				this.retriggerCounter = 0;
			}
		} else 
			this.retriggerCounter = -1;
	}

	public void runEffect(PatternData dataRow) {
		EffectType type = EffectType.get(dataRow.effectType, dataRow.effectData);
		int data = dataRow.effectData & 0xff;
		
		if (type.dataId != -1)
			data = data & 0xf;
		
		switch (type) {
		// Arpeggio
		case ARPEGGIO: 
			int noteA = data & 0xf;
			int noteB = (data>>4) & 0xf;
			this.arpNotes[0] = 0;
			this.arpNotes[1] = noteA;
			this.arpNotes[2] = noteB;
			break;
		
		// Portamento
		case PORTA_UP: 
			if (data == 0) data = this.memory_portaUp;
			else           this.memory_portaUp = data;
			
			this.portaTickAmount = 4*data;
			break;
		case PORTA_DOWN: 
			if (data == 0) data = this.memory_portaDown;
			else           this.memory_portaDown = data;
			
			this.portaTickAmount = -4*data;
			break;
		case PORTA_UP_FINE: 
			if (data == 0) data = this.memory_portaUpFine;
			else           this.memory_portaUpFine = data;
			
			this.pitchBend -= 4*data;
			break;
		case PORTA_DOWN_FINE: 
			if (data == 0) data = this.memory_portaDownFine;
			else           this.memory_portaDownFine = data;
			
			this.pitchBend += 4*data;
			break;
		case PORTA_EXTRA_FINE_UP: 
			if (data == 0) data = this.memory_portaUpExtraFine;
			else           this.memory_portaUpExtraFine = data;
			
			this.pitchBend -= data;
			break;
		case PORTA_EXTRA_FINE_DOWN: 
			if (data == 0) data = this.memory_portaDownExtraFine;
			else           this.memory_portaDownExtraFine = data;
			
			this.pitchBend += data;
			break;
		case PORTA_NOTE: 
			if (dataRow.note >= Constants.NOTE_FIRST && dataRow.note < Constants.NOTE_FIRST + Constants.NOTE_COUNT)
				this.portaNoteTarget = this.sample.getRelativeNote() + (dataRow.note - Constants.NOTE_FIRST);

			if (data == 0) data = this.memory_tonePorta;
			else           this.memory_tonePorta = data;
			
			if (this.portaNoteTarget != -1) {
				this.portaNotePower = 4*data;
				this.portaNoteAmount++;
			}
			break;
		
		case VIBRATO: 
			int vibeDepth = data & 0xf;
			int vibeSpeed = (data >> 4) & 0xf;

			if (vibeSpeed == 0) vibeSpeed = this.memory_vibratoSpeed;
			else                this.memory_vibratoSpeed = vibeSpeed;
			
			if (vibeDepth == 0) vibeDepth = this.memory_vibratoDepth;
			else                this.memory_vibratoDepth = vibeDepth;
			
			this.vibratoEnabled = true;
			this.vibratoLFO.setAmplitudeParameter(vibeDepth);
			this.vibratoLFO.setFrequencyParameter(4*vibeSpeed);
			break;
		case TREMOLO: 
			int tremoloDepth = data & 0xf;
			int tremoloSpeed = (data >> 4) & 0xf;

			if (tremoloSpeed == 0) tremoloSpeed = this.memory_tremoloSpeed;
			else                   this.memory_tremoloSpeed = tremoloSpeed;
			
			if (tremoloDepth == 0) tremoloDepth = this.memory_tremoloDepth;
			else                   this.memory_tremoloDepth = tremoloDepth;
			
			this.tremoloEnabled = true;
			this.tremoloLFO.setAmplitudeParameter(tremoloDepth);
			this.tremoloLFO.setFrequencyParameter(4*tremoloSpeed);
			break;
		
		// Panning
		case SET_PANNING: 
			this.channelPanning = data;
			break;
		case PANNING_SLIDE: 
			if (data == 0) data = this.memory_panningSlide;
			else           this.memory_panningSlide = data;
			
			int down_PAN = data & 0xf;
			int up_PAN = (data >> 4) & 0xf;
			
			this.panningSlideAmount += up_PAN > 0 ? up_PAN : -down_PAN;
			break;
		case SET_PANNING_EXTCMD: 
			this.channelPanning = 0x11 * data;
			break;
		
		// Volume
		case SET_VOLUME: 
			this.channelVolume = Math.min(0x40, data);
			this.tremoloOverwritten = true;
			break;
		case VOLUME_SLIDE: 
			if (data == 0) data = this.memory_volumeSlide;
			else           this.memory_volumeSlide = data;

			int down_VOL = data & 0xf;
			int up_VOL = (data >> 4) & 0xf;
			
			this.volumeSlideAmount += up_VOL > 0 ? up_VOL : -down_VOL;
			break;
		case VOLUME_SLIDE_FINE_UP: 
			if (data == 0) data = this.memory_volumeSlideFine;
			else           this.memory_volumeSlideFine = data;
			
			this.channelVolume = Math.max(0, Math.min(0x40, this.channelVolume + data));
			this.tremoloOverwritten = true;
			break;
		case VOLUME_SLIDE_FINE_DOWN: 
			if (data == 0) data = this.memory_volumeSlideFine;
			else           this.memory_volumeSlideFine = data;
			
			this.channelVolume = Math.max(0, Math.min(0x40, this.channelVolume - data));
			this.tremoloOverwritten = true;
			break;
		
		// Volume slide combos
		case VOLUME_SLIDE_CONT_PORTA: 
			if (dataRow.note >= Constants.NOTE_FIRST && dataRow.note < Constants.NOTE_FIRST + Constants.NOTE_COUNT)
				this.portaNoteTarget = this.sample.getRelativeNote() + (dataRow.note - Constants.NOTE_FIRST);

			if (this.portaNoteTarget != -1) {
				this.portaNotePower = 4*this.memory_tonePorta;
				this.portaNoteAmount++;
			}

			if (data == 0) data = this.memory_volumeSlide;
			else           this.memory_volumeSlide = data;

			int down_PORTAVOL = data & 0xf;
			int up_PORTAVOL = (data >> 4) & 0xf;
			
			this.volumeSlideAmount += up_PORTAVOL > 0 ? up_PORTAVOL : -down_PORTAVOL;
			break;
		case VOLUME_SLIDE_CONT_VIBRATO: 
			this.vibratoEnabled = true;
			
			if (data == 0) data = this.memory_volumeSlide;
			else           this.memory_volumeSlide = data;

			int down_VIBVOL = data & 0xf;
			int up_VIBVOL = (data >> 4) & 0xf;
			
			this.volumeSlideAmount += up_VIBVOL > 0 ? up_VIBVOL : -down_VIBVOL;
			break;
		case VOLUME_SLIDE_AND_RETRIGGER_NOTE:
			this.retriggerEnabled = true;
			if ((data & 0xf) != 0) this.memory_retriggerDelay = data & 0xf;
			if (((data>>4) & 0xf) != 0) this.memory_retriggerMode = (data>>4) & 0xf;
			this.retriggerMode = this.memory_retriggerMode;
			break;
		
		// Note Things
		case CUT: 
			this.cutTick = data; 
			break;
		case DELAY_NOTE: 
			this.playTick = data;
			this.playRow = dataRow;
			break;
		case KEY_OFF: 
			this.keyOffTick = data;
			break;
		
		case RETRIGGER_NOTE: 
			if (data != 0) {
				this.retriggerEnabled = true;
				this.memory_retriggerDelay = data;
			} else {
				int volume = this.channelVolume;
				int pan = this.channelPanning;
				this.resetNote();
				this.channelPanning = pan;
				this.channelVolume = volume;
				this.resetSample();
			}
			break;
		case SAMPLE_OFFSET: 
			if (this.sample != null && dataRow.note >= Constants.NOTE_FIRST && dataRow.note < Constants.NOTE_FIRST + Constants.NOTE_COUNT) {
				if (data == 0) data = this.memory_sampleOffset;
				else           this.memory_sampleOffset = data;
				
				this.sampleNumber = data * 0x100;
				this.sampleFraction = 0f;
			}
			break;
		case SET_ENVELOPE_FRAME: 
			if (this.volumeEnv != null) {
				this.volumeEnv.setFrame(data);
				if (this.volumeEnv.getSustainEnabled() && this.panningEnv != null)
					this.panningEnv.setFrame(data);
			}
			break;
		case SET_FINETUNE: 
			this.fineTune = (byte) (data - 128);
			break;
		
		// Other
		case VIBRATO_CONTROL: break;
		case TREMOLO_CONTROL: break;
		case TREMOR: 
			if (data == 0) data = this.memory_tremor;
			else           this.memory_tremor = data;

			this.tremorOnTicks = ((data>>4) & 0xf) + 1;
			this.tremorOffTicks = (data & 0xf) + 1;
			break;
		
		default:
			break;
		
		}
		
	}
	public void runVolumeColumn(PatternData data) {
		int volume = 0xff & data.volume;

		int upperNibble = (volume >> 4) & 0xf;
		int lowerNibble = volume & 0xf;
		
		if (volume >= 0x10 && volume <= 0x40) {
		}
		switch (upperNibble) {
		case 0x01:
		case 0x02:
		case 0x03:
		case 0x04:
		case 0x05: // Set Volume
			this.channelVolume = Math.min(volume - 0x10, 0x40);
			this.tremoloOverwritten = true;
			break;
			
		case 0x06: // Volume slide down.
			this.volumeSlideAmount -= lowerNibble; 
			this.tremoloOverwritten = true;
			break;
		case 0x07: // Volume slide up.
			this.volumeSlideAmount += lowerNibble; 
			this.tremoloOverwritten = true;
			break;
			
		case 0x08: // Fine volume slide down.
			this.channelVolume = Math.max(0, this.channelVolume - lowerNibble); 
			this.tremoloOverwritten = true;
			break;
		case 0x09: // Fine volume slide up.
			this.channelVolume = Math.min(0x40, this.channelVolume + lowerNibble);
			break;
			
		case 0x0A: // Set vibrato speed. (param shared with effect column)
			int vibeSpeed = lowerNibble;

			if (vibeSpeed == 0) vibeSpeed = this.memory_vibratoSpeed;
			else                this.memory_vibratoSpeed = vibeSpeed;
			
			this.vibratoLFO.setFrequencyParameter(4*vibeSpeed);
			break;
		case 0x0B: // Run vibrato. (set depth) (param shared with effect column)
			int vibeDepth = lowerNibble;
			
			if (vibeDepth == 0) vibeDepth = this.memory_vibratoDepth;
			else                this.memory_vibratoDepth = vibeDepth;
			
			this.vibratoEnabled = true;
			this.vibratoLFO.setAmplitudeParameter(vibeDepth);
			break;
			
		case 0x0C: // Set panning
			this.channelPanning = 0x11 * lowerNibble;
			break;
		case 0x0D: // Pan left
			this.panningSlideAmount -= lowerNibble;
			break;
		case 0x0E: // Pan right
			this.panningSlideAmount += lowerNibble;
			break;
			
		case 0x0F: // Portamento to note

			if (data.note >= Constants.NOTE_FIRST && data.note < Constants.NOTE_FIRST + Constants.NOTE_COUNT)
				this.portaNoteTarget = this.sample.getRelativeNote() + (data.note - Constants.NOTE_FIRST);

			lowerNibble *= 0x10;
			
			if (lowerNibble == 0) lowerNibble = this.memory_tonePorta;
			else                  this.memory_tonePorta = lowerNibble;
			
			if (this.portaNoteTarget != -1) {
				this.portaNotePower = 4*lowerNibble;
				this.portaNoteAmount++;
			}
			break;
			
			
		}
	}
	
	public void resetEffectData() {
		this.portaNoteAmount = 0;
		this.portaTickAmount = 0;
		this.portaNotePower = 0;

		this.panningSlideAmount = 0;
		this.volumeSlideAmount = 0;
		
		this.arpIndex = 0;
		Arrays.fill(this.arpNotes, 0);
		this.arpNotes[0] = 0;

		this.cutTick = -1;
		this.keyOffTick = -1;
		this.playTick = -1;
		this.playRow = null;
		
		this.retriggerEnabled = false;
		this.retriggerMode = -1;

		this.tremorOnTicks = 1;
		this.tremorOffTicks = 0;
		
		this.vibratoEnabled = false;
		this.vibratoEnabledVolumeColumn = false;
		this.tremoloEnabled = false;
	}

	
	public void runRow(PatternData data, boolean isLateTrigger) {
		Instrument instrument = this.controller.song.getInstrument(data.instrument-1);
		boolean noteValid = data.note >= Constants.NOTE_FIRST && data.note < Constants.NOTE_FIRST + Constants.NOTE_COUNT;
		
		if (data.instrument > 0 && (noteValid || data.note == Constants.NOTE_KEYOFF) && instrument == null)
			this.cut();
		else if (!noteValid && instrument != null) {
			if (isLateTrigger || this.canNotePlayImmediately(data))
				this.resetNote();
		} else if ((noteValid || data.note == Constants.NOTE_KEYOFF) && (isLateTrigger || this.canNotePlayImmediately(data))) {
			if (data.note == Constants.NOTE_KEYOFF)
				this.noteOff();
			else if (instrument != null)
				this.playNote(data.note-Constants.NOTE_FIRST, instrument);
			else 
				this.switchNote(data.note-Constants.NOTE_FIRST);
		} else if (this.isNotePorta(data) && this.hasSample() && data.instrument > 0)
			this.resetNote();
		
		if (!isLateTrigger) {
			this.resetEffectData();
			this.runVolumeColumn(data);
			this.runEffect(data);
		} else {
			int volume = data.volume & 0xff;
			// Only run set volume and fine volume slide.
			if (volume >= 0x10 && volume < 0x50 || volume >= 0x80 && volume < 0xA0)
				this.runVolumeColumn(data);
			
			EffectType type = EffectType.get(data.effectType, data.effectData);
			if (type == EffectType.SET_PANNING || 
				type == EffectType.SET_PANNING_EXTCMD || 
				type == EffectType.SET_FINETUNE || 
				type == EffectType.SET_VOLUME || 
				type == EffectType.SAMPLE_OFFSET || 
				type == EffectType.PORTA_DOWN_FINE || 
				type == EffectType.PORTA_EXTRA_FINE_DOWN ||
				type == EffectType.PORTA_EXTRA_FINE_UP || 
				type == EffectType.PORTA_UP_FINE || 
				type == EffectType.VOLUME_SLIDE_FINE_DOWN || 
				type == EffectType.VOLUME_SLIDE_FINE_UP) {
				this.runEffect(data);
			}
		}
	}
	public void runRow(PatternData data) {
		this.runRow(data, false);
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

		this.bufferFadeout();
		
		this.note = note + this.sample.getRelativeNote();
		this.fineTune = this.sample.getFineTune();
		this.pitchBend = 0;
		this.sampleNumber = 0;
		this.sampleFraction = 0f;
		this.lastVolume = 0f;
		this.justCut = true;
	}
	/**
	 * Reset the sample to its start.
	 */
	public void resetSample() {
		if (this.sample == null) return;

		this.bufferFadeout();
		this.sampleNumber = 0;
		this.sampleFraction = 0f;
		this.lastVolume = 0f;
		this.justCut = true;
	}
	/**
	 * Reset the envelopes and volume of a note while it is playing.
	 */
	public void resetNote() {
		if (this.sample == null) return;
		
		this.channelVolume = this.sample.getVolume();
		this.channelPanning = this.sample.getPanning();
		
		this.autoVibratoLFO = this.instrument.getAutoVibratoLFO();
		
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
		this.justCut = true;
	}
	/**
	 * Mute the channel by nullifying the sample.
	 */
	public void hardCut() {
		this.bufferFadeout();
		this.lastVolume = 0f;
		this.sample = null;
		this.isHeld = false;
		
	}
	
	private void bufferFadeout() {
		int fadeoutSize = Constants.FADEOUT_SAMPLES;
		DoubleBuffer fadeoutSamples = DoubleBuffer.allocate(fadeoutSize);
		for (int i = 0; i < fadeoutSize; i++) {
			float power = (fadeoutSize-i)/(float)fadeoutSize;
			fadeoutSamples.put(power * this.sample());
		}
		fadeoutSamples.position(0);
		this.fadeoutSamples = fadeoutSamples;
	}
	
	/**
	 * Returns true if the PatternData provided will result in a note playing on the first tick.
	 * @param data The PatternData to examine.
	 * @return If a note will play immediately given the PatternData data.
	 */
	public boolean canNotePlayImmediately(PatternData data) {
		return data.effectType != 3 && data.effectType != 5 && 
			!(data.effectType == 20 && data.effectData == 0) && 
			!(data.effectType == 14 && ((data.effectData & 0xff) == 0xC0 || (data.effectData & 0xf0) == 0xd0 && (data.effectData & 0xf) > 0)) && 
			(data.volume & 0xF0) != 0xF0;
	}
	/**
	 * Returns True if the note playing will result in note to note portamento.
	 * @param data The PatternData to examine.
	 * @return If the note playing will result in note to note portamento.
	 */
	public boolean isNotePorta(PatternData data) {
		return data.note >= Constants.NOTE_FIRST && data.note < Constants.NOTE_FIRST + Constants.NOTE_COUNT && 
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
