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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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
public class Recorder extends AsyncTask<AudioAnalyzer, ArrayList<Float>, Void>{

	public boolean isRecording = false;
	
	private AudioRecord audioRecord = null;
	private DataOutputStream dos = null;
	
	private RecorderListener rl;
	
	/**
	 * @param rl
	 * 
	 * As the Player, we assume there will be only one listener
	 */
	public void addRecorderListener(RecorderListener rl){
		this.rl = rl;
	}
	
	@Override
	protected void onProgressUpdate(ArrayList<Float>... list) {
		//super.onProgressUpdate(values);
		synchronized(this){
			rl.recordPart(list[0]);
		}
	}

	@Override
	protected Void doInBackground(AudioAnalyzer... params) {
		int frequency = 44100; //hard coded, not idea
		int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
		int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
		File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.pcm");

		// Delete any previous recording.
		if (file.exists())
			file.delete();


		// Create the new file.
		try {
			file.createNewFile();
			Log.i("HUGO", "file created fine");
		} catch (IOException e) {
			throw new IllegalStateException("Failed to create " + file.toString());
		}

		try {
			// Create a DataOuputStream to write the audio data into the saved file.
			OutputStream os = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(os);
			dos = new DataOutputStream(bos);

			// Create a new AudioRecord object to record the audio.
			int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration,  audioEncoding);
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 
					frequency, channelConfiguration, 
					audioEncoding, bufferSize);

			short[] buffer = new short[bufferSize];   
			Log.i("HUGO", "The audio record created fine ready to record");

			audioRecord.startRecording();
			isRecording = true;

			Log.i("HUGO", "Start recording fine");

			while (isRecording) {
				int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
				float maxOfBuffer = 0;
				for (int i = 0; i < bufferReadResult; i++){
					dos.writeShort(buffer[i]);
					
					//This two lines are for extracting the Mayor value in the current buffer
					float currentValue = (float)(Math.abs(buffer[i] * 1.0 / (Short.MAX_VALUE + 1)));
					if(currentValue > maxOfBuffer) maxOfBuffer = currentValue;
				}
				//These three lines are for sending (publishing) the value inside an array which is passed to the listener
				//so we can track the max value
				ArrayList<Float> maxData = new ArrayList<Float>();
				maxData.add(maxOfBuffer);
				publishProgress(maxData);
			}

		} catch (Throwable t) {
			Log.e("HUGO","Recording Failed");
		}
		return null;
	}

	/**
	 * This stop the recording, this is called from the main activity
	 */
	public void stopRecording(){
		isRecording = false;
		Log.i("HUGO", "Out of recording");
		audioRecord.stop();
		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
