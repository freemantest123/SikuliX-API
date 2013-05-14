/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

public class AppNotFound extends SikuliException {

  public AppNotFound(String msg) {
    super(msg);
    _name = "AppNotFound";
  }
}
