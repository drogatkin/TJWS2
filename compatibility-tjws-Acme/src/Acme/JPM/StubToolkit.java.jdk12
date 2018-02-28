// StubToolkit - bare-bones Toolkit for non-GUI applications
//
// Original copyright notice:
//
// Copyright (c) 1995 Sun Microsystems, Inc. All Rights Reserved.
//
// Permission to use, copy, modify, and distribute this software
// and its documentation for NON-COMMERCIAL purposes and without
// fee is hereby granted provided that this copyright notice
// appears in all copies. Please refer to the file "copyright.html"
// for further important copyright and licensing information.
//
// SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
// THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
// TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
// PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
// ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
// DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

package Acme.JPM;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.peer.*;
import java.awt.image.*;
import java.net.*;
import sun.awt.image.FileImageSource;
import sun.awt.image.OffScreenImageSource;
import sun.awt.image.ImageRepresentation;

/// Bare-bones Toolkit for non-GUI applications.
// <P>
// This Toolkit is for doing image manipulation in a non-GUI application.
// <P>
// You could do a Toolkit.getDefaultToolkit() and use that from your non-GUI
// applications.  The two problems with that are it takes way too long to
// initialize, and it starts a couple of daemon Threads that keep your
// application from exiting.  Instead, do a new Acme.JPM.StubToolkit().
// <P>
// The only public method of interest is the constructor.
// <P>
// Adapted by Jef Poskanzer <jef@mail.acme.com> from:
// <BLOCKQUOTE><PRE>
// TinyToolkit.java   1.10 95/12/14 Arthur van Hoff
// TinyImage.java     1.4 95/12/09 Arthur van Hoff
// TinyGraphics.java  1.10 95/12/04 Arthur van Hoff
// </PRE></BLOCKQUOTE>
// <P>
// <A HREF="/resources/classes/Acme/JPM/StubToolkit.java">Fetch the software.</A><BR>
// <A HREF="/resources/classes/Acme.tar.gz">Fetch the entire Acme package.</A>

