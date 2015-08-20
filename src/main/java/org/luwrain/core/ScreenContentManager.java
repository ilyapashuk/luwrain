/*
   Copyright 2012-2015 Michael Pozhidaev <michael.pozhidaev@gmail.com>

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

package org.luwrain.core;

import org.luwrain.core.events.*;
import org.luwrain.sounds.EnvironmentSounds;

class ScreenContentManager
{
    public static final int NO_APPLICATIONS = 0;
    public static final int EVENT_NOT_PROCESSED = 1;
    public static final int EVENT_PROCESSED = 2;

    private AppManager applications;
    private PopupManager popups;
    private boolean activePopup = false;

    public ScreenContentManager(AppManager applications,
				PopupManager popups)
    {
	this.applications = applications;
	this.popups = popups;
	if (applications == null)
	    throw new NullPointerException("apps may not be null");
	if (popups == null)
	    throw new NullPointerException("popups may not be null");
    }

    public  Application isNonPopupDest()
    {
	    if (isPopupActive())
		return null;
	final Area activeArea = applications.getActiveAreaOfActiveApp();
	return activeArea != null?applications.getActiveApp():null;
    }

    public  int onKeyboardEvent(KeyboardEvent event)
    {
	final Area activeArea = getActiveArea();
	if (activeArea == null)
	    return NO_APPLICATIONS;
	if (isActiveAreaBlocked())
	    Log.warning("core", "area " + activeArea.getClass().getName() + " is accepting an environment event even being blocked");
	return activeArea.onKeyboardEvent(event)?EVENT_PROCESSED:EVENT_NOT_PROCESSED;
    }

    public  int onEnvironmentEvent(EnvironmentEvent event)
    {
	final Area activeArea = getActiveArea();
	if (activeArea == null)
	return NO_APPLICATIONS;
	if (isActiveAreaBlocked())
	    Log.warning("core", "area " + activeArea.getClass().getName() + " is accepting an environment event even being blocked");
	    return activeArea.onEnvironmentEvent(event)?EVENT_PROCESSED:EVENT_NOT_PROCESSED;

    }

    public boolean setPopupActive()
    {
	if (!isPopupOpened())
	    return false;
	activePopup = true;
	return true;
    }

    public void updatePopupState()
    {
	if (activePopup)
	{
	    if (!isPopupOpened())
		activePopup = false;
	} else
	{
	    if (applications.noActiveArea() && isPopupOpened())
		activePopup = true;
	}
    }

    public Area getActiveArea()
    {
	    if (isPopupActive())
		return popups.getAreaOfLastPopup();
	final Area activeArea = applications.getActiveAreaOfActiveApp();
	if (activeArea != null)
	    return activeArea;
	if (isPopupOpened())
	{
	    activePopup = true;
	    return popups.getAreaOfLastPopup();
	}
	return null;
    }

    /**
     * Checks that the active area may accept events. Events accepting is
     * prohibited for non-popup areas of the application which has opened
     * popups. This method return false even if there is no active area at
     * all. Weak popups block areas as all others.
     *
     * @return False if the active area may accept events, true otherwise
     */
    public boolean isActiveAreaBlocked()
    {
	if (isPopupActive())
	    return false;
	final Application activeApp = applications.getActiveApp();
	if (activeApp == null)
	    return false;
	return popups.hasPopupOfApp(activeApp);
    }

    /**
     * Checks that there is an opened popup (probably, inactive). Opened
     * popup appears on screen in one of two cases: if the currently active
     * application has a popup or if the environment itself has it.  If the
     * application with a popup switches to the another one without a popup,
     * the popup hides.
     *
     * @return True if the environment has an opened popup (regardless active or inactive), false otherwise
     */
    public boolean isPopupOpened()
    {
	if (!popups.hasAny())
	    return false;
	final Application app = popups.getAppOfLastPopup();
	if (app == null)//it is an environment popup
	    return true;
	return applications.isAppActive(app);
    }

    /**
     * Checks that the environment has a proper popup, it opened and active.
     *
     * @return True if the popup opened and active, false otherwise
     */
    public boolean isPopupActive()
    {
	if (!activePopup)
	    return false;
	    if (isPopupOpened())
		return true;
	    activePopup = false;
	    return false;
    }

    public void activateNextArea()
    {
	Area activeArea = getActiveArea();
	if (activeArea == null)
	    return;
	Object[] objs = getWindows().getObjects();
	Window[] windows = new Window[objs.length];
	for(int i = 0;i < objs.length;++i)
	    windows[i] = (Window)objs[i];
	if (windows == null || windows.length <= 0)
	{
	    activePopup = isPopupOpened();
	    return;
	}
	int index;
	for(index = 0;index < windows.length;index++)
	    if (windows[index].area == activeArea)
		break;
	index++;
	if (index >= windows.length)
	    index = 0;
	activePopup = windows[index].popup;
	if (!activePopup)
	{
	    applications.setActiveAreaOfApp(windows[index].app, windows[index].area);
	    applications.setActiveApp(windows[index].app);
	}
    }

    TileManager getWindows()
    {
	TileManager windows;
	final Application activeApp = applications.getActiveApp();
	if (activeApp != null)
	    windows = constructWindowLayoutOfApp(activeApp); else
	    windows = new TileManager();
	if (isPopupOpened())
	{
	    Window popupWindow = new Window(popups.getAppOfLastPopup(), popups.getAreaOfLastPopup(), popups.getPositionOfLastPopup());
	    switch(popupWindow.popupPlace)
	    {
	    case PopupManager.BOTTOM:
		windows.addBottom(popupWindow);
		break;
	    case PopupManager.TOP:
		windows.addTop(popupWindow);
		break;
	    case PopupManager.LEFT:
		windows.addLeftSide(popupWindow);
		break;
	    case PopupManager.RIGHT:
		windows.addRightSide(popupWindow);
		break;
	    }
	}
	return windows;
    }

    private TileManager constructWindowLayoutOfApp(Application app)
    {
	if (app == null)
	    return null;
	final AreaLayout layout = app.getAreasToShow();
	TileManager tiles = new TileManager();
	switch(layout.getType())
	{
	case AreaLayout.SINGLE:
	    tiles.createSingle(new Window(app, layout.getArea1()));
	    break;
	case AreaLayout.LEFT_RIGHT:
	    tiles.createLeftRight(new Window(app, layout.getArea1()),
				  new Window(app, layout.getArea2()));
	    break;
	case AreaLayout.TOP_BOTTOM:
	    tiles.createTopBottom(new Window(app, layout.getArea1()),
				  new Window(app, layout.getArea2()));
	    break;
	case AreaLayout.LEFT_TOP_BOTTOM:
	    tiles.createLeftTopBottom(new Window(app, layout.getArea1()),
				      new Window(app, layout.getArea2()),
				      new Window(app, layout.getArea3()));
	    break;
	case AreaLayout.LEFT_RIGHT_BOTTOM:
	    tiles.createLeftRightBottom(new Window(app, layout.getArea1()),
					new Window(app, layout.getArea2()),
					new Window(app, layout.getArea3()));
	    break;
	}
	return tiles;
    }
}
