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

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.file.*;

import org.luwrain.base.*;
import org.luwrain.core.events.*;
import org.luwrain.core.queries.*;
import org.luwrain.speech.Channel;

final class LuwrainImpl implements Luwrain
{
    private final Core core;
    private String charsToSkip = "";

    LuwrainImpl(Core core)
    {
	NullCheck.notNull(core, "core");
	this.core = core;
	/**
	Registry registry = core.registry();
	final String path = "/org/luwrain/speech/preprocess-cchars-to-skip";
	if (registry.getTypeOf(path) == Registry.STRING)
	    charsToSkip = registry.getString(path);
	*/
    }

    @Override public CmdLine getCmdLine()
    {
	return core.cmdLine;
    }

    @Override public String getActiveAreaText(AreaTextType type, boolean issueErrorMessages)
    {
	NullCheck.notNull(type, "type");
	core.mainCoreThreadOnly();
	final Area activeArea = core.getValidActiveArea(issueErrorMessages);
	if (activeArea == null)
	    return null;
	return new AreaText(activeArea).get(type);
    }

    //Never returns null, returns user home dir if area doesn't speak about that
    @Override public String getActiveAreaDir()
    {
	final Area area = core.getValidActiveArea(false);
	if (area == null)
	    return getFileProperty("luwrain.dir.userhome").toString();
	final CurrentDirQuery query = new CurrentDirQuery();
	if (!area.onAreaQuery(query) || !query.hasAnswer())
	    return getFileProperty("luwrain.dir.userhome").toString();
	return query.getAnswer();
    }

    @Override public void sendBroadcastEvent(EnvironmentEvent e)
    {
	NullCheck.notNull(e, "e");
	core.enqueueEvent(e);
    }

        @Override public void sendInputEvent(KeyboardEvent e)
    {
	NullCheck.notNull(e, "e");
	core.enqueueEvent(e);
    }


    @Override public void xQuit()
    {
	core.quit();
    }

    /**
     * Returns a path to the directory where the application may safely store
     * its auxiliary data. The returned directory, if it it isn't {@code null},
     * always exists and always belongs to the current user. Meaning,
     * different users get different directories for the same
     * application. Application must be identified with some short string. We
     * discourage using application names starting with "luwrain.", because
     * such names are usually used by the applications from LUWRAIN standard
     * distribution.
     *
     * @param appName A short string for application identification, the same application name will result in the same directory
     * @return The application data directory or {@code null} if the directory cannot be created
     */
    @Override public Path getAppDataDir(String appName)
    {
	NullCheck.notEmpty(appName, "appName");
	if (appName.indexOf("/") >= 0)
	    throw new IllegalArgumentException("appName contains illegal characters");
	final Path res = getFileProperty("luwrain.dir.appdata").toPath().resolve(appName);
	try {
	    Files.createDirectories(res);
	    return res;
	}
	catch(IOException e)
	{
	    Log.error("core", "unable to prepare application data directory:" + res.toString() + ":" + e.getClass().getName() + ":" + e.getMessage());
	    return null;
	}
    }

    @Override public     void closeApp()
    {
	core.closeAppIface(this);
    }

    @Override public Registry getRegistry()
    {
	return core.registry();
    }

    @Override public I18n i18n()
    {
	return core.i18nIface();
    }

    @Override public void crash(Exception e)
    {
	NullCheck.notNull(e, "e");
	e.printStackTrace();
	final Luwrain instance = this;
	runInMainThread(()->core.launchAppCrash(this, e));
    }

    @Override public void launchApp(String shortcutName)
    {
	NullCheck.notNull(shortcutName, "shortcutName");
	core.launchAppIface(shortcutName, new String[0]);
    }

    @Override public void launchApp(String shortcutName, String[] args)
    {
	NullCheck.notNull(shortcutName, "shortcutName");
	NullCheck.notNullItems(args, "args");
	core.launchAppIface(shortcutName, args != null?args:new String[0]);
    }

    @Override public void message(String text)
    {
	NullCheck.notNull(text, "text");
	if (text.trim().isEmpty())
	    return;
	runUiSafely(()->{
		core.braille.textToSpeak(text);
		core.message(text, MessageType.REGULAR);
	    });
    }

