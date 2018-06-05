package org.luwrain.core.properties;

import java.io.*;
import java.util.*;

import org.luwrain.base.*;
import org.luwrain.core.*;

public final class Braille implements CorePropertiesProvider
{
    static private final String LOG_COMPONENT = Init.LOG_COMPONENT;

    private final org.luwrain.core.Braille braille;
    private CorePropertiesProvider.Listener listener = null;

    public Braille(org.luwrain.core.Braille braille)
    {
	NullCheck.notNull(braille, "braille");
	this.braille = braille;
    }

    @Override public String getExtObjName()
    {
	return this.getClass().getName();
    }

    @Override public String[] getPropertiesRegex()
    {
	return new String[]{"^luwrain \\.braille\\."};
    }

    @Override public Set<CorePropertiesProvider.Flags> getPropertyFlags(String propName)
    {
	return EnumSet.of(CorePropertiesProvider.Flags.PUBLIC);
    }

    @Override public String getProperty(String propName)
    {
	NullCheck.notEmpty(propName, "propName");
	switch(propName)
	{
	case "luwrain.braille.active":
	    return braille.isActive()?"1":"0";
	case "luwrain.braille.driver":
	    return braille.getDriver();
	case "luwrain.braille.error":
	    return braille.getErrorMessage();
	case "luwrain.braille.displaywidth":
	    return "" + braille.getDisplayWidth();
	case "luwrain.braille.displayheight":
	    return "" + braille.getDisplayHeight();
	default:
	    return null;
	}
    }

    @Override public File getFileProperty(String propName)
    {
	NullCheck.notEmpty(propName, "propName");
	return null;
    }

    @Override public boolean setProperty(String propName, String value)
    {
	NullCheck.notEmpty(propName, "propName");
	NullCheck.notNull(value, "value");
	return false;
    }

    @Override public boolean setFileProperty(String propName, File value)
    {
	NullCheck.notEmpty(propName, "propName");
	NullCheck.notNull(value, "value");
	return false;
    }

    @Override public void setListener(CorePropertiesProvider.Listener listener)
    {
	this.listener = listener;
    }
}
