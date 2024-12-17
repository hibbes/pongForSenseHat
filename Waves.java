import java.io.* ;

/**
 * Displays patterns on the LEDs of the Sense Hat
 * for the Raspberry Pi
 *
 * @version 0.01
 * @author  P. Tellenbach
 */
public class Waves
{
   // Definions in Java byte order !
   public static final int BLACK = 0x0000 ;
   public static final int RED   = 0x0038 ;
   public static final int GREEN = 0xE001 ;
   public static final int BLUE  = 0x0e00 ;

   private static int[] COLORS = { RED, GREEN, BLUE } ;

   int[][] pixels ;

   public Waves()
   {
      pixels = new int[8][8] ;
   }

   /**
    *  Copies the pixels to the frame buffer
    */
   public void displayPixels()
   {
      try
      {
         FileOutputStream fos = new FileOutputStream( "/dev/fb0" ) ;
         DataOutputStream os  = new DataOutputStream( fos ) ;

         for( int row = 7; row >= 0; row-- )
         {
            for( int col = 0; col < 8; col++ )
            {
               os.writeShort( pixels[row][col] ) ;
            }
         }

         os.close() ;
         fos.close() ;
      }
      catch( IOException e )
      {
         System.out.println( e.getMessage() ) ;
      }
   }

   /**
    * Clears an area in the center of the pixel array
    *
    * @param size The size of the area
    */
   public void clear( int size )
   {
      int start = (8 - size) / 2 ;

      for( int r = 0; r < size; r++ )
      {
         for( int c = 0; c < size; c++ )
         {
             pixels[start + r][start + c] = BLACK ;
         }
      }
   }

   /**
    * Sleep for 100ms
    */
   public void delay()
   {
      try
      {
         Thread.sleep( 100 ) ;
      }
      catch( InterruptedException e )
      {
         System.out.println( e.getMessage() ) ;
      }
   }

   /**
    * Draws a wave on the display
    *
    * @param row   The starting row
    * @param col   The starting column
    * @param len   The length of the lines
    * @param color The color of the pixels
    * @param size  The area to clear before drawing
    */
   public void showWave( int row, int col, int len, int color, int size )
   {
      clear( size ) ;

      pixels[row--][col] = color ;

      for( int p = 1; p < len; p++ )
      {
         pixels[row--][col] = color ;
      }

      for( int p = 0; p < len; p++ )
      {
         pixels[row][col--] = color ;
      }

      for( int p = 0; p < len; p++ )
      {
         pixels[row++][col] = color ;
      }

      for( int p = 0; p < len; p++ )
      {
         pixels[row][col++] = color ;
      }

      displayPixels() ;
      delay() ;
   }

   /**
    * Increment the color index modulo the
    * list size
    *
    * @param index The number to increment
    */
   private int inc( int index )
   {
      if( ++index >= COLORS.length )
      {
         return 0 ;
      }

      return index ;
   }

   /**
    * Shows a few patterns on the pixels
    */
   public void show()
   {
      int index  = 0 ;
      int top    = 7 ;

      for( int len = 8; len > 0; len -= 2 )
      {
         for( int i = 0; i < len; i++ )
            showWave( top, 7 - top + i, i, COLORS[index], len ) ;
         index = inc( index ) ;

         for( int i = 0; i < len; i++ )
            showWave( top, top, i, COLORS[index], len ) ;
         index = inc( index ) ;

         for( int i = 0; i < len; i++ )
            showWave( 7 - top + i, top, i, COLORS[index], len ) ;
         index = inc( index ) ;

         for( int i = 0; i < len; i++ )
            showWave( 7 - top + i, 7 - top + i, i, COLORS[index], len ) ;
         index = inc( index ) ;

         --top ;
      }
   }

   /**
    * The program starts here
    *
    * @param args Ignored
    */
   public static void main( String[] args )
   {
      Waves waves = new Waves() ;
      waves.show() ;
   }
}
