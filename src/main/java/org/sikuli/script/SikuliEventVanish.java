/*
 * Copyright 2010-2011, Sikuli.org
 * Released under the MIT License.
 *
 */
package org.sikuli.script;

public class SikuliEventVanish extends SikuliEvent {
   public SikuliEventVanish(Object ptn, Match m, Region r){
      super(ptn, m, r);
      type = Type.VANISH;
   }
}

