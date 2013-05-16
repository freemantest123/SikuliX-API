/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * to define a more complex search target<br />
 * - non-standard minimum similarity <br />
 * - click target other than center <br />
 * - image as in-memory image
 */
public class Pattern {

  private String imgURL = null;
	private BufferedImage imgBuf = null;
  private float similarity = (float) Settings.MinSimilarity;
  private Location offset = new Location(0, 0);
  private final static String isBImg = "-- BufferedImage --";

  /**
	 * creates empty Pattern object
	 * at least setFilename() or setImage() must be used before
	 * the Pattern object is ready for anything
	 */
	public Pattern() {
  }

  /**
	 * create a new Pattern from another (attribs are copied)
	 *
	 * @param p
	 */
	public Pattern(Pattern p) {
		if (p.imgBuf != null) {
			imgBuf = p.imgBuf.getSubimage(0, 0, p.imgBuf.getWidth(), imgBuf.getHeight());
		}
    imgURL = p.imgURL;
    similarity = p.similarity;
    offset.x = p.offset.x;
    offset.y = p.offset.y;
  }

  /**
	 * create a Pattern with an image file name<br />
	 * checked only when used<br />
   * see checkFile()
	 *
	 * @param imgpath
	 */
	public Pattern(String imgpath) {
    imgURL = imgpath;
  }

  /**
	 * A Pattern from a BufferedImage
	 * ** not tested yet totally **
	 *
	 * @param bimg
	 */
	public Pattern(BufferedImage bimg) {
		imgBuf = bimg;
		imgURL = isBImg;
	}

  /**
	 * A Pattern from a ScreenImage
	 * ** not tested yet totally **
	 *
	 * @param simg
	 */
	public Pattern(ScreenImage simg) {
		imgBuf = simg.getImage();
		imgURL = "-- BufferedImage --";
	}

	/**
	 * sets the minimum Similarity to use with find
	 *
	 * @param sim
	 * @return the Pattern object itself
	 */
	public Pattern similar(float sim) {
    similarity = sim;
    return this;
  }

	/**
	 * sets the minimum Similarity to 0.99 which means exact match
	 *
	 * @return  the Pattern object itself
	 */
	public Pattern exact() {
    similarity = 0.99f;
    return this;
  }

  /**
	 *
	 * @return the current minimum similarity
	 */
	public float getSimilar() {
    return this.similarity;
  }

  /**
	 * set the offset from the match's center to be used with mouse actions
	 *
	 * @param dx
	 * @param dy
	 * @return the Pattern object itself
	 */
	public Pattern targetOffset(int dx, int dy) {
    offset.x = dx;
    offset.y = dy;
    return this;
  }

  /**
	 * set the offset from the match's center to be used with mouse actions
	 *
	 * @param loc
	 * @return the Pattern object itself
	 */
	public Pattern targetOffset(Location loc) {
    offset.x = loc.x;
    offset.y = loc.y;
    return this;
  }

  /**
	 *
	 * @return the current offset
	 */
	public Location getTargetOffset() {
    return offset;
  }

  /**
	 * set the Patterns image file name
	 * It is only checked if Pattern is used or with getFilename()
	 *
	 * @param imgURL_
	 * @return the Pattern object itself
	 */
	public Pattern setFilename(String imgURL_) {
    imgURL = imgURL_;
    return this;
  }

  /**
	 * the current image absolute filepath if any
	 *
	 * @return might be null
	 */
	public String getFilename() {
		if (imgURL != null) {
      if (isBImg.equals(imgURL)) {
        return isBImg;
      }
			try {
				return ImageLocator.locate(imgURL);
			} catch (IOException ex) {
			}
		}
		return null;
	}

  /**
	 * check for a valid image file
	 *
	 * @return path or null
	 */
	public String checkFile() {
		if (imgBuf != null) {
			return imgURL;
		}
    try {
      ImageLocator.locate(imgURL);
      return imgURL;
    } catch (IOException ex) {
      return null;
    }
  }

  /**
	 * return the image if any
	 *
	 * @return might be null
	 */
	public BufferedImage getImage() {
		if (imgBuf != null) {
			return imgBuf;
		}
		if (null != getFilename()) {
			return ImageLocator.getImage(getFilename());
		}
		return null;
  }

	/**
	 * sets the Pattern's image
	 *
	 * @param bimg
	 * @return the Pattern object itself
	 */
	public Pattern setImage(BufferedImage bimg) {
		imgBuf = bimg;
		return this;
	}

	/**
	 * sets the Pattern's image
	 *
	 * @param simg
	 * @return the Pattern object itself
	 */
	public Pattern setImage(ScreenImage simg) {
		imgBuf = simg.getImage();
		return this;
	}

  @Override
  public String toString() {
    String ret = "P(" + imgURL + ")";
    ret += " S: " + similarity;
    if (offset.x != 0 || offset.y != 0) {
      ret += " T: " + offset.x + "," + offset.y;
    }
    return ret;
  }
}
