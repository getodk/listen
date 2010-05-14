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

/**
 * @author hugosg
 * 
 * This listener is used to send the values and the Done state from the Analyzer
 * to the AnalyzerActivity
 *
 */
public interface AnalyzerListener {
	
	/**
	 * @param list
	 * 
	 * Send of the values, please see the Analyzer code for proper protocol
	 */
	void analyzePart(ArrayList<Float> list);
	/**
	 * Send of the Done state
	 */
	void analyzeDone();

}
