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

package org.luwrain.settings;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.cpanel.*;

final class Speech extends FormArea implements SectionArea
{
    private final ControlPanel controlPanel;
    private final Luwrain luwrain;
    private final Settings.SpeechParams sett;

    Speech(ControlPanel controlPanel, String name)
    {
	super(new DefaultControlEnvironment(controlPanel.getCoreInterface()), name);
	this.controlPanel = controlPanel;
	this.luwrain = controlPanel.getCoreInterface();
	this.sett = Settings.createSpeechParams(luwrain.getRegistry());
	addEdit("main-engine-name", luwrain.i18n().getStaticStr("CpSpeechMainEngineName"), sett.getMainEngineName(""));
	addEdit("main-engine-params", luwrain.i18n().getStaticStr("CpSpeechMainEngineParams"), sett.getMainEngineParams(""));
	addEdit("listening-engine-name", luwrain.i18n().getStaticStr("CpSpeechListeningEngineName"), sett.getListeningEngineName(""));
	addEdit("listening-engine-params", luwrain.i18n().getStaticStr("CpSpeechListeningEngineParams"), sett.getListeningEngineParams(""));
    }

    @Override public boolean saveSectionData()
    {
	sett.setMainEngineName(getEnteredText("main-engine-name"));
	sett.setMainEngineParams(getEnteredText("main-engine-params"));
	sett.setListeningEngineName(getEnteredText("listening-engine-name"));
	sett.setListeningEngineParams(getEnteredText("listening-engine-params"));
	return true;
    }

    @Override public boolean onInputEvent(KeyboardEvent event)
    {
	if (controlPanel.onInputEvent(event))
	    return true;
	return super.onInputEvent(event);
    }

    @Override public boolean onSystemEvent(EnvironmentEvent event)
    {
	if (controlPanel.onSystemEvent(event))
	    return true;
	return super.onSystemEvent(event);
    }

    static Speech create(ControlPanel controlPanel)
    {
	NullCheck.notNull(controlPanel, "controlPanel");
	final Luwrain luwrain = controlPanel.getCoreInterface();
	return new Speech(controlPanel, luwrain.i18n().getStaticStr("CpSpeechGeneral"));
    }
}
