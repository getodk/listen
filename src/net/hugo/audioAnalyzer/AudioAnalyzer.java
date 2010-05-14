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

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * @author hugosg
 *
 * This is the main entrance to the program. It is an 
 * Android activity with four buttons, an one private class that
 * extends a view for drawing a simple image during recording.
 * 
 * The buttons create and execute AsynTask task for play, record, and stop.
 * 
 * For analyze it generates a new Activity
 * 
 * It implements the RecorderListener so it could get the value of the
 * amplitude during recording.
 * 
 * It implements PlayerListener so it pops a dialog during the time it takes
 * to load the file into memory
 * 
 */
public class AudioAnalyzer extends Activity implements RecorderListener, PlayerListener {

	private Recorder myRecorder;
	private AudioShape audioShape;
	private Button recButton, playButton, stopButton, analyzerButton;
	private ProgressDialog dialog;

	public class AudioShape extends View {
		
		private ShapeDrawable mDrawable;
		private int x = 100;
		private int y = 100;
		
		/**
		 * The previous values is keep to be able to do a simple low pass filter and smooth the value
		 */
		private float previousSize = 0;
		
		public AudioShape(Context context) {
			super(context);
			mDrawable = new ShapeDrawable(new OvalShape());
			mDrawable.getPaint().setColor(0xFF0000FF);
			mDrawable.setBounds(x, y, x, y);
		}

		protected void onDraw(Canvas canvas) {
			mDrawable.draw(canvas);
		}
		
		/**
		 * @param sizeF
		 * 
		 * It assumes it will get a value between 0.0 and 1.0
		 * The location is rigid --not ideal
		 */
		public void setSize(float sizeF){
			float size = ((previousSize * 0.35f) + (sizeF * 0.65f)) / 2.0f; //Low pass filtering
			int sizeI = (int) (size * 200);
			mDrawable.getPaint().setColor(Color.rgb(0xFF, 255- sizeI, 0));
			mDrawable.setBounds(x - sizeI + 60, y - sizeI, x + sizeI + 60, y + sizeI);
			previousSize = size;
		}
	}
	
	/** Called when the activity is first created. */	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//the id of the main layout was manually added in the XML file!
		LinearLayout ll = (LinearLayout) findViewById(R.id.main_layout);
		
		recButton = (Button) findViewById(R.id.recButton);
		recButton.setEnabled(true);
		recButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				startRecording();
				
			}
		});

		stopButton = (Button) findViewById(R.id.stopButton);
		stopButton.setEnabled(false);
		stopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				stopRecording();
			}
		});

		playButton = (Button) findViewById(R.id.playButton);
		playButton.setEnabled(false);
		playButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				playRecording();
			}
		});

		analyzerButton = (Button) findViewById(R.id.analyzerButton);
		analyzerButton.setEnabled(false);
		analyzerButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				analyzeRecording();
			}
		});
		
		audioShape = new AudioShape(this);
		ll.addView(audioShape);
	}

	private void startRecording(){
		recButton.setEnabled(false);
		stopButton.setEnabled(true);
		playButton.setEnabled(false);
		analyzerButton.setEnabled(false);
		myRecorder = new Recorder();
		myRecorder.addRecorderListener(this);
		myRecorder.execute(this);
	}

	private void playRecording(){
		Player myPlayer = new Player();
		myPlayer.addPlayerListener(this);
		myPlayer.execute(this);
	}

	private void analyzeRecording(){
		Intent intent = new Intent(this, AnalyzerActivity.class);
		startActivity(intent);
	}

	private void stopRecording(){
		recButton.setEnabled(true);
		stopButton.setEnabled(false);
		playButton.setEnabled(true);
		analyzerButton.setEnabled(true);
		myRecorder.stopRecording();
	}

	@Override
	public void recordPart(ArrayList<Float> arrayList) {
		Log.i("HUGO", "getting value from Recorder");
		audioShape.setSize(arrayList.get(0).floatValue());
		audioShape.invalidate();
	}

	@Override
	public void postPlayer() {
		dialog.dismiss();
	}

	@Override
	public void prePlayer() {
		dialog = ProgressDialog.show(this, "", "Playing. Please wait...", true);
	}
}