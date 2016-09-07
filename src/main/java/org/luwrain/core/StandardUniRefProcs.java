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

package org.luwrain.core;

class StandardUniRefProcs
{
    static UniRefProc[] createStandardUniRefProcs(Luwrain l)
    {
	final Luwrain luwrain = l;
	return new UniRefProc[]{

	    //file;
	    new UniRefProc() {
		@Override public String getUniRefType()
		{
		    return "file";
		}
		@Override public UniRefInfo getUniRefInfo(String uniRef)
		{
		    if (uniRef == null || uniRef.isEmpty())
			return null;
		    if (!uniRef.startsWith("file:"))
			return null;
		    if (uniRef.indexOf("ncc.html") >= 0)
			return new UniRefInfo(uniRef, "Учебник", "\"Обществознание\"");
		    return new UniRefInfo(uniRef, luwrain.i18n().getStaticStr("UniRefPrefixFile"), uniRef.substring(5));
		}
		public void openUniRef(String uniRef, Luwrain luwrain)
		{
		    if (uniRef == null || uniRef.isEmpty())
			return;
		    if (!uniRef.startsWith("file:"))
			return;
		    luwrain.openFile(uniRef.substring(5));
		}
	    },

	    //command;
	    new UniRefProc() {
		@Override public String getUniRefType()
		{
		    return "command";
		}
		@Override public UniRefInfo getUniRefInfo(String uniRef)
		{
		    if (uniRef == null || uniRef.isEmpty())
			return null;
		    if (!uniRef.startsWith("command:"))
			return null;
		    return new UniRefInfo(uniRef, "", luwrain.i18n().getCommandTitle(uniRef.substring(8)));
		}
		@Override public void openUniRef(String uniRef, Luwrain luwrain)
		{
		    if (uniRef == null || uniRef.isEmpty())
			return;
		    if (!uniRef.startsWith("command:"))
			return;
		    luwrain.runCommand(uniRef.substring(8));
		}
	    },

	};
    }
}
