/*
   Copyright 2012-2016 Michael Pozhidaev <michael.pozhidaev@gmail.com>

   This file is part of the LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.controls;

//FIXME:DESCRIBE;
//FIXME:ControlEnvironment interface support;

import java.util.*;
import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.core.queries.*;
import org.luwrain.util.*;

public class ListArea  implements Area, RegionProvider
{
    public enum Flags {EMPTY_LINE_TOP, EMPTY_LINE_BOTTOM, CYCLING};

    static protected final Set<Appearance.Flags> NONE_APPEARANCE_FLAGS = EnumSet.noneOf(Appearance.Flags.class);
    static protected final Set<Appearance.Flags> BRIEF_ANNOUNCEMENT_ONLY = EnumSet.of(Appearance.Flags.BRIEF);

    protected final RegionTranslator region = new RegionTranslator(this);
    protected ControlEnvironment environment;
    protected String areaName = "";
    protected Model model;
    protected Appearance appearance;
    protected Set<Flags> flags;
    protected ListClickHandler clickHandler;
    protected ItemsLayout itemsLayout = new ListUtils.DefaultItemsLayout();
    protected int hotPointX = 0;
    protected int hotPointY = 0;

    public ListArea(Params params)
    {
	NullCheck.notNull(params, "params");
	NullCheck.notNull(params.environment, "params.environment");
	NullCheck.notNull(params.model, "params.model");
	NullCheck.notNull(params.appearance, "params.appearance");
	NullCheck.notNull(params.name, "params.name");
	NullCheck.notNull(params.flags, "params.flags");
	this.environment = params.environment;
	this.model = params.model;
	this.appearance = params.appearance;
	this.clickHandler = params.clickHandler;
	this.areaName = params.name;
	this.flags = params.flags;
	itemsLayout.setFlags(params.flags);
	resetHotPoint();
    }

    public void setClickHandler(ListClickHandler clickHandler)
    {
	this.clickHandler = clickHandler;
    }

    public Model model()
    {
	return model;
    }

    public Appearance getAppearance()
    {
	return appearance;
    }

    /**
     * Returns the object in the model corresponding to current hot point
     * position.  If the model is empty or hot point is on an empty line,
     * this method always returns {@code null}. 
     *
     * @return The object in the model associated with the currently selected line or {@code null} if there is no any
     */
    public final Object selected()
    {
	final int index = hotPointY - itemsLayout.numberOfEmptyLinesTop();
	return (index >= 0 && index < model.getItemCount())?model.getItem(index):null;
    }

    /**
     * The index of the item in the model which is under the hot point in
     * this list. This method returns the index in the model, not on the
     * screen. It means that the value returned by this method may be
     * different than the value returned by {@code getHotPointY()} (but may
     * be equal as well). If the list is empty or an empty line is selected,
     * this method returns -1. 
     *
     * @return The index of the selected line in the model or -1 if there is no any
     */
    public final int selectedIndex()
    {
	final int index = hotPointY - itemsLayout.numberOfEmptyLinesTop();
	return (index >= 0 && index < model.getItemCount())?index:-1;
    }

    /*
    public void selectEmptyLine()
    {
	hotPointX = 0;
	hotPointY = model.getItemCount();
	environment.onAreaNewHotPoint(this);
    }
    */

    /**
     * Searches for the item in the model and sets hot point on it. Given an
     * arbitrary object, this method looks through all items in the model and
     * does a couple of checks: literal pointers equality and a check with
     * {@code equals()} method. If at least one of these checks succeeds, the
     * item is considered equal to the given one, and hot points is set on
     * it.  
     *
     * @param obj The object to search for
     * @param introduce Must be true if it is necessary to introduce the object, once it's found
     * @return True if the request object is found, false otherwise
     */
    public boolean find(Object obj, boolean introduce)
    {
	NullCheck.notNull(obj, "obj");
	for(int i = 0;i < model.getItemCount();++i)
	{
	    final Object o = model.getItem(i);
	    if (o == null ||
		(obj != o && !obj.equals(o)))
	continue;
	hotPointY = i;
	hotPointX = appearance.getObservableLeftBound(o);
	environment.onAreaNewHotPoint(this);
	if (introduce)
	    appearance.announceItem(o, NONE_APPEARANCE_FLAGS);
	return true;
	}
	return false;
    }

    /**
     * Selects the item by its index. Given the non-negative integer value as
     * an index, this method sets the hot point on the item addressed with
     * this index, checking only that index is in appropriate bounds. Index must address
     * the object as a number in the model, ignoring any empty lines.
     *
     * @param index The item index to select
     * @param announce Must be true, if it is necessary to announce the item , once it has been selected
     * @return True if the index is valid and the item gets hot point on it
     */
    public boolean select(int index, boolean announce)
    {
	if (index < 0 || index >= model.getItemCount())
	    return false;
	hotPointY = index + itemsLayout.numberOfEmptyLinesTop();
	final Object item = model.getItem(index);
	if (item != null)
	{
	    hotPointX = appearance.getObservableLeftBound(item);
	    if (announce)
		appearance.announceItem(item, NONE_APPEARANCE_FLAGS);
	} else
	{
	    hotPointX = 0;
	    if (announce)
		environment.hint(Hints.EMPTY_LINE);
	}
	environment.onAreaNewHotPoint(this);
	return true;
    }

    public int itemIndexOnLine(int index)
    {
	final int linesTop = itemsLayout.numberOfEmptyLinesTop();
	if (index < linesTop)
	    return -1;
	if (index - linesTop < model.getItemCount())
	    return index - linesTop;
	return -1;
    }

    public int getLineIndexByItemIndex(int index)
    {
	final int linesTop = itemsLayout.numberOfEmptyLinesTop();
	return index + linesTop;
    }

    public void reset(boolean introduce)
    {
	EnvironmentEvent.resetRegionPoint(this);
	resetHotPoint(introduce);
    }

    public void resetHotPoint()
    {
	resetHotPoint(false);
    }

    public void resetHotPoint(boolean introduce)
    {
	hotPointY = 0;
	final int count = model.getItemCount();
	if (count < 1)
	{
	    hotPointX = 0;
	    environment.onAreaNewHotPoint(this);
	    return;
	}
	final Object item = model.getItem(0);
	if (item != null)
	{
	    hotPointX = item != null?appearance.getObservableLeftBound(item):0;
	    if (introduce)
		appearance.announceItem(item, NONE_APPEARANCE_FLAGS);
	} else
	{
	    hotPointX = 0;
	    environment.hint(Hints.EMPTY_LINE);
	}
	environment.onAreaNewHotPoint(this);
    }

    public void announceSelected()
    {
	final Object item = selected();
	if (item != null)
	    appearance.announceItem(item, NONE_APPEARANCE_FLAGS);
    }

    /**
     * Refreshes the content of the list. This method calls {@code refresh()}
     * method of the model and displays new items. It does not produce any
     * speech announcement of the change. HotPointY is preserved if it is
     * possible (meaning, the new number of lines not less than old value of
     * hotPointY), but hotPointX is moved to the beginning of the line.
     */
    public void refresh()
    {
	model.refresh();
	final int count = model.getItemCount();
	if (count == 0)
	{
	    hotPointX = 0;
	    hotPointY = 0;
	    environment.onAreaNewContent(this);
	    environment.onAreaNewHotPoint(this);
	    return;
	}
	hotPointY = hotPointY < count?hotPointY :count - 1;
	final Object item = model.getItem(hotPointY);
	if (item != null)
	    hotPointX = appearance.getObservableLeftBound(item); else
	    hotPointX = 0;
	environment.onAreaNewContent(this);
	environment.onAreaNewHotPoint(this);
    }

    public boolean isEmpty()
    {
	return model.getItemCount() <= 0;
    }

    @Override public boolean onKeyboardEvent(KeyboardEvent event)
    {
	NullCheck.notNull(event, "event");
	if (!event.isSpecial() && (!event.isModified() || event.withShiftOnly()))
	    return onChar(event);
	if (event == null)
	    throw new NullPointerException("event may not be null");
	if (!event.isSpecial() || event.isModified())
	    return false;
	switch(event.getSpecial())
	{
	case ARROW_DOWN:
	    return onArrowDown(event, false);
	case ARROW_UP:
	    return onArrowUp(event, false);
	case ARROW_RIGHT:
	    return onArrowRight(event);
	case ARROW_LEFT:
	    return onArrowLeft(event);
	case ALTERNATIVE_ARROW_DOWN:
	    return onArrowDown(event, true);
	case ALTERNATIVE_ARROW_UP:
	    return onArrowUp(event, true);
	case ALTERNATIVE_ARROW_RIGHT:
	    return onAltRight(event);
	case ALTERNATIVE_ARROW_LEFT:
	    return onAltLeft(event);
	case HOME:
	    return onHome(event);
	case END:
	    return onEnd(event);
	case ALTERNATIVE_HOME:
	    return onAltHome(event);
	case ALTERNATIVE_END:
	    return onAltEnd(event);
	case PAGE_DOWN:
	    return onPageDown(event, false);
	case PAGE_UP:
	    return onPageUp(event, false);
	case  ALTERNATIVE_PAGE_DOWN:
	    return onPageDown(event, true);
	case ALTERNATIVE_PAGE_UP:
	    return onPageUp(event, true);
	case INSERT:
	    return onInsert(event);
	case ENTER:
	    return onEnter(event);
	default:
	    return false;
	}
    }

    @Override public boolean onEnvironmentEvent(EnvironmentEvent event)
    {
	NullCheck.notNull(event, "event");
	if (event.getType() != EnvironmentEvent.Type.REGULAR)
	    return false;
	switch (event.getCode())
	{
	case REFRESH:
	    refresh();
	    return true;
	case INTRODUCE:
	    return onAnnounce();
	case ANNOUNCE_LINE:
	    return onAnnounceLine();
	case OK:
	    return onOk(event);
	case LISTENING_FINISHED:
	    if (event instanceof ListeningFinishedEvent)
		return onListeningFinishedEvent((ListeningFinishedEvent)event);
	    return false;
	case MOVE_HOT_POINT:
	    if (event instanceof MoveHotPointEvent)
		return onMoveHotPoint((MoveHotPointEvent)event);
	    return false;
	default:
	    return region.onEnvironmentEvent(event, hotPointX, hotPointY);
	}
    }

    @Override public boolean onAreaQuery(AreaQuery query)
    {
	NullCheck.notNull(query, "query");
	switch(query.getQueryCode())
	{
	case AreaQuery.BEGIN_LISTENING:
	    if (query instanceof BeginListeningQuery)
		return onBeginListeningQuery((BeginListeningQuery)query);
		return false;
	default:
	    return region.onAreaQuery(query, hotPointX, hotPointY);
	}
    }

    @Override public Action[] getAreaActions()
    {
	return new Action[0];
    }

    @Override public int getLineCount()
    {
	final int res = model.getItemCount() + itemsLayout.numberOfEmptyLinesTop() + itemsLayout.numberOfEmptyLinesBottom();
	return res>= 1?res:1;
    }

    @Override public String getLine(int index)
    {
	if (isEmpty())
	    return index == 0?noContentStr():"";
	final int modelIndex = index - itemsLayout.numberOfEmptyLinesTop();
	if (modelIndex < 0 || modelIndex >= model.getItemCount())
	    return "";
	final Object res = model.getItem(modelIndex);
	return res != null?appearance.getScreenAppearance(res, NONE_APPEARANCE_FLAGS):"";
    }

    @Override public int getHotPointX()
    {
	return hotPointX >= 0?hotPointX:0;
    }

    @Override public int getHotPointY()
    {
	return hotPointY >= 0?hotPointY:0;
    }

    @Override public String getAreaName()
    {
	return areaName;
    }

    public void setAreaName(String areaName)
    {
	NullCheck.notNull(areaName, "areaName");
	this.areaName = areaName;
    }

    protected boolean onAnnounce()
    {
	environment.playSound(Sounds.INTRO_REGULAR);
	String item = "";
	if (selected() != null)
	    item = appearance.getScreenAppearance(selected(), EnumSet.noneOf(Appearance.Flags.class)).trim();
	if (!item.isEmpty())
	    item = " " + item;
	environment.say(getAreaName() + item);
	return true;
    }

    protected boolean onAnnounceLine()
    {
	if (isEmpty())
	    return false;
	final Object item = selected();
	if (item == null)
	{
	    environment.hint(Hints.EMPTY_LINE);
	    return true;
	}
	appearance.announceItem(item, NONE_APPEARANCE_FLAGS);
	return true;
    }

    protected boolean onMoveHotPoint(MoveHotPointEvent event)
    {
	NullCheck.notNull(event, "event");
	final int x = event.getNewHotPointX();
	final int y = event.getNewHotPointY();
	final int newY;
	if (y >= getLineCount())
	{
	    if (event.precisely())
		return false;
newY = getLineCount() - 1;
	} else
newY = y;
	    if (itemIndexOnLine(newY) >= 0)
	    {
		//Line with item, not empty
		final Object item = model.getItem(itemIndexOnLine(newY));
		final int leftBound = appearance.getObservableLeftBound(item);
final int rightBound = appearance.getObservableRightBound(item);
		if (event.precisely() &&
		    (x < leftBound || x > rightBound))
		    return false;
		hotPointY = newY;
		hotPointX = x;
		if (hotPointX < leftBound)
		    hotPointX = leftBound;
		if (hotPointX > rightBound)
		    hotPointX = rightBound;
		environment.onAreaNewHotPoint(this);
		return true;
	    }
	    //On empty line
	    hotPointY = newY;
	    hotPointX = 0;
	    environment.onAreaNewHotPoint(this);
	    return true;
    }

    protected boolean onBeginListeningQuery(BeginListeningQuery query)
    {
	NullCheck.notNull(query, "query");
	final int index = selectedIndex();
	if (index < 0)
	    return false;
	final int count = model.getItemCount();
	if (index >= count)
	    return false;
	final Object current = model.getItem(index);
	final String text = appearance.getScreenAppearance(current, NONE_APPEARANCE_FLAGS).substring(hotPointX, appearance.getObservableRightBound(current));
	//	Log.debug("listen", appearance.getScreenAppearance(current, NONE_APPEARANCE_FLAGS));
	//	Log.debug("listen", "" + hotPointX);
	if (text.isEmpty() && index + 1 >= count)
	    return false;
	if (index + 1 < count)
	{
	    final Object next = model.getItem(index + 1);
	    query.answer(new BeginListeningQuery.Answer(text, new ListeningInfo(index + 1, appearance.getObservableLeftBound(next))));
	} else
	    query.answer(new BeginListeningQuery.Answer(text, new ListeningInfo(index, appearance.getObservableRightBound(current))));
	return true;
    }

    protected boolean onListeningFinishedEvent(ListeningFinishedEvent event)
    {
	NullCheck.notNull(event, "event");
	if (!(event.getExtraInfo() instanceof ListeningInfo))
	    return false;
	final ListeningInfo info = (ListeningInfo)event.getExtraInfo();
	final int count = model.getItemCount();
	if (info.itemIndex >= count)
	    return false;
	final Object item = model.getItem(info.itemIndex);
	final int leftBound = appearance.getObservableLeftBound(item);
	final int rightBound = appearance.getObservableRightBound(item);
	if (info.pos < leftBound || info.pos > rightBound)
	    return false;
	hotPointY = getLineIndexByItemIndex(info.itemIndex);
	hotPointX = info.pos;
	environment.onAreaNewHotPoint(this);
	return true;
    }

    protected boolean onChar(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	final int count = model.getItemCount();
	final char c = event.getChar();
	String beginning = "";
	if (hotPointY < count)
	{
	    if (hotPointX >= appearance.getObservableRightBound(model.getItem(hotPointY)))
		return false;
	    final String name = appearance.getScreenAppearance(model.getItem(hotPointY), NONE_APPEARANCE_FLAGS);
	    final int pos = hotPointX < name.length()?hotPointX:name.length();
	    beginning = name.substring(0, pos);
	}
	final String mustBegin = beginning + c;
	for(int i = 0;i < count;++i)
	{
	    final String name = appearance.getScreenAppearance(model.getItem(i), NONE_APPEARANCE_FLAGS);
	    if (!name.startsWith(mustBegin))
		continue;
	    hotPointY = i;
	    ++hotPointX;
	    //	    onNewHotPointY(false);
	    appearance.announceItem(model.getItem(hotPointY), NONE_APPEARANCE_FLAGS);
	    environment.onAreaNewHotPoint(this);
	    return true;
	}
	return false;
    }

    protected boolean onArrowDown(KeyboardEvent event, boolean briefAnnouncement)
    {
	if (noContent())
	    return true;
	final int newHotPointY = itemsLayout.oneLineDown(hotPointY, model.getItemCount());
	if (newHotPointY < 0)
	{
	    environment.hint(Hints.NO_ITEMS_BELOW);
		return true;
	}
	hotPointY = newHotPointY;
	onNewHotPointY(briefAnnouncement);
	return true;
    }

    protected boolean onArrowUp(KeyboardEvent event, boolean briefAnnouncement)
    {
	if (noContent())
	return true;
	final int newHotPointY = itemsLayout.oneLineUp(hotPointY, model.getItemCount());
	if (newHotPointY < 0)
	{
	    environment.hint(Hints.NO_ITEMS_ABOVE);
	    return true;
	}
	hotPointY = newHotPointY;
	    onNewHotPointY(briefAnnouncement);
	return true;
    }

    private boolean onPageDown(KeyboardEvent event, boolean briefAnnouncement)
    {
	if (noContent())
	    return true;
	final int count = model.getItemCount();
	if (hotPointY >= count)
	{
	    environment.hint(Hints.NO_ITEMS_BELOW);
		return true;
	}
	hotPointY += environment.getAreaVisibleHeight(this);
	if (hotPointY >= count)
	    hotPointY = count;
	onNewHotPointY(briefAnnouncement);
	return true;
    }

    private boolean onPageUp(KeyboardEvent event, boolean briefAnnouncement)
    {
	if (noContent())
	    return true;
	if (hotPointY <= 0)
	{
	    environment.hint(Hints.NO_ITEMS_ABOVE);
	    return true;
	}
	final int height = environment.getAreaVisibleHeight(this);
	if (hotPointY > height)
	hotPointY -= height; else
	    hotPointY = 0;
	onNewHotPointY(briefAnnouncement);
	return true;
    }

    private boolean onEnd(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	hotPointY = model.getItemCount() + itemsLayout.numberOfEmptyLinesTop();
	if (itemsLayout.numberOfEmptyLinesBottom() <= 0)
	    --hotPointY;
	onNewHotPointY(false);
	return true;
    }

    private boolean onHome(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	hotPointY = itemsLayout.numberOfEmptyLinesTop();
	onNewHotPointY(false);
	return true;
    }

    private boolean onArrowRight(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	final Object item = selected();
	if (item == null)
	{
	    environment.hint(Hints.EMPTY_LINE);
	    return true;
	}
	final String line = appearance.getScreenAppearance(item, NONE_APPEARANCE_FLAGS);
	NullCheck.notNull(line, "line");
	if (line.isEmpty())
	{
	    environment.hint(Hints.EMPTY_LINE);
	    return true;
	}
final int rightBound = appearance.getObservableRightBound(item);
	if (hotPointX >= rightBound)
	{
	    environment.hint(Hints.END_OF_LINE);
	    return true;
	}
	++hotPointX;
	announceChar(line, hotPointX);
	environment.onAreaNewHotPoint(this);
	return true;
    }

    protected boolean onArrowLeft(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	final Object item = selected();
	if (item == null)
	{
	    environment.hint(Hints.EMPTY_LINE);
	    return true;
	}
	final String line = appearance.getScreenAppearance(item, NONE_APPEARANCE_FLAGS);
	NullCheck.notNull(line, "line");
	if (line.isEmpty())
	{
	    environment.hint(Hints.EMPTY_LINE);
	    return true;
	}
	final int leftBound = appearance.getObservableLeftBound(item);
	if (hotPointX <= leftBound)
	{
	    environment.hint(Hints.BEGIN_OF_LINE);
	    return true;
	}
	--hotPointX;
	announceChar(line, hotPointX);
	environment.onAreaNewHotPoint(this);
	return true;
    }

    private boolean onAltRight(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	final Object item = selected();
	if (item == null)
	{
	    environment.hint(Hints.EMPTY_LINE);
	    return true;
	}
	final String line = appearance.getScreenAppearance(item, NONE_APPEARANCE_FLAGS);
	NullCheck.notNull(line, "line");
	if (line.isEmpty())
	{
	    environment.hint(Hints.EMPTY_LINE);
	    return true;
	}
		final int leftBound = appearance.getObservableLeftBound(item);
final int rightBound = appearance.getObservableRightBound(item);
	if (hotPointX >= rightBound)
	{
	    environment.hint(Hints.END_OF_LINE);
	    return true;
	}
	final String subline = line.substring(leftBound, rightBound);
	final WordIterator it = new WordIterator(subline, hotPointX - leftBound);
	if (!it.stepForward())
	{
	    environment.hint(Hints.END_OF_LINE);
	    return true;
	}
	hotPointX = it.pos() + leftBound;
	if (it.announce().length() > 0)
	    environment.say(it.announce()); else
	    environment.hint(Hints.END_OF_LINE);
	environment.onAreaNewHotPoint(this);
	return true;
    }

    private boolean onAltLeft(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	final Object item = selected();
	if (item == null)
	{
	    environment.hint(Hints.EMPTY_LINE);
	    return true;
	}
	final String line = appearance.getScreenAppearance(item, NONE_APPEARANCE_FLAGS);
	NullCheck.notNull(line, "line");
	if (line.isEmpty())
	{
	    environment.hint(Hints.EMPTY_LINE);
	    return true;
	}
	final int leftBound = appearance.getObservableLeftBound(item);
	final int rightBound = appearance.getObservableRightBound(item);
	if (hotPointX <= leftBound)
	{
	    environment.hint(Hints.BEGIN_OF_LINE);
	    return true;
	}
	final String subline = line.substring(leftBound, rightBound);
	final WordIterator it = new WordIterator(subline, hotPointX - leftBound);
	if (!it.stepBackward())
	{
	    environment.hint(Hints.BEGIN_OF_LINE);
	    return true;
	}
	hotPointX = it.pos() + leftBound;
	    environment.say(it.announce());
	environment.onAreaNewHotPoint(this);
	return true;
    }

    private boolean onAltEnd(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	final Object item = selected();
	if (item == null)
	{
	    environment.hint(Hints.EMPTY_LINE);
	    return true;
	}
	final String line = appearance.getScreenAppearance(item, NONE_APPEARANCE_FLAGS);
	NullCheck.notNull(line, "line");
	hotPointX = appearance.getObservableRightBound(item);
	environment.hint(Hints.END_OF_LINE);
	environment.onAreaNewHotPoint(this);
	return true;
    }

    private boolean onAltHome(KeyboardEvent event)
    {
	if (noContent())
	    return true;
	final Object item = selected();
	if (item == null)
	{
	    environment.hint(Hints.EMPTY_LINE);
	    return true;
	}
	final String line = appearance.getScreenAppearance(item, NONE_APPEARANCE_FLAGS);
	NullCheck.notNull(line, "line");
	hotPointX = appearance.getObservableLeftBound(item);
	announceChar(line, hotPointX);
	environment.onAreaNewHotPoint(this);
	return true;
    }

    protected boolean onInsert(KeyboardEvent event)
    {
	final int index = selectedIndex();
	if (index < 0)
	    return false;
	if (!model.toggleMark(index))
	    return false;
	environment.onAreaNewContent(this);
	if (index + 1 < model.getItemCount() || itemsLayout.numberOfEmptyLinesBottom() > 0)
	{
	    ++hotPointY;
	    onNewHotPointY(false); 
	}
	return true;
    }

    protected boolean onEnter(KeyboardEvent event)
    {
	if (isEmpty() || clickHandler == null)
	    return false;
	    final int count = model.getItemCount();
	    final int index = hotPointY - itemsLayout.numberOfEmptyLinesTop();
	    if (index < 0 || index >= count)
		return false;
	    return clickHandler.onListClick(this, index, model.getItem(index));
    }

    private boolean onOk(EnvironmentEvent event)
    {
	if (clickHandler == null)
	    return false;
	final int index = selectedIndex();
	final Object item = selected();
	if (index < 0 || item == null)
	    return false;
	    return clickHandler.onListClick(this, index, item);
    }

    @Override public RegionContent getWholeRegion()
    {
	if (model == null || model.getItemCount() < 0)
	    return null;
	final LinkedList<String> res = new LinkedList<String>();
	final int count = model.getItemCount();
	for(int i = 0;i < count;++i)
	{
	    final String line = appearance.getScreenAppearance(model.getItem(i), NONE_APPEARANCE_FLAGS);
	    res.add(line != null?line:"");
	}
	res.add("");
	return new RegionContent(res.toArray(new String[res.size()]));
    }

    @Override public RegionContent getRegion(int fromX, int fromY, int toX, int toY)
    {
	if (model == null || model.getItemCount() < 0)
	    return null;
	if (fromY >= model.getItemCount() || toY > model.getItemCount())
	    return null;
	if (fromY == toY)
	{
	    final String line = appearance.getScreenAppearance(model.getItem(fromY), NONE_APPEARANCE_FLAGS);
	    if (line == null || line.isEmpty())
		return null;
	    final int fromPos = fromX < line.length()?fromX:line.length();
	    final int toPos = toX < line.length()?toX:line.length();
	    if (fromPos >= toPos)
		return null;
	    return new RegionContent(new String[]{line.substring(fromPos, toPos)});
	}
	final LinkedList<String> res = new LinkedList<String>();
	for(int i = fromY;i < toY;++i)
	{
	    final String line = appearance.getScreenAppearance(model.getItem(i), NONE_APPEARANCE_FLAGS);
	    res.add(line != null?line:"");
	}
	res.add("");
	return new RegionContent(res.toArray(new String[res.size()]));
    }

    @Override public boolean deleteWholeRegion()
    {
	return false;
    }

    @Override public boolean deleteRegion(int fromX, int fromY, int toX, int toY)
    {
	return false;
    }

    @Override public boolean insertRegion(int x, int y, RegionContent data)
    {
	return false;
    }

    protected void onNewHotPointY(boolean briefAnnouncement)
    {
	final int index = hotPointY - itemsLayout.numberOfEmptyLinesTop();
	final int count = model.getItemCount();
	if (index < 0 || index >= count)
	{
	    environment.hint(Hints.EMPTY_LINE);
	    hotPointX = 0;
	    environment.onAreaNewHotPoint(this);
	    return;
	}
	final Object item = model.getItem(index);
	if (item == null)
	{
	    environment.hint(Hints.EMPTY_LINE);
	    hotPointX = 0;
	    environment.onAreaNewHotPoint(this);
	    return;
	}
	appearance.announceItem(item, briefAnnouncement?BRIEF_ANNOUNCEMENT_ONLY:NONE_APPEARANCE_FLAGS);
	hotPointX = appearance.getObservableLeftBound(item);
	environment.onAreaNewHotPoint(this);
    }

    protected String noContentStr()
    {
	return environment.getStaticStr("ListNoContent");
    }

    protected void announceChar(String  line, int pos)
    {
	NullCheck.notNull(line, "line");
	if (pos < line.length())
	    environment.sayLetter(line.charAt(pos)); else
	    environment.hint(Hints.END_OF_LINE);
    }

	protected boolean noContent()
    {
	if (model == null || model.getItemCount() < 1)
	{
	    environment.hint(noContentStr(), Hints.NO_CONTENT);
	    return true;
	}
	return false;
    }

    public interface Model
    {
	int getItemCount();
	Object getItem(int index);
	boolean toggleMark(int index);
	void refresh();
    }

    public interface Appearance
    {
	public enum Flags { BRIEF };

	void announceItem(Object item, Set<Flags> flags);
	String getScreenAppearance(Object item, Set<Flags> flags);
	int getObservableLeftBound(Object item);
	int getObservableRightBound(Object item);
    }

