/*
 * Copyright 2010-2011, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2012
 */
package org.sikuli.script;

public interface EventSubject {

  public void addObserver(EventObserver o);

  public void notifyObserver();
}
