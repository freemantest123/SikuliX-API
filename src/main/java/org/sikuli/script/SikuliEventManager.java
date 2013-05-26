/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.awt.AWTException;
import java.util.*;
import org.sikuli.script.natives.FindInput;
import org.sikuli.script.natives.FindResult;
import org.sikuli.script.natives.FindResults;
import org.sikuli.script.natives.Mat;
import org.sikuli.script.natives.OpenCV;
import org.sikuli.script.natives.Vision;

public class SikuliEventManager {

  protected enum State {

    UNKNOWN, MISSING, APPEARED, VANISHED
  }
  private Region _region;
  private Mat _lastImgMat = null;
  private Map<Object, State> _state;
  private Map<Object, Match> _lastMatch;
  private Map<Object, SikuliEventObserver> _appearOb, _vanishOb;
  private Map<Integer, SikuliEventObserver> _changeOb;
  private int _minChanges;
  private boolean sthgLeft;

  public SikuliEventManager(Region region) {
    _region = region;
    _state = new HashMap<Object, State>();
    _lastMatch = new HashMap<Object, Match>();
    _appearOb = new HashMap<Object, SikuliEventObserver>();
    _vanishOb = new HashMap<Object, SikuliEventObserver>();
    _changeOb = new HashMap<Integer, SikuliEventObserver>();
  }

  public void initialize() {
    Debug.log(2, "SikuliEventManager: resetting observe states");
    sthgLeft = true;
    for (Object ptn : _state.keySet()) {
      _state.put(ptn, State.UNKNOWN);
    }
  }

  private <PSC> float getSimiliarity(PSC ptn) {
    float similarity = -1f;
    if (ptn instanceof Pattern) {
      similarity = ((Pattern) ptn).getSimilar();
    }
    if (similarity < 0) {
      similarity = (float) Settings.MinSimilarity;
    }
    return similarity;
  }

  public <PSC> void addAppearObserver(PSC ptn, SikuliEventObserver ob) {
    _appearOb.put(ptn, ob);
    _state.put(ptn, State.UNKNOWN);
  }

  public <PSC> void removeAppearObserver(PSC ptn) {
    _appearOb.remove(ptn);
    _state.remove(ptn);
  }

  public <PSC> void addVanishObserver(PSC ptn, SikuliEventObserver ob) {
    _vanishOb.put(ptn, ob);
    _state.put(ptn, State.UNKNOWN);
  }

  public <PSC> void removeVanishObserver(PSC ptn) {
    _vanishOb.remove(ptn);
    _state.remove(ptn);
  }

  protected void callAppearObserver(Object ptn, Match m) {
    SikuliEventAppear se = new SikuliEventAppear(ptn, m, _region);
    SikuliEventObserver ob = _appearOb.get(ptn);
    ob.targetAppeared(se);
  }

  protected void callVanishObserver(Object ptn, Match m) {
    SikuliEventVanish se = new SikuliEventVanish(ptn, m, _region);
    SikuliEventObserver ob = _vanishOb.get(ptn);
    ob.targetVanished(se);
  }

  protected void checkPatterns(ScreenImage simg) {
    Finder finder = new Finder(simg, _region);
    String imgOK;
    for (Object ptn : _state.keySet()) {
      if (_state.get(ptn) != State.UNKNOWN) {
        continue;
      }
      if (ptn.getClass().isInstance("")) {
        imgOK = finder.find((String) ptn);
      } else {
        imgOK = finder.find((Pattern) ptn);
      }
      if (null == imgOK) {
        Debug.error("Observe: ImageFile not found", ptn);
        _state.put(ptn, State.MISSING);
        continue;
      }
      Match m = null;
      boolean hasMatch = false;
      if (finder.hasNext()) {
        m = finder.next();
        if (m.getScore() >= getSimiliarity(ptn)) {
          hasMatch = true;
          _lastMatch.put(ptn, m);
        }
      }
      Debug.log(9, "check pattern: " + _state.get(ptn) + " match:" + hasMatch);
      sthgLeft = true;
      if (_appearOb.containsKey(ptn)) {
        if (_state.get(ptn) != State.APPEARED && hasMatch) {
          _state.put(ptn, State.APPEARED);
          sthgLeft = false;
          callAppearObserver(ptn, m);
        }
      } else if (_vanishOb.containsKey(ptn)) {
        if (_state.get(ptn) != State.VANISHED && !hasMatch) {
          sthgLeft = false;
          _state.put(ptn, State.VANISHED);
          callVanishObserver(ptn, _lastMatch.get(ptn));
        }
      }
    }

  }

  public void addChangeObserver(int threshold, SikuliEventObserver ob) {
    _changeOb.put(new Integer(threshold), ob);
    _minChanges = getMinChanges();
  }

  public void removeChangeObserver(int threshold) {
    _changeOb.remove(new Integer(threshold));
    _minChanges = getMinChanges();
  }

  private int getMinChanges() {
    int min = Integer.MAX_VALUE;
    for (Integer n : _changeOb.keySet()) {
      if (n < min) {
        min = n;
      }
    }
    return min;
  }

  protected void callChangeObserver(FindResults results) throws AWTException {
    for (Integer n : _changeOb.keySet()) {
      List<Match> changes = new ArrayList<Match>();
      for (int i = 0; i < results.size(); i++) {
        FindResult r = results.get(i);
        if (r.getW() * r.getH() >= n) {
          changes.add(_region.toGlobalCoord(new Match(r, _region.getScreen())));
        }
      }
      if (changes.size() > 0) {
        SikuliEventChange se = new SikuliEventChange(changes, _region);
        SikuliEventObserver ob = _changeOb.get(n);
        ob.targetChanged(se);
      }
    }
  }

  protected void checkChanges(ScreenImage img) {
    if (_lastImgMat == null) {
      _lastImgMat = OpenCV.convertBufferedImageToMat(img.getImage());
      return;
    }
    FindInput fin = new FindInput();
    fin.setSource(_lastImgMat);
    Mat target = OpenCV.convertBufferedImageToMat(img.getImage());
    fin.setTarget(target);
    fin.setSimilarity(_minChanges);
    FindResults results = Vision.findChanges(fin);
    try {
      callChangeObserver(results);
    } catch (AWTException e) {
      e.printStackTrace();
    }
    _lastImgMat = target;
  }

  public boolean update(ScreenImage simg) {
    boolean ret;
    ret = sthgLeft;
    if (sthgLeft) {
      checkPatterns(simg);
    }
    ret = sthgLeft;
    if (_changeOb.size() > 0) {
      checkChanges(simg);
      ret = true;
    }
    return ret;
  }

  protected void finalize() throws Throwable {
  }
}