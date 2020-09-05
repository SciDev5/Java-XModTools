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

package me.scidev5.xmodtools;

import java.io.File;

import me.scidev5.xmodtools.data.file.SongData;
import me.scidev5.xmodtools.player.PlayerThread;
import me.scidev5.xmodtools.player.XMAudioController;

public class Demo {

	public static void main(String[] args) throws Exception {
		
		// replace "./test.xm" with the location of your .xm file.
		PlayerThread player = new PlayerThread();
		XMAudioController controller = new XMAudioController(SongData.loadFromFile(new File("./test.xm")), player.getAudioFormat());
		player.setController(controller);
		
		player.start();
		
		System.in.read();
		
		player.end();
	}

}