public interface ItemsLayout
{
    int numberOfEmptyLinesTop();
    int numberOfEmptyLinesBottom();
    int oneLineUp(int index, int modelLineCount);
    int oneLineDown(int index, int modelLineCount);
    void setFlags(Set<Flags> flags);
}

    static public class Params
    {
	public ControlEnvironment environment;
	public Model model;
    public Appearance appearance;
	public ListClickHandler clickHandler;
	public String name;
	public Set<Flags> flags = EnumSet.of(Flags.EMPTY_LINE_BOTTOM);

	static public Set<Flags> loadRegularFlags(Registry registry)
	{
	    NullCheck.notNull(registry, "registry");
	    final Set<Flags> res = EnumSet.noneOf(Flags.class);
	    final Settings.UserInterface settings = Settings.createUserInterface(registry);
	    if (settings.getEmptyLineUnderRegularLists(true))
		res.add(Flags.EMPTY_LINE_BOTTOM);
	    if (settings.getCyclingRegularLists(false))
		res.add(Flags.CYCLING);
	    return res;
	}

	static public Set<Flags> loadPopupFlags(Registry registry)
	{
	    NullCheck.notNull(registry, "registry");
	    final Set<Flags> res = EnumSet.noneOf(Flags.class);
	    final Settings.UserInterface settings = Settings.createUserInterface(registry);
	    if (settings.getEmptyLineAbovePopupLists(true))
		res.add(Flags.EMPTY_LINE_TOP);
	    if (settings.getCyclingPopupLists(false))
		res.add(Flags.CYCLING);
	    return res;
	}
    }

static protected class ListeningInfo
{
    final int itemIndex;
    final int pos;

    ListeningInfo(int itemIndex, int pos)
    {
	this.itemIndex = itemIndex;
	this.pos = pos;
    }
}
}