    @Override public void message(String text, MessageType messageType)
    {
	NullCheck.notNull(text, "text");
	NullCheck.notNull(messageType, "messageType");
	if (text.trim().isEmpty())
	    return;
	    runUiSafely(()->{
		    core.braille.textToSpeak(text);
	core.message(text, messageType);
		});
    }

    @Override public void message(String text, Sounds sound)
    {
	NullCheck.notNull(text, "text");
	NullCheck.notNull(sound, "sound");
	if (text.trim().isEmpty())
	    return;
	runUiSafely(()->{
		core.braille.textToSpeak(text);
		core.message(text, sound);
	    });
    }

    /**
     * Notifies the environment that the area gets new position of the hot
     * point. This method causes updating of the visual position of the hot
     * point on the screen for low vision users.  Please keep in mind that
     * this method doesn't produce any speech announcement of the new
     * position and you should do that on your own, depending on the
     * behaviour of your application.
     *
     * @param area The area which gets new position of the hot point
     */
    @Override public void onAreaNewHotPoint(Area area)
    {
	NullCheck.notNull(area, "area");
	core.onAreaNewHotPointIface(this, area);
    }

    /**
     * Notifies the environment that the area gets new content. This method
     * causes updating of the visual representation of the area content on
     * the screen for low vision users.  Please keep in mind that this method
     * doesn't produce any speech announcement of the changes and you should
     * do that on your own, depending on the behaviour of your application.
     *
     * @param area The area which gets new content
     */
    @Override public void onAreaNewContent(Area area)
    {
	NullCheck.notNull(area, "area");
	core.onAreaNewContentIface(this, area);
    }

    /**
     * Notifies the environment that the area gets new name. This method
     * causes updating of the visual title of the area on the screen for low
     * vision users.  Please keep in mind that this method doesn't produce
     * any speech announcement of name changes and you should do that on your
     * own, depending on the behaviour of your application.
     *
     * @param area The area which gets new name
     */
    @Override public void onAreaNewName(Area area)
    {
	NullCheck.notNull(area, "area");
	core.onAreaNewNameIface(this, area);
    }

    @Override public void onAreaNewBackgroundSound(Area area)
    {
	NullCheck.notNull(area, "area");
	core.onAreaNewBackgroundSound(this, area);
    }

    //May return -1 if area is not shown on the screen;
    @Override public int getAreaVisibleHeight(Area area)
    {
	NullCheck.notNull(area, "area");
	return core.getAreaVisibleHeightIface(this, area);
    }

    @Override public int getAreaVisibleWidth(Area area)
    {
	NullCheck.notNull(area, "area");
	return core.getAreaVisibleWidthIface(this, area);
    }

    @Override public int getScreenWidth()
    {
	return core.getScreenWidthIface();
    }

    @Override public int getScreenHeight()
    {
	return core.getScreenHeightIface();
    }

    @Override public void announceActiveArea()
    {
	core.announceActiveAreaIface();
    }

    @Override public Clipboard getClipboard()
    {
	return core.getClipboard();
    }

    //Doesn't produce any announcement
    @Override public void onNewAreaLayout()
    {
	core.onNewAreaLayoutIface(this);
    }

    @Override public void openFile(String fileName)
    {
	NullCheck.notNull(fileName, "fileName");
	String[] s = new String[1];
	s[0] = fileName;
	core.openFiles(s);
    }

    @Override public void openFiles(String[] fileNames)
    {
	NullCheck.notNullItems(fileNames, "fileNames");
	core.openFiles(fileNames);
    }


    //never returns null
    @Override public String suggestContentType(java.net.URL url, ContentTypes.ExpectedType expectedType)
    {
	NullCheck.notNull(url, "url");
	NullCheck.notNull(expectedType, "expectedType");
	return core.contentTypes.suggestContentType(url, expectedType);
    }

        //never returns null
        @Override public String suggestContentType(java.io.File file, ContentTypes.ExpectedType expectedType)
    {
	NullCheck.notNull(file, "file");
	NullCheck.notNull(expectedType, "expectedType");
	return core.contentTypes.suggestContentType(file, expectedType);
    }


