/*
 * Copyright 2010-2011, Sikuli.org
 * Released under the MIT License.
 *
 */
package org.sikuli.script;

import java.util.*;


public interface SikuliEventObserver extends EventListener {
   public void targetAppeared(SikuliEventAppear e);
   public void targetVanished(SikuliEventVanish e);
   public void targetChanged(SikuliEventChange e);
}
