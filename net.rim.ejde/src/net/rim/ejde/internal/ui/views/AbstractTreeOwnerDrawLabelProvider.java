/*
* Copyright (c) 2010-2012 Research In Motion Limited. All rights reserved.
*
* This program and the accompanying materials are made available
* under the terms of the Eclipse Public License, Version 1.0,
* which accompanies this distribution and is available at
*
* http://www.eclipse.org/legal/epl-v10.html
*
*/
package net.rim.ejde.internal.ui.views;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;

public abstract class AbstractTreeOwnerDrawLabelProvider extends OwnerDrawLabelProvider {
    private static final Logger log = Logger.getLogger( AbstractTreeOwnerDrawLabelProvider.class );
    // the margin between a minus/plus image and the bounds of a draw event
    protected int _margin;
    // the center point (width equals height) of a minus/plus image
    protected int _midpoint;
    // the size (width equals height) of a minus/plus image
    protected int _imageSize;
    // the margin between a minus/plus image and the text
    protected int _imageInterval;
    // the color of the frame of a minus/plus image
    protected Color _imageFramColor;

    protected TableViewer _viewer;

    int _displayLevel = -1;

    static final int MINUS_IMAGE_INDEX = -1;

    static final int PLUS_IMAGE_INDEX = 1;

    public AbstractTreeOwnerDrawLabelProvider( TableViewer viewer ) {
        _viewer = viewer;
    }

    /**
     * Sets the display level. If the display level is <em>-1</em>, there is no level restriction and all data should be
     * displayed.
     *
     * @param level
     */
    public void setDiaplsyLevel( int level ) {
        _displayLevel = level;
    }

    /**
     * Gets the display level. If the display level is <em>-1</em>, there is no level restriction and all data should be
     * displayed.
     *
     * @return
     */
    public int getDisplayLevel() {
        return _displayLevel;
    }

    private void initialization( Event event ) {
        int height = event.getBounds().height;
        _margin = (int) ( height * 0.2 );
        _margin = Math.max( 0, _margin );
        _imageSize = (int) ( height * 0.6 );
        _imageSize = ( ( _imageSize + 1 ) / 2 ) * 2; // size must be an even
        // number
        _midpoint = _margin + _imageSize / 2;
        _imageInterval = height;
        _imageFramColor = event.display.getSystemColor( SWT.COLOR_WIDGET_NORMAL_SHADOW );
        int index = _viewer.getTable().getSelectionIndex();
        if( index != -1 ) {
            TableItem item = _viewer.getTable().getItem( index );
            Rectangle rect = item.getBounds( event.index );
            Rectangle headRect = new Rectangle( 0, rect.y, rect.x, rect.height );
            event.gc.fillRectangle( headRect );
            event.gc.fillRectangle( rect );
        }
    }

