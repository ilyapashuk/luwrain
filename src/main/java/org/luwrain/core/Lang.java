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

/**
 * The interface for classes implementing national languages. This
 * interface has necessary operations to make environment comfortable for
 * people speaking on some particular language.  If you would like to add
 * support of some new foreign language, you certainly should start with
 * this interface.
 *
 * @sa I18n
 */
public interface Lang
{
    /**
     * Provides some statically stored string on corresponding national
     *language. This method should process values related to environment
     * itself, rather than taking care about particular applications. 
     *
     * @param id The identifier of the string
     * @return Requested string or <code>null</code>, if <code>id</code> is unknown
     */
    String getStaticStr(String id);

    /**
     * Provides some language-dependent name of the character in one or several words.
     *
     * @param ch The character to get name of
     * @return The name of the given character
     */
    String hasSpecialNameOfChar(char ch);

    //FIXME:
    String pastTimeBrief(java.util.Date date);
    String getNumberStr(int count, String entities);

}
