// SynthAudioClip - a synthesized audio clip
//
// Copyright (C) 1996 by Jef Poskanzer <jef@mail.acme.com>. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

package Acme;

import java.applet.AudioClip;
import java.io.IOException;
import java.io.InputStream;

import sun.audio.AudioData;
import sun.audio.AudioDataStream;
import sun.audio.AudioPlayer;
import sun.audio.ContinuousAudioDataStream;

/// A synthesized audio clip.
// <P>
// Generates a synthetic audio tone of a specified frequency and duration.
// <P>
// <A HREF="/resources/classes/Acme/SynthAudioClip.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class SynthAudioClip implements AudioClip {
	private static final long SAMPLES_PER_SEC = 8000;
	
	AudioData data;
	InputStream stream;
	
	/// Constructor.
	// Generates a single tone.
	// @param hz the frequency of the tone
	// @param millis the duration of the tone, in milliseconds
	// @param amplitude the loudness of the tone, from 0 to 32767
	public SynthAudioClip(int hz, long millis, double amplitude) {
		int samples = (int) (millis * SAMPLES_PER_SEC / 1000);
		byte[] buf = new byte[samples];
		for(int i = 0; i < buf.length; ++i)
			buf[i] = toUlaw((int) (amplitude * Math.sin(2.0D * Math.PI * i * hz / SAMPLES_PER_SEC)));
		data = new AudioData(buf);
	}
	
	/// Constructor.
	// Generates a single tone, with default loudness.
	// @param hz the frequency of the tone
	// @param millis the duration of the tone, in milliseconds
	public SynthAudioClip(int hz, long millis) {
		this(hz, millis, 16384.0D);
	}
	
	/*****
	 * // This is not currently used here, so we comment it out because
	 * // it compiles to a whole lot of bytecodes.
	 * 
	 * private static final int[] ulawTab = {
	 * -32124, -31100, -30076, -29052, -28028, -27004, -25980, -24956,
	 * -23932, -22908, -21884, -20860, -19836, -18812, -17788, -16764,
	 * -15996, -15484, -14972, -14460, -13948, -13436, -12924, -12412,
	 * -11900, -11388, -10876, -10364, -9852, -9340, -8828, -8316,
	 * -7932, -7676, -7420, -7164, -6908, -6652, -6396, -6140,
	 * -5884, -5628, -5372, -5116, -4860, -4604, -4348, -4092,
	 * -3900, -3772, -3644, -3516, -3388, -3260, -3132, -3004,
	 * -2876, -2748, -2620, -2492, -2364, -2236, -2108, -1980,
	 * -1884, -1820, -1756, -1692, -1628, -1564, -1500, -1436,
	 * -1372, -1308, -1244, -1180, -1116, -1052, -988, -924,
	 * -876, -844, -812, -780, -748, -716, -684, -652,
	 * -620, -588, -556, -524, -492, -460, -428, -396,
	 * -372, -356, -340, -324, -308, -292, -276, -260,
	 * -244, -228, -212, -196, -180, -164, -148, -132,
	 * -120, -112, -104, -96, -88, -80, -72, -64,
	 * -56, -48, -40, -32, -24, -16, -8, 0,
	 * 32124, 31100, 30076, 29052, 28028, 27004, 25980, 24956,
	 * 23932, 22908, 21884, 20860, 19836, 18812, 17788, 16764,
	 * 15996, 15484, 14972, 14460, 13948, 13436, 12924, 12412,
	 * 11900, 11388, 10876, 10364, 9852, 9340, 8828, 8316,
	 * 7932, 7676, 7420, 7164, 6908, 6652, 6396, 6140,
	 * 5884, 5628, 5372, 5116, 4860, 4604, 4348, 4092,
	 * 3900, 3772, 3644, 3516, 3388, 3260, 3132, 3004,
	 * 2876, 2748, 2620, 2492, 2364, 2236, 2108, 1980,
	 * 1884, 1820, 1756, 1692, 1628, 1564, 1500, 1436,
	 * 1372, 1308, 1244, 1180, 1116, 1052, 988, 924,
	 * 876, 844, 812, 780, 748, 716, 684, 652,
	 * 620, 588, 556, 524, 492, 460, 428, 396,
	 * 372, 356, 340, 324, 308, 292, 276, 260,
	 * 244, 228, 212, 196, 180, 164, 148, 132,
	 * 120, 112, 104, 96, 88, 80, 72, 64,
	 * 56, 48, 40, 32, 24, 16, 8, 0
	 * };
	 * 
	 * private static int toLinear( byte ulaw )
	 * {
	 * return ulawTab[ulaw & 0xff];
	 * }
	 *****/
	
	private static final int[] expLut = { 0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,
			6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
			7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7 };
	private static final int CLIP = 32635;
	private static final int BIAS = 0x84;
	
	private static byte toUlaw(int linear) {
		int sign, exponent, mantissa;
		byte ulaw;
		
		// Get the sample into sign-magnitude.
		if(linear >= 0)
			sign = 0;
		else {
			sign = 0x80;
			linear = -linear;
		}
		if(linear > CLIP)
			linear = CLIP;	// clip the magnitude
			
		// Convert from 16 bit linear to ulaw.
		linear = linear + BIAS;
		exponent = expLut[(linear >> 7) & 0xFF];
		mantissa = (linear >> (exponent + 3)) & 0x0F;
		ulaw = (byte) (~(sign | (exponent << 4) | mantissa));
		return ulaw;
	}
	
	public synchronized void play() {
		stop();
		if(data != null) {
			stream = new AudioDataStream(data);
			AudioPlayer.player.start(stream);
		}
	}
	
	public synchronized void loop() {
		stop();
		if(data != null) {
			stream = new ContinuousAudioDataStream(data);
			AudioPlayer.player.start(stream);
		}
	}
	
	public synchronized void stop() {
		if(stream != null) {
			AudioPlayer.player.stop(stream);
			try {
				stream.close();
			} catch(IOException iignore) {
			}
			stream = null;
		}
	}
	
}
