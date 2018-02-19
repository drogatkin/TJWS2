// Dbz - access a Unix DBZ file
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Dictionary;
import java.util.Enumeration;

/// Access a Unix DBZ file.
// <P>
// DBZ files are similar to DBM files. From the Java point of view,
// they are just on-disk hash tables. Comments from the C version:
// <P>
// The dbz database exploits the fact that when news stores a <key,value>
// tuple, the `value' part is a seek offset into a text file, pointing to
// a copy of the `key' part. This avoids the need to store a copy of
// the key in the dbz files. However, the text file *must* exist and be
// consistent with the dbz files, or things will fail.
// <P>
// The basic format of the database is a simple hash table containing the
// values. A value is stored by indexing into the table using a hash value
// computed from the key; collisions are resolved by linear probing (just
// search forward for an empty slot, wrapping around to the beginning of
// the table if necessary). Linear probing is a performance disaster when
// the table starts to get full, so a complication is introduced. The
// database is actually one *or more* tables, stored sequentially in the
// .pag file, and the length of linear-probe sequences is limited. The
// search (for an existing item or an empty slot) always starts in the
// first table, and whenever MAXRUN probes have been done in table N,
// probing continues in table N+1. This behaves reasonably well even in
// cases of massive overflow. There are some other small complications
// added, see comments below.
// <P>
// The table size is fixed for any particular database, but is determined
// dynamically when a database is rebuilt. The strategy is to try to pick
// the size so the first table will be no more than 2/3 full, that being
// slightly before the point where performance starts to degrade. (It is
// desirable to be a bit conservative because the overflow strategy tends
// to produce files with holes in them, which is a nuisance.)
// <P>
// <A HREF="/resources/classes/Acme/Dbz.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class Dbz extends Dictionary {
	
	/// For validating .dir format.
	private static final int dbzVersion = 3;
	
	/// Size of an offset - in Java that's an int.
	private static final int SOF = 4;
	
	// We assume that unused areas of a binary file are zeros, and that the
	// bit pattern of `(of_t)0' is all zeros. The alternative is rather
	// painful file initialization. Note that okayvalue(), if OVERFLOW is
	// defined, knows what value of an offset would cause overflow.
	private static final int VACANT = 0;
	
	private static int BIAS(int o) {
		return o + 1;
	}
	
	private static int UNBIAS(int o) {
		return o - 1;
	}
	
	// In a Unix implementation, or indeed any in which an of_t is a byte
	// count, there are a bunch of high bits free in an of_t. There is a
	// use for them. Checking a possible hit by looking it up in the base
	// file is relatively expensive, and the cost can be dramatically reduced
	// by using some of those high bits to tag the value with a few more bits
	// of the key's hash. This detects most false hits without the overhead of
	// seek+read+strcmp. We use the top bit to indicate whether the value is
	// tagged or not, and don't tag a value which is using the tag bits itself.
	// We're in trouble if the of_t representation wants to use the top bit.
	// The actual bitmasks and offset come from the configuration stuff,
	// which permits fiddling with them as necessary, and also suppressing
	// them completely (by defining the masks to 0). We build pre-shifted
	// versions of the masks for efficiency.
	private int tagbits;
	private int taghere;
	private int tagboth;
	
	private boolean HASTAG(int o) {
		return (o & taghere) != 0;
	}
	
	private int TAG(int o) {
		return o & tagbits;
	}
	
	private int NOTAG(int o) {
		return o & (~tagboth);
	}
	
	private boolean CANTAG(int o) {
		return (o & tagboth) == 0;
	}
	
	private int MKTAG(int v) {
		return (v << tagshift) & tagbits;
	}
	
	// A new, from-scratch database, not built as a rebuild of an old one,
	// needs to know table size, casemap algorithm, and tagging. Normally
	// the user supplies this info, but there have to be defaults.
	private static final int DEFSIZE = 120011;	// 300007 might be better
	private static final char DEFCASE = 'C';
	private static final int TAGENB = 0x80;
	private static final int TAGMASK = 0x7f;
	private static final int TAGSHIFT = 24;
	
	// We read configuration info from the .dir file into these variables,
	// so we can avoid wired-in assumptions for an existing database.
	//
	// Among the info is a record of recent peak usages, so that a new table
	// size can be chosen intelligently when rebuilding. 10 is a good
	// number of usages to keep, since news displays marked fluctuations
	// in volume on a 7-day cycle.
	boolean olddbz;			// .dir file empty but .pag not?
	int tsize;				// table size
	int[] used = new int[11];		// entries used today, yesterday, ...
	int valuesize;			// size of table values, == SOF
	int[] bytemap = new int[SOF];	// byte-order map
	char casemap;			// case-mapping algorithm
	char fieldsep;			// field separator in base file, if any
	int tagenb;				// unshifted tag-enable bit
	int tagmask;			// unshifted tag mask
	int tagshift;			// shift count for tagmask and tagenb
	
	// For a program that makes many, many references to the database, it
	// is a large performance win to keep the table in core, if it will fit.
	// Note that this does hurt robustness in the event of crashes, and
	// dbmclose() *must* be called to flush the in-core database to disk.
	// The code is prepared to deal with the possibility that there isn't
	// enough memory. There *is* an assumption that a size_t is big enough
	// to hold the size (in bytes) of one table, so dbminit() tries to figure
	// out whether this is possible first.
	//
	// The preferred way to ask for an in-core table is to do dbzincore(1)
	// before dbminit(). The default is not to do it, although -DINCORE
	// overrides this for backward compatibility with old dbz.
	//
	// We keep only the first table in core. This greatly simplifies the
	// code, and bounds memory demand. Furthermore, doing this is a large
	// performance win even in the event of massive overflow.
	private boolean incore = true;
	
	// Buffer for .pag reads. Buffering more than about 16 does not help
	// significantly at the densities we try to maintain
	private int[] pagbuf = new int[16];
	
	// Buffer for base-file reads. Message-IDs (all news ever needs to
	// read) are essentially never longer than 64 bytes.
	private byte[] basebuf = new byte[64];
	
	private static final int NOTFOUND = -1;
	
	// Arguably the searcher struct for a given routine ought to be local to
	// it, but a fetch() is very often immediately followed by a store(), and
	// in some circumstances it is a useful performance win to remember where
	// the fetch() completed. So we use a global struct and remember whether
	// it is current.
	DbzSearcher srch = new DbzSearcher();
	DbzSearcher prev;		// srch or null
	
	// Byte-ordering stuff.
	private int[] mybmap = new int[SOF];	// my byte order
	private boolean bytesame;
	
	private int MAPIN(int o) {
		return bytesame ? o : bytemap(o, bytemap, mybmap);
	}
	
	private int MAPOUT(int o) {
		return bytesame ? o : bytemap(o, mybmap, bytemap);
	}
	
	// The files.
	private RandomAccessFile basef;	// descriptor for base file
	private RandomAccessFile dirf;	// descriptor for .dir file
	private RandomAccessFile pagf;	// descriptor for .pag file
	private boolean readOnly;		// files open read-only
	private int pagpos;		      // posn in pagf; only search may set != -1
	private int[] corepag;		// incore version of .pag file, if any
	private boolean written;		// has a store() been done?
	
	/// Constructor.
	// @exception IOException if something goes wrong
	public Dbz(File file) throws IOException {
		try {
			basef = new RandomAccessFile(file, "rw");
			dirf = new RandomAccessFile(file.getPath() + ".dir", "rw");
			pagf = new RandomAccessFile(file.getPath() + ".pag", "rw");
			readOnly = false;
		} catch(Exception e) {
			basef = new RandomAccessFile(file, "r");
			dirf = new RandomAccessFile(file.getPath() + ".dir", "r");
			pagf = new RandomAccessFile(file.getPath() + ".pag", "r");
			readOnly = true;
		}
		pagpos = -1;
		getconf();
		tagbits = tagmask << tagshift;
		taghere = tagenb << tagshift;
		tagboth = tagbits | taghere;
		mybytemap(mybmap);
		bytesame = true;
		for(int i = 0; i < SOF; ++i)
			if(mybmap[i] != bytemap[i])
				bytesame = false;
		// Get first table into core, if it looks desirable and feasible.
		int s = tsize * SOF;
		if(incore && s / SOF == tsize)
			corepag = getcore();
		else
			corepag = null;
		// Misc. setup.
		crcinit();
		written = false;
		prev = null;
	}
	
	/// Returns the number of elements contained within the dbz file.
	public int size() {
		// !!!
		return 0;
	}
	
	/// Returns true if the dbz file contains no elements.
	public boolean isEmpty() {
		// !!!
		return true;
	}
	
	/// Returns an enumeration of the dbz file's keys.
	public Enumeration keys() {
		// !!!
		return new DbzKeyEnumerator();
	}
	
	/// Returns an enumeration of the elements. Use the Enumeration methods
	// on the returned object to fetch the elements sequentially.
	public Enumeration elements() {
		return new DbzElementEnumerator(this);
	}
	
	/// Gets the object associated with the specified key in the dbz file.
	// @param key the key in the hash table
	// @returns the element for the key, or null if the key
	// is not defined in the hash table.
	public Object get(Object key) {
		return fetch(mapcase((String) key));
	}
	
	/// Puts the specified element into the Dictionary, using the specified
	// key. The element may be retrieved by doing a get() with the same
	// key. The key and the element cannot be null.
	// @param key the specified hashtable key
	// @param value the specified element
	// @return the old value of the key, or null if it did not have one.
	// @exception NullPointerException If the value of the specified
	// element is null.
	public Object put(Object key, Object value) {
		return store(mapcase((String) key), value);
	}
	
	/// Removes the element corresponding to the key. Does nothing if the
	// key is not present.
	// @param key the key that needs to be removed
	// @return the value of key, or null if the key was not found.
	public Object remove(Object key) {
		// !!!
		return null;
	}
	
	/// Main program - for testing etc.
	public static void main(String[] args) {
		// !!!
	}
	
	/// What's a good table size to hold this many entries?
	public int dbzsize(int contents) {
		int n;
		if(contents <= 0)
			return DEFSIZE;
		n = (contents / 2) * 3;	// try to keep table at most 2/3 full
		if(Utils.even(n))		// make it odd
			++n;
		while(!isprime(n))	// and look for a prime
			n += 2;
		return n;
	}
	
	private static final int[] smallPrimes = { 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37 };
	
	/// Is a number prime?
	private boolean isprime(int x) {
		int div = 0, stop;
		// Hit the first few primes quickly to eliminate easy ones.
		// This incidentally prevents ridiculously small tables.
		for(int i = 0; i < smallPrimes.length; ++i) {
			div = smallPrimes[i];
			if(x % div == 0)
				return false;
		}
		// Approximate square root of x.
		for(stop = x; x / stop < stop; stop >>= 1)
			;
		stop <<= 1;
		// Try odd numbers up to stop.
		for(div += 2; div < stop; div += 2)
			if(x % div == 0)
				return false;
		return true;
	}
	
	/// Close a database.
	public void dbmclose() {
		dbzsync();
		try {
			basef.close();
			dirf.close();
			pagf.close();
		} catch(IOException ignore) {
		}
	}
	
	/// Push all in-core data out to disk.
	public void dbzsync() {
		if(!written)
			return;
		if(corepag != null)
			putcore(corepag);
		if(!olddbz)
			putconf();
	}
	
	/// Cancel writing of in-core data.
	// Mostly for use from child process.
	public void dbzcancel() {
		written = false;
	}
	
	/// Like get(), but assumes the key has already been case-mapped.
	public Object fetch(Object key) {
		Object output = null;
		prev = null;
		String skey = (String) key;
		int cmplen = skey.length();
		// !!!
		return output;
	}
	
	/// Like put(), but assumes the key has already been case-mapped.
	public Object store(Object key, Object value) {
		if(readOnly)
			throw new RuntimeException("database is read-only");
		// !!!
		return null;
	}
	
	/// Control attempts to keep .pag file in core.
	public void dbzincore(boolean value) {
		incore = value;
	}
	
	/// Get configuration from .dir file.
	private void getconf() {
		// !!!
	}
	
	/// Write configuration to .dir file.
	private void putconf() {
		// !!!
	}
	
	/// Try to set up an in-core copy of .pag file.
	private int[] getcore() {
		// !!!
		return null;
	}
	
	/// Try to rewrite an in-core table.
	private void putcore(int[] tab) {
		// !!!
	}
	
	/// Set up to start or restart a search.
	private void start() {
		// !!!
	}
	
	/// Conduct part of a searc.
	private int search() {
		// !!!
		return NOTFOUND;
	}
	
	/// Check that a value can be stored.
	private boolean okayvalue(int value) {
		if(HASTAG(value))
			return false;
		return true;
	}
	
	/// Store a value into a location previously found by search.
	private void set(DbzSearcher s, int value) {
		// !!!
	}
	
	/// Determine this machine's byte map.
	private void mybytemap(int[] map) {
		// !!!
	}
	
	/// Transform an offset from byte ordering map1 to map2.
	private int bytemap(int ino, int[] map1, int[] map2) {
		// !!!
		return ino;
	}
	
	// This is a simplified version of the pathalias hashing function.
	// Thanks to Steve Belovin and Peter Honeyman
	//
	// Hash a string into a long int. 31 bit crc (from andrew appel).
	// The crc table is computed at run time by crcinit() -- we could
	// precompute, but it takes 1 clock tick on a 750.
	//
	// This fast table calculation works only if POLY is a prime polynomial
	// in the field of integers modulo 2. Since the coefficients of a
	// 32-bit polynomial won't fit in a 32-bit word, the high-order bit is
	// implicit. IT MUST ALSO BE THE CASE that the coefficients of orders
	// 31 down to 25 are zero. Happily, we have candidates, from
	// E. J. Watson, "Primitive Polynomials (Mod 2)", Math. Comp. 16 (1962):
	// x^32 + x^7 + x^5 + x^3 + x^2 + x^1 + x^0
	// x^31 + x^3 + x^0
	//
	// We reverse the bits to get:
	// 111101010000000000000000000000001 but drop the last 1
	// f 5 0 0 0 0 0 0
	// 010010000000000000000000000000001 ditto, for 31-bit crc
	// 4 8 0 0 0 0 0 0
	
	// 31-bit polynomial (avoids sign problems).
	private static final int POLY = 0x48000000;
	
	private int[] crctable = new int[128];
	
	/// Initialize tables for hash function.
	private void crcinit() {
		for(int i = 0; i < crctable.length; ++i) {
			int sum = 0;
			for(int j = 7 - 1; j >= 0; --j)
				if((i & (1 << j)) != 0)
					sum ^= POLY >> j;
			crctable[i] = sum;
		}
	}
	
	/// Honeyman's nice hashing function.
	private int hash(String name) {
		int sum = 0;
		for(int i = 0; i < name.length(); ++i) {
			int b = (int) name.charAt(i);
			int j = (sum ^ b) & 0x7f;
			sum = (sum >> 7) ^ crctable[j];
		}
		return sum;
	}
	
	// Case-mapping stuff.
	// We exploit the fact that we are dealing only with headers here, and
	// headers are limited to the ASCII characters by RFC822. It is barely
	// possible that we might be dealing with a translation into another
	// character set, but in particular it's very unlikely for a header
	// character to be outside -128..255.
	
	// !!!
	
	private String mapcase(String src) {
		// !!!
		return src;
	}
	
}

class DbzSearcher {
	public int place;		// current location in file
	public int tabno;		// which table we're in
	public int run;		// how long we'll stay in this table
	public int hash;		// the key's hash code (for optimization)
	public int tag;		// tag we are looking for
	public boolean seen;	// have we examined current location?
	public boolean aborted;	// has i/o error aborted search?
}

class DbzKeyEnumerator implements Enumeration {
	
	public DbzKeyEnumerator() {
		// !!!
	}
	
	public boolean hasMoreElements() {
		// !!!
		return false;
	}
	
	public Object nextElement() {
		// !!!
		return null;
	}
	
}

class DbzElementEnumerator implements Enumeration {
	
	private Dbz dbz;
	private Enumeration keys;
	
	public DbzElementEnumerator(Dbz dbz) {
		this.dbz = dbz;
		keys = dbz.keys();
	}
	
	public boolean hasMoreElements() {
		return keys.hasMoreElements();
	}
	
	public Object nextElement() {
		Object key = keys.nextElement();
		return dbz.get(key);
	}
	
}
