/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2012
 */
package org.sikuli.script;

import java.io.File;
import java.io.IOException;

public interface INativeLoader {

  /**
   * Can be used to initialize the NativeLoader. This method is called at the beginning of program
   * execution. The given parameters can be used to parse any ScriptRunner specific custom options.
   *
   * @param args 
   */
  public void init(String[] args);
  
  /**
   * checks, wether a valid libs folder is available and accessible
   */
  public void check(String what);
  
  /**
   * copy the native stuff from the jar to the libs folder
   */
  public void export(String res, String target);
  
  /**
   * to be called from a main() to support standalone features
   */
  public void install(String[] args);
  
  /**
   * 
   * @return the name of this loader
   */
  public String getName();
  
  public String getLibType();
}
