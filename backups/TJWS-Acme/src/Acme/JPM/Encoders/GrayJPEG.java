package Acme.JPM.Encoders;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageProducer;
import java.awt.image.PixelGrabber;

// DISCLAIMER
//
// This software is provided by Sean Patrick Breslin for
// your use or abuse, free of charge and royalties. This
// software is provide 'AS IS' and as such there are no
// warrenties expressed or implied. This software is by
// no means robust, optimized, efficient, tight or any of
// a number of adjectives used to describe software. This
// in mind, this software is not to be used for nuclear
// applications, world domination schemes or discount
// remote laser surgery. You may copy or modify this
// code as you see fit. This code works on my PC other
// than that I cannot help you with this code in any way.
// I have not decided whether or not I will support this
// code with bug fixes or enhancements but I'll think
// about it.
//
// USAGE: instanciate the class:
// GrayJPEG jpg = new GrayJPEG();
//
// and call the method:
// jpg.compress(Image i, OutputStream os)
//
// Where 'i' is a standard Java Image object and
// 'os' is an output stream where you want the
// JPEG file written.
//
// NOTE: This code will only compress a grayscale image
// i.e. Black and White, though a color image will
// not crash the code the results are unknown.
//
// PS: I have compiled this with Java 1.0.2 and 1.1.3
// and it works under both versions using windows
// 95 operating system, so knock yourself out and
// have fun with my code.
//
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class GrayJPEG {
	// quantization table in zigzag form for output file
	private byte[] QNT = { -1, -37, 0, 67, 0, 13, 9, 10, 11, 10, 8, 13, 11, 10, 11, 14, 14, 13, 15, 19, 32, 21, 19, 18, 18, 19, 39, 28, 30, 23, 32, 46, 41, 49, 48, 46, 41, 45, 44, 51, 58, 74, 62, 51, 54, 70, 55, 44, 45, 64, 87, 65, 70, 76, 78, 82, 83, 82, 50, 62, 90, 97, 90, 80, 96, 74, 81, 82,
			79 };
	
	// table for doing quantization
	private float[][] QT = { { 13, 9, 8, 13, 19, 32, 41, 49 }, { 10, 10, 11, 15, 21, 46, 48, 44 }, { 11, 10, 13, 19, 32, 46, 55, 45 }, { 11, 14, 18, 23, 41, 70, 64, 50 }, { 14, 18, 30, 45, 54, 87, 82, 62 }, { 19, 28, 44, 51, 65, 83, 90, 74 }, { 39, 51, 62, 70, 82, 97, 96, 81 },
			{ 58, 74, 76, 78, 90, 80, 82, 79 } };
	
	// Zig Zag array
	private int[][] ZZ = { { 0, 1, 5, 6, 14, 15, 27, 28 }, { 2, 4, 7, 13, 16, 26, 29, 42 }, { 3, 8, 12, 17, 25, 30, 41, 43 }, { 9, 11, 18, 24, 31, 40, 44, 53 }, { 10, 19, 23, 32, 39, 45, 52, 54 }, { 20, 22, 33, 38, 46, 51, 55, 60 }, { 21, 34, 37, 47, 50, 56, 59, 61 },
			{ 35, 36, 48, 49, 57, 58, 62, 63 } };
	
	// AC Huffman table
	private byte[] Bits = { 0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 125 };
	private byte[] Huffval = { 1, 2, 3, 0, 4, 17, 5, 18, 33, 49, 65, 6, 19, 81, 97, 7, 34, 113, 20, 50, -127, -111, -95, 8, 35, 66, -79, -63, 21, 82, -47, -16, 36, 51, 98, 114, -126, 9, 10, 22, 23, 24, 25, 26, 37, 38, 39, 40, 41, 42, 52, 53, 54, 55, 56, 57, 58, 67, 68, 69, 70, 71, 72, 73, 74, 83,
			84, 85, 86, 87, 88, 89, 90, 99, 100, 101, 102, 103, 104, 105, 106, 115, 116, 117, 118, 119, 120, 121, 122, -125, -124, -123, -122, -121, -120, -119, -118, -110, -109, -108, -107, -106, -105, -104, -103, -102, -94, -93, -92, -91, -90, -89, -88, -87, -86, -78, -77, -76, -75, -74, -73, -72,
			-71, -70, -62, -61, -60, -59, -58, -57, -56, -55, -54, -46, -45, -44, -43, -42, -41, -40, -39, -38, -31, -30, -29, -28, -27, -26, -25, -24, -23, -22, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6 };
	
	// Header data
	private String str = "JFIF Breslin Engineering JPEG Image Compression";
	private byte[] BE, APP0;
	private byte[] SOI = { -1, -40 };
	private byte[] EOI = { -1, -39 };
	private byte[] SOF = { -1, -64, 0, 11, 8, 0, 0, 0, 0, 1, 1, 17, 0 };
	private byte[] HuffDC = { -1, -60, 0, 31, 0, 0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
	private byte[] HuffACHeader = { -1, -60, 0, -75, 16 };
	private byte[] SOS = { -1, -38, 0, 8, 1, 1, 0, 0, 63, 0 };
	
	// Global variables
	private int ln, X, Y, LASTK, I, J;
	private int BitCnt, K, R, CODE, SI, SSSS;
	private int[] BITS = new int[17];
	private int[] HUFFVAL = new int[162];
	private int[] HUFFCODE = new int[257];
	private int[] HUFFSIZE = new int[257];
	private int[] EHUFCO = new int[257];
	private int[] EHUFSI = new int[257];
	private OutputStream fos;
	private Image image;
	private long DATA = 0;
	
	public GrayJPEG() {
		int x, y, ln = str.length();
		BE = new byte[ln];
		str.getBytes(0, ln, BE, 0);
		ln += 2;
		byte[] ap = { -1, -32, 0, (byte) ln };
		APP0 = ap;
		
		// Generate AC Huffman tables
		// Get BITS data
		BITS[0] = 0;
		for(x = 1; x < 17; x++)
			BITS[x] = Bits[x - 1];
		
		// Get HUFFVAL data
		for(x = 0; x < 162; x++) {
			HUFFVAL[x] = Huffval[x];
			if(HUFFVAL[x] < 0)
				HUFFVAL[x] = 256 + HUFFVAL[x];
		}
		
		Generate_size_table();
		Generate_code_table();
		Order_codes();
	}
	
	public void compress(Image i, OutputStream os) {
		int bkk, x, y, w, h, a, b, c, d;
		int[][] blocks;
		int[][] blk = new int[8][8];
		
		image = i;
		fos = os;
		X = image.getWidth(null);
		Y = image.getHeight(null);
		ln = X * Y;
		int[] data = new int[ln];
		w = (X >> 3) << 3;
		h = (Y >> 3) << 3;
		getPixels(data);
		
		// Set Image dimensions in Start of Frame header
		SOF[5] = (byte) ((h & 0x0000ff00) >> 8);
		SOF[6] = (byte) (h & 0x000000ff);
		SOF[7] = (byte) ((w & 0x0000ff00) >> 8);
		SOF[8] = (byte) (w & 0x000000ff);
		
		// Set number of blocks
		w = X / 8;
		h = Y / 8;
		ln = w * h;
		blocks = new int[ln][];
		bkk = 0;
		
		// break image in to 8x8 blocks
		for(a = 0; a < (h * 8); a += 8) {
			// Get 8 lines of image
			for(b = 0; b < (w * 8); b += 8) {
				// Get block for FDCT
				for(c = 0; c < 8; c++)
					for(d = 0; d < 8; d++)
						// mask off 3 upper bytes of image data
						blk[c][d] = (data[(a + c) * X + b + d] & 0x000000ff) - 128;
					
				// Do FDCT on Block
				blocks[bkk++] = FDCT(blk);
			}
		}
		// Write out header data
		writeHeaders();
		
		// Encode image scan
		Huffman(blocks);
		
		// Write out last bits of image scan
		writeEndData();
		
		// Write End of Image marker
		writeEnd();
	}
	
	private void Huffman(int[][] blocks) {
		int tmp, lp, DIFF, dat, PRED, bits;
		int[] blk;
		byte[] data = new byte[1];
		int[][] HuffTbl = { { 2, 0 }, { 3, 2 }, { 3, 3 }, { 3, 4 }, { 3, 5 }, { 3, 6 }, { 4, 14 }, { 5, 30 }, { 6, 62 }, { 7, 126 }, { 8, 254 }, { 9, 510 } };
		long buffer;
		
		// Encode data blocks
		// Initialize predictor for new scan
		PRED = 0;
		BitCnt = 0;
		for(lp = 0; lp < ln; lp++) {
			blk = blocks[lp];
			buffer = 0;
			bits = 0;
			
			// Encode DC coefficient
			tmp = DIFF = blk[0] - PRED;
			if(tmp < 0) {
				tmp = -tmp;
				DIFF--;
			}
			
			// find magnitude category data for DC edcoding
			SSSS = MagCat(tmp);
			
			// Total number of bits output
			bits = HuffTbl[SSSS][0] + SSSS;
			
			// Code word for this category shifted
			// to accept magnitude data
			buffer = HuffTbl[SSSS][1] << SSSS;
			
			// append bits to code word
			buffer += (DIFF & ((1 << SSSS) - 1));
			
			// Write out data if greater then or equal to one byte
			writeData(buffer, bits);
			
			// Assign PRED for next DC DIFF
			PRED = blk[0];
			
			// Encode AC coefficients
			Encode_AC_coefficients(blk);
			
			// free up memory for next compression
			blocks[lp] = null;
		}
	}
	
	private void Encode_AC_coefficients(int[] ZZ) {
		boolean done = false;
		int size, code, dat;
		K = 0;
		R = 0;
		
		while(!done) {
			K++;
			dat = ZZ[K];
			
			if(dat == 0) {
				if(K == 63) {
					// mark EOB
					code = EHUFCO[0x00];
					size = EHUFSI[0x00];
					writeData(code, size);
					done = true;
					break;
				} else {
					R++;
					continue;
				}
			} else {
				
				while(true) {
					if(R > 15) {
						R -= 16;
						// Mark RLZ in scan
						code = EHUFCO[0xf0];
						size = EHUFSI[0xf0];
						writeData(code, size);
						continue;
					}
					Encode_R(ZZ[K]);
					R = 0;
					if(K == 63)
						done = true;
					break;
				}
			}
		}
	}
	
	private void Encode_R(int ZZ) {
		int dat, RS, size, code;
		dat = ZZ;
		if(ZZ < 0) {
			dat = -dat;
			ZZ--;
		}
		
		// Get AC magnitude category
		SSSS = MagCat(dat);
		RS = (R << 4) + SSSS;
		
		// append bits
		code = EHUFCO[RS];
		size = EHUFSI[RS];
		writeData(code, size);
		
		// Mask off upper bits of ZZ
		ZZ &= ((1 << SSSS) - 1);
		
		// append SSS low order bits of ZZ(K)
		writeData(ZZ, SSSS);
	}
	
	private void Generate_size_table() {
		// Generate HUFFSIZE table Flow Chart C.1
		K = 0;
		I = 1;
		J = 1;
		while(true) {
			if(J > BITS[I]) {
				J = 1;
				I++;
				if(I > 16)
					break;
			} else {
				HUFFSIZE[K++] = I;
				J++;
			}
		}
		HUFFSIZE[K] = 0;
		LASTK = K;
	}
	
	private void Generate_code_table() {
		// Generate Code table Flow Chart C.2
		K = 0;
		CODE = 0;
		SI = HUFFSIZE[0];
		while(true) {
			HUFFCODE[K++] = CODE++;
			if(HUFFSIZE[K] == SI)
				continue;
			if(HUFFSIZE[K] == 0)
				break;
			while(true) {
				CODE <<= 1;
				SI++;
				if(HUFFSIZE[K] == SI)
					break;
			}
		}
	}
	
	private void Order_codes() {
		// Order Codes Flow Chart C.3
		K = 0;
		while(true) {
			I = HUFFVAL[K];
			EHUFCO[I] = HUFFCODE[K];
			EHUFSI[I] = HUFFSIZE[K++];
			if(K >= LASTK)
				break;
		}
	}
	
	private int MagCat(int dat) {
		int ln;
		for(;;) {
			if(dat == 0) {
				ln = 0;
				break;
			}
			if(dat == 1) {
				ln = 1;
				break;
			}
			if(dat <= 3) {
				ln = 2;
				break;
			}
			if(dat <= 7) {
				ln = 3;
				break;
			}
			if(dat <= 15) {
				ln = 4;
				break;
			}
			if(dat <= 31) {
				ln = 5;
				break;
			}
			if(dat <= 63) {
				ln = 6;
				break;
			}
			if(dat <= 127) {
				ln = 7;
				break;
			}
			if(dat <= 255) {
				ln = 8;
				break;
			}
			if(dat <= 511) {
				ln = 9;
				break;
			}
			if(dat <= 1023) {
				ln = 10;
				break;
			}
			if(dat <= 2047) {
				ln = 11;
				break;
			}
		}
		return ln;
	}
	
	private void writeHeaders() {
		try {
			fos.write(SOI);          // Start of Image marker
			fos.write(APP0);         // Application header
			fos.write(BE);           // JFIF BE jpg compression
			fos.write(QNT);          // Quantization table
			fos.write(SOF);          // Start of Frame marker
			fos.write(HuffDC);       // DC Huffman Table
			fos.write(HuffACHeader); // AC Huffman Table Header
			fos.write(Bits);         // AC Huffman Table
			fos.write(Huffval);      // AC Huffman Table
			fos.write(SOS);          // Start of Scan header
		} catch(IOException ioe) {
			System.err.println("IOException: " + ioe);
		}
	}
	
	private void writeEnd() {
		try {
			fos.write(EOI);  // End of Image/File
			fos.close();
		} catch(IOException ioe) {
			System.err.println("IOException: " + ioe);
		}
	}
	
	private void getPixels(int[] data) {
		PixelGrabber pg = new PixelGrabber(image.getSource(), 0, 0, X, Y, data, 0, X);
		try {
			if(pg.grabPixels() != true) {
				try {
					throw new AWTException("Grabber returned false: " + pg.status());
				} catch(Exception ex) {
					System.err.println("System Failed to get Pixels! - " + ex);
					System.exit(0);
				}
				;
			}
		} catch(InterruptedException ent) {
		}
		;
	}
	
	private void writeData(long dat, int bits) {
		int tmp;
		byte[] stuff = { 0 };
		byte[] d = new byte[1];
		
		if(bits > 0) {
			DATA <<= bits;
			DATA += dat;
			BitCnt += bits;
		} else
			return;
		
		// output bytes untill cnt is less then 8
		while(BitCnt > 7) {
			BitCnt -= 8;
			tmp = (int) (DATA >> BitCnt);
			d[0] = (byte) tmp;
			
			// mask off 8 msb of DATA
			DATA &= ((1 << BitCnt) - 1);
			
			// write out data
			try {
				fos.write(d);  // End of Image/File
				if(d[0] == -1)
					// if 0xFF data stuff 0x00 byte
					fos.write(stuff);
			} catch(IOException ioe) {
				System.err.println("IOException: " + ioe);
			}
		}
	}
	
	private void writeEndData() {
		byte[] d = new byte[1];
		// finish off scan data to byte boundry
		if(BitCnt > 0) {
			DATA <<= (8 - BitCnt);
			DATA += ((1 << (8 - BitCnt)) - 1);
			d[0] = (byte) DATA;
			try {
				fos.write(d);
			} catch(IOException ioe) {
				System.err.println("IOException: " + ioe);
			}
		}
	}
	
	private int[] FDCT(int[][] block) {
		int j1, i, j;
		int[] blk = new int[64];
		float[] b = new float[8];
		float temp;
		float[] b1 = new float[8];
		float[][] d = new float[8][8];
		float f0 = (float) 0.7071068, f1 = (float) 0.4903926;
		float f2 = (float) 0.4619398, f3 = (float) 0.4157348;
		float f4 = (float) 0.3535534, f5 = (float) 0.2777851;
		float f6 = (float) 0.1913417, f7 = (float) 0.0975452;
		
		float df7f1 = (float) -0.3928475, sf7f1 = (float) 0.5879378;
		float df3f5 = (float) 0.1379497, sf3f5 = (float) 0.6935199;
		float df6f2 = (float) -0.27059805, sf6f2 = (float) .6532815;
		
		for(i = 0; i < 8; i++) {
			for(j = 0; j < 8; j++) {
				b[j] = block[i][j];
			}
			// Horizontal transform
			for(j = 0; j < 4; j++) {
				j1 = 7 - j;
				b1[j] = b[j] + b[j1];
				b1[j1] = b[j] - b[j1];
			}
			b[0] = b1[0] + b1[3];
			b[1] = b1[1] + b1[2];
			b[2] = b1[1] - b1[2];
			b[3] = b1[0] - b1[3];
			b[4] = b1[4];
			b[5] = (b1[6] - b1[5]) * f0;
			b[6] = (b1[6] + b1[5]) * f0;
			b[7] = b1[7];
			d[i][0] = (b[0] + b[1]) * f4;
			d[i][4] = (b[0] - b[1]) * f4;
			
			temp = (b[3] + b[2]) * f6;
			d[i][2] = temp - b[3] * df6f2;
			d[i][6] = temp - b[2] * sf6f2;
			
			b1[4] = b[4] + b[5];
			b1[7] = b[7] + b[6];
			b1[5] = b[4] - b[5];
			b1[6] = b[7] - b[6];
			
			temp = (b1[7] + b1[4]) * f7;
			d[i][1] = temp - b1[7] * df7f1;
			d[i][7] = temp - b1[4] * sf7f1;
			
			temp = (b1[6] + b1[5]) * f3;
			d[i][5] = temp - b1[6] * df3f5;
			d[i][3] = temp - b1[5] * sf3f5;
		}
		
		// Vertical transform
		for(i = 0; i < 8; i++) {
			for(j = 0; j < 4; j++) {
				j1 = 7 - j;
				b1[j] = d[j][i] + d[j1][i];
				b1[j1] = d[j][i] - d[j1][i];
			}
			b[0] = b1[0] + b1[3];
			b[1] = b1[1] + b1[2];
			b[2] = b1[1] - b1[2];
			b[3] = b1[0] - b1[3];
			b[4] = b1[4];
			b[5] = (b1[6] - b1[5]) * f0;
			b[6] = (b1[6] + b1[5]) * f0;
			b[7] = b1[7];
			d[0][i] = (b[0] + b[1]) * f4;
			d[4][i] = (b[0] - b[1]) * f4;
			
			temp = (b[3] + b[2]) * f6;
			d[2][i] = temp - b[3] * df6f2;
			d[6][i] = temp - b[2] * sf6f2;
			
			b1[4] = b[4] + b[5];
			b1[7] = b[7] + b[6];
			b1[5] = b[4] - b[5];
			b1[6] = b[7] - b[6];
			
			temp = (b1[7] + b1[4]) * f7;
			d[1][i] = temp - b1[7] * df7f1;
			d[7][i] = temp - b1[4] * sf7f1;
			
			temp = (b1[6] + b1[5]) * f3;
			d[5][i] = temp - b1[6] * df3f5;
			d[3][i] = temp - b1[5] * sf3f5;
			
		}
		for(i = 0; i < 8; i++) {
			for(j = 0; j < 8; j++) {
				// Quantize and ZigZag data block
				blk[ZZ[i][j]] = (int) (d[i][j] / QT[i][j]);
			}
		}
		return blk;
	}
	
	public static void main(String[] args) {
		try {
			URL url = new URL(args[0]);
			URLConnection uc = url.openConnection();
			uc.connect();
			
			// Got to open a stream to get around Java NullPointerException bug.
			InputStream f = uc.getInputStream();
			
			String mimeType = uc.getContentType();
			
			// Java prints (not throws!) a ClassNotFoundException if you try
			// a getContent on text.html or audio/basic (or no doubt many
			// other types).
			if(!mimeType.startsWith("image/"))
				System.err.println(args[0] + " is not an image");
			else {
				Object content = uc.getContent();
				if(!(content instanceof ImageProducer))
					System.err.println(args[0] + " is not a known image type");
				else {
					ImageProducer prod = (ImageProducer) content;
					Toolkit tk = new Acme.JPM.StubToolkit();
					Image img = tk.createImage(prod);
					GrayJPEG jpg = new GrayJPEG();
					jpg.compress(img, System.out);
				}
			}
			
			// Done with unnecessary stream.
			f.close();
		} catch(Exception e) {
			System.err.println(e);
		}
	}
}
