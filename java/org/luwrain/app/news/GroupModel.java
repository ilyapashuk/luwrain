/*
   Copyright 2012-2014 Michael Pozhidaev <msp@altlinux.org>

   This file is part of the Luwrain.

   Luwrain is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   Luwrain is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.app.news;

import org.luwrain.core.Log;
import org.luwrain.controls.*;
import org.luwrain.pim.*;

class GroupModel implements ListModel
{
    private NewsStoring newsStoring;
    private StoredNewsGroup[] items;

    public GroupModel(NewsStoring newsStoring)
    {
	this.newsStoring = newsStoring;
	refresh();
    }

    public int getItemCount()
    {
	return items != null?items.length:0;
    }

    public Object getItem(int index)
    {
	if (items == null || index >= items.length)
	    return null;
	return items[index];
    }

    public void refresh()
    {
	if (newsStoring == null)
	{
	    items = null;
	    return;
	}
	try {
	    items = newsStoring.loadNewsGroups();
	}
	catch(Exception e)
	{
	    Log.error("news", "could not construct list of groups:" + e.getMessage());
	    e.printStackTrace();
	    items = null;
	}
    }
}