public class StubToolkit extends Toolkit
    {

    static
	{
	// Load C library to get GIF-parsing routine.  Dunno how portable
	// this is to non-Solaris machines.
	System.loadLibrary("awt");
	}

    public StubToolkit()
	{
	}

    // Fail to create peer objects.
    public WindowPeer createWindow( Window target )
	{
	throw new InternalError( "not implemented" );
	}
    public FramePeer createFrame( Frame target )
	{
	throw new InternalError( "not implemented" );
	}
    public CanvasPeer createCanvas( Canvas target )
	{
	throw new InternalError( "not implemented" );
	}
    public PanelPeer createPanel( Panel target )
	{
	throw new InternalError( "not implemented" );
	}
    public ButtonPeer createButton( Button target )
	{
	throw new InternalError( "not implemented" );
	}
    public TextFieldPeer createTextField( TextField target )
	{
	throw new InternalError( "not implemented" );
	}
    public ChoicePeer createChoice( Choice target )
	{
	throw new InternalError( "not implemented" );
	}
    public LabelPeer createLabel( Label target )
	{
	throw new InternalError( "not implemented" );
	}
    public ListPeer createList( java.awt.List target )
	{
	throw new InternalError( "not implemented" );
	}
    public CheckboxPeer createCheckbox( Checkbox target )
	{
	throw new InternalError( "not implemented" );
	}
    public ScrollbarPeer createScrollbar( Scrollbar target )
	{
	throw new InternalError( "not implemented" );
	}
    public ScrollPanePeer createScrollPane( ScrollPane target )
	{
	throw new InternalError( "not implemented" );
	}
    public TextAreaPeer createTextArea( TextArea target )
	{
	throw new InternalError( "not implemented" );
	}
    public DialogPeer createDialog( Dialog target )
	{
	throw new InternalError( "not implemented" );
	}
    public FileDialogPeer createFileDialog( FileDialog target )
	{
	throw new InternalError( "not implemented" );
	}
    public MenuBarPeer createMenuBar( MenuBar target )
	{
	throw new InternalError( "not implemented" );
	}
    public MenuPeer createMenu( Menu target )
	{
	throw new InternalError( "not implemented" );
	}
    public PopupMenuPeer createPopupMenu( PopupMenu target )
	{
	throw new InternalError( "not implemented" );
	}
    public MenuItemPeer createMenuItem( MenuItem target )
	{
	throw new InternalError( "not implemented" );
	}
    public CheckboxMenuItemPeer createCheckboxMenuItem( CheckboxMenuItem target )
	{
	throw new InternalError( "not implemented" );
	}
    public FontPeer getFontPeer( String name, int style )
	{
	throw new InternalError( "not implemented" );
	}

    public Dimension getScreenSize()
	{
	return new Dimension( getScreenWidth(), getScreenHeight() );
	}

    static ColorModel colorModel = null;

    public synchronized ColorModel getColorModel()
	{
	if ( colorModel == null )
	    colorModel = ColorModel.getRGBdefault();
	return colorModel;
	}
    
    static Toolkit toolkit = null;

    public static synchronized Toolkit getDefaultToolkit()
	{
	if ( toolkit == null )
	    toolkit = new StubToolkit();
	return toolkit;
	}

    public int getScreenResolution()
	{
	throw new InternalError( "not implemented" );
	}
    int getScreenWidth()
	{
	throw new InternalError( "not implemented" );
	}
    int getScreenHeight()
	{
	throw new InternalError( "not implemented" );
	}
    public String[] getFontList()
	{
	throw new InternalError( "not implemented" );
	}
    public FontMetrics getFontMetrics( Font font )
	{
	throw new InternalError( "not implemented" );
	}

    public void sync()
	{
	}

    static Hashtable imgHash = new Hashtable();

    static synchronized Image getImageFromHash( Toolkit tk, URL url )
	{
	SecurityManager security = System.getSecurityManager();
	if ( security != null )
	    security.checkConnect( url.getHost(), url.getPort() );
	Image img = (Image) imgHash.get( url );
	if ( img == null )
	    {
	    try
		{
		img = tk.createImage( (ImageProducer) url.getContent() );
		imgHash.put( url, img );
		}
	    catch ( Exception e )
		{}
	    }
	return img;
	}

    static synchronized Image getImageFromHash( Toolkit tk, String filename )
	{
	SecurityManager security = System.getSecurityManager();
	if ( security != null )
	    security.checkRead( filename );
	Image img = (Image) imgHash.get( filename );
	if ( img == null )
	    {
	    try
		{
		img = tk.createImage( new FileImageSource( filename ) );
		imgHash.put( filename, img );
		}
	    catch ( Exception e )
		{}
	    }
	return img;
	}

    public Image getImage( String filename )
	{
	return getImageFromHash( this, filename );
	}

    public Image getImage( URL url )
	{
	return getImageFromHash( this, url );
	}

    static boolean prepareScrImage( Image img, int w, int h, ImageObserver o )
	{
	if ( w == 0 || h == 0 )
	    return true;
	StubImage ximg = (StubImage) img;
	if ( ximg.hasError() )
	    {
	    if ( o != null )
		o.imageUpdate( img, ImageObserver.ERROR|ImageObserver.ABORT,
			      -1, -1, -1, -1 );
	    return false;
	    }
	if ( w < 0 ) w = -1;
	if ( h < 0 ) h = -1;
	ImageRepresentation ir = ximg.getImageRep();
	return ir.prepare( o );
	}

    static int checkScrImage( Image img, int w, int h, ImageObserver o )
	{
	StubImage ximg = (StubImage) img;
	int repbits;
	if ( w == 0 || h == 0 )
	    repbits = ImageObserver.ALLBITS;
	else
	    {
	    if ( w < 0 ) w = -1;
	    if ( h < 0 ) h = -1;
	    repbits = ximg.getImageRep().check( o );
	    }
	return ximg.check( o ) | repbits;
	}

    public int checkImage( Image img, int w, int h, ImageObserver o )
	{
	return checkScrImage( img, w, h, o );
	}

    public boolean prepareImage( Image img, int w, int h, ImageObserver o )
	{
	return prepareScrImage( img, w, h, o );
	}

    public Image createImage( ImageProducer producer )
	{
	return new StubImage( producer );
	}
    public Image createImage( byte[] imagedata, int imageoffset, int imagelength)
	{
	/*
	return new StubImage( imagedata, imageoffset, imagelength );
	*/
	throw new InternalError( "not implemented" );
	}
    
    public PrintJob getPrintJob( Frame frame, String jobtitle, Properties props )
	{
	throw new InternalError( "not implemented" );
	}
    public void beep()
	{
	( new Acme.SynthAudioClip( 440, 250 ) ).play();
	}
    public java.awt.datatransfer.Clipboard getSystemClipboard()
	{
	throw new InternalError( "not implemented" );
	}
    public java.awt.EventQueue getSystemEventQueueImpl()
	{
	throw new InternalError( "not implemented" );
	}
    public java.awt.dnd.peer.DragSourceContextPeer createDragSourceContextPeer( java.awt.dnd.DragSource ds, java.awt.Component c )
	{
	throw new InternalError( "not implemented" );
	}

    }


class StubImage extends sun.awt.image.Image
    {

    /*
    // Construct an image from image data.
    public StubImage( byte[] imagedata, int imageoffset, int imagelength )
	{
	super( imagedata, imageoffset, imagelength );
	}
    */

    // Construct an image from an ImageProducer object.
    public StubImage( ImageProducer producer )
	{
	super( producer );
	}

    public Graphics getGraphics()
	{
	return new StubGraphics( this );
	}

    protected sun.awt.image.ImageRepresentation getImageRep()
	{
	return super.getImageRep();
	}

    protected sun.awt.image.ImageRepresentation makeImageRep()
	{
	return null;
	}

    }


