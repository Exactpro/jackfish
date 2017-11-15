////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2009-2017, Exactpro Systems
// Quality Assurance & Related Software Development for Innovative Trading Systems.
// London Stock Exchange Group.
// All rights reserved.
// This is unpublished, licensed software, confidential and proprietary
// information which is the property of Exactpro Systems or its licensors.
////////////////////////////////////////////////////////////////////////////////

package com.exactprosystems.jf.documents.msgdic;

import java.math.BigDecimal;
import java.util.Date;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import com.exactprosystems.jf.api.client.IType;


@XmlType(name = "JavaType")
@XmlEnum
public enum JavaType implements IType
{

    @XmlEnumValue("java.lang.Boolean")
    JAVA_LANG_BOOLEAN(Boolean.class),
    @XmlEnumValue("java.lang.Short")
    JAVA_LANG_SHORT(Short.class),
    @XmlEnumValue("java.lang.Integer")
    JAVA_LANG_INTEGER(Integer.class),
    @XmlEnumValue("java.lang.Long")
    JAVA_LANG_LONG(Long.class),
    @XmlEnumValue("java.lang.Byte")
    JAVA_LANG_BYTE(Byte.class),
    @XmlEnumValue("java.lang.Float")
    JAVA_LANG_FLOAT(Float.class),
    @XmlEnumValue("java.lang.Double")
    JAVA_LANG_DOUBLE(Double.class),
    @XmlEnumValue("java.lang.String")
    JAVA_LANG_STRING(String.class),
    @XmlEnumValue("java.util.Date")
    JAVA_UTIL_DATE(Date.class),
    @XmlEnumValue("java.lang.Character")
    JAVA_LANG_CHARACTER(Character.class),
    @XmlEnumValue("java.math.BigDecimal")
    JAVA_MATH_BIG_DECIMAL(BigDecimal.class),
    @XmlEnumValue("org.threeten.bp.LocalDateTime")
    ORG_THREETEN_BP_LOCALDATETIME(org.threeten.bp.LocalDateTime.class),
    @XmlEnumValue("org.threeten.bp.LocalDate")
    ORG_THREETEN_BP_LOCALDATE(org.threeten.bp.LocalDate.class),
    @XmlEnumValue("org.threeten.bp.LocalTime")
    ORG_THREETEN_BP_LOCALTIME(org.threeten.bp.LocalTime.class)
    ;
    
    
    private final Class<?> type;

	JavaType(Class<?> type)
	{
		this.type = type; 
	}

	public static JavaType fromValue(String v)
	{
		for (JavaType c : JavaType.values())
		{
			if (c.type.getCanonicalName().equals(v))
			{
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

	@Override
	public Class<?> getJavaClass()
	{
		return this.type;
	}
}
