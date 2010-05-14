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
import java.util.ArrayList;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Environment;

/**
 * @author hugosg
 * 
 * This is the most important file of the project.
 * Here is where the sound analysis is generated.
 * It was elaborated by extending the Player class.
 * Instead of setting up and audioPlayer we get the values of
 * the file, do an FFT, and run all the different analyzis steps
 *
 */
public class Analyzer extends AsyncTask<Activity, ArrayList<Float>, Void>{

	AnalyzerListener al;

	/**
	 * @param al
	 * 
	 * We assume only one listener
	 */
	public void addListener(AnalyzerListener al){
		this.al = al;
	}

	/* (non-Javadoc)
	 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
	 * 
	 * Remember, this will be called on each call of publishProgress inside the doInBackground method
	 */
	@Override
	protected void onProgressUpdate(ArrayList<Float>... list) {
		//super.onProgressUpdate(values);
		synchronized(this){
			al.analyzePart(list[0]);
		}
	}

	@Override
	protected void onPostExecute(Void result) {
		//super.onPostExecute(result);
		synchronized(this){
			al.analyzeDone();
		}
	}

	@Override
	protected Void doInBackground(Activity... params) {
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
				//  music[musicLength-1-i] = dis.readShort();
				i++;
			}

			// Close the input streams.
			dis.close();
		} catch (Throwable t) {}
		
		
		/*
		*STARTING OF THE ANALYZIS
		*
		*NOTE: after each step of the analysis, the values are passed -thru the publishProgress which in turn pass the values
		*to the listener using a simple protocol where the first value of the list is used as a ID of the type of analysis
		*/
		
		int fft_size = 256; //remember power of two!!!
		int slope = fft_size / 2; //how much do we move the window
		int frames = (int)(musicLength / slope) - 1;//getting number of frames, discard last part if minor than size. Not ideal

		//wave form
		for(int f = 0; f < frames; f++){
			//copy slide of signal only one sample per frame
			//normalized to -1, 1
			ArrayList<Float> audioData = new ArrayList<Float>();
			audioData.add(new Float(1)); //heather ONE for audio data
			audioData.add(new Float(frames)); //keep track of how many frames
			audioData.add(new Float(f)); //counter of current frame
			//in the nest line the short.Max_value + 1 as a way to get the max positive value possible in a Short
			audioData.add(new Float(music[f * slope] * 1.0 / (Short.MAX_VALUE + 1))); //sending the value in a range of -1.0 to 1.0
			publishProgress(audioData);
		}

		//FFT
		FFT fft = new FFT(fft_size);
		double[] window = fft.getWindow();
		double[] re = new double[fft_size]; //array for the real part
		double[] im = new double[fft_size];	//array for the imaginary part. NOT USED FOR NOW
		
		float[] centroids = new float[frames]; //array for the centroids of the entire file
		float[] energies = new float[frames]; //array for the energies of the entire file
		
		for(int f = 0; f < frames; f++){
			//copy slide of signal to re normalized to -1 to 1
			//fill im with zeros
			//multiply signal for window
			for(int i = 0; i < fft_size; i++){
				re[i] = music[(f * slope) + i] * 1.0 / (Short.MAX_VALUE + 1);
				re[i] *= window[i];
				im[i] = 0;
			}
			fft.fft(re, im); //call of the magic function! :-) Thanks to the MeapSoft guys!

			//copy fft data to the array and send to other thread. Remember, Only half of the array is necessary
			ArrayList<Float> fftData = new ArrayList<Float>();
			fftData.add(new Float(2)); //heather TWO for fft
			fftData.add(new Float(frames)); //keep track of how many frames
			fftData.add(new Float(fft_size / 2)); //size of fft_window
			fftData.add(new Float(f)); //counter of current frame
			
			//these two values are used for getting the Spectral Centroid
			//http://en.wikipedia.org/wiki/Spectral_centroid
			double centroidFnXn = 0;
			double centroidXn = 0;
			
			double energy = 0;
			
			//after heuristic observation it seems the max that the fft can give is around 70
			//this for loop is where we traverse all the bands of the FFT -it can be expensive
			for(int i = 0; i < fft_size / 2; i++){
				fftData.add((float)Math.sqrt(Math.abs(re[i]))); //remove negatives
				
				//for the centroid we keep value of the 
				centroidFnXn += i * Math.abs(re[i]); //keeping track of the magnitude of each bin multipied by it's index, it does the trick!
				centroidXn += Math.abs(re[i]); //keep track of the total value of the addition of the magnitudes of all the bins
				
				//For the energy we just add all the values of all the bands
				energy += Math.sqrt(Math.abs(re[i])); //use of the Math.sqrt as a way to compress the values into a smaller range
			}
			
			centroids[f] = (float)(centroidFnXn / centroidXn);

			energies[f] = (float)(energy);
			
			publishProgress(fftData);
		}
		
		//Spectral Centroid
		double average_sc = 0; //for keeping track of the overal Centroid value
		for(int f = 0; f < frames; f++){
			ArrayList<Float> scData = new ArrayList<Float>();
			scData.add(new Float(3)); //heather THREE for spectral centroid
			scData.add(new Float(frames)); //keep track of how many frames
			scData.add(new Float(f)); //counter of current frame
			scData.add(centroids[f]);
			average_sc += centroids[f]; // we add all the values into the average
			publishProgress(scData);
		}
		
		//Spectral Centroid variance (derivative of Spectral Centroid over time)
		//one value less because its the difference between two points
		float[] sc_derivative = new float[frames - 1];
		for(int f = 0; f < sc_derivative.length; f++){ //getting variance
			sc_derivative[f] = centroids[f] - centroids[f + 1];
		}
		float max_scd = Float.MIN_VALUE; //setting just a very low floor!
		for(int f = 0; f < sc_derivative.length; f++){ //getting the major number in positive value for NORMALIZATION
			if(Math.abs(sc_derivative[f]) > max_scd) max_scd = Math.abs(sc_derivative[f]);
		}
		for(int f = 0; f < sc_derivative.length; f++){ //normalize values to 1.0
			sc_derivative[f] = sc_derivative[f] / max_scd;
		}
		for(int f = 0; f < sc_derivative.length; f++){
			ArrayList<Float> scVarianceData = new ArrayList<Float>();
			scVarianceData.add(new Float(35)); //heather THREE FIVE for spectral centroid variance
			scVarianceData.add(new Float(sc_derivative.length)); //keep track of how many frames
			scVarianceData.add(new Float(f)); //counter of current frame
			scVarianceData.add(sc_derivative[f]); //putting the value between -1.0 and 1.0
			publishProgress(scVarianceData); //sending the value
		}
		
		//eneergy defined as the addition of the 
		double average_en = 0; //for keeping track of the overall value
		for(int f = 0; f < frames; f++){
			ArrayList<Float> enData = new ArrayList<Float>();
			enData.add(new Float(4)); //heather FOUR for spectral centroid
			enData.add(new Float(frames)); //keep track of how many frames
			enData.add(new Float(f)); //counter of current frame
			enData.add(energies[f]); //putting the actual value
			average_en += energies[f]; //adding all the values together
			publishProgress(enData); //sending the value
		}
		
		//Energy variance (derivative of energy over time)
		float[] en_derivative = new float[frames - 1];
		for(int f = 0; f < en_derivative.length; f++){ //getting variance
			en_derivative[f] = energies[f] - energies[f + 1];
		}
		float max_end = Float.MIN_VALUE;
		for(int f = 0; f < en_derivative.length; f++){ //getting the major number in positive value for NORMALIZATION
			if(Math.abs(en_derivative[f]) > max_end) max_end = Math.abs(en_derivative[f]);
		}
		for(int f = 0; f < en_derivative.length; f++){ //normalize values to 1.0
			en_derivative[f] = en_derivative[f] / max_end;
		}
		for(int f = 0; f < en_derivative.length; f++){
			ArrayList<Float> enVarianceData = new ArrayList<Float>();
			enVarianceData.add(new Float(45)); //heather THREE FIVE for spectral centroid variance
			enVarianceData.add(new Float(en_derivative.length)); //keep track of how many frames
			enVarianceData.add(new Float(f)); //counter of current frame
			enVarianceData.add(en_derivative[f]); //add the value between -1.0 and 1.0
			publishProgress(enVarianceData); //send the value
		}
		
		//Zero Crossing. Raw estimation of frequency tendency and level of noise
		float[] zc = new float[frames];
		double average_zc = 0;
		for(int f = 0; f < frames; f++){
			float zc_counter = 0;
			short previous_value = 0;
			for(int i = 0; i < fft_size; i++){ //running all the bands again. Why? It could be merged with the FFT main analyzis
				short currentValue = music[(f * slope) + i];
				//count only then change of from symbol
				if((currentValue >= 0.0 && previous_value < 0.0) ||
				   (currentValue < 0.0 && previous_value >= 0.0)) zc_counter++;
				previous_value = currentValue;
			}
			zc_counter = zc_counter / fft_size; //convert a value to range 0 to 1.0
			zc[f] = zc_counter; // add to an array with all the values
			average_zc += zc_counter; //adding all the values to the average for later division
			ArrayList<Float> zcData = new ArrayList<Float>();
			zcData.add(new Float(5)); //heather FIVE for zero crossing
			zcData.add(new Float(frames)); //keep track of how many frames
			zcData.add(new Float(f)); //counter of current frame
			zcData.add((float)zc_counter); //add the value
			publishProgress(zcData);
		}
		
		//Zero crossing variance (Derivative of ZC over time)
		float[] zc_derivative = new float[frames - 1];
		for(int f = 0; f < zc_derivative.length; f++){ //getting variance
			zc_derivative[f] = zc[f] - zc[f + 1];
		}
		float max_zcd = Float.MIN_VALUE;
		for(int f = 0; f < zc_derivative.length; f++){ //getting the major number in positive value for normalization
			if(Math.abs(zc_derivative[f]) > max_zcd) max_zcd = Math.abs(zc_derivative[f]);
		}
		for(int f = 0; f < zc_derivative.length; f++){ //normalize values to 1.0
			zc_derivative[f] = zc_derivative[f] / max_zcd;
		}
		for(int f = 0; f < zc_derivative.length; f++){
			ArrayList<Float> zcVarianceData = new ArrayList<Float>();
			zcVarianceData.add(new Float(55)); //heather THREE FIVE for spectral centroid variance
			zcVarianceData.add(new Float(en_derivative.length)); //keep track of how many frames
			zcVarianceData.add(new Float(f)); //counter of current frame
			zcVarianceData.add(zc_derivative[f]); //add the value between -1.0 and 1.0
			publishProgress(zcVarianceData);//send the value
		}
		
		//HEAD DATA to be filled during process.... not send until the end with all the information
		ArrayList<Float> headData = new ArrayList<Float>();
		headData.add(new Float(0)); //heather ZERO for head data, music length,
		headData.add(new Float(musicLength)); //adding the number of samples
		average_sc = average_sc / frames; // dividing for getting average
		headData.add((float)average_sc); //adding Spectral Centroid
		average_en = average_en / frames; // dividing for getting average
		headData.add((float)average_en); //adding Energy
		average_zc = average_zc / frames; // dividing for getting average
		headData.add((float)average_zc); //adding Zero Crossing
		publishProgress(headData);
		
		return null;
	}
}
