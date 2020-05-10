/*
   Copyright 2012-2020 Michael Pozhidaev <msp@luwrain.org>

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

//LWR_API 1.0

package org.luwrain.script;

import java.util.*;
import java.util.function.*;

import org.luwrain.core.*;

public class HotPointHookObject extends EmptyHookObject
{
    protected final HotPoint hotPoint;

    public HotPointHookObject(HotPoint hotPoint)
    {
	NullCheck.notNull(hotPoint, "hotPoint");
	this.hotPoint = hotPoint;
    }

    @Override public Object getMember(String name)
    {
	NullCheck.notEmpty(name, "name");
	switch(name)
	{
	case "x":
	    return new Integer(hotPoint.getHotPointX());
	case "y":
	    return new Integer(hotPoint.getHotPointY());
	default:
	    return super.getMember(name);
	}
    }
    }
