// Rc6Cipher - the RC6 encryption method
//
// This was one of the finalists for the Advanced Encryption Standard.
// Most of the algorithm is Copyright (C) 1998 RSA Data Security, Inc.
// A few modifications to the algorithm, and the surrounding glue, are:
//
// Copyright (C)1996,2000 by Jef Poskanzer <jef@mail.acme.com>.
// All rights reserved.
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

package Acme.Crypto;

/// The RC6 encryption method.
// <P>
// This was one of the finalists for the Advanced Encryption Standard.
// <P>
// <A HREF="/resources/classes/Acme/Crypto/Rc6Cipher.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see EncryptedOutputStream
// @see EncryptedInputStream

public class Rc6Cipher extends BlockCipher {
	
	// Constructor, string key.
	public Rc6Cipher(String keyStr) {
		super(0, 16);
		setKey(keyStr);
	}
	
	// Constructor, byte-array key.
	public Rc6Cipher(byte[] key) {
		super(0, 16);
		setKey(key);
	}
	
	// Key routines.
	
	private int S0, S1, S42, S43;
	private int[] Sp0 = new int[5];
	private int[] Sp1 = new int[5];
	private int[] Sp2 = new int[5];
	private int[] Sp3 = new int[5];
	private int[] Sp4 = new int[5];
	private int[] Sp5 = new int[5];
	private int[] Sp6 = new int[5];
	private int[] Sp7 = new int[5];
	
	/// Set the key.
	public void setKey(byte[] key) {
		int keyLength = key.length;
		if(keyLength > 255)
			keyLength = 255;
		
		// A convenient table of values for f(x) = (x+1) mod 36.
		final byte[] nextScheduleDwordIndex = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 0 };
		
		// Compute how many dwords of key we need.
		int c = ((keyLength + 3) >> 2) + ((keyLength - 1) >>> 31);
		
		int L[] = new int[c];
		short nextKeyDwordIndex[] = new short[c];
		
		// Load all the key dwords that get a full 4 bytes of key. At
		// the same time, set up a table of values for g(x) = (x+1) mod c.
		int almostEnd = keyLength & 0xfffffffc;
		for(int i = almostEnd - 4; i >= 0; i -= 4) {
			int index = i >> 2;
			L[index] = ((((int) key[i]) & 0xff)) + ((((int) key[i + 1]) & 0xff) << 8) + ((((int) key[i + 2]) & 0xff) << 16) + ((((int) key[i + 3]) & 0xff) << 24);
			nextKeyDwordIndex[index] = (short) (index + 1);
		}
		
		// Now fill up last dword.
		int remainder = keyLength & 3;
		if(remainder > 0) {
			int lastDword = ((int) key[almostEnd]) & 0xff;
			if(remainder > 1) {
				lastDword |= (((int) key[almostEnd + 1]) & 0xff) << 8;
				if(remainder > 2)
					lastDword |= (((int) key[almostEnd + 2]) & 0xff) << 16;
			}
			
			L[c - 1] = lastDword;
		}
		
		// Finish setting up table by setting last value.
		nextKeyDwordIndex[c - 1] = 0;
		
		// Key schedule table. This starts out having the values generated
		// from the magic constants P32 and Q32.
		int[] S = { 0xb7e15163, 0x5618cb1c,  // Pseudo-round #0
				0xf45044d5, 0x9287be8e,  // Round #1
				0x30bf3847, 0xcef6b200,  // Round #2
				0x6d2e2bb9, 0x0b65a572,  // Round #3
				0xa99d1f2b, 0x47d498e4,  // Round #4
				0xe60c129d, 0x84438c56,  // Round #5
				0x227b060f, 0xc0b27fc8,  // Round #6
				0x5ee9f981, 0xfd21733a,  // Round #7
				0x9b58ecf3, 0x399066ac,  // Round #8
				0xd7c7e065, 0x75ff5a1e,  // Round #9
				0x1436d3d7, 0xb26e4d90,  // Round #10
				0x50a5c749, 0xeedd4102,  // Round #11
				0x8d14babb, 0x2b4c3474,  // Round #12
				0xc983ae2d, 0x67bb27e6,  // Round #13
				0x05f2a19f, 0xa42a1b58,  // Round #14
				0x42619511, 0xe0990eca,  // Round #15
				0x7ed08883, 0x1d08023c,  // Round #16
				0xbb3f7bf5, 0x5976f5ae,  // Round #17
				0xf7ae6f67, 0x95e5e920,  // Round #18
				0x341d62d9, 0xd254dc92,  // Round #19
				0x708c564b, 0x0ec3d004,  // Round #20
				0xacfb49bd, 0x4b32c376   // Pseudo-round #21
		};
		
