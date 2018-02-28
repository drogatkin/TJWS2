// Matrix - simple double matrix class
//
// Copyright (C)1996,2000 by Jef Poskanzer <jef@mail.acme.com>. All rights
// reserved.
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

/// Simple double matrix class.
// <P>
// <A HREF="/resources/classes/Acme/Matrix.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class Matrix {
	
	private int rows, cols;
	private double values[][];
	
	// Constructor - create a matrix of a given size, initialized to all zero.
	public Matrix(int rows, int cols) throws ArithmeticException {
		this(rows, cols, 0.0);
	}
	
	// Constructor - create a matrix of a given size, initialized to a value.
	public Matrix(int rows, int cols, double value) throws ArithmeticException {
		if(rows <= 0 || cols <= 0)
			throw new ArithmeticException();
		this.rows = rows;
		this.cols = cols;
		values = new double[rows][cols];
		for(int row = 0; row < rows; ++row)
			for(int col = 0; col < cols; ++col)
				values[row][col] = value;
	}
	
	/// Constructor - create an identity matrix.
	public Matrix(int size) throws ArithmeticException {
		this(size, size);
		for(int i = 0; i < size; ++i)
			values[i][i] = 1.0;
	}
	
	/// Constructor - copy a matrix.
	public Matrix(Matrix o) throws ArithmeticException {
		this(o.rows, o.cols);
		for(int row = 0; row < rows; ++row)
			for(int col = 0; col < cols; ++col)
				values[row][col] = o.values[row][col];
	}
	
	/// Get the row count.
	public int getRows() {
		return rows;
	}
	
	/// Get the column count.
	public int getCols() {
		return cols;
	}
	
	/// Get an element.
	public double get(int row, int col) {
		return values[row][col];
	}
	
	/// Set an element.
	public void set(int row, int col, double value) {
		values[row][col] = value;
	}
	
	/// Equality test.
	public boolean equals(Object o) {
		if(o == null || !(o instanceof Matrix))
			return false;
		Matrix m = (Matrix) o;
		if(m.rows != rows || m.cols != cols)
			return false;
		for(int row = 0; row < rows; ++row)
			for(int col = 0; col < cols; ++col)
				if(m.values[row][col] != values[row][col])
					return false;
		return true;
	}
	
	/// Compute a hash code for the matrix.
	public int hashCode() {
		int h = 0;
		for(int row = 0; row < rows; ++row)
			for(int col = 0; col < cols; ++col) {
				Double d = new Double(values[row][col]);
				h = h ^ d.hashCode();
			}
		return h;
	}
	
	/// Convert to a string.
	public String toString() {
		return Acme.Utils.arrayToString(values);
	}
	
	/// Compute the sum of all the elements.
	public double sum() {
		double s = 0.0;
		for(int row = 0; row < rows; ++row)
			for(int col = 0; col < cols; ++col)
				s += values[row][col];
		return s;
	}
	
	/// Compute the mean of all the elements.
	public double mean() {
		return sum() / (rows * cols);
	}
	
	/// Find the largest element.
	public double max() {
		double m = values[0][0];
		for(int row = 0; row < rows; ++row)
			for(int col = 0; col < cols; ++col)
				if(values[row][col] > m)
					m = values[row][col];
		return m;
	}
	
	/// Find the smallest element.
	public double min() {
		double m = values[0][0];
		for(int row = 0; row < rows; ++row)
			for(int col = 0; col < cols; ++col)
				if(values[row][col] < m)
					m = values[row][col];
		return m;
	}
	
	/// Swap rows in a matrix.
	public void swapRows(int r1, int r2) {
		double t;
		for(int col = 0; col < cols; ++col) {
			t = values[r1][col];
			values[r1][col] = values[r2][col];
			values[r2][col] = t;
		}
	}
	
	/// Swap columns in a matrix.
	public void swapCols(int c1, int c2) {
		double t;
		for(int row = 0; row < rows; ++row) {
			t = values[row][c1];
			values[row][c1] = values[row][c2];
			values[row][c2] = t;
		}
	}
	
	/// Transpose a matrix.
	public static Matrix transpose(Matrix m) {
		Matrix result = new Matrix(m.cols, m.rows);
		for(int row = 0; row < m.rows; ++row)
			for(int col = 0; col < m.cols; ++col)
				result.values[col][row] = m.values[row][col];
		return result;
	}
	
	/// Add two matricies.
	public static Matrix add(Matrix a, Matrix b) throws ArithmeticException {
		if(a.rows != b.rows || a.cols != b.cols)
			throw new ArithmeticException();
		Matrix result = new Matrix(a.rows, a.cols);
		for(int row = 0; row < a.rows; ++row)
			for(int col = 0; col < a.cols; ++col)
				result.values[row][col] = a.values[row][col] + b.values[row][col];
		return result;
	}
	
	/// Subtract one matrix from another.
	public static Matrix subtract(Matrix a, Matrix b) throws ArithmeticException {
		if(a.rows != b.rows || a.cols != b.cols)
			throw new ArithmeticException();
		Matrix result = new Matrix(a.rows, a.cols);
		for(int row = 0; row < a.rows; ++row)
			for(int col = 0; col < a.cols; ++col)
				result.values[row][col] = a.values[row][col] - b.values[row][col];
		return result;
	}
	
	/// Multiply a matrix by a scalar.
	public static Matrix multiply(Matrix a, double b) {
		Matrix result = new Matrix(a.rows, a.cols);
		for(int row = 0; row < a.rows; ++row)
			for(int col = 0; col < a.cols; ++col)
				result.values[row][col] = a.values[row][col] * b;
		return result;
	}
	
	/// Multiply a scalar by a matrix.
	public static Matrix multiply(double a, Matrix b) {
		return Matrix.multiply(b, a);
	}
	
	/// Multiply two matricies.
	public static Matrix multiply(Matrix a, Matrix b) throws ArithmeticException {
		if(a.cols != b.rows)
			throw new ArithmeticException();
		Matrix result = new Matrix(a.rows, b.cols);
		for(int row = 0; row < a.rows; ++row)
			for(int col = 0; col < b.cols; ++col) {
				double sum = 0.0;
				for(int i = 0; i < a.cols; ++i)
					sum += a.values[row][i] * b.values[i][col];
				result.values[row][col] = sum;
			}
		return result;
	}
	
	/// Solve ax=b using Gaussian elimination. The matrix a must be square,
	// and b must be a vector with the same number of rows as a. Returns
	// another vector of the same size as b.
	public static Matrix solve(Matrix a, Matrix b) throws ArithmeticException {
		int i, j, k;
		double t;
		if(a.rows != a.cols || a.cols != b.rows || b.cols != 1)
			throw new ArithmeticException();
		int n = a.rows;
		a = new Matrix(a);
		b = new Matrix(b);
		Matrix x = new Matrix(n, 1);
		// Elimination with partial pivoting.
		for(i = 0; i < n; ++i) {
			// Find the largest pivot in a.
			int max = i;
			for(j = i + 1; j < n; ++j)
				if(Math.abs(a.values[j][i]) > Math.abs(a.values[max][i]))
					max = j;
			if(a.values[max][i] == 0.0)
				throw new ArithmeticException();
			// Exchange rows i and max in both a and b.
			a.swapRows(i, max);
			b.swapRows(i, max);
			// And eliminate.
			for(j = i + 1; j < n; ++j) {
				t = a.values[j][i] / a.values[i][i];
				for(k = n - 1; k >= i; --k)
					a.values[j][k] -= a.values[i][k] * t;
				b.values[j][0] -= b.values[i][0] * t;
			}
		}
		// Back-substitution.
		for(j = n - 1; j >= 0; --j) {
			t = 0.0;
			for(k = j + 1; k < n; ++k)
				t += a.values[j][k] * x.values[k][0];
			x.values[j][0] = (b.values[j][0] - t) / a.values[j][j];
		}
		return x;
	}
	
}
