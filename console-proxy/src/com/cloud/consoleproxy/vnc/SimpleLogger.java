package com.cloud.consoleproxy.vnc;

public class SimpleLogger {

  public static void log(String message) {
    System.out.println(getPrefix(1) + " LOG: " + message);
  }

  public static void log(int skipFrames, String message) {
    System.out.println(getPrefix(1+skipFrames) + " LOG: " + message);
  }

  public static void debug(String message) {
    System.out.println(getPrefix(1) + " DEBUG: " + message);
  }

  public static void info(String message) {
    System.out.println(getPrefix(1) + " INFO: " + message);
  }

  public static void warn(String message) {
    System.err.println(getPrefix(1) + " WARN: " + message);
  }

  public static void error(String message) {
    System.err.println(getPrefix(1) + " ERROR: " + message);
  }

  private static String getPrefix(int skipFrames) {
    StackTraceElement frame;
    try {
      throw new RuntimeException();
    } catch (Exception e) {
      frame = e.getStackTrace()[1+skipFrames];
    }

    return "(" + frame.getFileName() + ":" + frame.getLineNumber() + ") " + frame.getMethodName() + "()";
  }

}