    /**
     * This method should be override in subclass, but subclass method must call the super.paint(Event, Object) first.
     */
    protected void paint( Event event, Object element ) {
        initialization( event );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#erase(org.eclipse.swt.widgets.Event, java.lang.Object)
     */
    protected void erase( Event event, Object element ) {
        // do nothing
    }

    protected void drawFirstColumn( Event event, Object element, String text, boolean highlight ) {
        int startX = event.x;
        int startY = event.y;
        int indent = getIndent( element );
        int extraWidth = indent * _imageInterval;
        event.width += extraWidth + _imageInterval;

        for( int i = 0; i < indent; i++ ) {
            if( findRowAtSameIndent( event.item, i ) ) {
                int lineStyle = event.gc.getLineStyle();
                event.gc.setLineDash( new int[] { 1 } );
                // draw line "|"
                drawLine1( event, startX + i * _imageInterval, startY );
                event.gc.setLineStyle( lineStyle );
            }
        }
        boolean shouldDisplayImage = true;
        if( _displayLevel >= 0 && _displayLevel >= calculateDisplayLevel( element ) )
            shouldDisplayImage = false;

        if( hasChildren( element ) && shouldDisplayImage ) {
            if( indent != 0 ) {
                // draw line "|" (above an image)
                drawLine6( event, startX + extraWidth, startY );
            }
            // draw minus/plus image
            if( isExpanded( element ) ) {
                drawImage( MINUS_IMAGE_INDEX, event, startX + extraWidth, startY, _imageInterval );
                // draw line "|" (below half size)
                drawLine9( event, startX + extraWidth + _imageInterval, startY );
            } else
                drawImage( PLUS_IMAGE_INDEX, event, startX + extraWidth, startY, _imageInterval );
            // draw line "-" (right side of an image)
            drawLine7( event, startX + extraWidth, startY );

            if( findRowAtSameIndent( event.item, indent ) )
                // draw line "|" (below an image)
                drawLine8( event, startX + extraWidth, startY );
        } else {
            if( indent != 0 ) {
                if( findRowAtSameIndent( event.item, indent ) )
                    // draw line "|-"
                    drawLine2( event, startX + extraWidth, startY );
                else
                    // line "|_".
                    drawLine3( event, startX + extraWidth, startY );
            }
        }

        drawText( event, text, startX + extraWidth + _imageInterval + _imageInterval, startY, highlight );
    }

    private void drawLine( Event event, int lineStartX, int lineStartY, int lineEndX, int lineEndY ) {
        int lineStyle = event.gc.getLineStyle();
        event.gc.setLineDash( new int[] { 1 } );
        Color frontColor = event.gc.getForeground();
        event.gc.setForeground( _imageFramColor );
        event.gc.drawLine( lineStartX, lineStartY, lineEndX, lineEndY );
        event.gc.setForeground( frontColor );
        event.gc.setLineStyle( lineStyle );
    }

    /**
     * Draws line "|".
     *
     * @param event
     * @param startX
     * @param startY
     */
    private void drawLine1( Event event, int startX, int startY ) {
        int lineStartX = startX + _midpoint;
        int lineStartY = startY;
        int lineEndX = lineStartX;
        int lineEndY = startY + _imageInterval;
        drawLine( event, lineStartX, lineStartY, lineEndX, lineEndY );
    }

    /**
     * Draws line "|" (above an image).
     *
     * @param event
     * @param startX
     * @param startY
     */
    private void drawLine6( Event event, int startX, int startY ) {
        int lineStartX = startX + _midpoint;
        int lineStartY = startY;
        int lineEndX = lineStartX;
        int lineEndY = startY + _margin;
        drawLine( event, lineStartX, lineStartY, lineEndX, lineEndY );
    }

    /**
     * Draws line "-" (right side of an image).
     *
     * @param event
     * @param startX
     * @param startY
     */
    private void drawLine7( Event event, int startX, int startY ) {
        int lineStartX = startX + _margin + _imageSize;
        int lineStartY = startY + _midpoint;
        int lineEndX = startX + event.getBounds().height + _imageInterval;
        int lineEndY = lineStartY;
        drawLine( event, lineStartX, lineStartY, lineEndX, lineEndY );
    }

    /**
     * Draws line "|" (below an image).
     *
     * @param event
     * @param startX
     * @param startY
     */
    private void drawLine8( Event event, int startX, int startY ) {
        int lineStartX = startX + _midpoint;
        int lineStartY = startY + _margin + _imageSize;
        int lineEndX = lineStartX;
        int lineEndY = startY + event.getBounds().height;
        drawLine( event, lineStartX, lineStartY, lineEndX, lineEndY );
    }

    /**
     * Draws line "|-".
     *
     * @param event
     * @param startX
     * @param startY
     */
    private void drawLine2( Event event, int startX, int startY ) {
        drawLine1( event, startX, startY );
        drawLine4( event, startX, startY );
    }

    /**
     * Draws line "|_".
     *
     * @param event
     * @param startX
     * @param startY
     */
    private void drawLine3( Event event, int startX, int startY ) {
        drawLine5( event, startX, startY );
        drawLine4( event, startX, startY );
    }

    /**
     * Draws "-".
     *
     * @param event
     * @param startX
     * @param startY
     */
    private void drawLine4( Event event, int startX, int startY ) {
        int lineStartX = startX + _midpoint;
        int lineStartY = startY + _midpoint;
        int lineEndX = lineStartX + _midpoint + _imageInterval;
        int lineEndY = lineStartY;
        drawLine( event, lineStartX, lineStartY, lineEndX, lineEndY );
    }

    /**
     * Draws "|" (up half size).
     *
     * @param event
     * @param startX
     * @param startY
     */
    private void drawLine5( Event event, int startX, int startY ) {
        int lineStartX = startX + _midpoint;
        int lineStartY = startY;
        int lineEndX = lineStartX;
        int lineEndY = startY + _midpoint;
        drawLine( event, lineStartX, lineStartY, lineEndX, lineEndY );
    }

    /**
     * Draws "|" (below half size).
     *
     * @param event
     * @param startX
     * @param startY
     */
    private void drawLine9( Event event, int startX, int startY ) {
        int lineStartX = startX + _midpoint;
        int lineStartY = startY + _midpoint;
        int lineEndX = lineStartX;
        int lineEndY = startY + _imageInterval;
        drawLine( event, lineStartX, lineStartY, lineEndX, lineEndY );
    }

    private void drawImage( int image, Event event, int x, int y, int height ) {
        GC gc = event.gc;
        Color foreground = gc.getForeground();
        /* Plus image */
        gc.setForeground( _imageFramColor );
        gc.drawRectangle( x + _margin, y + _margin, _imageSize, _imageSize );
        gc.setForeground( foreground );
        if( image == MINUS_IMAGE_INDEX )
            drawMinusImage( event, x, y, height );
        else if( image == PLUS_IMAGE_INDEX )
            drawPlusImage( event, x, y, height );
        else
            log.error( NLS.bind( Messages.AbstractTreeOwnerDrawLabelProvider_UNKNOWN_IMAGE_MESSAGE, image ) );
    }

    /**
     * Draws a "+" sign with a frame.
     *
     * @param event
     * @param x
     * @param y
     * @param height
     */
    private void drawPlusImage( Event event, int x, int y, int height ) {
        event.gc.drawLine( x + _midpoint, y + _margin + 2, x + _midpoint, y + _margin + _imageSize - 2 );
        event.gc.drawLine( x + _margin + 2, y + _midpoint, x + _margin + _imageSize - 2, y + _midpoint );
    }

    /**
     * Draws a "-" sign with a frame.
     *
     * @param event
     * @param x
     * @param y
     * @param height
     */
    private void drawMinusImage( Event event, int x, int y, int height ) {
        event.gc.drawLine( x + _margin + 2, y + _midpoint, x + _margin + _imageSize - 2, y + _midpoint );
    }

    protected Color getForeground() {
        if( _viewer.getTable().getSelectionIndex() >= 0 )
            return _viewer.getTable().getItem( _viewer.getTable().getSelectionIndex() ).getForeground();
        else
            return _viewer.getTable().getForeground();
    }

    protected Color getBackground() {
        // This method must be overriden otherwise, in a TableTree in which the
        // first
        // item has no sub items, a grey (Widget background colour) square will
        // appear in
        // the first column of the first item.
        // It is not possible in the constructor to set the background of the
        // TableTree
        // to be the same as the background of the Table because this interferes
        // with n
        // the TableTree adapting to changes in the System color settings.
        if( _viewer.getTable().getSelectionIndex() >= 0 )
            return _viewer.getTable().getItem( _viewer.getTable().getSelectionIndex() ).getBackground();
        else
            return _viewer.getTable().getBackground();
    }

    public ColumnViewer getViewer() {
        return _viewer;
    }

    public void drawText( Event event, String text, int x, int y, boolean highlight ) {
        if( highlight )
            event.gc.setForeground( event.display.getSystemColor( SWT.COLOR_DARK_RED ) );
        event.gc.drawText( IConstants.ONE_BLANK_STRING + IConstants.ONE_BLANK_STRING + text.trim(), x, y );
    }

    public Rectangle getImageBounds( Object item, Rectangle rectangle ) {
        return new Rectangle( rectangle.x + getIndent( item ) * _imageInterval + _margin, rectangle.y + _margin, _imageSize,
                _imageSize );
    }

    // --- Abstract methods ---
    /**
     * Finds if there is any other row which has the same indent as the given <code>indent</code>.
     *
     * @param obj
     *            It could be a TableItem object or the data of a TableItem.
     * @param indent
     *            indent of given <code>obj</code>.
     * @return <code>true</code>if there is any other row which has the same indent as the given <code>indent</code>,
     *         <code>false</code> otherwise;
     */
    abstract public boolean findRowAtSameIndent( Object obj, int indent );

    /**
     * Gets the indent of given <code>obj</code>.
     *
     * @param obj
     * @return indent of given <code>obj</code>.
     */
    abstract public int getIndent( Object obj );

    /**
     * Gets the index of given <code>obj</code>.
     *
     * @param obj
     * @return index of given <code>obj</code>.
     */
    abstract public int getIndex( Object obj );

    /**
     * Checks if <code>obj</code> has children.
     *
     * @param obj
     * @return <code>true</code>if <code>obj</code> has children, <code>false</code>otherwise;
     */
    abstract public boolean hasChildren( Object obj );

    /**
     * Checks if <code>obj</code> was expanded..
     *
     * @param obj
     * @return <code>true</code>if <code>obj</code> was expanded, <code>false</code>otherwise;
     */
    abstract public boolean isExpanded( Object obj );

    /**
     * Calculates the display level of given <code>obj</code>.
     *
     * @param obj
     * @return display level of given <code>obj</code>.
     */
    abstract public int calculateDisplayLevel( Object obj );
}
