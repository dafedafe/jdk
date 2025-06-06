/*
 * Copyright (c) 2016, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jfr;

import java.util.Set;

/**
 * Base class to extend to create setting controls.
 * <p>
 * The following example shows an implementation of a setting control for
 * regular expressions:
 *
 * {@snippet class="Snippets" region="SettingControlOverview1"}
 *
 * The {@code setValue(String)}, {@code getValue()} and
 * {@code combine(Set<String>)} methods are invoked when a setting value
 * changes, which typically happens when a recording is started or stopped. The
 * {@code combine(Set<String>)} method is invoked to resolve what value to use
 * when multiple recordings are running at the same time.
 * <p>
 * The setting control must have a default constructor that can be invoked when
 * the event is registered.
 * <p>
 * To use a setting control with an event, add a method that returns a
 * {@code boolean} value and takes the setting control as a parameter. Annotate
 * the method with the {@code @SettingDefinition} annotation. By default, the
 * method name is used as the setting name, but the name can be set explicitly
 * by using the {@code @Name} annotation. If the method returns {@code true},
 * the event will be committed.
 * <p>
 * It is recommended that the {@code setValue(String)} method updates an
 * efficient data structure that can be quickly checked when the event is
 * committed.
 * <p>
 * The following example shows how to create an event that uses the
 * regular expression filter defined above.
 *
 * {@snippet class="Snippets" region="SettingControlOverview2"}
 *
 * <p>
 * The following example shows how an event can be filtered by assigning the
 * {@code "uriFilter"} setting with the specified regular expressions.
 *
 * {@snippet class="Snippets" region="SettingControlOverview3"}
 *
 * @see SettingDefinition
 *
 * @since 9
 */
@MetadataDefinition
public abstract class SettingControl {

    /**
     * Constructor for invocation by subclass constructors.
     */
    protected SettingControl() {
    }

    /**
     * Combines the setting values for all running recordings into one value when
     * multiple recordings are running at the same time,
     * <p>
     * The semantics of how setting values are combined depends on the setting
     * control that is implemented, but all recordings should get at least all the
     * events they request.
     * <p>
     * This method should have no side effects, because the caller might cache values.
     * This method should never return {@code null} or throw an exception. If a
     * value is not valid for this setting control, the value should be ignored.
     * <p>
     * Examples:
     * <p>
     * if the setting control represents a threshold and three recordings are
     * running at the same time with the setting values {@code "10 ms"},
     * {@code "8 s"}, and {@code  "1 ms"}, this method returns {@code "1 ms"}
     * because it means that all recordings get at least all the requested data.
     * <p>
     * If the setting control represents a set of names and two recordings are
     * running at the same time with the setting values {@code "Smith, Jones"} and {@code "Jones,
     * Williams"} the returned value is {@code "Smith, Jones, Williams"} because all names would be accepted.
     * <p>
     * If the setting control represents a boolean condition and four recordings are
     * running at the same time with the following values {@code "true"}, {@code "false"}, {@code "false"}, and
     * {@code "incorrect"}, this method returns {@code "true"}, because all
     * recordings get at least all the requested data.
     *
     * @param settingValues the set of values, not {@code null}
     *
     * @return the value to use, not {@code null}
     */
    public abstract String combine(Set<String> settingValues);

    /**
     * Sets the value for this setting.
     * <p>
     * If the setting value is not valid for this setting, this method
     * does not throw an exception. Instead, the value is ignored.
     *
     * @param settingValue the string value, not {@code null}
     */
    public abstract void setValue(String settingValue);

    /**
     * Returns the currently used value for this setting, not {@code null}.
     * <p>
     * The value returned by this method is valid as an argument to both
     * the {@code setValue(String)} method and {@code combine(Set)} method.
     * <p>
     * This method is invoked when an event is registered to obtain the
     * default value. It is therefore important that a valid value can be
     * returned immediately after an instance of this class is created. It is
     * not valid to return {@code null}.
     *
     * @return the setting value, not {@code null}
     */
    public abstract String getValue();
}
