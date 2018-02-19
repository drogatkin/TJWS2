// AesCipher - the AES encryption method
//
// Before being selected as the Advanced Encryption Standard this was
// known as Rijndael, and was designed by Joan Daemen and Vincent Rijmen.
// Most of the algorithm code is copyright by them. A few modifications
// to the algorithm, and the surrounding glue, are:
//
// Copyright (C) 1996,2000 by Jef Poskanzer <jef@mail.acme.com>.
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

/// The AES encryption method.
// <P>
// Before being selected as the Advanced Encryption Standard this was
// known as Rijndael, and was designed by Joan Daemen and Vincent Rijmen.
// <P>
// <A HREF="/resources/classes/Acme/Crypto/AesCipher.java">Fetch the
/// software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>
// <P>
// @see EncryptedOutputStream
// @see EncryptedInputStream

public class AesCipher extends BlockCipher {
	
	// Constants, variables, and auxillary routines.
	
	// Key size in bytes. Valid values are 16, 24, and 32.
	public static final int KEY_SIZE = 16;
	
	// Block size in bytes. Valid values are 16, 24, and 32.
	public static final int BLOCK_SIZE = 16;
	
	private static final int[] alog = new int[256];
	private static final int[] log = new int[256];
	
	private static final byte[] S = new byte[256];
	private static final byte[] Si = new byte[256];
	private static final int[] T1 = new int[256];
	private static final int[] T2 = new int[256];
	private static final int[] T3 = new int[256];
	private static final int[] T4 = new int[256];
	private static final int[] T5 = new int[256];
	private static final int[] T6 = new int[256];
	private static final int[] T7 = new int[256];
	private static final int[] T8 = new int[256];
	private static final int[] U1 = new int[256];
	private static final int[] U2 = new int[256];
	private static final int[] U3 = new int[256];
	private static final int[] U4 = new int[256];
	private static final byte[] rcon = new byte[30];
	
