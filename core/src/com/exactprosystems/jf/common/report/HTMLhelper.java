////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2009-2015, Exactpro Systems, LLC
//  Quality Assurance & Related Development for Innovative Trading Systems.
//  All rights reserved.
//  This is unpublished, licensed software, confidential and proprietary
//  information which is the property of Exactpro Systems, LLC or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.common.report;

import java.util.HashMap;
import java.util.Map;

public class HTMLhelper
{
	// see http://hotwired.lycos.com/webmonkey/reference/special_characters/
	static Object[][] entities = 
		{
			{ "#39", new Integer(39)}, // ' - apostrophe
			{ "quot", new Integer(34) }, // " - double-quote
			{ "amp", new Integer(38) }, // & - ampersand
			{ "lt", new Integer(60) }, // < - less-than
			{ "gt", new Integer(62) }, // > - greater-than
			{ "nbsp", new Integer(160) }, // non-breaking space
			{ "copy", new Integer(169) }, // ї - copyright
			{ "reg", new Integer(174) }, // R - registered trademark
			{ "Agrave", new Integer(192) }, // А - uppercase A, grave accent
			{ "Aacute", new Integer(193) }, // Б - uppercase A, acute accent
			{ "Acirc", new Integer(194) }, // В - uppercase A, circumflex accent
			{ "Atilde", new Integer(195) }, // Г - uppercase A, tilde
			{ "Auml", new Integer(196) }, // Д - uppercase A, umlaut
			{ "Aring", new Integer(197) }, // Е - uppercase A, ring
			{ "AElig", new Integer(198) }, // Ж - uppercase AE
			{ "Ccedil", new Integer(199) }, // З - uppercase C, cedilla
			{ "Egrave", new Integer(200) }, // И - uppercase E, grave accent
			{ "Eacute", new Integer(201) }, // Й - uppercase E, acute accent
			{ "Ecirc", new Integer(202) }, // К - uppercase E, circumflex accent
			{ "Euml", new Integer(203) }, // Л - uppercase E, umlaut
			{ "Igrave", new Integer(204) }, // М - uppercase I, grave accent
			{ "Iacute", new Integer(205) }, // Н - uppercase I, acute accent
			{ "Icirc", new Integer(206) }, // О - uppercase I, circumflex accent
			{ "Iuml", new Integer(207) }, // П - uppercase I, umlaut
			{ "ETH", new Integer(208) }, // Р - uppercase Eth, Icelandic
			{ "Ntilde", new Integer(209) }, // С - uppercase N, tilde
			{ "Ograve", new Integer(210) }, // Т - uppercase O, grave accent
			{ "Oacute", new Integer(211) }, // У - uppercase O, acute accent
			{ "Ocirc", new Integer(212) }, // Ф - uppercase O, circumflex accent
			{ "Otilde", new Integer(213) }, // Х - uppercase O, tilde
			{ "Ouml", new Integer(214) }, // Ц - uppercase O, umlaut
			{ "Oslash", new Integer(216) }, // Ш - uppercase O, slash
			{ "Ugrave", new Integer(217) }, // Щ - uppercase U, grave accent
			{ "Uacute", new Integer(218) }, // Ъ - uppercase U, acute accent
			{ "Ucirc", new Integer(219) }, // Ы - uppercase U, circumflex accent
			{ "Uuml", new Integer(220) }, // Ь - uppercase U, umlaut
			{ "Yacute", new Integer(221) }, // Э - uppercase Y, acute accent
			{ "THORN", new Integer(222) }, // Ю - uppercase THORN, Icelandic
			{ "szlig", new Integer(223) }, // Я - lowercase sharps, German
			{ "agrave", new Integer(224) }, // а - lowercase a, grave accent
			{ "aacute", new Integer(225) }, // б - lowercase a, acute accent
			{ "acirc", new Integer(226) }, // в - lowercase a, circumflex accent
			{ "atilde", new Integer(227) }, // г - lowercase a, tilde
			{ "auml", new Integer(228) }, // д - lowercase a, umlaut
			{ "aring", new Integer(229) }, // е - lowercase a, ring
			{ "aelig", new Integer(230) }, // ж - lowercase ae
			{ "ccedil", new Integer(231) }, // з - lowercase c, cedilla
			{ "egrave", new Integer(232) }, // и - lowercase e, grave accent
			{ "eacute", new Integer(233) }, // й - lowercase e, acute accent
			{ "ecirc", new Integer(234) }, // к - lowercase e, circumflex accent
			{ "euml", new Integer(235) }, // л - lowercase e, umlaut
			{ "igrave", new Integer(236) }, // м - lowercase i, grave accent
			{ "iacute", new Integer(237) }, // н - lowercase i, acute accent
			{ "icirc", new Integer(238) }, // о - lowercase i, circumflex accent
			{ "iuml", new Integer(239) }, // п - lowercase i, umlaut
			{ "igrave", new Integer(236) }, // м - lowercase i, grave accent
			{ "iacute", new Integer(237) }, // н - lowercase i, acute accent
			{ "icirc", new Integer(238) }, // о - lowercase i, circumflex accent
			{ "iuml", new Integer(239) }, // п - lowercase i, umlaut
			{ "eth", new Integer(240) }, // р - lowercase eth, Icelandic
			{ "ntilde", new Integer(241) }, // с - lowercase n, tilde
			{ "ograve", new Integer(242) }, // т - lowercase o, grave accent
			{ "oacute", new Integer(243) }, // у - lowercase o, acute accent
			{ "ocirc", new Integer(244) }, // ф - lowercase o, circumflex accent
			{ "otilde", new Integer(245) }, // х - lowercase o, tilde
			{ "ouml", new Integer(246) }, // ц - lowercase o, umlaut
			{ "oslash", new Integer(248) }, // ш - lowercase o, slash
			{ "ugrave", new Integer(249) }, // щ - lowercase u, grave accent
			{ "uacute", new Integer(250) }, // ъ - lowercase u, acute accent
			{ "ucirc", new Integer(251) }, // ы - lowercase u, circumflex accent
			{ "uuml", new Integer(252) }, // ь - lowercase u, umlaut
			{ "yacute", new Integer(253) }, // э - lowercase y, acute accent
			{ "thorn", new Integer(254) }, // ю - lowercase thorn, Icelandic
			{ "yuml", new Integer(255) }, // я - lowercase y, umlaut
			{ "euro", new Integer(8364) }, // Euro symbol
		};
	static Map<String, Integer> e2i = new HashMap<String, Integer>();
	static Map<Integer, String> i2e = new HashMap<Integer, String>();
	static
	{
		for (int i = 0; i < entities.length; ++i)
		{
			e2i.put((String)entities[i][0], (Integer)entities[i][1]);
			i2e.put((Integer)entities[i][1], (String)entities[i][0]);
		}
	}

