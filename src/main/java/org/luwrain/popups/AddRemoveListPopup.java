
package org.luwrain.popups;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;

public class AddRemoveListPopup extends ListPopupBase
{
    static public class Model implements ListArea.Model
    {
	private final Vector items = new Vector();

	public Model(Object[] items)
	{
	    if (items == null || items.length < 1)
		return;
	    this.items.setSize(items.length);
	    for(int i = 0;i < items.length;++i)
		this.items.set(i, items[i]);
	}

	public Object[] getAllItems()
	{
	    return items.toArray(new Object[items.size()]);
	}

	public void add(Object o)
	{
	    if (o == null)
		return;
	    items.add(o);
	}

	public void remove(Object o)
	{
	    if (o == null)
		return;
	    items.remove(o);
	}

	@Override public int getItemCount()
	{
	    return items.size();
	}

	@Override public Object getItem(int index)
	{
	    return index >= 0 && index < items.size()?items.get(index):null;
	}

	@Override public void refresh()
	{
	}

	@Override public boolean toggleMark(int index)
	{
	    return false;
	}
    }

    public interface RemoveConfirmation
    {
	boolean mayRemove(Object o);
    }

    public interface ItemsSource
    {
	Object getNewItemToAdd();
    }

    protected final RemoveConfirmation removeConfirmation;
    protected final ItemsSource itemsSource;

    public AddRemoveListPopup(Luwrain luwrain, String name,
			      Object[] items, ItemsSource itemsSource, RemoveConfirmation removeConfirmation,
			      Set<Popup.Flags> popupFlags)
    {
	super(luwrain, constructParams(luwrain, items, name), popupFlags);
	NullCheck.notNull(itemsSource, "itemsSource");
	NullCheck.notNull(removeConfirmation, "removeConfirmation");
	this.itemsSource = itemsSource;
	this.removeConfirmation = removeConfirmation;
    }

    @Override public boolean onKeyboardEvent(KeyboardEvent event)
    {
	NullCheck.notNull(event, "event");
	if (event.isSpecial() && !event.isModified())
	    switch(event.getSpecial())
	    {
	    case INSERT:
		tryToAdd();
		return true;
	    case DELETE:
		return tryToRemove();
	    }
	return super.onKeyboardEvent(event);
    }

    //Returns true if new item has been added
    public boolean tryToAdd()
    {
	final Object newObj = itemsSource.getNewItemToAdd();
	if (newObj == null)
	    return false;
	final Model m = (Model)model;
	m.add(newObj);
	refresh();
	return true;
    }

    //Returns true if there is an item which we can try to remove
    public boolean tryToRemove()
    {
	final Object item = selected();
	if (item == null)
	    return false;
	if (!removeConfirmation.mayRemove(item))
	    return true;
	final Model m = (Model)model;
	m.remove(item);
	refresh();
	return true;
    }

    public Object[] result()
    {
	final Model m = (Model)model;
	return m.getAllItems();
    }

    static private ListArea.Params constructParams(Luwrain luwrain, 
						   Object[] items, String name)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNullItems(items, "items");
	NullCheck.notNull(name, "name");
	final ListArea.Params params = new ListArea.Params();
	params.environment = new DefaultControlEnvironment(luwrain);
	params.name = name;
	params.model = new Model(items);
	params.appearance = new DefaultListItemAppearance(params.environment);
	return params;
    }
}
