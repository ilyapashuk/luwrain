/*
   Copyright 2012-2021 Michael Pozhidaev <msp@luwrain.org>

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

import org.luwrain.base.*;
import org.luwrain.core.*;

final class JobsTracking 
{
    final ObjRegistry objRegistry;
    private final List<Entry> entries = new ArrayList();

    JobsTracking(ObjRegistry objRegistry)
    {
	NullCheck.notNull(objRegistry, "objRegistry");
	this.objRegistry = objRegistry;
    }

    Job.Instance run(String name, String[] args, Job.Listener listener)
    {
	NullCheck.notEmpty(name, "name");
	NullCheck.notNullItems(args, "args");
	NullCheck.notNull(listener, "listener");
	final Job job = objRegistry.getJob(name);
	if (job == null)
	    return null;
	final Entry entry = new Entry(listener);
	final Job.Instance instance = job.launch(entry, args);
	if (instance == null)
	    return null;
	entry.setInstance(instance);
	entries.add(entry);
	return entry;
    }

    private void onFinish(Entry entry)
    {
	NullCheck.notNull(entry, "entry");
    }

private class Entry implements Job.Listener, Job.Instance
    {
	private final Job.Listener listener;
	private Job.Instance instance = null;

	Entry(Job.Listener listener)
	{
	    NullCheck.notNull(listener, "listener");
	    this.listener = listener;
	}

	@Override public String getInstanceName()
	{
	    if (instance == null)
		return "";
	    return instance.getInstanceName();
	}

	@Override public Job.Status getStatus()
	{
	    if (instance == null)
		return Job.Status.RUNNING;
	    return instance.getStatus();
	}

	@Override public int getExitCode()
	{
	    if (instance == null)
		return 0;
	    return instance.getExitCode();
	}

	@Override public boolean isFinishedSuccessfully()
	{
	    if (instance == null)
		return false;
	    return instance.isFinishedSuccessfully();
	}

	@Override public String getSingleLineState()
	{
	    if (instance == null)
		return "";
	    return instance.getSingleLineState();
	}

	@Override public String[] getMultilineState()
	{
	    if (instance == null)
		return new String[0];
	    return instance.getMultilineState();
	}

	@Override public String[] getNativeState()
	{
	    if (instance == null)
		return new String[0];
	    return instance.getNativeState();
	}

	@Override public void stop()
	{
	    if (instance == null)
		return;
	    instance.stop();
	}

	@Override public void onStatusChange(Job.Instance instance)
	{
	    listener.onStatusChange(this);
	    if (instance.getStatus() == Job.Status.FINISHED)
		onFinish(this);
	}

	@Override public void onSingleLineStateChange(Job.Instance instance)
	{
	    listener.onSingleLineStateChange(this);
	}

	@Override public void onMultilineStateChange(Job.Instance instance)
	{
	    listener.onMultilineStateChange(this);	    
	}

	@Override public void onNativeStateChange(Job.Instance instance)
	{
	    listener.onNativeStateChange(this);
	}

	void setInstance(Job.Instance instance)
	{
	    NullCheck.notNull(instance, "instance");
	    if (this.instance == null)
		this.instance = instance;
	}
    }
}
