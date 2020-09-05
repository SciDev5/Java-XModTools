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
 * master branch of theGitHub repositiory (https://github.com/SciDev5/Java-XModTools).
 */

package me.scidev5.xmodtools.player.util;

public enum SampleInterpolation {
	LINEAR((short sampleStart, short sampleEnd, double interpolate) -> {
		double start = ((sampleStart+0x8000)/(double)0xffff)*2-1;
		double end = ((sampleEnd+0x8000)/(double)0xffff)*2-1;
		return start + interpolate * (end - start);
	}),
	STEP((short sampleStart, short sampleEnd, double interpolate) -> {
		return ((sampleStart+0x8000)/(double)0xffff)*2-1;
	});
	// TODO add more interpolators
	
	
	private Calculate calculateFunction;
	private SampleInterpolation(Calculate calculateFunction) {
		this.calculateFunction = calculateFunction;
	}
	public double apply(short sampleStart, short sampleEnd, double interpolate) {
		return this.calculateFunction.apply(sampleStart, sampleEnd, interpolate);
	}
	public double apply(byte sampleStart, byte sampleEnd, double interpolate) {
		return this.calculateFunction.apply((short)(sampleStart<<8), (short)(sampleEnd<<8), interpolate);
	}
	
	@FunctionalInterface
	private interface Calculate {
		public double apply(short sampleStart, short sampleEnd, double interpolate);
	}
}