	static private final int[][][] shifts = new int[][][] { { { 0, 0 }, { 1, 3 }, { 2, 2 }, { 3, 1 } }, { { 0, 0 }, { 1, 5 }, { 2, 4 }, { 3, 3 } }, { { 0, 0 }, { 1, 7 }, { 3, 5 }, { 4, 4 } } };
	
	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	
	// Static initializer - to intialise S-boxes and T-boxes
	static {
		int ROOT = 0x11B;
		int i, j = 0;
		
		// Produce log and alog tables, needed for multiplying in the
		// field GF(2^m) (generator = 3).
		alog[0] = 1;
		for(i = 1; i < 256; ++i) {
			j = (alog[i - 1] << 1) ^ alog[i - 1];
			if((j & 0x100) != 0)
				j ^= ROOT;
			alog[i] = j;
		}
		for(i = 1; i < 255; ++i)
			log[alog[i]] = i;
		byte[][] A = new byte[][] { { 1, 1, 1, 1, 1, 0, 0, 0 }, { 0, 1, 1, 1, 1, 1, 0, 0 }, { 0, 0, 1, 1, 1, 1, 1, 0 }, { 0, 0, 0, 1, 1, 1, 1, 1 }, { 1, 0, 0, 0, 1, 1, 1, 1 }, { 1, 1, 0, 0, 0, 1, 1, 1 }, { 1, 1, 1, 0, 0, 0, 1, 1 }, { 1, 1, 1, 1, 0, 0, 0, 1 } };
		byte[] B = new byte[] { 0, 1, 1, 0, 0, 0, 1, 1 };
		
		// Substitution box based on F^{-1}(x).
		int t;
		byte[][] box = new byte[256][8];
		box[1][7] = 1;
		for(i = 2; i < 256; ++i) {
			j = alog[255 - log[i]];
			for(t = 0; t < 8; ++t)
				box[i][t] = (byte) ((j >>> (7 - t)) & 0x01);
		}
		// Affine transform: box[i] <- B + A*box[i].
		byte[][] cox = new byte[256][8];
		for(i = 0; i < 256; ++i)
			for(t = 0; t < 8; ++t) {
				cox[i][t] = B[t];
				for(j = 0; j < 8; ++j)
					cox[i][t] ^= A[t][j] * box[i][j];
			}
		// S-boxes and inverse S-boxes.
		for(i = 0; i < 256; ++i) {
			S[i] = (byte) (cox[i][0] << 7);
			for(t = 1; t < 8; ++t)
				S[i] ^= cox[i][t] << (7 - t);
			Si[S[i] & 0xFF] = (byte) i;
		}
		// T-boxes.
		byte[][] G = new byte[][] { { 2, 1, 1, 3 }, { 3, 2, 1, 1 }, { 1, 3, 2, 1 }, { 1, 1, 3, 2 } };
		byte[][] AA = new byte[4][8];
		for(i = 0; i < 4; ++i) {
			for(j = 0; j < 4; ++j)
				AA[i][j] = G[i][j];
			AA[i][i + 4] = 1;
		}
		byte pivot, tmp;
		byte[][] iG = new byte[4][4];
		for(i = 0; i < 4; ++i) {
			pivot = AA[i][i];
			if(pivot == 0) {
				t = i + 1;
				while((AA[t][i] == 0) && (t < 4))
					++t;
				if(t == 4)
					throw new RuntimeException("G matrix is not invertible");
				else {
					for(j = 0; j < 8; ++j) {
						tmp = AA[i][j];
						AA[i][j] = AA[t][j];
						AA[t][j] = (byte) tmp;
					}
					pivot = AA[i][i];
				}
			}
			for(j = 0; j < 8; ++j)
				if(AA[i][j] != 0)
					AA[i][j] = (byte) alog[(255 + log[AA[i][j] & 0xFF] - log[pivot & 0xFF]) % 255];
			for(t = 0; t < 4; ++t)
				if(i != t) {
					for(j = i + 1; j < 8; ++j)
						AA[t][j] ^= mul(AA[i][j], AA[t][i]);
					AA[t][i] = 0;
				}
		}
		for(i = 0; i < 4; ++i)
			for(j = 0; j < 4; ++j)
				iG[i][j] = AA[i][j + 4];
			
		int s;
		for(t = 0; t < 256; ++t) {
			s = S[t];
			T1[t] = mul4(s, G[0]);
			T2[t] = mul4(s, G[1]);
			T3[t] = mul4(s, G[2]);
			T4[t] = mul4(s, G[3]);
			
			s = Si[t];
			T5[t] = mul4(s, iG[0]);
			T6[t] = mul4(s, iG[1]);
			T7[t] = mul4(s, iG[2]);
			T8[t] = mul4(s, iG[3]);
			
			U1[t] = mul4(t, iG[0]);
			U2[t] = mul4(t, iG[1]);
			U3[t] = mul4(t, iG[2]);
			U4[t] = mul4(t, iG[3]);
		}
		// Round constants.
		rcon[0] = 1;
		int r = 1;
		for(t = 1; t < 30;)
			rcon[t++] = (byte) (r = mul(2, r));
	}
	
	/// Multiply two elements of GF(2^m).
	static final int mul(int a, int b) {
		return (a != 0 && b != 0) ? alog[(log[a & 0xFF] + log[b & 0xFF]) % 255] : 0;
	}
	
	/// Convenience method used in generating Transposition boxes.
	static final int mul4(int a, byte[] b) {
		if(a == 0)
			return 0;
		a = log[a & 0xFF];
		int a0 = (b[0] != 0) ? alog[(a + log[b[0] & 0xFF]) % 255] & 0xFF : 0;
		int a1 = (b[1] != 0) ? alog[(a + log[b[1] & 0xFF]) % 255] & 0xFF : 0;
		int a2 = (b[2] != 0) ? alog[(a + log[b[2] & 0xFF]) % 255] & 0xFF : 0;
		int a3 = (b[3] != 0) ? alog[(a + log[b[3] & 0xFF]) % 255] & 0xFF : 0;
		return a0 << 24 | a1 << 16 | a2 << 8 | a3;
	}
	
	/// Return the number of rounds for a given Rijndael's key and block sizes.
	// @param keySize The size of the user key material in bytes.
	// @param blockSize The desired block size in bytes.
	// @return The number of rounds for a given Rijndael's key and block sizes.
	public static int getRounds(int keySize, int blockSize) {
		switch(keySize) {
			case 16:
				return blockSize == 16 ? 10 : (blockSize == 24 ? 12 : 14);
			case 24:
				return blockSize != 32 ? 12 : 14;
			default: // 32 bytes = 256 bits
				return 14;
		}
	}
	
	// Constructors.
	
	// Constructor, string key.
	public AesCipher(String keyStr) {
		super(KEY_SIZE, BLOCK_SIZE);
		setKey(keyStr);
	}
	
