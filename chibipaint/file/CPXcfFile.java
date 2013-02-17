package chibipaint.file;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.swing.filechooser.FileNameExtensionFilter;
// TODO: make abstract class to serve as parent for all file classes + possibly add ones for simple images (line PNG) too

import chibipaint.engine.CPArtwork;
import chibipaint.engine.CPLayer;

public class CPXcfFile extends CPAbstractFile {
	protected static final byte gimp_xcf_[] = { 'g', 'i', 'm', 'p', ' ', 'x', 'c', 'f', ' '};
	protected static final byte v0[] = { 'f', 'i', 'l', 'e'};
	protected static final byte v2[] = { 'v', '0', '0', '2'};



	public boolean write(OutputStream os, CPArtwork a)
	{
		if (! (os instanceof FileOutputStream)) // For now only FileOutputStream supported for that kind of file
			return false;
		FileOutputStream fos = (FileOutputStream) os;
		try {
			writeMagic (fos);
			writeVersion (fos, minVersion (a));
			writeInt (fos, a.width);  // image width
			writeInt (fos, a.height); // image height
			writeInt (fos, 0); // RGB Color - mode, other modes don't concern us
			// Image properties
			writeProperties (fos);
			// LayerLinks
			int[] layerLinks = new int[a.getLayersVector().size ()];
			int layerLinksPoisiton = (int) fos.getChannel ().position ();
			// dummy links to fill later
			for (int i = 0; i < a.getLayersVector().size (); i++)
				writeInt (fos, 0);
			writeInt (fos, 0); // Terminator int for layers
			// Channels (?)
			writeInt (fos, 0); // Terminator int for channels
			// Layers
			for (int i = 0; i < a.getLayersVector().size (); i++)
			{
				layerLinks[i] = (int) fos.getChannel ().position ();
				int usedLayer = a.getLayersVector().size () - 1 - i; // Since layers are reversed in xcf
				writeLayer (fos, a.getLayersVector().get(usedLayer), a.getActiveLayerNum() == usedLayer);
			}

			long actualPosition = fos.getChannel ().position ();
			fos.getChannel ().position (layerLinksPoisiton);
			for (int i = 0; i < a.getLayersVector().size (); i++)
				writeInt (fos, layerLinks[i]);

			fos.getChannel ().position (actualPosition);

			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	static public int minVersion (CPArtwork a)
	{
		for (int i = 0; i < a.getLayersVector().size (); i++)
			if (a.getLayersVector().get(i).getBlendMode() == CPLayer.LM_SOFTLIGHT)
				return 2;
		return 0;
	}

	// TODO:These functions should be outside of these classes:
	static public void writeInt(FileOutputStream fos, int i) throws IOException {
		byte[] temp = { (byte) (i >>> 24), (byte) ((i >>> 16) & 0xff), (byte) ((i >>> 8) & 0xff), (byte) (i & 0xff) };
		fos.write(temp);
	}

	static public void writeString (FileOutputStream fos, String s) throws IOException
	{
		byte[] s_data = s.getBytes ("UTF-8"); // converting to utf-8
		writeInt (fos, s_data.length + 1); // size of bytes + 1 for terminating byte
		fos.write(s_data);
		fos.write(0); // null-terminating byte
	}

	static public void writeFloat (FileOutputStream fos, float f) throws IOException
	{
		writeInt (fos, Float.floatToRawIntBits (f));
	}

	// End of todo

	static public void writeProperties(FileOutputStream fos) throws IOException {
		// Compression, using default one - RLE
		writeInt (fos, 17); // PROP_COMPRESSION
		writeInt (fos, 1); // payload size
		fos.write (1); // Means RLE

		writeInt (fos, 19); // PROP_RESOLUTION
		writeInt (fos, 8); // 2 Floats payload
		writeFloat (fos, 72.0f);
		writeFloat (fos, 72.0f);

		writeInt (fos, 0); // PROP_END
		writeInt (fos, 0); // empty payload
	}

	static public void writeLayer(FileOutputStream fos, CPLayer layer, boolean isActive) throws IOException {
		writeInt (fos, layer.getWidth()); // layer width // Well in our case they are the as picture all the time
		writeInt (fos, layer.getHeight()); // layer height
		writeInt (fos, 1); // layer type, 1 - means 24 bit color with alpha
		writeString (fos, layer.name); // layer name
		// Layer properties

		// Is Layer Active
		if (isActive) // TODO:implement this stuff in chi also (very convinient)
		{
			writeInt (fos, 2); // PROP_ACTIVE_LAYER
			writeInt (fos, 0); // empty payload
		}

		// Layer Mode
		writeInt (fos, 7); // PROP_MODE
		writeInt (fos, 4); // payload size, 1 int
		switch (layer.blendMode) {
		case CPLayer.LM_NORMAL:
			writeInt (fos, 0); // 0: Normal
			break;

		case CPLayer.LM_MULTIPLY:
			writeInt (fos, 3); // 3: Multiply
			break;

		case CPLayer.LM_ADD:
			writeInt (fos, 7); // 7: Addition
			break;

		case CPLayer.LM_SCREEN:
			writeInt (fos, 4); // 4: Screen
			break;

		case CPLayer.LM_LIGHTEN:
			writeInt (fos, 10); // 10: Lighten Only
			break;

		case CPLayer.LM_DARKEN:
			writeInt (fos, 9); // 9: Darken Only
			break;

		case CPLayer.LM_SUBTRACT:
			writeInt (fos, 8); // 8: Subtract
			break;

		case CPLayer.LM_DODGE:
			writeInt (fos, 16); // 16: Dodge
			break;

		case CPLayer.LM_BURN:
			writeInt (fos, 17); // 17: Burn
			break;

		case CPLayer.LM_OVERLAY:
			writeInt (fos, 5); // 5: Overlay
			break;

		case CPLayer.LM_HARDLIGHT:
			writeInt (fos, 18); // 18: Hard Light
			break;

		case CPLayer.LM_SOFTLIGHT:
			writeInt (fos, 19); // 19: Soft Light (XCF version >= 2 only)
			break;
			// TODO: Three below looks like missing in gimp, maybe I should check formulas to investigate it
		case CPLayer.LM_VIVIDLIGHT:
			writeInt (fos, 18); // 18: Hard Light
			break;

		case CPLayer.LM_LINEARLIGHT:
			writeInt (fos, 18); // 18: Hard Light
			break;

		case CPLayer.LM_PINLIGHT:
			writeInt (fos, 18); // 18: Hard Light
			break;
		}

		// Offsets in our case it's dummy 0s

		writeInt (fos, 15); // PROP_OFFSETS
		writeInt (fos, 8); // Two ints
		writeInt (fos, 0);
		writeInt (fos, 0);

		// Layer opacity:

		writeInt (fos, 6); // PROP_OPACITY
		writeInt (fos, 4); // one Int
		writeInt (fos, layer.getAlpha () * 255 / 100);

		// Layer visibility:

		writeInt (fos, 8); // PROP_VISIBLE
		writeInt (fos, 4); // one Int
		writeInt (fos, layer.visible ? 1 : 0);

		writeInt (fos, 0); // PROP_END
		writeInt (fos, 0); // empty payload

		// Pixel Data
		writeInt (fos, (int)fos.getChannel().position() + 8); // After this and 0 for mask goes hierarchy structure

		// Mask (in our case 0)
		writeInt (fos, 0);

		//Hierarchy Structure
		writeInt (fos, layer.getWidth()); // Once again width
		writeInt (fos, layer.getHeight()); // Once again height
		writeInt (fos, 4); // Bytes per pixel
		// Now we need to calculate how many level structures will be there

		// now we have to write 1 + dummyLevelsCount int pointer then 3 ints for each dummy level then real level
		int curPos = (int) fos.getChannel ().position();
		writeInt (fos, curPos + 4 + 4); // The pointer to structure beyond this pointer and terminating zero
		writeInt (fos, 0); // Terminating zero for levels

		// Actual level
		writeInt (fos, layer.getWidth());
		writeInt (fos, layer.getHeight());
		// Now there goes some pointers for tile data
		int wTiles = (int) (Math.ceil ((double )layer.getWidth() / 64));
		int hTiles = (int) (Math.ceil ((double )layer.getHeight() / 64));
		int numberOfTiles = wTiles * hTiles;
		int[] tilePointers = new int[numberOfTiles];
		int pointerPos = (int) fos.getChannel ().position(); // Remembering position to fill it up later
		for (int i = 0; i < numberOfTiles; i++)
			writeInt (fos, 0); // Just reserving places for future writing of actual pointer

		writeInt (fos, 0); // Terminating zero;
		// Then we're starting to write actual tiles
		for (int i = 0; i < hTiles; i++) // outer loop is vertical one
			for (int j = 0; j < wTiles; j++)
			{
				tilePointers[i * wTiles + j] = (int) fos.getChannel ().position();
				writeTileRLE (fos, layer.data, j, i, layer.getWidth(), layer.getHeight()); // Writing actual tile Info
			}

		int actualPos = (int) fos.getChannel ().position();

		fos.getChannel ().position (pointerPos);
		for (int i = 0; i < numberOfTiles; i++)
			writeInt (fos, tilePointers[i]);

		fos.getChannel ().position (actualPos);
	}

	static public byte getByte (int x, int pos)
	{
		return (byte) ((x >> ((3 - pos) * 8)) & 0xFF);
	}

	static public void writeTileRLE (FileOutputStream fos, int[] data, int x, int y, int w, int h)  throws IOException
	{
		int sizeX = (x + 1) * 64 <= w ? 64 : w - x * 64;
		int sizeY = (y + 1) * 64 <= h ? 64 : h - y * 64;
		int m = sizeX * sizeY;
		byte[] arr = new byte[m]; // uncompressed info
		for (int p = 1; p < 5; p++)
		{
			int k = p % 4; // To write in order r-g-b alpha
			int t = 0;
			for (int i = 0; i < sizeY; i++)
				for (int j = 0; j < sizeX; j++)
				{
					arr[t] = getByte (data [(y * 64 + i) * w + (x * 64 + j)], k);
					t++;
				}
			int curPos = 0;

			while (curPos < m)
			{
				// scanning for identical bytes
				if (curPos + 1 < m && arr[curPos] == arr[curPos + 1])
				{
					int pos = curPos + 2;
					while (pos < m && arr[pos] == arr[curPos])
						pos++;
					pos--; // getting back where we were still identical / in array boundaries
					// writing (pos - curPos + 1) identical bytes, 2 cases
					if (pos - curPos <= 126) // short run of identical bytes
					{
						fos.write (pos - curPos); // -1, cause value will be repeated n+1 times actually
						fos.write (arr[curPos]);
					}
					else  // long run of identical bytes
					{
						fos.write (127);
						int count = pos - curPos + 1;
						fos.write (getByte (count, 2));
						fos.write (getByte (count, 3));
						fos.write (arr[curPos]);
					}
					curPos = pos + 1;
				}
				if (curPos >= m)
					break;
				// scanning for different bytes
				if (curPos + 1 >= m || arr[curPos] != arr[curPos + 1])
				{
					int pos = 0;
					if (curPos + 1 >= m)
						pos = m - 1;
					else
					{
						pos = curPos + 1;
						while (pos + 1 < m && arr[pos] != arr[pos + 1])
							pos++;
						if (pos != m - 1)
							pos--;
						// getting back where we were still different
					}

					// writing (pos - curPos + 1) different bytes, 2 cases
					if (pos - curPos + 1 <= 127) // short run of different bytes
					{
						fos.write (256 - (pos - curPos + 1));
						for (int i = curPos; i <= pos; i++)
							fos.write (arr[i]);
					}
					else  // long run of identical bytes
					{
						fos.write (128);
						int count = pos - curPos + 1;
						fos.write (getByte (count, 2));
						fos.write (getByte (count, 3));
						for (int i = curPos; i <= pos; i++)
							fos.write (arr[i]);
					}
					curPos = pos + 1;
				}
			}
		}
	}

	static public void writeMagic (FileOutputStream fos) throws IOException
	{
		fos.write (gimp_xcf_);
	}

	static public void writeVersion (FileOutputStream fos, int minVersion) throws IOException
	{
		// For now let's try version 0
		switch (minVersion)
		{
		case 0:
			fos.write (v0);
			break;
		case 2:
			fos.write (v2);
			break;
		}
		fos.write (0); // zero-terminator byte for version
	}

	public CPArtwork read(InputStream is)
	{
		return null; // Warning - For now it's a stub
	}

	@Override
	public String ext() {
		return "XCF";
	}

	@Override
	public FileNameExtensionFilter fileFilter ()
	{
		return new FileNameExtensionFilter ("GIMP XCF Images (.xcf)", "xcf");
	}

	@Override
	public boolean isNative ()
	{
		return false;
	}
}

