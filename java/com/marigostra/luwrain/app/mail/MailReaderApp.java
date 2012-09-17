/*
   Copyright 2012 Michael Pozhidaev <msp@altlinux.org>

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

package com.marigostra.luwrain.app.mail;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

import com.marigostra.luwrain.core.*;
//FIXME:import com.marigostra.luwrain.pim.*;

public class MailReaderApp implements Application, MailReaderActions
{
    private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

    private Object instance = null;
    private MailReaderStringConstructor stringConstructor = null;
    private GroupArea groupArea;
    private SummaryArea summaryArea;
    private MessageArea messageArea;
    //FIXME:    private MailStoring MailStoring;

    private Session session;
    private MailGroup topLevelGroups[]; 

    private void initSession()
    {
	Properties p = new Properties();
	p.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
	p.setProperty("mail.pop3.socketFactory.fallback", "false");
	p.setProperty("mail.pop3.port",  "995");
	p.setProperty("mail.pop3.socketFactory.port", "995");//FIXME:
	session = Session.getInstance(p, null);
    }

    private void fillTopLevelGroups()
    {
	LocalDirectory dir = new LocalDirectory(session, new java.io.File("/tmp/mail"));
	dir.update();
	topLevelGroups = new MailGroup[1];
	topLevelGroups[0] = dir;
}

    public boolean onLaunch(Object instance)
    {
	//Interface initialization;
	Object o = Langs.requestStringConstructor("mail-reader");
	if (o == null)
	    return false;
	stringConstructor = (MailReaderStringConstructor)o;
	groupArea = new GroupArea(this, stringConstructor);
	summaryArea = new SummaryArea(this, stringConstructor);
	messageArea = new MessageArea(this, stringConstructor);
	//FIXME:	mailStoring = PimManager.createMailStoring();
	this.instance = instance;
	//Groups initialization;
	initSession();
	fillTopLevelGroups();
	summaryArea.show(topLevelGroups[0]);
	return true;
    }

    public AreaLayout getAreasToShow()
    {
	return new AreaLayout(AreaLayout.LEFT_TOP_BOTTOM, groupArea, summaryArea, messageArea);
    }

    public void gotoGroups()
    {
	Dispatcher.setActiveArea(instance, groupArea);
    }

    public void gotoSummary()
    {
	Dispatcher.setActiveArea(instance, summaryArea);
    }

    public void gotoMessage()
    {
	Dispatcher.setActiveArea(instance, messageArea);
    }

    public void closeMailReader()
    {
	Dispatcher.closeApplication(instance);
    }
}