	// Constructor, byte-array key.
	public AesCipher(byte[] key) {
		super(KEY_SIZE, BLOCK_SIZE);
		setKey(key);
	}
	
	// Key routines.
	
	private int ROUNDS = getRounds(KEY_SIZE, BLOCK_SIZE);
	private int BC = BLOCK_SIZE / 4;
	private int[][] Ke = new int[ROUNDS + 1][BC];  // encryption round keys
	private int[][] Kd = new int[ROUNDS + 1][BC];  // decryption round keys
	
	/// Set the key.
	public void setKey(byte[] key) {
		if(key.length != KEY_SIZE)
			throw new RuntimeException("Incorrect key length");
		int ROUND_KEY_COUNT = (ROUNDS + 1) * BC;
		int KC = KEY_SIZE / 4;
		int[] tk = new int[KC];
		int i, j;
		
		// Copy user material bytes into temporary ints.
		for(i = 0, j = 0; i < KC;)
			tk[i++] = (key[j++] & 0xFF) << 24 | (key[j++] & 0xFF) << 16 | (key[j++] & 0xFF) << 8 | (key[j++] & 0xFF);
		// Copy values into round key arrays.
		int t = 0;
		for(j = 0; (j < KC) && (t < ROUND_KEY_COUNT); ++j, ++t) {
			Ke[t / BC][t % BC] = tk[j];
			Kd[ROUNDS - (t / BC)][t % BC] = tk[j];
		}
		int tt, rconpointer = 0;
		while(t < ROUND_KEY_COUNT) {
			// Extrapolate using phi (the round key evolution function).
			tt = tk[KC - 1];
			tk[0] ^= (S[(tt >>> 16) & 0xFF] & 0xFF) << 24 ^ (S[(tt >>> 8) & 0xFF] & 0xFF) << 16 ^ (S[tt & 0xFF] & 0xFF) << 8 ^ (S[(tt >>> 24) & 0xFF] & 0xFF) ^ (rcon[rconpointer++] & 0xFF) << 24;
			if(KC != 8)
				for(i = 1, j = 0; i < KC;)
					tk[i++] ^= tk[j++];
			else {
				for(i = 1, j = 0; i < KC / 2;)
					tk[i++] ^= tk[j++];
				tt = tk[KC / 2 - 1];
				tk[KC / 2] ^= (S[tt & 0xFF] & 0xFF) ^ (S[(tt >>> 8) & 0xFF] & 0xFF) << 8 ^ (S[(tt >>> 16) & 0xFF] & 0xFF) << 16 ^ (S[(tt >>> 24) & 0xFF] & 0xFF) << 24;
				for(j = KC / 2, i = j + 1; i < KC;)
					tk[i++] ^= tk[j++];
			}
			// Copy values into round key arrays.
			for(j = 0; (j < KC) && (t < ROUND_KEY_COUNT); ++j, ++t) {
				Ke[t / BC][t % BC] = tk[j];
				Kd[ROUNDS - (t / BC)][t % BC] = tk[j];
			}
		}
		for(int r = 1; r < ROUNDS; ++r)  // inverse MixColumn where needed
			for(j = 0; j < BC; ++j) {
				tt = Kd[r][j];
				Kd[r][j] = U1[(tt >>> 24) & 0xFF] ^ U2[(tt >>> 16) & 0xFF] ^ U3[(tt >>> 8) & 0xFF] ^ U4[tt & 0xFF];
			}
	}
	
	// Block encryption routines.
	
	private int[] tempInts = new int[8];
	
