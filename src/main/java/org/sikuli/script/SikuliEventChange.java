/*
 * Copyright 2010-2011, Sikuli.org
 * Released under the MIT License.
 *
 */
package org.sikuli.script;

import java.util.List;

public class SikuliEventChange extends SikuliEvent {
   public SikuliEventChange(List<Match> results, Region r){
      type = Type.CHANGE;
      changes = results;
      region = r;
   }

	@Override
   public String toString(){
      return String.format("ChangeEvent on %s | %d changes",
               region, changes.size());
   }
}
