package com.exactprosystems.jf.app;

import com.sun.jna.Library;

public interface JnaDriver extends Library {
	String lastError();

	void connect(String title);
	void run(String exec, String workDir, String param);
	void stop();
	void refresh();
	String title();
	String listAll(String ownerId, int controlKindId, String uid, String xpath, String clazz, String name, String title, String text);
	String elementAttribute(String elementId, int partId);
	int elementByCoords(int[] resultId, int length, int controlKindId, int x, int y);
	void sendKey(String key);
	void mouse(String elementId, int actionId, int x, int y);
	int findAllForLocator(int[] arr, int len, String ownerId, int controlKindId, String uid, String xpath, String clazz, String name, String title, String text);
	int findAll(int[] arr, int len, String elementId, int scopeId, int propertyId, String value);

	/**
	 * if @param c == -1 -> arg is null;
	 * if @param c == 0 -> arg is array of string with separator %
	 * if @param c == 1 -> arg is array of int with separator %
	 * if @param c == 2 -> arg is array of double with separator %
	 */
	String doPatternCall(String elementId, int patternId, String method, String arg, int c);
	String getProperty(String elementId, int propertyId);
	int getPatterns(int[] arr, int len, String elementId);
	int getImage(int[] arr, int len, String id);
}