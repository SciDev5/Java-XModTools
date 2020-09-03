package me.scidev5.xmodtools;

import java.io.File;

import me.scidev5.xmodtools.data.file.SongData;
import me.scidev5.xmodtools.data.Song;
import me.scidev5.xmodtools.player.XMAudioController;
import me.scidev5.xmodtools.player.PlayerThread;

public class TestMain {

	public static void main(String[] args) throws Exception {
		
		String[] files = new String[] {"Example Songs/milky.xm","Example Songs/sv_ttt.xm","other stuff/dev/TEST4.xm"};
		int i = 1;
		
		File root = new File("D:/Art/milkytracker-1.02.00/");
		Song song = SongData.loadFromFile(new File(root,files[i]));
		
		
		PlayerThread player = new PlayerThread();
		
		XMAudioController controller = new XMAudioController(song, player.getAudioFormat());
		player.setController(controller);
		
		player.start();

		System.in.read();
		
		player.end();
	}

}