	/// Encrypt a block.
	public void encrypt(byte[] clearText, int clearOff, byte[] cipherText, int cipherOff) {
		int SC = (BC == 4 ? 0 : (BC == 6 ? 1 : 2));
		int s1 = shifts[SC][1][0];
		int s2 = shifts[SC][2][0];
		int s3 = shifts[SC][3][0];
		int[] a = new int[BC];
		int[] t = new int[BC];	// temporary work array
		int i;
		int tt;
		
		for(i = 0; i < BC; ++i)	// plaintext to ints + key
			t[i] = ((clearText[clearOff++] & 0xFF) << 24 | (clearText[clearOff++] & 0xFF) << 16 | (clearText[clearOff++] & 0xFF) << 8 | (clearText[clearOff++] & 0xFF)) ^ Ke[0][i];
		// Apply round transforms.
		for(int r = 1; r < ROUNDS; ++r) {
			for(i = 0; i < BC; ++i)
				a[i] = (T1[(t[i] >>> 24) & 0xFF] ^ T2[(t[(i + s1) % BC] >>> 16) & 0xFF] ^ T3[(t[(i + s2) % BC] >>> 8) & 0xFF] ^ T4[t[(i + s3) % BC] & 0xFF]) ^ Ke[r][i];
			System.arraycopy(a, 0, t, 0, BC);
		}
		// Last round is special.
		for(i = 0; i < BC; ++i) {
			tt = Ke[ROUNDS][i];
			cipherText[cipherOff++] = (byte) (S[(t[i] >>> 24) & 0xFF] ^ (tt >>> 24));
			cipherText[cipherOff++] = (byte) (S[(t[(i + s1) % BC] >>> 16) & 0xFF] ^ (tt >>> 16));
			cipherText[cipherOff++] = (byte) (S[(t[(i + s2) % BC] >>> 8) & 0xFF] ^ (tt >>> 8));
			cipherText[cipherOff++] = (byte) (S[t[(i + s3) % BC] & 0xFF] ^ tt);
		}
	}
	
	/// Decrypt a block.
	public void decrypt(byte[] cipherText, int cipherOff, byte[] clearText, int clearOff) {
		int SC = (BC == 4 ? 0 : (BC == 6 ? 1 : 2));
		int s1 = shifts[SC][1][1];
		int s2 = shifts[SC][2][1];
		int s3 = shifts[SC][3][1];
		int[] a = new int[BC];
		int[] t = new int[BC];	// temporary work array
		int i;
		int tt;
		
		for(i = 0; i < BC; ++i)	// ciphertext to ints + key
			t[i] = ((cipherText[cipherOff++] & 0xFF) << 24 | (cipherText[cipherOff++] & 0xFF) << 16 | (cipherText[cipherOff++] & 0xFF) << 8 | (cipherText[cipherOff++] & 0xFF)) ^ Kd[0][i];
		// Apply round transforms.
		for(int r = 1; r < ROUNDS; ++r) {
			for(i = 0; i < BC; ++i)
				a[i] = (T5[(t[i] >>> 24) & 0xFF] ^ T6[(t[(i + s1) % BC] >>> 16) & 0xFF] ^ T7[(t[(i + s2) % BC] >>> 8) & 0xFF] ^ T8[t[(i + s3) % BC] & 0xFF]) ^ Kd[r][i];
			System.arraycopy(a, 0, t, 0, BC);
		}
		// Last round is special.
		for(i = 0; i < BC; ++i) {
			tt = Kd[ROUNDS][i];
			clearText[clearOff++] = (byte) (Si[(t[i] >>> 24) & 0xFF] ^ (tt >>> 24));
			clearText[clearOff++] = (byte) (Si[(t[(i + s1) % BC] >>> 16) & 0xFF] ^ (tt >>> 16));
			clearText[clearOff++] = (byte) (Si[(t[(i + s2) % BC] >>> 8) & 0xFF] ^ (tt >>> 8));
			clearText[clearOff++] = (byte) (Si[t[(i + s3) % BC] & 0xFF] ^ tt);
		}
	}
	
	/// Test routine.
	public static void main(String[] args) {
		byte[] cipherText = new byte[16];
		byte[] decipherText = new byte[16];
		
		BlockCipher aesa = new AesCipher("0123456789");
		byte[] clearText1 = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
		System.out.println("cleartext: " + toStringBlock(clearText1));
		aesa.encrypt(clearText1, cipherText);
		System.out.println("encrypted: " + toStringBlock(cipherText));
		aesa.decrypt(cipherText, decipherText);
		System.out.println("decrypted: " + toStringBlock(decipherText));
		
		System.out.println();
		
		BlockCipher aesb = new AesCipher("abcdefghijklmnopqrstuvwxyz");
		byte[] clearText2 = { (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b, (byte) 0x0c, (byte) 0x0d, (byte) 0x0e, (byte) 0x0f };
		System.out.println("cleartext: " + toStringBlock(clearText2));
		aesb.encrypt(clearText2, cipherText);
		System.out.println("encrypted: " + toStringBlock(cipherText));
		aesb.decrypt(cipherText, decipherText);
		System.out.println("decrypted: " + toStringBlock(decipherText));
	}
	
}