	/**
	 * Turns funky characters into HTML entity equivalents
	 * <p>
	 * e.g. <tt>"bread" & "butter"</tt> =>
	 * <tt>&amp;quot;bread&amp;quot; &amp;amp; &amp;quot;butter&amp;quot;</tt>.
	 * Update: supports nearly all HTML entities, including funky accents. See
	 * the source code for more detail.
	 * 
	 * @see #htmlunescape(String)
	 **/
	public static String htmlescape(String s1)
	{
		if (s1 == null)
		{
			return null;
		}
		StringBuffer buf = new StringBuffer();
		int i;
		for (i = 0; i < s1.length(); ++i)
		{
			char ch = s1.charAt(i);
			String entity = (String) i2e.get(new Integer((int) ch));
			if (entity == null)
			{
				if (((int) ch) > 128)
				{
					buf.append("&#" + ((int) ch) + ";");
				} 
				else if (ch == '\n')
				{
					buf.append("<p>");
				}
				else
				{
					buf.append(ch);
				}
			} else
			{
				buf.append("&" + entity + ";");
			}
		}
		return buf.toString();
	}

	/**
	 * Given a string containing entity escapes, returns a string containing the
	 * actual Unicode characters corresponding to the escapes.
	 * 
	 * @see #htmlescape(String)
	 **/
	public static String htmlunescape(String s1)
	{
		if (s1 == null)
		{
			return null;
		}
		StringBuffer buf = new StringBuffer();
		int i;
		for (i = 0; i < s1.length(); ++i)
		{
			char ch = s1.charAt(i);
			if (ch == '&')
			{
				int semi = s1.indexOf(';', i + 1);
				if (semi == -1)
				{
					buf.append(ch);
					continue;
				}
				String entity = s1.substring(i + 1, semi);
				Integer iso;
				if (entity.charAt(0) == '#')
				{
					iso = new Integer(entity.substring(1));
				} else
				{
					iso = (Integer) e2i.get(entity);
				}
				if (iso == null)
				{
					buf.append("&" + entity + ";");
				} else
				{
					buf.append((char) (iso.intValue()));
				}
				i = semi;
			} else
			{
				buf.append(ch);
			}
		}
		return buf.toString();
	}
}
