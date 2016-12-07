/*
   Copyright 2012-2016 Michael Pozhidaev <michael.pozhidaev@gmail.com>

   This file is part of LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.core;

import java.util.Properties;
import java.net.URL;
import java.io.IOException;

public class RegistryKeys
{
    private static final String REGISTRY_KEYS_RESOURCE = "org/luwrain/core/registry-keys.properties";

    private Properties properties;

    public RegistryKeys()
    {
	URL url = ClassLoader.getSystemResource(REGISTRY_KEYS_RESOURCE);
	if (url == null)
	{
	    Log.error("core", "no resource with name " + REGISTRY_KEYS_RESOURCE + " needed for registry keys loading");
	    return;
	}
	properties = new Properties();
	try {
	    properties.load(url.openStream());
	}
	catch (IOException e)
	{
	    Log.error("core", "error loading of properties with registry keys:" + e.getMessage());
	    e.printStackTrace();
	}
    }

    private String getProperty(String name)
    {
	if (name == null)
	    throw new NullPointerException("name may not be null");
	if (name.isEmpty())
	    throw new IllegalArgumentException("name may not be empty");
	//	Log.debug("param", name);
	if (properties == null)
	    throw new NullPointerException("properties object is null");
	final String value = properties.getProperty(name);
	if (value == null)
	    throw new NullPointerException("property value is null");
	return value;
    }
};
