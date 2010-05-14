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
import java.util.Iterator;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * @author hugosg
 *
 * This Activity, which is started by the press of the Analyze button
 * on the AudioAnalyzer activity -the main- will start the Analyzer and will be
 * register as a listener to such analyzer plotting all the values as they
 * are generated. Be carefull with the correspondence in the head, order, and
 * number of values inside the lists that are pass throw the listener.
 */
public class AnalyzerActivity extends Activity {

	private Analyzer analyzer;
	private AnalyzerView mAnalView;
	private ProgressDialog dialog;

	/** Called when the activity is first created. */	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAnalView = new AnalyzerView(this);
		setContentView(mAnalView);
		analyzer = new Analyzer();
		analyzer.addListener(mAnalView);
		analyzer.execute(this);
		dialog = ProgressDialog.show(AnalyzerActivity.this, "", "Analyzing. Please wait...", true);
	}

	private class AnalyzerView extends View implements AnalyzerListener {

		private Bitmap  mBitmap;
		private Paint   mPaint = new Paint();
		private Canvas  mCanvas = new Canvas();
		private float   mWidth;
		private float   mHeight;
		
		final int YLocation = 100;
		float previousX = 0;
		float previousY = YLocation;

		public AnalyzerView(Context context) {
			super(context);
			mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		}

		/* (non-Javadoc)
		 * @see net.hugo.audioAnalyzer.AnalyzerListener#analyzeDone()
		 * We track the moment when the analysis is done to
		 * remove the dialog
		 */
		@Override
		public void analyzeDone() {
			Log.i("HUGO", "done with the analyzis");
			dialog.dismiss();
		}

		/* (non-Javadoc)
		 * @see net.hugo.audioAnalyzer.AnalyzerListener#analyzePart(java.util.ArrayList)
		 * 
		 * This method comes from the listener... everything for the ploting happens here
		 * The first value of the list is just an ID to identify which step of the analyzis has
		 * been done.
		 */
		@Override
		public void analyzePart(ArrayList<Float> list) {
			synchronized (this) {
				Iterator<Float> iter = list.iterator();
				int ID = (int)iter.next().floatValue(); //get the first value and use as ID
				switch(ID){
				case 0:
					//called at the end with all the global values, Just draw the values as text
					Log.i("HUGO", "got HEAD listener call");
					if (mBitmap != null) {
						final Canvas canvas = mCanvas;
						final Paint paint = mPaint;
						paint.setColor(0xFFFFFFFF);
						int nSamples = (int)iter.next().floatValue(); //get mSamples
						canvas.drawText("No Samples: " + nSamples, 1, 400, paint);
						float duration = nSamples / 44100.0f;
						canvas.drawText("Duration: " + duration, 1, 410, paint);
						float sc_average = iter.next().floatValue(); //get SC
						canvas.drawText("Centroid Avg: " + sc_average, 150, 400, paint);
						float en_average = iter.next().floatValue(); //get EN
						canvas.drawText("Energy Avg: " + en_average, 150, 410, paint);
						float zc_average = iter.next().floatValue(); //get ZC
						canvas.drawText("Zero Cross Avg: " + zc_average, 150, 420, paint);
						invalidate(); //repaint the screen.
					}
					break;

				case 1:
					//plotted as a blue wave form.
					//Log.i("HUGO", "got AUDIOdata listener call");
					int frames = (int)iter.next().floatValue();
					int current_frame = (int)iter.next().floatValue();
					if (mBitmap != null) {
						final Canvas canvas = mCanvas;
						final Paint paint = mPaint;
						paint.setColor(0xFF0000FF);
						float value = iter.next().floatValue();
						float x = mWidth / frames * current_frame * 1.0f; //set x in the screen size range
						float y = (value + 1.0f) * YLocation; //200 ranges because y 0 to 2
						canvas.drawLine(previousX, previousY + 10, x, y + 10, paint); //10  just to put it a little bite down
						previousX = x; //track of previous values so we can trace the line
						previousY = y;
						invalidate();
					}
					break;

				case 2:
					//this is plotted as a normal spectrogram win an yellow->orange->red colors. With some tricks for visualziation normalization
					//Log.i("HUGO", "got FFTdata listener call");
					frames = (int)iter.next().floatValue();
					int fft_size = (int)iter.next().floatValue();
					current_frame = (int)iter.next().floatValue();
					if (mBitmap != null) {
						final Canvas canvas = mCanvas;
						final Paint paint = mPaint;
						for(int i = 0; i < fft_size; i++){
							float x = mWidth / frames * current_frame * 1.0f; //range of screen size
							//this implentation has a very important NOTGOOD! The hight of the FFT is determined by the FFT window size.
							//in this case 256. There is one Ypixel per FFT band. This is clearly not a good architecutre becuase as soon
							//as we change the FFT window size the visualization will change and break!!!!!!
							float y = 339 - i; //hard number, not good idea.
							float value = iter.next().floatValue();
							//Log.i("HUGO", Float.toString(value));
							//we are assuming the max value will be around 0.5 if its bigger we set a hard cut Good for visualization
							int valueColor = (int)Math.min(value * 512, 255.0); //512 just as guess with a min value. Not very elegant
							paint.setColor(Color.rgb(0xFF,valueColor, 0));
							canvas.drawPoint(x, y, paint);
							invalidate();
						}
					}
					break;
					
				case 3:
					//this is plotted as a blue dot for each window on top of the spectrogram
					//Log.i("HUGO", "got SPECTRALCENTROIDdata listener call");
					frames = (int)iter.next().floatValue();
					current_frame = (int)iter.next().floatValue();
					float value = iter.next().floatValue();
					//Log.i("HUGO", "Spectral centroid " + Float.toString(value));
					if (mBitmap != null) {
						final Canvas canvas = mCanvas;
						final Paint paint = mPaint;
						float x = mWidth / frames * current_frame * 1.0f; //all the file in the screen range
						int y = (int)(339 - value); //hard number. Same bad architecture abut the FFTSize Vs Pixels than the FFT
						paint.setColor(Color.rgb(0x00,0x00, 0xFF));
						canvas.drawPoint(x, y, paint);
						invalidate();
					}
					break;
				
				case 35:
					//plotted as a grayscale bar
					//Log.i("HUGO", "got ESPECTALCENTROIDDERIATIVEdata listener call");
					frames = (int)iter.next().floatValue();
					current_frame = (int)iter.next().floatValue();
					value = iter.next().floatValue();
					//Log.i("HUGO", "Spectral derivative " + Float.toString(value));
					if (mBitmap != null) {
						final Canvas canvas = mCanvas;
						final Paint paint = mPaint;
						float x = mWidth / frames * current_frame * 1.0f; //in screen range
						int valueColor = (int)(Math.abs(value * 255)); //we are getting values between -1 and 1
						paint.setColor(Color.rgb(valueColor,valueColor, valueColor));
						canvas.drawLine(x, 340, x, 349, paint);
						invalidate();
					}
					break;
					
				case 4:
					//plotted as a grayscale bar
					//Log.i("HUGO", "got ENERGYdata listener call");
					frames = (int)iter.next().floatValue();
					current_frame = (int)iter.next().floatValue();
					value = iter.next().floatValue();
					//Log.i("HUGO", "ENERGY " + Float.toString(value));
					if (mBitmap != null) {
						final Canvas canvas = mCanvas;
						final Paint paint = mPaint;
						float x = mWidth / frames * current_frame * 1.0f;
						int valueColor = (int)(Math.min(value * 10, 255));
						paint.setColor(Color.rgb(valueColor,valueColor, valueColor));
						canvas.drawLine(x, 350, x, 359, paint);
						invalidate();
					}
					break;
					
				case 45:
					//plotted as a grayscale bar
					//Log.i("HUGO", "got ENERGYCENTROIDDERIATIVEdata listener call");
					frames = (int)iter.next().floatValue();
					current_frame = (int)iter.next().floatValue();
					value = iter.next().floatValue();
					//Log.i("HUGO", "ENERGY Derivative " + Float.toString(value));
					if (mBitmap != null) {
						final Canvas canvas = mCanvas;
						final Paint paint = mPaint;
						float x = mWidth / frames * current_frame * 1.0f;
						int valueColor = (int)(Math.abs(value * 255));
						paint.setColor(Color.rgb(valueColor,valueColor, valueColor));
						canvas.drawLine(x, 360, x, 369, paint);
						invalidate();
					}
					break;
					
				case 5:
					//plotted as a grayscale bar
					//Log.i("HUGO", "got ZEROCROSSINGdata listener call");
					frames = (int)iter.next().floatValue();
					current_frame = (int)iter.next().floatValue();
					value = iter.next().floatValue();
					if (mBitmap != null) {
						final Canvas canvas = mCanvas;
						final Paint paint = mPaint;
						float x = mWidth / frames * current_frame * 1.0f;
						int valueColor = (int)(256 * Math.min(value, 1.0) * 5);
						paint.setColor(Color.rgb(valueColor,valueColor, valueColor));
						canvas.drawLine(x, 370, x, 379, paint);
						invalidate();
					}
					break;
					
				case 55:
					//plotted as a grayscale bar
					//Log.i("HUGO", "got ZeroCrossingCENTROIDDERIATIVEdata listener call");
					frames = (int)iter.next().floatValue();
					current_frame = (int)iter.next().floatValue();
					value = iter.next().floatValue();
					//Log.i("HUGO", "Zro crossing Derivative " + Float.toString(value));
					if (mBitmap != null) {
						final Canvas canvas = mCanvas;
						final Paint paint = mPaint;
						float x = mWidth / frames * current_frame * 1.0f;
						int valueColor = (int)(Math.abs(value * 255));
						paint.setColor(Color.rgb(valueColor,valueColor, valueColor));
						canvas.drawLine(x, 380, x, 389, paint);
						invalidate();
					}
					break;
				}
			}
		}

		/* (non-Javadoc)
		 * @see android.view.View#onSizeChanged(int, int, int, int)
		 * Each time the size changed -when we rotate- or at the beginning
		 * we track the size of our window
		 */
		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
			mCanvas.setBitmap(mBitmap);
			mWidth = w;
			mHeight = h;
			super.onSizeChanged(w, h, oldw, oldh);
		}

		/* (non-Javadoc)
		 * @see android.view.View#onDraw(android.graphics.Canvas)
		 * Redraw the entire canvas on each call. Not a good idea!
		 */
		@Override
		protected void onDraw(Canvas canvas) {
			synchronized (this) {
				if (mBitmap != null) {
					final Canvas cavas = mCanvas;
					canvas.drawBitmap(mBitmap, 0, 0, null);
				}
			}
		}
	}
}
