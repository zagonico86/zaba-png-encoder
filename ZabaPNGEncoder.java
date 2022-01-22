/**
  * MIT License
  * 
  * Copyright (c) 2022 Nicola Zago, zagonico.com
  * 
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  * 
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  * 
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;


public class ZabaPNGEncoder
{
	int filter;
	String fileName;
	FileOutputStream out;
	public static int MODE_RGB = 1, MODE_BLACK_WHITE = 2, MODE_SEPIA = 3;
	int colorMode;
	boolean isStart, isData, isEnd;
	int compressionStrategy, compressionLevel;
	int filterMode;
	int h,w;
	int defaultDataChunkSize;
	long dataCrc;
	Deflater compressor;
	ByteArrayOutputStream outBytes;
	DeflaterOutputStream compBytes;

	/* Initialize a new Encoder */
	public ZabaPNGEncoder(String file, int w, int h) 
	{
		init(file);
		setImageSize(w,h);
	}


	private void init(String file)
	{
		crcTable = makeCRCtable();

		fileName = file;

		setMode(MODE_RGB);
		setFilter(false);
		setCompressionLevel(0);
		defaultDataChunkSize = 32768;

		isStart = false;
		isData = false;
		isEnd = false;
	}

	public ZabaPNGEncoder(String file)
	{
		init(file);
	}

	/* color / gray */
	public boolean setMode(int n)
	{
		if (isStart) return false;
		colorMode = n;

		return true;
	}

	/* compression level */
	public void setCompressionLevel(int nl)
	{
		compressionLevel = nl;
	}

	/* filter data before compression */
	public void setFilter(boolean enable)
	{
		if (enable) {
			filter = 1;
			compressionStrategy = Deflater.FILTERED;
		}
		else {
			filter = 0;
			compressionStrategy = Deflater.BEST_SPEED;    		
		}
	}

	/* set width and height */
	public boolean setImageSize(int newW, int newH)
	{
		if (newW<1 || newH<1 || isStart) return false;

		w = newW; h = newH;
		return true;
	}

	/* start writing the image */
	public boolean start() throws Exception
	{
		if (isStart) return false;

		try
		{
			out = new FileOutputStream(fileName);
			if (!writePNGHeader()) return false;
			if (!writeIHDR()) return false;
			isStart=true;

			/* inizializzo flusso compressione data */
			compressor = new Deflater(compressionLevel);
			compressor.setStrategy(compressionStrategy);

			// sliding window for zlib should be at most 32768 bytes
			outBytes = new ByteArrayOutputStream(defaultDataChunkSize);

			compBytes = new DeflaterOutputStream(outBytes, compressor, defaultDataChunkSize);
		}
		catch(Exception e)
		{
			throw new Exception(""+e+": "+e.getMessage());
		}

		return true;
	}

	/* start writing the image */
	public boolean end() throws Exception
	{
		if (!isStart) return false;

		try
		{
			writeIEND();
			out.flush();
			out.close();
			isEnd = true;
		}
		catch(Exception e)
		{
			throw new Exception(""+e+": "+e.getMessage());
		}
		return true;
	}

	/* AE 42 60 82 
	// test case: draw a 6000x4000 color scale
    public static void main(String[] args)
    {
        ZabaPNGEncoder ze = new ZabaPNGEncoder("prova.png",6000,4000);
        ze.setMode(MODE_RGB);
        ze.setFilter(true);
        ze.setCompressionLevel(6);
        try
        {
            ze.start();
            for (int i=0; i<40; i++)
            {
                int[] data = new int[6000*100];
                int temp = 0xFF000000 | (((0xFF*i/40)<<8));
                for (int ji=0;ji<data.length; ji++) data[ji] = temp*((ji%6000+ji/6000)%2);

                ze.writeData(data, (i==39));
            }
            ze.end();
        } catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }//*/

	/***************************  CRC Management **************************/
	/* Table of CRCs of all 8-bit messages. */
	private long[] crcTable;

	/* Make the table for a fast CRC. */
	private long[] makeCRCtable()
	{
		long[] table = new long[256];
		long c;
		int n, k;

		for (n = 0; n < 256; n++)
		{
			c = n;
			for (k = 0; k < 8; k++)
			{
				if ( (c&1) == 1 )
					c = 0xedb88320L ^ (c >>> 1);
				else
					c = c >>> 1;
			}
			table[n] = c;
		}
		return table;
	}

	/* Update a running CRC with the bytes buf[0..len-1]--the CRC
      should be initialized to all 1's, and the transmitted value
      is the 1's complement of the final running CRC (see the
      crc() routine below)). */
	public long updateCrc(long crc, byte[] buf, int start, int len)
	{
		long c = crc;

		while (--len >= 0)
			c = crcTable[(int)(c ^ buf[start++]) & 0xff] ^ (c >>> 8);

		return c;
	}

	/* Return the CRC of the bytes buf[0..length-1]. */
	public long crc(byte[] buf)
	{
		return updateCrc(0xffffffffL, buf, 0, buf.length) ^ 0xffffffffL;
	}

	/* Return the CRC of the bytes buf[start..start+len-1]. */
	public long crc(byte[] buf, int start, int len)
	{
		return updateCrc(0xffffffffL, buf, start, len) ^ 0xffffffffL;
	}
	/**********************************************************************/

	/* write a number in array[] from position start */
	public void setNumber(int number, byte[] array, int start)
	{
		array[start+3] = (byte)number; number>>>=8;
		array[start+2] = (byte)number; number>>>=8;
		array[start+1] = (byte)number; number>>>=8;
		array[start] = (byte)number;
	}

	/* get the first 8 bytes of a png header */
	private boolean writePNGHeader()
	{
		byte[] head = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
		try
		{
			out.write(head);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public boolean writeChunk(String type, byte[] data, int start, int len)
	{
		byte[] n = new byte[len-start];
		System.arraycopy(data, start, n, 0, len);

		return writeChunk(type, n);
	}

	/* write a chunk of the specified type and data */
	public boolean writeChunk(String type, byte[] data)
	{
		try
		{
			byte[] temp = new byte[4];
			setNumber(data.length, temp, 0);
			out.write(temp);
			out.write(type.getBytes());
			out.write(data);
			long crcChunk = updateCrc(0xffffffffL, type.getBytes(), 0, 4);
			crcChunk = updateCrc(crcChunk, data, 0, data.length) ^ 0xffffffffL;
			setNumber((int)crcChunk, temp,0);
			out.write(temp);
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}

	/* write a chunk of the specified type and data */
	public boolean writeChunks(String type, byte[] data, int size)
	{
		try
		{
			byte[] temp = new byte[4];
			for (int i=0; i<data.length; i+=size)
			{
				int currSize = Math.min(size, data.length-i);
				setNumber(currSize, temp, 0);
				out.write(temp);
				out.write(type.getBytes());
				out.write(data, i, currSize);
				long crcChunk = updateCrc(0xffffffffL, type.getBytes(), 0, 4);
				crcChunk = updateCrc(crcChunk, data, i, currSize) ^ 0xffffffffL;
				setNumber((int)crcChunk, temp, 0);
				out.write(temp);
			}
		}
		catch (Exception e)
		{
			return false;
		}

		return true;
	}

	/* return a IHDR chunk respecting the specified parameters */
	private boolean writeIHDR()
	{
		byte[] data = new byte[13];

		setNumber(w, data, 0);
		setNumber(h, data, 4);
		// if (colorMode != GRAY_MODE)
		// {
		data[8] = (byte) 8;
		data[9] = (byte) 2;
		// }
		// else {
		// 	data[8] = (byte) 8;
		//     data[9] = (byte) 0;
		// }
		// not used
		// data[10] = 0;
		// data[11] = 0;
		// data[12] = 0;

		return writeChunk("IHDR", data);
	}

	/* return the IEND chunk */
	private boolean writeIEND()
	{
		return writeChunk("IEND", new byte[0]);
	}

	/* return the raw data for the png */
	private byte[] getRawData(int[] data)
	{
		if (colorMode == MODE_RGB) {
			byte[] raw_data = new byte[3*data.length + data.length/w];

			int rows = data.length/w;
			int iData=0, iRaw=0;

			for (int i=0; i<rows; i++)
			{
				raw_data[iRaw++] = (byte)filterMode;
				// if filterMode == 0
						for (int k=0; k<w; k++, iData++, iRaw+=3)
						{
							raw_data[iRaw] = (byte)(data[iData]>>16);
							raw_data[iRaw+1] = (byte)(data[iData]>>8);
							raw_data[iRaw+2] = (byte)data[iData];
						}
			}

			return raw_data;
		}
		else if (colorMode == MODE_SEPIA)
		{
			byte[] raw_data = new byte[3*data.length + data.length/w];

			int rows = data.length/w;
			int iData=0, iRaw=0;
			int r,g,b;

			for (int i=0; i<rows; i++)
			{
				raw_data[iRaw++] = (byte)filterMode;
				// if filterMode == 0
						for (int k=0; k<w; k++, iData++, iRaw+=3)
						{
							r = (data[iData]>>16)&255;
							g = (data[iData]>>8)&255;
							b = data[iData]&255;

							raw_data[iRaw] = (byte) Math.min((100*r+196*g+48*b)>>8, 255);
							raw_data[iRaw+1] = (byte) Math.min((89*r+175*g+43*b)>>8, 255);
							raw_data[iRaw+2] = (byte) ((69*r+136*g+33*b)>>8);
						}
			}

			return raw_data;
		}

		byte[] raw_data = new byte[3*data.length + data.length/w];

		int rows = data.length/w;
		int r,g,b;
		int iData=0, iRaw=0;

		for (int i=0; i<rows; i++)
		{
			raw_data[iRaw++] = (byte)filterMode;
			// if filterMode == 0
			for (int k=0; k<w; k++, iData++, iRaw+=3)
			{
				r = (data[iData]>>16)&255;
				g = (data[iData]>>8)&255;
				b = data[iData]&255;

				raw_data[iRaw] = raw_data[iRaw+1] = raw_data[iRaw+2] = 
						(byte) ((r*76 + g*150 + b*29)>>8);		//((r*76 + 150*g +29*b)>>8)
			}
		}

		return raw_data;
	}

	/* aggiungo data */
	public boolean writeData(int[] data, boolean last)
	{
		// dati da encodare, con già il flag iniziale per il compressore e
		// quelli per il filter a inizio di ogni riga
		byte[] raw_data = getRawData(data);

		try
		{
			compBytes.write(raw_data, 0, raw_data.length);
			if (last) compBytes.finish();
			if ( (outBytes.size() > defaultDataChunkSize) || (last) )
			{
				writeChunks("IDAT", outBytes.toByteArray(), defaultDataChunkSize);
				outBytes.reset();
			}

			return true;
		}
		catch (Exception e)
		{
			System.out.println(e+": "+e.getMessage());
		}

		return false;
	}
}
