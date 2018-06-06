/*
   Copyright 2012-2018 Michael Pozhidaev <michael.pozhidaev@gmail.com>

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

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.luwrain.base.*;

final class PropertiesRegistry implements PropertiesBase
{
    private final PropertiesProvider[] basicProviders;
    private Provider[] providers = new Provider[0];

    PropertiesRegistry(PropertiesProvider[] basicProviders)
    {
	NullCheck.notNullItems(basicProviders, "basicProviders");
	this.basicProviders = basicProviders;
	setProviders(new PropertiesProvider[0]);//to apply the list of basic providers
    }

    /**
     * Sets new list of providers.
     *
     * @param providers The new providers
     * @return True if all providers are valid and the current list was updated, FALSE otherwise
     */
    boolean setProviders(PropertiesProvider[] providers)
    {
	NullCheck.notNullItems(providers, "providers");
	final List<Provider> newProviders = new LinkedList();
	for(PropertiesProvider p: basicProviders)
	    newProviders.add(new Provider(p));
		for(PropertiesProvider p: providers)
	    newProviders.add(new Provider(p));
		/*
			for(Provider pr: newProviders)
	    if (!pr.isValid())
		return false;
		*/
	this.providers = newProviders.toArray(new Provider[newProviders.size()]);
	return true;
    }

    /**
     * Returns a value of the property.
     *
     * @param propName A name of the property, may not be empty
     * @returns A value of the property or {@code null}, if there is no such property
     */
    @Override public String getProperty(String propName)
    {
	NullCheck.notEmpty(propName, "propName");

		for(Provider p: providers)
		    if (!p.hasResponsibilitySpace())
		    {
			final String value = p.provider.getProperty(propName);
			if (value != null)
			    return value;
		    }
		

		/*
	for(Provider p: providers)
	    if (p.matches(propName))
		return p.provider.getProperty(propName);
		*/
	return "";
	}

    @Override public File getFileProperty(String propName)
    {
	NullCheck.notNull(propName, "propName");

			for(Provider p: providers)
		    if (!p.hasResponsibilitySpace())
		    {
			final File value = p.provider.getFileProperty(propName);
			if (value != null)
			    return value;
		    }

	return null;
    }

    static private final class Provider
    {
	final PropertiesProvider provider;
	final Pattern[] patterns;

	Provider(PropertiesProvider provider)
	{
	    NullCheck.notNull(provider, "provider");
	    this.provider = provider;
	    final String[] regex = provider.getPropertiesRegex();
	    NullCheck.notNullItems(regex, "regex");
	    final List<Pattern> patterns = new LinkedList();
	    for (String r: regex)
		if (!r.isEmpty())
		    patterns.add(Pattern.compile(r));//Pattern.CASE_INSENSITIVE	    this.patterns = patterns.toArray(new Pattern[patterns.size()]);
	    this.patterns = patterns.toArray(new Pattern[patterns.size()]);
	}

	boolean hasResponsibilitySpace()
	{
	    return patterns.length > 0;
	}

	boolean matches(String propName)
	{
	    NullCheck.notEmpty(propName, "propName");
	    for(Pattern p: patterns)
	    {
	    	final Matcher matcher = p.matcher(propName);
		if (matcher.find())
		    return true;
	    }
	    return false;
	}
    }
}
