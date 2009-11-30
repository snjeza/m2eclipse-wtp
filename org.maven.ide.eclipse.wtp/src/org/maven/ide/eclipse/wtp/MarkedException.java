package org.maven.ide.eclipse.wtp;

/**
 * @author Fred Bricon
 */
public class MarkedException extends Exception {

  private static final long serialVersionUID = 7182756387257983149L;

  public MarkedException() {
  }

  public MarkedException(String message) {
    super(message);
  }

  public MarkedException(Throwable cause) {
    super(cause);
  }

  public MarkedException(String message, Throwable cause) {
    super(message, cause);
  }

}
