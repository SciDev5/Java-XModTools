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

package me.scidev5.xmodtools.player.automation;

import java.util.function.Function;

public class LFO {
	
	/*
	 * NOTES:
	 *   DEPTH: (LINEAR) 0x8 -> amplitude 0.5.
	 *   FREQUENCY: 0.006*param (units: pos/tick)
	 *   
	 *   autovibrato:
	 *     speed $10, depth: %f ~= normal $4, $2
	 */
	
	protected double position = 0;
	
	protected int frequencyParameter = 0;
	protected float amplitude = 0;
	
	protected float value = 0;

	protected int sweepAmount = 0;
	protected int sweepTotal = 0;
	
	protected WaveForm waveForm = WaveForm.SINE;
	
	/**
	 * Calculate the LFO's new value and increment wave position and sweep values.
	 * @return The new calculated value.
	 */
	public float calculate() {
		if (this.amplitude == 0 || this.frequencyParameter == 0) {
			this.value = 0;
			return this.value;
		}
		
		this.value = this.position == 0 ? 0 : this.waveForm.apply(this.position) * amplitude;
		
		if (this.sweepTotal > this.sweepAmount) {
			this.sweepAmount ++;
			this.value *= this.sweepAmount / (float)this.sweepTotal;
		}
		
		
		this.position += 0.004f * this.frequencyParameter;
		this.position = Math.max(0, this.position) % 1.0;
		
		return this.value;
	}
	
	/**
	 * Get the LFO's value at the last calculation.
	 * @return The LFO's current value.
	 */
	public float get() {
		return this.value;
	}
	
	/**
	 * Get the waveform used by this LFO.
	 * @return The current waveForm.
	 */
	public WaveForm getWaveForm() {
		return this.waveForm;
	}
	/**
	 * Set the waveForm of the LFO. (Continuous waveforms don't jump back to their start
	 * when the note is reset.)
	 * @param waveForm The waveForm for the LFO.
	 * @throws IllegalArgumentException If the given waveform is null.
	 */
	public void setWaveForm(WaveForm waveForm) throws IllegalArgumentException {
		if (waveForm == null) throw new IllegalArgumentException("waveForm in was null");
		this.waveForm = waveForm;
	}

	/**
	 * Set the frequency parameter which will result in a true frequency of 
	 * <code>(LFO calculations per second)/(0.004*(frequency parameter))</code>.
	 * @param frequencyParam The speed of the LFO oscillation.
	 * @throws IllegalArgumentException If a negative value was provided.
	 */
	public void setFrequencyParameter(int frequencyParam) throws IllegalArgumentException {
		if (frequencyParam < 0) throw new IllegalArgumentException("Frequency parameter was less than 0.");
		this.frequencyParameter = frequencyParam;
	}
	/** 
	 * Set the LFO depth by an integer.
	 * @param amplitude Depth in 16ths of maximum.
	 */
	public void setAmplitudeParameter(int amplitude) {
		this.amplitude = amplitude / (float) 0x10;
	}
	/**
	 * Set the LFO depth by an integer.
	 * @param amplitude Depth in 128ths of maximum.
	 */
	public void setAmplitudeParameterFinely(int amplitude) {
		this.amplitude = amplitude / (float) 0x80;
	}
	/**
	 * Set the default depth (amplitude) of this LFO.
	 * @param amplitude The depth of the LFO. (0 -> no effect; 1 -> maximum effect; range not restrained).
	 */
	public void setAmplitude(float amplitude) {
		this.amplitude = amplitude;
	}

	/**
	 * Set the number of ticks before full depth.
	 * @param sweep The sweep parameter.
	 */
	public void setSweep(int sweep) {
		this.sweepTotal = Math.max(0, sweep);
	}
	
	/**
	 * Creates a copy of this LFO with all the same parameters set to wave position 0.
	 * @return A copy of this LFO. (Not linked)
	 */
	public LFO copy() {
		LFO lfo = new LFO();
		lfo.setAmplitude(this.amplitude);
		lfo.setFrequencyParameter(this.frequencyParameter);
		lfo.setSweep(this.sweepTotal);
		return lfo;
	}
	/**
	 * Reset the wave position of the LFO.
	 */
	public void resetPosition() {
		if (this.waveForm.resetOnNote)
			this.position = 0;
		this.sweepAmount = 0;
	}
	
	
	public enum WaveForm {
		SINE(frac -> {
			return (float) Math.sin(Math.PI*2*frac);
		},true),
		RAMP_DOWN(frac -> {
			return 1-2*frac;
		},true),
		SQUARE(frac -> {
			return frac > 0.5f ? -1.0f : 1.0f;
		},true),
		SINE_CONTINUOUS(frac -> {
			return (float) Math.sin(Math.PI*2*frac);
		},false),
		RAMP_DOWN_CONTINUOUS(frac -> {
			return 1-2*frac;
		},false),
		SQUARE_CONTINUOUS(frac -> {
			return frac > 0.5f ? -1.0f : 1.0f;
		},false);
		
		private final Function<Float, Float> calculation;
		public final boolean resetOnNote;
		private WaveForm(Function<Float, Float> calculation, boolean resetOnNote) {
			this.calculation = calculation;
			this.resetOnNote = resetOnNote;
		}
		
		/**
		 * Get the value for the waveForm at a given time.
		 * @param position The time. (in wave cycles, 0 -> beginning; 1 -> end)
		 * @return The value for the waveForm.
		 */
		public float apply(double position) {
			return this.calculation.apply((float) (position - Math.floor(position)));
		}
		
		/**
		 * Get a waveForm by index.
		 * <br>
		 * <code>0: SINE, 1: RAMP_DOWN, 2: SQUARE, 3: null
		 * 4: SINE_CONTINUOUS, 5: RAMP_DOWN_CONTINUOUS, 
		 * 6: SQUARE_CONTINUOUS, 7+: null</code>
		 * 
		 * @param i The index to look at.
		 * @return The waveForm at that index.
		 */
		public static WaveForm get(int i) {
			if (i >= 0 && i < 3)
				return (new WaveForm[] {SINE,RAMP_DOWN,SQUARE})[i];
			else if (i >= 4 && i < 8)
				return (new WaveForm[] {SINE_CONTINUOUS,RAMP_DOWN_CONTINUOUS,SQUARE_CONTINUOUS})[i-4];
			else 
				return null;
		}
	}
}
