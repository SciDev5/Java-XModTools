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