class StubGraphics extends Graphics {

    private void imageCreate( ImageRepresentation ir )
	{
	// !!!
	throw new InternalError( "not implemented" );
	}

    public StubGraphics( Graphics g )
	{
	}

    public StubGraphics( Image image )
	{
	// !!!
	throw new InternalError( "not implemented" );
	}

    // Create a new Graphics Object based on this one.
    public Graphics create()
	{
	StubGraphics g = new StubGraphics( this );
	return g;
	}

    public void translate( int x, int y )
	{
	throw new InternalError( "not implemented" );
	}

    public void dispose()
	{
	// Nothing.
	}

    public Font getFont()
	{
	throw new InternalError( "not implemented" );
	}
    public void setFont( Font font )
	{
	throw new InternalError( "not implemented" );
	}
    public FontMetrics getFontMetrics( Font font )
	{
	throw new InternalError( "not implemented" );
	}
    public Color getColor()
	{
	throw new InternalError( "not implemented" );
	}
    public void setColor( Color c )
	{
	throw new InternalError( "not implemented" );
	}
    public void setPaintMode()
	{
	throw new InternalError( "not implemented" );
	}
    public void setXORMode( Color c )
	{
	throw new InternalError( "not implemented" );
	}

    public Rectangle getClipRect()
	{
	throw new InternalError( "not implemented" );
	}
    public Rectangle getClipBounds()
	{
	throw new InternalError( "not implemented" );
	}
    public void clipRect( int x, int y, int w, int h )
	{
	throw new InternalError( "not implemented" );
	}
    public void setClip( int x, int y, int w, int h )
	{
	throw new InternalError( "not implemented" );
	}
    public Shape getClip()
	{
	throw new InternalError( "not implemented" );
	}
    public void setClip( Shape clip )
	{
	throw new InternalError( "not implemented" );
	}
    public void drawLine( int x1, int y1, int x2, int y2 )
	{
	throw new InternalError( "not implemented" );
	}
    public void fillRect( int x, int y, int w, int h )
	{
	throw new InternalError( "not implemented" );
	}
    public void clearRect( int x, int y, int w, int h )
	{
	throw new InternalError( "not implemented" );
	}
    public void drawString( String str, int x, int y )
	{
	throw new InternalError( "not implemented" );
	}
    public void drawChars( char[] data, int offset, int length, int x, int y )
	{
	throw new InternalError( "not implemented" );
	}
    public void drawBytes( byte[] data, int offset, int length, int x, int y )
	{
	throw new InternalError( "not implemented" );
	}
    public boolean drawImage( Image img, int x, int y, ImageObserver observer )
	{
	throw new InternalError( "not implemented" );
	}
    public boolean drawImage(
	Image img, int x, int y, int width, int height, ImageObserver observer )
	{
	throw new InternalError( "not implemented" );
	}
    public boolean drawImage(
	Image img, int x, int y, Color bg, ImageObserver observer )
	{
	throw new InternalError( "not implemented" );
	}
    public boolean drawImage(
	Image img, int x, int y, int width, int height, Color bg,
	ImageObserver observer )
	{
	throw new InternalError( "not implemented" );
	}
    public boolean drawImage(
        Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1,
        int sx2, int sy2, ImageObserver observer )
        {
        throw new InternalError( "not implemented" );
        }
    public boolean drawImage(
        Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1,
        int sx2, int sy2, Color bgcolor, ImageObserver observer)
        {
        throw new InternalError( "not implemented" );
        }
 
    public void copyArea( int x, int y, int w, int h, int dx, int dy )
	{
	throw new InternalError( "not implemented" );
	}
    public void drawRoundRect( int x, int y, int w, int h, int arcWidth, int arcHeight )
	{
	throw new InternalError( "not implemented" );
	}
    public void fillRoundRect( int x, int y, int w, int h, int arcWidth, int arcHeight )
	{
	throw new InternalError( "not implemented" );
	}
    public void drawPolygon( int[] xPoints, int[] yPoints, int nPoints )
	{
	throw new InternalError( "not implemented" );
	}
    public void fillPolygon( int[] xPoints, int[] yPoints, int nPoints )
	{
	throw new InternalError( "not implemented" );
	}
    public void drawOval( int x, int y, int w, int h )
	{
	throw new InternalError( "not implemented" );
	}
    public void fillOval( int x, int y, int w, int h )
	{
	throw new InternalError( "not implemented" );
	}
    public void drawArc( int x, int y, int w, int h, int startAngle, int endAngle )
	{
	throw new InternalError( "not implemented" );
	}
    public void fillArc( int x, int y, int w, int h, int startAngle, int endAngle )
	{
	throw new InternalError( "not implemented" );
	}
    public void drawPolyline( int[] xPoints, int[] yPoints, int nPoints )
	{
	throw new InternalError( "not implemented" );
	}

    }