    /**
    /**
    * Plays one of the system sounds.  This method takes an identifier of
    * the system sound, stops any previous playing, if there was any, and
    * plays. The exact sound is selected depending on user's
    * settings. Please node that sounds playing isn't interfering with
    * speech. 
    *
    * @param sound The identifier of the sound to play
    */
    @Override public void playSound(Sounds sound)
    {
	NullCheck.notNull(sound, "sound");
	runUiSafely(()->core.playSound(sound));
    }

    @Override public void popup(Popup popup)
    {
	NullCheck.notNull(popup, "popup");
	core.popupIface(popup);
    }

    @Override public boolean runCommand(String command)
    {
	NullCheck.notNull(command, "command");
	return core.runCommand(command);
    }

    @Override public org.luwrain.base.CommandLineTool.Instance runCommandLineTool(String name, String[] args, org.luwrain.base.CommandLineTool.Listener listener)
    {
	NullCheck.notNull(name, "name");
	NullCheck.notNullItems(args, "args");
	NullCheck.notNull(listener, "listener");
	return core.commandLineTools.run(name, args, listener);
    }

    @Override public void say(String text)
    {
	NullCheck.notNull(text, "text");
	core.braille.textToSpeak(text);
	core.getSpeech().speak(preprocess(text), 0, 0);
    }

    @Override public void say(String text, Sounds sound)
    {
	NullCheck.notNull(text, "text");
	NullCheck.notNull(sound, "sound");
	playSound(sound);
	say(text);
    }

    @Override public void say(String text, int pitch)
    {
	NullCheck.notNull(text, "text");
	core.braille.textToSpeak(text);
	core.getSpeech().speak(preprocess(text), pitch, 0);
    }

    @Override public void say(String text,
		    int pitch, int rate)
    {
	NullCheck.notNull(text, "text");
	core.getSpeech().speak(preprocess(text), pitch, rate);
    }

    @Override public void sayLetter(char letter)
    {
	core.braille.textToSpeak("" + letter);
	switch(letter)
	{
	case ' ':
	    sayHint(Hint.SPACE);
	    return;
	case '\t':
	    sayHint(Hint.TAB);
	    return;
	}
	final String value = i18n().hasSpecialNameOfChar(letter);
	if (value == null)
	    core.getSpeech().speakLetter(letter, 0, 0); else
	    say(value, Speech.PITCH_HINT);//FIXME:
    }

    @Override public void sayLetter(char letter, int pitch)
    {
	switch(letter)
	{
	case ' ':
	    sayHint(Hint.SPACE);
	    return;
	case '\t':
	    sayHint(Hint.TAB);
	    return;
	}
	final String value = i18n().hasSpecialNameOfChar(letter);
	if (value == null)
	    core.getSpeech().speakLetter(letter, pitch, 0); else
	    say(value, Speech.PITCH_HINT);
    }

    @Override public void speakLetter(char letter,
			    int pitch, int rate)
    {
	switch(letter)
	{
	case ' ':
	    sayHint(Hint.SPACE);
	    return;
	case '\t':
	    sayHint(Hint.TAB);
	    return;
	}
	final String value = i18n().hasSpecialNameOfChar(letter);
	if (value == null)
	    core.getSpeech().speakLetter(letter, pitch, rate); else
	    say(value, Speech.PITCH_HINT);
    }

    @Override public void silence()
    {
	core.getSpeech().silence();
    }

    /**
     * Sets the new active area of the application. This method asks the
     * environment to choose another visible area as an active area of the
     * application. This operation is applicable only to regular areas, not
     * for popup areas, for which it is pointless. In contrast to
     * {@code onAreaNewHotPoint()}, {@code onAreaNewName()} and 
     * {@code onAreaNewContent()} methods, this one produces proper introduction of
     * the area being activated.
     *
     * @param area The area to choose as an active
     */
    @Override public void setActiveArea(Area area)
    {
	NullCheck.notNull(area, "area");
	core.setActiveAreaIface(this, area);
    }

    @Override public String staticStr(LangStatic id)
    {
	NullCheck.notNull(id, "id");
	return i18n().staticStr(id);
    }

    @Override public UniRefInfo getUniRefInfo(String uniRef)
    {
	NullCheck.notNull(uniRef, "uniRef");
	if (uniRef.isEmpty())
	    return null;
	return core.uniRefProcs.getInfo(uniRef);
    }

