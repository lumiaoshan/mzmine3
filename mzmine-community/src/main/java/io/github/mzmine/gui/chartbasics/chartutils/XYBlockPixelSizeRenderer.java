/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.gui.chartbasics.chartutils;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.util.Args;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

/**
 * A renderer that represents data from an {@link XYZDataset} by drawing a color block at each (x,
 * y) point, where the color is a function of the z-value from the dataset. The example shown here
 * is generated by the {@code XYBlockChartDemo1.java} program included in the JFreeChart demo
 * collection: <br>
 * <br>
 * <img src="../../../../../images/XYBlockRendererSample.png" alt= "XYBlockRendererSample.png">
 *
 * @since 1.0.4
 */
public class XYBlockPixelSizeRenderer extends AbstractXYItemRenderer
    implements XYItemRenderer, Cloneable, PublicCloneable, Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  /**
   * The block width (defaults to 1.0).
   */
  private double blockWidth = 1.0;

  /**
   * The block height (defaults to 1.0).
   */
  private double blockHeight = 1.0;

  /**
   * The block width (defaults to 1.0).
   */
  private double blockWidthPixel = 3;

  /**
   * The block height (defaults to 1.0).
   */
  private double blockHeightPixel = 3;

  /**
   * The anchor point used to align each block to its (x, y) location. The default value is
   * {@code RectangleAnchor.CENTER}.
   */
  private RectangleAnchor blockAnchor = RectangleAnchor.CENTER;

  /** Temporary storage for the x-offset used to align the block anchor. */
  private double xOffset;

  /** Temporary storage for the y-offset used to align the block anchor. */
  private double yOffset;

  /** The paint scale. */
  private PaintScale paintScale;

  /**
   * Creates a new {@code XYBlockRenderer} instance with default attributes.
   */
  public XYBlockPixelSizeRenderer() {
    updateOffsets();
    this.paintScale = new LookupPaintScale();
  }

  /**
   * Returns the block width, in data/axis units.
   *
   * @return The block width.
   *
   * @see #setBlockWidth(double)
   */
  public double getBlockWidth() {
    return this.blockWidth;
  }

  /**
   * Returns the block width, in pixel units.
   *
   * @return The block width.
   *
   * @see #setBlockWidth(double)
   */
  public double getBlockWidthPixel() {
    return this.blockWidthPixel;
  }

  /**
   * Sets the width of the blocks used to represent each data item and sends a
   * {@link RendererChangeEvent} to all registered listeners.
   *
   * @param width the new width, in data/axis units (must be &gt; 0.0).
   *
   * @see #getBlockWidth()
   */
  public void setBlockWidth(double width) {
    if (width <= 0.0) {
      throw new IllegalArgumentException("The 'width' argument must be > 0.0");
    }
    this.blockWidth = width;
    updateOffsets();
    fireChangeEvent();
  }

  /**
   * Sets the width of the blocks used to represent each data item and sends a
   * {@link RendererChangeEvent} to all registered listeners.
   *
   * @param width the new width, in pixel units (must be &gt; 0.0).
   *
   * @see #getBlockWidth()
   */
  public void setBlockWidthPixel(int width) {
    if (width <= 0) {
      throw new IllegalArgumentException("The 'width' argument must be > 0");
    }
    this.blockWidthPixel = width;
    updateOffsets();
    fireChangeEvent();
  }

  /**
   * Returns the block height, in data/axis units.
   *
   * @return The block height.
   *
   * @see #setBlockHeight(double)
   */
  public double getBlockHeight() {
    return this.blockHeight;
  }

  /**
   * Returns the block height, in pixel units.
   *
   * @return The block height.
   *
   * @see #setBlockHeight(double)
   */
  public double getBlockHeightPixel() {
    return this.blockHeightPixel;
  }

  /**
   * Sets the height of the blocks used to represent each data item and sends a
   * {@link RendererChangeEvent} to all registered listeners.
   *
   * @param height the new height, in data/axis units (must be &gt; 0.0).
   *
   * @see #getBlockHeight()
   */
  public void setBlockHeight(double height) {
    if (height <= 0.0) {
      throw new IllegalArgumentException("The 'height' argument must be > 0.0");
    }
    this.blockHeight = height;
    updateOffsets();
    fireChangeEvent();
  }

  /**
   * Sets the height of the blocks used to represent each data item and sends a
   * {@link RendererChangeEvent} to all registered listeners.
   *
   * @param height the new height, in data/axis units (must be &gt; 0.0).
   *
   * @see #getBlockHeight()
   */
  public void setBlockHeightPixel(int height) {
    if (height <= 0) {
      throw new IllegalArgumentException("The 'height' argument must be > 0");
    }
    this.blockHeightPixel = height;
    updateOffsets();
    fireChangeEvent();
  }

  /**
   * Returns the anchor point used to align a block at its (x, y) location. The default values is
   * {@link RectangleAnchor#CENTER}.
   *
   * @return The anchor point (never {@code null}).
   *
   * @see #setBlockAnchor(RectangleAnchor)
   */
  public RectangleAnchor getBlockAnchor() {
    return this.blockAnchor;
  }

  /**
   * Sets the anchor point used to align a block at its (x, y) location and sends a
   * {@link RendererChangeEvent} to all registered listeners.
   *
   * @param anchor the anchor.
   *
   * @see #getBlockAnchor()
   */
  public void setBlockAnchor(RectangleAnchor anchor) {
    Args.nullNotPermitted(anchor, "anchor");
    if (this.blockAnchor.equals(anchor)) {
      return; // no change
    }
    this.blockAnchor = anchor;
    updateOffsets();
    fireChangeEvent();
  }

  /**
   * Returns the paint scale used by the renderer.
   *
   * @return The paint scale (never {@code null}).
   *
   * @see #setPaintScale(PaintScale)
   * @since 1.0.4
   */
  public PaintScale getPaintScale() {
    return this.paintScale;
  }

  /**
   * Sets the paint scale used by the renderer and sends a {@link RendererChangeEvent} to all
   * registered listeners.
   *
   * @param scale the scale ({@code null} not permitted).
   *
   * @see #getPaintScale()
   * @since 1.0.4
   */
  public void setPaintScale(PaintScale scale) {
    Args.nullNotPermitted(scale, "scale");
    this.paintScale = scale;
    fireChangeEvent();
  }

  /**
   * Updates the offsets to take into account the block width, height and anchor.
   */
  private void updateOffsets() {
    if (this.blockAnchor.equals(RectangleAnchor.BOTTOM_LEFT)) {
      this.xOffset = 0.0;
      this.yOffset = 0.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.BOTTOM)) {
      this.xOffset = -this.blockWidth / 2.0;
      this.yOffset = 0.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.BOTTOM_RIGHT)) {
      this.xOffset = -this.blockWidth;
      this.yOffset = 0.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.LEFT)) {
      this.xOffset = 0.0;
      this.yOffset = -this.blockHeight / 2.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.CENTER)) {
      this.xOffset = -this.blockWidth / 2.0;
      this.yOffset = -this.blockHeight / 2.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.RIGHT)) {
      this.xOffset = -this.blockWidth;
      this.yOffset = -this.blockHeight / 2.0;
    } else if (this.blockAnchor.equals(RectangleAnchor.TOP_LEFT)) {
      this.xOffset = 0.0;
      this.yOffset = -this.blockHeight;
    } else if (this.blockAnchor.equals(RectangleAnchor.TOP)) {
      this.xOffset = -this.blockWidth / 2.0;
      this.yOffset = -this.blockHeight;
    } else if (this.blockAnchor.equals(RectangleAnchor.TOP_RIGHT)) {
      this.xOffset = -this.blockWidth;
      this.yOffset = -this.blockHeight;
    }
  }

  /**
   * Returns the lower and upper bounds (range) of the x-values in the specified dataset.
   *
   * @param dataset the dataset ({@code null} permitted).
   *
   * @return The range ({@code null} if the dataset is {@code null} or empty).
   *
   * @see #findRangeBounds(XYDataset)
   */
  @Override
  public Range findDomainBounds(XYDataset dataset) {
    if (dataset == null) {
      return null;
    }
    Range r = DatasetUtils.findDomainBounds(dataset, false);
    if (r == null) {
      return null;
    }
    return new Range(r.getLowerBound() + this.xOffset,
        r.getUpperBound() + this.blockWidth + this.xOffset);
  }

  /**
   * Returns the range of values the renderer requires to display all the items from the specified
   * dataset.
   *
   * @param dataset the dataset ({@code null} permitted).
   *
   * @return The range ({@code null} if the dataset is {@code null} or empty).
   *
   * @see #findDomainBounds(XYDataset)
   */
  @Override
  public Range findRangeBounds(XYDataset dataset) {
    if (dataset != null) {
      Range r = DatasetUtils.findRangeBounds(dataset, false);
      if (r == null) {
        return null;
      } else {
        return new Range(r.getLowerBound() + this.yOffset,
            r.getUpperBound() + this.blockHeight + this.yOffset);
      }
    } else {
      return null;
    }
  }

  /**
   * Draws the block representing the specified item.
   *
   * @param g2 the graphics device.
   * @param state the state.
   * @param dataArea the data area.
   * @param info the plot rendering info.
   * @param plot the plot.
   * @param domainAxis the x-axis.
   * @param rangeAxis the y-axis.
   * @param dataset the dataset.
   * @param series the series index.
   * @param item the item index.
   * @param crosshairState the crosshair state.
   * @param pass the pass index.
   */
  @Override
  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {

    double x = dataset.getXValue(series, item);
    double y = dataset.getYValue(series, item);
    double z = 0.0;
    if (dataset instanceof XYZDataset) {
      z = ((XYZDataset) dataset).getZValue(series, item);
    }

    Paint p = this.paintScale.getPaint(z);
    double xx0 = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
    double yy0 = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge());
    double xx1 = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
    double yy1 = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge());
    Rectangle2D block;
    PlotOrientation orientation = plot.getOrientation();
    if (orientation.equals(PlotOrientation.HORIZONTAL)) {
      block = new Rectangle2D.Double(Math.min(yy0, yy1), Math.min(xx0, xx1), blockWidthPixel,
          blockHeightPixel);
    } else {
      block = new Rectangle2D.Double(Math.min(xx0, xx1), Math.min(yy0, yy1), blockWidthPixel,
          blockHeightPixel);
    }
    g2.setPaint(p);
    g2.fill(block);
    g2.setStroke(new BasicStroke(1.0f));
    g2.draw(block);

    if (isItemLabelVisible(series, item)) {
      drawItemLabel(g2, orientation, dataset, series, item, block.getCenterX(), block.getCenterY(),
          y < 0.0);
    }

    int datasetIndex = plot.indexOf(dataset);
    double transX = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
    double transY = rangeAxis.valueToJava2D(y, dataArea, plot.getRangeAxisEdge());
    updateCrosshairValues(crosshairState, x, y, datasetIndex, transX, transY, orientation);

    EntityCollection entities = state.getEntityCollection();
    if (entities != null) {
      addEntity(entities, block, dataset, series, item, block.getCenterX(), block.getCenterY());
    }

  }

  /**
   * Tests this {@code XYBlockRenderer} for equality with an arbitrary object. This method returns
   * {@code true} if and only if:
   * <ul>
   * <li>{@code obj} is an instance of {@code XYBlockRenderer} (not {@code null});</li>
   * <li>{@code obj} has the same field values as this {@code XYBlockRenderer};</li>
   * </ul>
   *
   * @param obj the object ({@code null} permitted).
   *
   * @return A boolean.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof XYBlockPixelSizeRenderer)) {
      return false;
    }
    XYBlockPixelSizeRenderer that = (XYBlockPixelSizeRenderer) obj;
    if (this.blockHeight != that.blockHeight) {
      return false;
    }
    if (this.blockWidth != that.blockWidth) {
      return false;
    }
    if (!this.blockAnchor.equals(that.blockAnchor)) {
      return false;
    }
    if (!this.paintScale.equals(that.paintScale)) {
      return false;
    }
    return super.equals(obj);
  }

  /**
   * Returns a clone of this renderer.
   *
   * @return A clone of this renderer.
   *
   * @throws CloneNotSupportedException if there is a problem creating the clone.
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    XYBlockPixelSizeRenderer clone = (XYBlockPixelSizeRenderer) super.clone();
    if (this.paintScale instanceof PublicCloneable) {
      PublicCloneable pc = (PublicCloneable) this.paintScale;
      clone.paintScale = (PaintScale) pc.clone();
    }
    return clone;
  }

}