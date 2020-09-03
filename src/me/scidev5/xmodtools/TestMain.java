package me.scidev5.xmodtools;

import java.io.File;

import me.scidev5.xmodtools.data.file.SongData;
import me.scidev5.xmodtools.data.Song;
import me.scidev5.xmodtools.player.XMAudioController;
import me.scidev5.xmodtools.player.PlayerThread;

public class TestMain {

	public static void main(String[] args) throws Exception {
		
		String name = "TEST3";
		
		File root = new File("C:/Users/Joey/Desktop/Art/milkytracker-1.02.00/");
		//File dir = new File(root,"Example Songs/");
		File dir = new File(root,"other stuff/dev/");
		Song song = SongData.loadFromFile(new File(dir,name+".xm"));
		
		
		PlayerThread player = new PlayerThread();
		
		XMAudioController controller = new XMAudioController(song, player.getAudioFormat());
		player.setController(controller);
		
		player.start();

		Thread.sleep(10000);
		player.end();
	}

}
