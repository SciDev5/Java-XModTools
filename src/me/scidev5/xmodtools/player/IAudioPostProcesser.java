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

package me.scidev5.xmodtools.player;

public interface IAudioPostProcesser {
	public void processData(short[] dataL, short[] dataR);
}
