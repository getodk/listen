/*
 * This code is part of the project "Audio Analyzer for the Android"
 * developed for the course CSE 599Y
 * "Mobile and Cloud Applications for Emerging Regions" 
 * at the University of Washington Computer Science & Engineering
 * 
 * The goal of this project is to create an audio analyzer that
 * allows the user to record, play and analyze audio files.
 * The program plot the waveform of the recording, the spectrogram,
 * and plot several audio descriptors.
 * 
 * At the current state the audio descriptors are:
 * 	- Spectral Centroid
 * 	- Spectral Centroid Variation
 * 	- Energy
 * 	- Energy Variation
 * 	- Zero Crossing
 * 	- Zero Crossing Variation
 * 
 * In addition to this temporal descriptors the total average of them
 * is presented in numeral format with the duration of the recording, and
 * the number of samples.
 * 
 * Otherwise noticed, the code was created by Hugo Solis
 * hugosg@uw.edu, feel free to contact me if you have any questions.
 * Dec 16, 2009
 * hugosg
 */
package net.hugo.audioAnalyzer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

/**
 * @author hugosg
 * 
 * This code is an extension and variation of the code in
 * http://emeadev.blogspot.com/2009/09/raw-audio-manipulation-in-android.html
 *
 */
public class Player extends AsyncTask<AudioAnalyzer, Void, Void>{
	
	PlayerListener pl;
	
	/**
	 * @param pl
	 * 
	 * This class waits for only one listener
	 */
	public void addPlayerListener(PlayerListener pl){
		this.pl = pl;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		//super.onPostExecute(result);
		synchronized(this){
			pl.postPlayer();
		}
	}

	@Override
	protected void onPreExecute() {
		//super.onPreExecute();
		synchronized(this){
			pl.prePlayer();
		}
	}

	@Override
	protected Void doInBackground(AudioAnalyzer... params) {
		// Get the file we want to playback.
		File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.pcm");
		// Get the length of the audio stored in the file (16 bit so 2 bytes per short)
		// and create a short array to store the recorded audio.
		int musicLength = (int)(file.length()/2);
		short[] music = new short[musicLength];

		try {
			// Create a DataInputStream to read the audio data back from the saved file.
			InputStream is = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(is);
			DataInputStream dis = new DataInputStream(bis);

			// Read the file into the music array.
			int i = 0;
			while (dis.available() > 0) {
				music[i] = dis.readShort();
				i++;
			}

			// Close the input streams.
			dis.close();     

			// Create a new AudioTrack object using the same parameters as the AudioRecord
			// object used to create the file.
			AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 
					44100, //hard coded! not ideal
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT, 
					musicLength, 
					AudioTrack.MODE_STREAM);
			
			//There is a minor issue. This audio Track player will play the file
			//until the end without a way to track the location of the reading or the end of playing :-(
			//at least as we use it here
			// Start playback
			audioTrack.play();
			// Write the music buffer to the AudioTrack object
			audioTrack.write(music, 0, musicLength);

		} catch (Throwable t) {
			Log.e("AudioTrack","Playback Failed");
		}
		//Log.i("HUGO", "DONE PLAYING");
		return null;
	}
}