		int i = 0;
		int j = 0;
		int A = 0;
		int B = 0;
		
		int sum;
		
		// Now we actually mix the key into the key schedule array.
		for(int counter = ((c <= 44) ? 44 : c); --counter >= 0;) {
			// A = S[i] = (S[i]+A+B) rotated left by 3.
			A += S[i] + B;
			S[i] = A = (A << 3) | (A >>> 29);
			
			// B = L[j] = (L[j]+A+B) rotated left by (A+B).
			sum = A + B;
			B = sum + L[j];
			L[j] = B = (B << sum) | (B >>> (-sum));
			
			// i = (i+1) mod t and j = (j+1) mod c.
			i = nextScheduleDwordIndex[i];
			j = nextKeyDwordIndex[j];
			
			// A = S[i] = (S[i]+A+B) rotated left by 3.
			A += S[i] + B;
			S[i] = A = (A << 3) | (A >>> 29);
			
			// B = L[j] = (L[j]+A+B) rotated left by (A+B).
			sum = A + B;
			B = sum + L[j];
			L[j] = B = (B << sum) | (B >>> (-sum));
			
			// i = (i+1) mod t and j = (j+1) mod c.
			i = nextScheduleDwordIndex[i];
			j = nextKeyDwordIndex[j];
			
			// A = S[i] = (S[i]+A+B) rotated left by 3.
			A += S[i] + B;
			S[i] = A = (A << 3) | (A >>> 29);
			
			// B = L[j] = (L[j]+A+B) rotated left by (A+B).
			sum = A + B;
			B = sum + L[j];
			L[j] = B = (B << sum) | (B >>> (-sum));
			
			// i = (i+1) mod t and j = (j+1) mod c.
			i = nextScheduleDwordIndex[i];
			j = nextKeyDwordIndex[j];
		}
		
		S0 = S[0];
		S1 = S[1];
		
		int count = 0;
		int offset = 1;
		do {
			Sp0[count] = S[++offset];
			Sp1[count] = S[++offset];
			Sp2[count] = S[++offset];
			Sp3[count] = S[++offset];
			Sp4[count] = S[++offset];
			Sp5[count] = S[++offset];
			Sp6[count] = S[++offset];
			Sp7[count] = S[++offset];
			++count;
		} while(offset <= 33);
		
