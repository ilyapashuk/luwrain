/*
   Copyright 2012-2022 Michael Pozhidaev <msp@luwrain.org>

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

import java.util.*;
import java.util.jar.*;
import java.io.*;

public final class ExtensionsManager
{
    static final String
	LOG_COMPONENT = Core.LOG_COMPONENT,
	EXTENSIONS_LIST_PREFIX = "--extensions=";

    private final InterfaceManager interfaces;
    private LoadedExtension[] extensions = new LoadedExtension[0];
    private final List<LoadedExtension> dynamicExtensions = new ArrayList<>();

    ExtensionsManager(InterfaceManager interfaces)
    {
	NullCheck.notNull(interfaces, "interfaces");
	this.interfaces = interfaces;
    }

    void load(InterfaceRequest interfaceRequest, CmdLine cmdLine, ClassLoader classLoader)
    {
	NullCheck.notNull(interfaceRequest, "interfaceRequest");
	NullCheck.notNull(cmdLine, "cmdLine");
	NullCheck.notNull(classLoader, "classLoader");
	final String[] extensionsList = getExtensionsList(cmdLine, classLoader);
	if (extensionsList == null || extensionsList.length == 0)
	    return;
	final List<LoadedExtension> res = new ArrayList<>();
	for(String s: extensionsList)
	{
	    if (s == null || s.trim().isEmpty())
		continue;
	    Log.debug(LOG_COMPONENT, "loading " + s);
	    final Object o;
	    try {
		o = Class.forName(s, true, classLoader).getDeclaredConstructor().newInstance();
	    }
	    catch (Throwable e)
	    {
		Log.error(LOG_COMPONENT, "loading of extension " + s + " failed:" + e.getClass().getName() + ":" + e.getMessage());
		continue;
	    }
	    if (!(o instanceof Extension))
	    {
		Log.error(LOG_COMPONENT, "loading of extension " + s + " failed: this object isn\'t an instance of org.luwrain.core.Extension");
		continue;
	    }
	    final Extension ext = (Extension)o;
	    final Luwrain iface = interfaceRequest.getInterfaceObj(ext);
	    final String message;
	    try {
		message = ext.init(iface);
	    }
	    catch (Throwable ee)
	    {
		Log.error(LOG_COMPONENT, "loading of extension " + s + " failed:" + ee.getClass().getName() + ":" + ee.getMessage());
		interfaces.release(iface);
		continue;
	    }
	    if (message != null)
	    {
		Log.error(LOG_COMPONENT, "loading of extension " + s + " failed: " + message);
		interfaces.release(iface);
		continue;
	    }
	    res.add(createLoadedExtension(ext, iface));
	}
	extensions = res.toArray(new LoadedExtension[res.size()]);
	Log.debug(LOG_COMPONENT, "loaded " + extensions.length + " extensions");
    }

    void close()
    {
	for(LoadedExtension e: extensions)
	{
	    try {
		e.ext.close();
	    }
	    catch (Throwable t)
	    {
		t.printStackTrace();
	    }
	    interfaces.release(e.luwrain);
	}
	extensions = null;
    }

    LoadedExtension[] getAllLoadedExtensions()
    {
	final List<LoadedExtension> res = new ArrayList<>();
	res.addAll(Arrays.asList(extensions));
	res.addAll(dynamicExtensions);
	return res.toArray(new LoadedExtension[res.size()]);
    }

    //From any thread
    public boolean runHooks(String hookName, Luwrain.HookRunner runner)
    {
	NullCheck.notEmpty(hookName, "hookName");
	NullCheck.notNull(runner, "runner");
	for(LoadedExtension e: getAllLoadedExtensions())
	    if (e.ext instanceof HookContainer && !((HookContainer)e.ext).runHooks(hookName, runner))
		return false;
	return true;
    }

    LoadedExtension addDynamicExtension(Extension ext, Luwrain luwrain)
    {
	NullCheck.notNull(ext, "ext");
	NullCheck.notNull(luwrain, "luwrain");
	for(LoadedExtension e: getAllLoadedExtensions())
	    if (e.ext == ext)
		return null;
	final LoadedExtension loadedExt = createLoadedExtension(ext, luwrain);
	dynamicExtensions.add(loadedExt);
	return loadedExt;
    }

    boolean unloadDynamicExtension(Extension ext)
    {
	NullCheck.notNull(ext, "ext");
	final Iterator<LoadedExtension> it = dynamicExtensions.iterator();
	while (it.hasNext())
	    if (it.next().ext == ext)
	    {
		dynamicExtensions.remove(it);
		return true;
	    }
	return false;
    }

    LoadedExtension getDynamicExtensionById(String id)
    {
	NullCheck.notEmpty(id, "id");
	for(LoadedExtension e: dynamicExtensions)
	    if (e.id.equals(id))
		return e;
	return null;
    }

    private LoadedExtension createLoadedExtension(Extension ext, Luwrain iface)
    {
	NullCheck.notNull(ext, "ext");
	NullCheck.notNull(iface, "iface");
	final ExtensionObject[] extObjects = getExtObjects(ext, iface);
	final LoadedExtension loadedExt = new LoadedExtension(ext, iface, extObjects);
	loadedExt.commands = getCommands(ext, iface);
	loadedExt.uniRefProcs = getUniRefProcs(ext, iface);
	loadedExt.controlPanelFactories = getControlPanelFactories(ext, iface);
	return loadedExt;
    }

    private ExtensionObject[] getExtObjects(Extension ext, Luwrain luwrain)
    {
	final List<ExtensionObject> res = new ArrayList<>();
	try {
	    final ExtensionObject[] e = ext.getExtObjects(luwrain);
	    for(int i = 0;i < e.length;++i)
		if (e[i] != null)
		    res.add(e[i]);
	    final Shortcut[] s = ext.getShortcuts(luwrain);
	    for(int i = 0;i < s.length;++i)
		if (s[i] != null)
		    res.add(s[i]);
	    return res.toArray(new ExtensionObject[res.size()]);
	}
	catch (Throwable ee)
	{
	    Log.error(LOG_COMPONENT, "extension " + ext.getClass().getName() + " thrown an exception on providing the list of extension objects:" + ee.getClass().getName() + ":" + ee.getMessage());
	    return new ExtensionObject[0];
	}
    }

    private Command[] getCommands(Extension ext, Luwrain luwrain)
    {
	try {
	    final Command[] res = ext.getCommands(luwrain);
	    return res != null?res:new Command[0];
	}
	catch (Exception ee)
	{
	    Log.error("environment", "extension " + ee.getClass().getName() + " has thrown an exception on providing the list of commands:" + ee.getMessage());
	    ee.printStackTrace();
	    return new Command[0];
	}
    }

    private UniRefProc[] getUniRefProcs(Extension ext, Luwrain luwrain)
    {
	try {
	    final UniRefProc[] res = ext.getUniRefProcs(luwrain);
	    return res != null?res:new UniRefProc[0];
	}
	catch (Exception ee)
	{
	    Log.error("environment", "extension " + ee.getClass().getName() + " has thrown an exception on providing the list of uniRefProcs:" + ee.getMessage());
	    ee.printStackTrace();
	    return new UniRefProc[0];
	}
    }

    private org.luwrain.cpanel.Factory[] getControlPanelFactories(Extension ext, Luwrain luwrain)
    {
	try {
	    final org.luwrain.cpanel.Factory[] res = ext.getControlPanelFactories(luwrain);
	    return res != null?res:new org.luwrain.cpanel.Factory[0];
	}
	catch (Exception ee)
	{
	    Log.error("environment", "extension " + ee.getClass().getName() + " has thrown an exception on providing the list of control panel factories:" + ee.getMessage());
	    ee.printStackTrace();
	    return new org.luwrain.cpanel.Factory[0];
	}
    }

    private String[] getExtensionsListFromManifest(ClassLoader classLoader)
    {
	NullCheck.notNull(classLoader, "classLoader");
	final List<String> res = new ArrayList<>();
	try {
	    Enumeration<java.net.URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
	    while (resources.hasMoreElements())
	    {                                                                                                         
		try {
		    Manifest manifest = new Manifest(resources.nextElement().openStream());
		    Attributes attr = manifest.getAttributes("org/luwrain");
		    if (attr == null)
			continue;
		    final String value = attr.getValue("Extensions");
		    if (value != null)
			res.add(value);
		}
		catch (IOException e)
		{                                                                                                                 
		    e.printStackTrace();
		}
	    }
	}
	catch (IOException ee)
	{
	    ee.printStackTrace();
	}
	return res.toArray(new String[res.size()]);
    }

    private String[] getExtensionsList(CmdLine cmdLine, ClassLoader classLoader)
    {
	NullCheck.notNull(cmdLine, "cmdLine");
	NullCheck.notNull(classLoader, "classLoader");
	final String[] cmdlineExtList = cmdLine.getArgs(EXTENSIONS_LIST_PREFIX);
	if(cmdlineExtList.length > 0)
	{
	    for(String s: cmdlineExtList)
		return s.split(":",-1);
	    return new String[0];
	}
	return getExtensionsListFromManifest(classLoader);
    }

    interface InterfaceRequest 
    {
	Luwrain getInterfaceObj(Extension ext);
    }

    static final class LoadedExtension
    {
	final Extension ext;
	final Luwrain luwrain;
	final String id;
	final ExtensionObject[] extObjects;
	Command[] commands;
	UniRefProc[] uniRefProcs;
	org.luwrain.cpanel.Factory[] controlPanelFactories;
	LoadedExtension(Extension ext, Luwrain luwrain,
			ExtensionObject[] extObjects)
	{
	    NullCheck.notNull(ext, "ext");
	    NullCheck.notNull(luwrain, "luwrain");
	    NullCheck.notNullItems(extObjects, "extObjects");
	    this.ext = ext;
	    this.luwrain = luwrain;
	    this.id = java.util.UUID.randomUUID().toString();
	    this.extObjects = extObjects;
	}
    }
}
