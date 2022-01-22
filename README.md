# ZaBa PNG Encoder
This is a mini Java library to encode very large PNG images. I originally developed it for an ZaBa Photomosaic Android App (no more in  the Play Store) when 16MB memory limit per App was not so uncommon. Yet this library allowed me to create images of hundred of MBs in those devices without problems.

# Methods
* setMode(int): color mode, accepts ZabaPNGEncoder.MODE_RGB, ZabaPNGEncoder.MODE_BLACK_WHITE, ZabaPNGEncoder.MODE_SEPIA
* setFilter(bool): filter data before compression of data (false to best speed, true for best compression)
* setCompressionLevel(int): 0-9: the level of compression. Usually 3 gives already a good compression level without slow down the execution
* start(): initialize the writing of the image 
* writeData(int[] RGBs, bool isLast): array with the pixels to be written (each pixel has format 0xFFrrggbb).
* end(): finalize the image

# Usage example
```
// draw a 6000x4000 color scale
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
}
```

