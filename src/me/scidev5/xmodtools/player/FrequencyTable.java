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

import java.util.function.Function;

public enum FrequencyTable {
	LINEAR((int trueNote, double fineTune) -> {
		return 7680 - (trueNote * 64) - (fineTune / 2.0);
	}, (double period) -> {
		return 8363 * Math.pow(2.0,((4608 - period) / 768.0));
	}),
	AMIGA((int trueNote, double fineTune) -> {
		short[] periodTable = new short[] {
			907,900,894,887,881,875,868,862,856,850,844,838,832,826,820,814,
			808,802,796,791,785,779,774,768,762,757,752,746,741,736,730,725,
			720,715,709,704,699,694,689,684,678,675,670,665,660,655,651,646,
			640,636,632,628,623,619,614,610,604,601,597,592,588,584,580,575,
			570,567,563,559,555,551,547,543,538,535,532,528,524,520,516,513,
			508,505,502,498,494,491,487,484,480,477,474,470,467,463,460,457
		};
		Function<Integer,Short> indexPeriodTable = i -> {
			if (i < 0 || i >= periodTable.length) return 0;
			return periodTable[i];
		};
		double fracFineTune = fineTune/16.0-Math.floor(fineTune/16.0);
		int v = -11;
		return (
			indexPeriodTable.apply((int) (((trueNote+v) % 12)*8 + Math.floor(fineTune/16.0)+0))*(1-fracFineTune)+
			indexPeriodTable.apply((int) (((trueNote+v) % 12)*8 + Math.floor(fineTune/16.0)+1))*fracFineTune
		) * 16.0 / Math.pow(2.0, (trueNote+v) / 12);
	}, (double period) -> {
		return 8363.0*1712.0/period;
	});
	

	private CalculatePeriod calculatePeriodFunction;
	private CalculateSampleRate calculateSampleRateFunction;
	private FrequencyTable(CalculatePeriod calculatePeriodFunction, CalculateSampleRate calculateSampleRateFunction) {
		this.calculatePeriodFunction = calculatePeriodFunction;
		this.calculateSampleRateFunction = calculateSampleRateFunction;
	}
	public double calculateSampleRate(int trueNote, byte fineTune) {
		return this.calculateSampleRateFunction.apply(this.calculatePeriod(trueNote, fineTune));
	}
	public double calculateSampleRate(int trueNote, byte fineTune, double pitchBend) {
		return this.calculateSampleRateFunction.apply(this.calculatePeriod(trueNote, fineTune, pitchBend));
	}
	public double calculateSampleRateGlissando(int trueNote, byte fineTune, double glissandoBend, double pitchBend) {
		return this.calculateSampleRateFunction.apply(this.calculatePeriodGlissandoBend(trueNote, fineTune, glissandoBend, pitchBend));
	}
	public double calculatePeriod(int trueNote, byte fineTune) {
		return this.calculatePeriod(trueNote, fineTune, 0);
	}
	public double calculatePeriod(int trueNote, byte fineTune, double pitchBend) {
		return Math.max(50, this.calculatePeriodFunction.apply(trueNote, fineTune) + pitchBend);
	}
	public double calculatePeriodGlissandoBend(int trueNote, byte fineTune, double glissandoBend, double pitchBend) {
		double value = this.calculatePeriodFunction.apply(trueNote, fineTune);
		if (glissandoBend > 0) {
			int i = 0;
			while (value - this.calculatePeriodFunction.apply(trueNote-i, fineTune) > -glissandoBend)
				i++;
			value = this.calculatePeriodFunction.apply(trueNote-i, fineTune);
		} else if (glissandoBend < 0) {
			int i = 0;
			while (value - this.calculatePeriodFunction.apply(trueNote+i, fineTune) < -glissandoBend)
				i++;
			value = this.calculatePeriodFunction.apply(trueNote+i, fineTune);
		}
		
		return Math.max(50, value+pitchBend);
	}

	@FunctionalInterface
	private interface CalculatePeriod {
		public double apply(int trueNote, double fineTune);
	}
	@FunctionalInterface
	private interface CalculateSampleRate {
		public double apply(double period);
	}
}