    @Override public boolean openUniRef(String uniRef)
    {
	NullCheck.notNull(uniRef, "uniRef");
	return core.openUniRefIface(uniRef);
    }

    @Override public boolean openUniRef(UniRefInfo uniRefInfo)
    {
	NullCheck.notNull(uniRefInfo, "uniRefInfo");
	return core.uniRefProcs.open(uniRefInfo.getValue());
    }

    @Override public org.luwrain.browser.Browser createBrowser()
    {
	return core.interaction.createBrowser();
    }

    @Override public Channel getAnySpeechChannelByCond(Set<Channel.Features> cond)
    {
	NullCheck.notNull(cond, "cond");
	return core.getSpeech().getAnyChannelByCond(cond);
    }

    @Override public Channel[] getSpeechChannelsByCond(Set<Channel.Features> cond)
    {
	NullCheck.notNull(cond, "cond");
	return core.getSpeech().getChannelsByCond(cond);
    }

    void runInMainThread(Runnable runnable)
    {
	NullCheck.notNull(runnable, "runnable");
	core.enqueueEvent(new Core.RunnableEvent(runnable));
    }

    @Override public void runUiSafely(Runnable runnable)
    {
	NullCheck.notNull(runnable, "runnable");
	if (!core.isMainCoreThread())
	    runInMainThread(runnable); else
	    runnable.run();
    }

    @Override public Object runLaterSync(Callable callable)
    {
	NullCheck.notNull(callable, "callable");
	final Core.CallableEvent event = new Core.CallableEvent(callable);
	core.enqueueEvent(event);
	try {
	    event.waitForBeProcessed();
	}
	catch(InterruptedException e)
	{
	    Thread.currentThread().interrupt();
	    return null;
	}
	return event.getResult();
    }

    @Override public Object callUiSafely(Callable callable)
    {
	NullCheck.notNull(callable, "callable");
	if (core.isMainCoreThread())
	{
	    try {
		return callable.call(); 
	    }
	    catch(Throwable e)
	    {
		Log.error(Core.LOG_COMPONENT, "exception on processing of CallableEvent:" + e.getClass().getName() + ":" + e.getMessage());
		return null;
	    }
	} else
	    return runLaterSync(callable);
    }

    @Override public int xGetSpeechRate()
    {
	return  core.getSpeech().getRate();
    }

    @Override public void xSetSpeechRate(int value)
    {
	core.getSpeech().setRate(value);
    }

    @Override public int xGetSpeechPitch()
    {
	return core.getSpeech().getPitch();
    }

    @Override public void xSetSpeechPitch(int value)
    {
	core.getSpeech().setPitch(value);
    }

    @Override public String[] getAllShortcutNames()
    {
	return core.objRegistry.getShortcutNames();
    }

    private String preprocess(String s)
    {
	StringBuilder b = new StringBuilder();
	for(int i = 0;i < s.length();++i)
	{
	    final char c = s.charAt(i);
	    int k;
	    for(k = 0;k < charsToSkip.length();++k)
		if (c == charsToSkip.charAt(k))
		    break;
	    if (k >= charsToSkip.length())
		b.append(c);
	}
	return b.toString();
    }

    @Override public java.io.File getFileProperty(String propName)
    {
	NullCheck.notEmpty(propName, "propName");
	return core.props.getFileProperty(propName);
    }


    @Override public OsCommand runOsCommand(String cmd, String dir,
			   OsCommand.Output output, OsCommand.Listener listener)
    {
	NullCheck.notEmpty(cmd, "cmd");
	NullCheck.notNull(dir, "dir");
	return core.os.runOsCommand(cmd, (!dir.isEmpty())?dir:getFileProperty("luwrain.dir.userhome").getAbsolutePath(), output, listener);
    }