		S42 = S[42];
		S43 = S[43];
	}
	
	// Encryption routines.
	
	private int[] tempInts = new int[4];
	
	/// Encrypt a block.
	public void encrypt(byte[] clearText, int clearOff, byte[] cipherText, int cipherOff) {
		// Get A, B, C, D.
		squashBytesToIntsLittle(clearText, clearOff, tempInts, 0, 4);
		int A = tempInts[0];
		int B = tempInts[1];
		int C = tempInts[2];
		int D = tempInts[3];
		
		// Do pseudo-round #0.
		B += S0;
		D += S1;
		
		int t, u;
		
		for(int i = 0; i < 5; ++i) {
			// Round #1, #5, #9, #13, #17.
			t = B * ((B << 1) + 1);
			u = D * ((D << 1) + 1);
			
			t = (t << 5) | (t >>> 27);
			u = (u << 5) | (u >>> 27);
			
			A ^= t;
			C ^= u;
			A = ((A << u) | (A >>> -u)) + Sp0[i];
			C = ((C << t) | (C >>> -t)) + Sp1[i];
			
			// Round #2, #6, #10, #14, #18.
			t = C * ((C << 1) + 1);
			u = A * ((A << 1) + 1);
			
			t = (t << 5) | (t >>> 27);
			u = (u << 5) | (u >>> 27);
			
			B ^= t;
			D ^= u;
			B = ((B << u) | (B >>> -u)) + Sp2[i];
			D = ((D << t) | (D >>> -t)) + Sp3[i];
			
			// Round #3, #7, #11, #15, #19.
			t = D * ((D << 1) + 1);
			u = B * ((B << 1) + 1);
			
			t = (t << 5) | (t >>> 27);
			u = (u << 5) | (u >>> 27);
			
			C ^= t;
			A ^= u;
			C = ((C << u) | (C >>> -u)) + Sp4[i];
			A = ((A << t) | (A >>> -t)) + Sp5[i];
			
			// Round #4, #8, #12, #16, #20.
			t = A * ((A << 1) + 1);
			u = C * ((C << 1) + 1);
			
			t = (t << 5) | (t >>> 27);
			u = (u << 5) | (u >>> 27);
			
			D ^= t;
			B ^= u;
			D = ((D << u) | (D >>> -u)) + Sp6[i];
			B = ((B << t) | (B >>> -t)) + Sp7[i];
		}
		
		// Do pseudo-round #21.
		A += S42;
		C += S43;
		
		// Return cipher text.
		tempInts[0] = A;
		tempInts[1] = B;
		tempInts[2] = C;
		tempInts[3] = D;
		spreadIntsToBytesLittle(tempInts, 0, cipherText, cipherOff, 4);
	}
	
	/// Decrypt a block.
	public void decrypt(byte[] cipherText, int cipherOff, byte[] clearText, int clearOff) {
		// Get A, B, C, D.
		squashBytesToIntsLittle(cipherText, cipherOff, tempInts, 0, 4);
		int A = tempInts[0];
		int B = tempInts[1];
		int C = tempInts[2];
		int D = tempInts[3];
		
		// Undo pseudo-round #21.
		C -= S43;
		A -= S42;
		
		int t, u;
		
		for(int i = 4; i >= 0; --i) {
			// Round #20, #16, #12, #8, #4.
			t = A * ((A << 1) + 1);
			u = C * ((C << 1) + 1);
			t = (t << 5) | (t >>> 27);
			u = (u << 5) | (u >>> 27);
			
			B -= Sp7[i];
			D -= Sp6[i];
			B = ((B >>> t) | (B << -t)) ^ u;
			D = ((D >>> u) | (D << -u)) ^ t;
			
			// Round #19, #15, #11, #7, #3.
			t = D * ((D << 1) + 1);
			u = B * ((B << 1) + 1);
			t = (t << 5) | (t >>> 27);
			u = (u << 5) | (u >>> 27);
			
			A -= Sp5[i];
			C -= Sp4[i];
			A = ((A >>> t) | (A << -t)) ^ u;
			C = ((C >>> u) | (C << -u)) ^ t;
			
			// Round #18, #14, #10, #6, #2.
			t = C * ((C << 1) + 1);
			u = A * ((A << 1) + 1);
			t = (t << 5) | (t >>> 27);
			u = (u << 5) | (u >>> 27);
			
			D -= Sp3[i];
			B -= Sp2[i];
			D = ((D >>> t) | (D << -t)) ^ u;
			B = ((B >>> u) | (B << -u)) ^ t;
			
			// Round #17, #13, #9, #5, #1.
			t = B * ((B << 1) + 1);
			u = D * ((D << 1) + 1);
			t = (t << 5) | (t >>> 27);
			u = (u << 5) | (u >>> 27);
			
			C -= Sp1[i];
			A -= Sp0[i];
			C = ((C >>> t) | (C << -t)) ^ u;
			A = ((A >>> u) | (A << -u)) ^ t;
		}
		
		// Undo pseudo-round #0.
		D -= S1;
		B -= S0;
		
		// Return clear text.
		tempInts[0] = A;
		tempInts[1] = B;
		tempInts[2] = C;
		tempInts[3] = D;
		spreadIntsToBytesLittle(tempInts, 0, clearText, clearOff, 4);
	}
	
	/// Test routine.
	public static void main(String[] args) {
		byte[] cipherText = new byte[16];
		byte[] decipherText = new byte[16];
		
		BlockCipher rc6a = new Rc6Cipher("0123456789");
		byte[] clearText1 = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
		System.out.println("cleartext: " + toStringBlock(clearText1));
		rc6a.encrypt(clearText1, cipherText);
		System.out.println("encrypted: " + toStringBlock(cipherText));
		rc6a.decrypt(cipherText, decipherText);
		System.out.println("decrypted: " + toStringBlock(decipherText));
		
		System.out.println();
		
		BlockCipher rc6b = new Rc6Cipher("abcdefghijklmnopqrstuvwxyz");
		byte[] clearText2 = { (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b, (byte) 0x0c, (byte) 0x0d, (byte) 0x0e, (byte) 0x0f };
		System.out.println("cleartext: " + toStringBlock(clearText2));
		rc6b.encrypt(clearText2, cipherText);
		System.out.println("encrypted: " + toStringBlock(cipherText));
		rc6b.decrypt(cipherText, decipherText);
		System.out.println("decrypted: " + toStringBlock(decipherText));
	}
	
}
