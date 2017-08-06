/*
   Copyright 2012-2017 Michael Pozhidaev <michael.pozhidaev@gmail.com>

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

package org.luwrain.controls;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

public class EditableListArea extends ListArea
{
    public interface EditableModel extends Model
    {
	boolean clearList();
	boolean addToList(int pos, Clipboard clipboard);
	boolean removeFromList(int index);
    }

    public interface Confirmation
    {
	boolean confirmDeleting(EditableListArea area, EditableModel model, int deleteFromIndex, int deleteToIndex);
    }

    static public class Params extends ListArea.Params
    {
	public Confirmation confirmation = null;
    }

    protected final EditableModel editableListModel;
    protected final Confirmation confirmation;

    public EditableListArea(Params params)
    {
	super(params);
	NullCheck.notNull(params, "params");
	NullCheck.notNull(params.model, "params.model");
	if (!(params.model instanceof EditableModel))
	    throw new IllegalArgumentException("params.model must be an instance of EditableModel");
	this.editableListModel = (EditableModel)params.model;
	this.confirmation = params.confirmation;
    }

    public Confirmation getEditableListConfirmation()
    {
	return confirmation;
    }

    @Override public boolean onKeyboardEvent(KeyboardEvent event)
    {
	NullCheck.notNull(event, "event");
	if (event.isSpecial() && !event.isModified())
	    switch(event.getSpecial())
	    { 
	    case DELETE:
		return onDeleteSingle(getHotPointY(), true);
	    }
	return super.onKeyboardEvent(event);
    }

    @Override public boolean onEnvironmentEvent(EnvironmentEvent event)
    {
	NullCheck.notNull(event, "event");
	if (event.getType() != EnvironmentEvent.Type.REGULAR)
	    return super.onEnvironmentEvent(event);
	switch(event.getCode())
	{
	case CLEAR:
	    if (isEmpty())
		return false;
	    if (confirmation != null && !confirmation.confirmDeleting(this, editableListModel, 0, editableListModel.getItemCount()))
		return true;
	    if (!editableListModel.clearList())
		return false;
	    refresh();
	    return true;
	case CLIPBOARD_PASTE:
	    return onClipboardPaste();
	default:
	    return super.onEnvironmentEvent(event);
	}
    }

    @Override public 		boolean onClipboardCopy(int fromX, int fromY, int toX, int toY, boolean withDeleting)
    {
	if (isEmpty())
	    return false;
	if (fromY >= 0 && fromY == toY && fromX != toX)//trying to cut a part of the item, it is impossible
	    return false;
	if (!super.onClipboardCopy(fromX, fromY, toX, toY, false))
	    return false;
	if (!withDeleting)
	    return true;
	if (fromX < 0 || fromY < 0 ||
	    (fromX == toX && fromY == toY))
	    return onDeleteSingle(toY, false);
	return onDeleteMultiple(fromY, toY, false);
    }

    @Override public boolean onDeleteRegion(int fromX, int fromY, int toX, int toY)
    {
	if (isEmpty())
	    return false;
	if (fromX < 0 || fromY < 0 ||
	    (fromX == toX && fromY == toY))
	    return onDeleteSingle(toY, true);
	if (fromY == toY)
	    return false;
	return onDeleteMultiple(fromY, toY, true);
    }

    protected boolean onClipboardPaste()
    {
	if (context.getClipboard().isEmpty())
	    return false;
	final int pos = getItemIndexOnLine(getHotPointY());
	if (pos < 0)
	    return false;
	if (!editableListModel.addToList(pos, context.getClipboard()))
	    return false;
	refresh();
	return true;
    }

    protected boolean onDeleteSingle(int lineIndex, boolean withConfirmation)
    {
	final int index = getExistingItemIndexOnLine(lineIndex);
	if (index < 0)
	    return false;
	if (withConfirmation && confirmation != null && !confirmation.confirmDeleting(this, editableListModel, index, index + 1))
	    return true;
	if (!editableListModel.removeFromList(index))
	    return false;
	refresh();
	return true;
    }

    protected boolean onDeleteMultiple(int fromLineIndex, int toLineIndex, boolean withConfirmation)
    {
	if (fromLineIndex + 1 == toLineIndex)
	    return onDeleteSingle(fromLineIndex, withConfirmation);
	final int fromIndex = getExistingItemIndexOnLine(fromLineIndex);
	final int toIndex = getItemIndexOnLine(toLineIndex);
	if (fromIndex < 0 || toIndex < 0 || fromIndex >= toIndex)
	    return false;
	if (withConfirmation && confirmation != null && !confirmation.confirmDeleting(this, editableListModel, fromIndex, toIndex))
	    return true;
	for(int i = fromIndex;i < toIndex;++i)
	    if (!editableListModel.removeFromList(fromLineIndex))
		return false;
	refresh();
	return true;
    }
}