    @Override public String getProperty(String propName)
    {
	NullCheck.notEmpty(propName, "propName");
	/*
	if (propName.startsWith("luwrain.speech.channel."))
	{
	    final String arg = propName.substring("luwrain.speech.channel.".length());
	    final String[] args = arg.split("\\.", -1);
	    if (args.length != 2 || 
		args[0].isEmpty() || args[1].isEmpty())
		return "";
	    int n = 0;
	    try {
		n = Integer.parseInt(args[0]);
	    }
	    catch(NumberFormatException e)
	    {
		return "";
	    }
	    final Channel[] channels = core.getSpeech().getAllChannels();
	    if (n >= channels.length)
		return "";
	    final Channel channel = channels[n];
	    switch(args[1])
	    {
	    case "name":
		return channel.getChannelName();
	    case "class":
		return channel.getClass().getName();
	    case "default":
		return core.getSpeech().isDefaultChannel(channel)?"1":"0";
	    case "cansynthtospeakers":
		return channel.getFeatures().contains(Channel.Features.CAN_SYNTH_TO_SPEAKERS)?"1":"0";
	    case "cansynthtostream":
		return channel.getFeatures().contains(Channel.Features.CAN_SYNTH_TO_STREAM)?"1":"0";
	    case "cannotifywhenfinished":
		return channel.getFeatures().contains(Channel.Features.CAN_NOTIFY_WHEN_FINISHED)?"1":"0";
	    default:
		return "";
	    }
	}
	*/
	    return core.props.getProperty(propName);
    }

    @Override public void setEventResponse(EventResponse eventResponse)
    {
	NullCheck.notNull(eventResponse, "eventResponse");
	core.setEventResponse(eventResponse);
    }

@Override public FilesOperations getFilesOperations()
    {
	return core.os.getFilesOperations();
    }

    @Override public org.luwrain.player.Player getPlayer()
    {
	return core.player;
    }

    @Override public org.luwrain.base.MediaResourcePlayer[] getMediaResourcePlayers()
    {
	final List<org.luwrain.base.MediaResourcePlayer> res = new LinkedList();
	res.add(core.wavePlayer);
	for(org.luwrain.base.MediaResourcePlayer p: core.objRegistry.getMediaResourcePlayers())
	    res.add(p);
	return res.toArray(new org.luwrain.base.MediaResourcePlayer[res.size()]);
    }

    @Override public String[] xGetLoadedSpeechFactories()
    {
	return new String[0];
    }

    @Override public boolean runWorker(String workerName)
    {
	NullCheck.notEmpty(workerName, "workerName");
	return core.workers.runExplicitly(workerName);
    }

    @Override public void executeBkg(java.util.concurrent.FutureTask task)
    {
	NullCheck.notNull(task, "task");
	//FIXME:maintaining the registry of executed tasks with their associations to Luwrain objects
	java.util.concurrent.Executors.newSingleThreadExecutor().execute(task);
    }

    /**
     * Registers new extension object.  This operation is allowed for highly
     * privileged interfaces only, what, for example, can be useful for
     * custom startup and shutdown procedures. Custom objects may be of any
     * kind, they are registered as they would be a part of the core.
     *
     * @param extObj The object to register
     * @return True if the object was successfully registered, false otherwise
     * @throws RuntimeException on any attempt to do this operation without enough privileged level
     */
    @Override public boolean registerExtObj(ExtensionObject extObj)
    {
	NullCheck.notNull(extObj, "extObj");
	if (this != core.getObjForEnvironment())
	    throw new RuntimeException("registerExtObj() may be called only for privileged interfaces");
	return core.objRegistry.add(null, extObj);
    }

    @Override public java.util.concurrent.Callable runScriptInFuture(org.luwrain.core.script.Context context, String text)
    {
	NullCheck.notNull(context, "context");
	return core.script.execFuture(this, context, text);
    }

    @Override public String loadScriptExtension(String text) throws org.luwrain.core.extensions.DynamicExtensionException
    {
	NullCheck.notNull(text, "text");
	return core.loadScriptExtension(text);
    }

    @Override public boolean unloadDynamicExtension(String extId)
    {
	NullCheck.notEmpty(extId, "extId");
	return core.unloadDynamicExtension(extId);
    }

    @Override public void xExecScript(String text)
    {
	NullCheck.notNull(text, "text");
	core.script.exec(text);
    }

    private void sayHint(Hint hint)
    {
	NullCheck.notNull(hint, "hint");
		    	final LangStatic staticStrId = EventResponses.hintToStaticStrMap(hint);
	if (staticStrId == null)
	    return;
	say(i18n().staticStr(staticStrId), Speech.PITCH_HINT);
    }
}
