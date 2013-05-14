/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;
import org.sikuli.script.natives.FindInput;
import org.sikuli.script.natives.FindResult;
import org.sikuli.script.natives.FindResults;
import org.sikuli.script.natives.OpenCV;
import org.sikuli.script.natives.TARGET_TYPE;
import org.sikuli.script.natives.Vision;

public class Finder implements Iterator<Match> {

  private Region _region = null;
  private Pattern _pattern = null;
  private FindInput _findInput = new FindInput();
  private FindResults _results = null;
  private int _cur_result_i;
  private boolean repeating = false;

//TODO Vision.setParameter("GPU", 1);
  static {
    FileManager.loadLibrary("VisionProxy");
  }

  /**
   * Finder constructor (finding within an image).
   * <br />internally used with a screen snapshot
   *
   * @param imageFilename a string (name, path, url)
   */
  public Finder(String imageFilename) throws IOException {
    this(imageFilename, null);
  }

  /**
   * Finder constructor (finding within an image within the given region).
   * <br />internally used with a screen snapshot
   *
   * @param imageFilename a string (name, path, url)
   * @param region search Region within image - topleft = (0,0)
   */
  public Finder(String imageFilename, Region region) throws IOException  {
    String fname = ImageLocator.locate(imageFilename);
    _findInput.setSource(fname);
    _region = region;
  }

	/**
	 * Constructor for special use from a BufferedImage
	 *
	 * @param bimg
	 */
	public Finder(BufferedImage bimg) {
    _findInput.setSource(OpenCV.convertBufferedImageToMat(bimg));
	}

  /**
	 * Finder constructor for special use froma a ScreenImage
	 *
	 * @param simg
	 */
	public Finder(ScreenImage simg) {
		initScreenFinder(simg, null);
  }

  /**
	 * Finder constructor for special use from a ScreenImage
	 *
	 * @param simg
	 * @param region
	 */
	public Finder(ScreenImage simg, Region region) {
		initScreenFinder(simg, region);
  }

	private void initScreenFinder(ScreenImage simg, Region region) {
		setScreenImage(simg);
    _region = region;
	}

  /**
   * to explicitly free the Finder's resources
   */
  public void destroy() {
		_findInput.delete();
		_findInput = null;
		_results.delete();
		_results = null;
		_pattern = null;
  }

	/**
	 * internal use: exchange the source image in existing Finder
	 *
	 * @param simg
	 */
	public void setScreenImage(ScreenImage simg) {
    _findInput.setSource(OpenCV.convertBufferedImageToMat(simg.getImage()));
	}

  /**
	 * internal use: to be able to reuse the same Finder
	 */
	public void setRepeating() {
    repeating = true;
  }

	/**
	 * internal use: repeat find with same Finder
	 */
	public void findRepeat() {
		_results = Vision.find(_findInput);
		_cur_result_i = 0;
	}

  /**
   *
   * @param imageOrText
   * @param minSimilarity
   */
  public String find(String imageOrText, double minSimilarity) {
		String target = setTargetSmartly(_findInput, imageOrText);
    if (null == target) {
      return null;
    }
		if (target.equals(imageOrText+"???")) {
			return target;
		}
    _findInput.setSimilarity(minSimilarity);
    _results = Vision.find(_findInput);
    _cur_result_i = 0;
    return target;
  }

  /**
   * find given pattern within the stored image
   *
   * @param aPtn
   */
  public String find(Pattern aPtn) {
//TODO Pattern with BufferedImage
    _pattern = (Pattern) aPtn;
    String img = aPtn.getFilename();
    if (img == null) {
      return null;
    }
		_findInput.setTarget(TARGET_TYPE.IMAGE, img);
    _findInput.setSimilarity(aPtn.getSimilar());
    _results = Vision.find(_findInput);
    _cur_result_i = 0;
    return img;
  }

  public String find(String imageOrText) {
    return find(imageOrText, Settings.MinSimilarity);
  }

	/**
	 * internal use: repeat find with same Finder
	 */
  public void findAllRepeat() {
    Debug timing = new Debug();
    timing.startTiming("Finder.findAll");
    _results = Vision.find(_findInput);
    _cur_result_i = 0;
    timing.endTiming("Finder.findAll");
	}

  /**
   *
   * @param imageOrText
   * @param minSimilarity
   */
  public String findAll(String imageOrText, double minSimilarity) {
		String target = setTargetSmartly(_findInput, imageOrText);
    if (null == target) {
      return null;
    }
		if (target.equals(imageOrText+"???")) {
			return target;
		}
    Debug timing = new Debug();
    timing.startTiming("Finder.findAll");

    setTargetSmartly(_findInput, imageOrText);
    _findInput.setSimilarity(minSimilarity);
    _findInput.setFindAll(true);
    _results = Vision.find(_findInput);
    _cur_result_i = 0;

    timing.endTiming("Finder.findAll");
    return target;
  }

	/**
   *
   * @param Pattern
   * @param aPtn
   */
  public String findAll(Pattern aPtn)  {
    _pattern = (Pattern) aPtn;
    String img = aPtn.getFilename();
    if (img == null) {
      return null;
    }
		_findInput.setTarget(TARGET_TYPE.IMAGE, img);
    _findInput.setSimilarity(aPtn.getSimilar());
    _findInput.setFindAll(true);
    Debug timing = new Debug();
    timing.startTiming("Finder.findAll");
    _results = Vision.find(_findInput);
    _cur_result_i = 0;
    timing.endTiming("Finder.findAll");
    return aPtn.getFilename();
  }

  public String findAll(String imageOrText) {
    return findAll(imageOrText, Settings.MinSimilarity);
  }

	private String setTargetSmartly(FindInput fin, String target) {
		if (isImageFile(target)) {
			try {
				//assume it's a file first
				String filename = ImageLocator.locate(target);
				fin.setTarget(TARGET_TYPE.IMAGE, filename);
				return filename;
			} catch (IOException e) {
				if (!repeating) {
					Debug.error(target
									+ " looks like a file, but not on disk. Assume it's text.");
				}
			}
		}
		if (!Settings.OcrTextSearch) {
			Debug.error("Region.find(text): text search is currently switched off");
			return target + "???";
		} else {
			fin.setTarget(TARGET_TYPE.TEXT, target);
			if (TextRecognizer.getInstance() == null) {
  			Debug.error("Region.find(text): text search is now switched off");
    		return target + "???";
      }
			return target;
		}
	}

	private static boolean isImageFile(String fname) {
		int dot = fname.lastIndexOf('.');
		if (dot < 0) {
			return false;
		}
		String suffix = fname.substring(dot + 1).toLowerCase();
		if (suffix.equals("png") || suffix.equals("jpg")) {
			return true;
		}
		return false;
	}

  /**
   *
   * @return true if Finder has a next match, false otherwise
   */
  @Override
  public boolean hasNext() {
    if (_results != null && _results.size() > _cur_result_i) {
      return true;
    }
    return false;
  }

  /**
   *
   * @return the next match or null
   */
  @Override
  public Match next() {
    Match ret = null;
    if (hasNext()) {
      FindResult fr = _results.get(_cur_result_i++);
      Screen parentScreen = null;
      if (_region != null) {
        parentScreen = _region.getScreen();
      }
      ret = new Match(fr, parentScreen);
			fr.delete();
      if (_region != null) {
        ret = _region.toGlobalCoord(ret);
      }
      if (_pattern != null) {
        Location offset = _pattern.getTargetOffset();
        ret.setTargetOffset(offset);
      }
    }
    return ret;
  }

  /**
   * not used
   */
  @Override
  public void remove(){}

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    destroy();
  }


}
