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

package org.luwrain.settings;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.cpanel.*;

final class HotKeys extends ListArea<HotKeys.Item> implements SectionArea
{
    private final ControlPanel controlPanel;

    HotKeys(ControlPanel controlPanel, ListArea.Params<Item> params)
    {
	super(params);
	NullCheck.notNull(controlPanel, "controlPanel");
	this.controlPanel = controlPanel;
	setListClickHandler((area, index, item)->editItem(item));
    }

        @Override public boolean saveSectionData()
    {
	return true;
    }


    @Override public boolean onInputEvent(InputEvent event)
    {
	NullCheck.notNull(event, "event");
	if (controlPanel.onInputEvent(event))
	    return true;
	return super.onInputEvent(event);
    }

    @Override public boolean onSystemEvent(SystemEvent event)
    {
	NullCheck.notNull(event, "event");
	if (controlPanel.onSystemEvent(event))
	    return true;
	return super.onSystemEvent(event);
    }

    private boolean editItem(Item item)
    {
	return false;
    }

    static private Item[] loadItems(Registry registry)
    {
	NullCheck.notNull(registry, "registry");
	final LinkedList<Item> res = new LinkedList<Item>();
	for(String d: registry.getDirectories(Settings.GLOBAL_KEYS_PATH))
	{
	    if (d.trim().isEmpty())
		continue;
	    final String path = Registry.join(Settings.GLOBAL_KEYS_PATH, d);
	    res.add(new Item(Settings.createHotKey(registry, path), d));
	}
final Item[] toSort = res.toArray(new Item[res.size()]);
Arrays.sort(toSort);
return toSort;
    }

    static HotKeys create(ControlPanel controlPanel)
    {
	NullCheck.notNull(controlPanel, "controlPanel");
	final Luwrain luwrain = controlPanel.getCoreInterface();
	final ListArea.Params<Item> params = new ListArea.Params<>();
	params.context = new DefaultControlContext(luwrain);
	params.appearance = new ListUtils.DefaultAppearance<>(params.context, Suggestions.LIST_ITEM);
	params.name = "Общие горячие клавиши";//FIXME:
	params.model = new ListUtils.FixedModel<>(loadItems(luwrain.getRegistry()));
	return new HotKeys(controlPanel, params);
    }

        static final class Item implements Comparable
    {
	final InputEvent event;
	final Settings.HotKey settings;
	final String command;
	Item(Settings.HotKey settings, String command)
	{
	    NullCheck.notNull(settings, "settings");
	    NullCheck.notNull(command, "command");
	    this.settings = settings;
	    this.event = new InputEvent(InputEvent.Special.ENTER);
	    this.command = command;
	}
	@Override public String toString()
	{
	    return command;
	}
	@Override public int compareTo(Object o)
	{
	    if (o == null || !(o instanceof Item))
		return 0;
	    return command.compareTo(((Item)o).command);
	}
    }
}
