/*
 * Copyright (c) 2001, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package nsk.jdi.StepRequest.addClassExclusionFilter;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type
 * StepRequest.
 *
 * The test checks that results of the method
 * <code>com.sun.jdi.StepRequest.addClassExclusionFilter()</code>
 * complies with its spec.
 *
 * The test checks up on the following assertion:
 *    Restricts the events generated by this request to those
 *    whose method is in a class whose name does not match
 *    this restricted regular expression. e.g. "java.*" or "*.Foo".
 * The cases to check include both a pattern that begin with '*' and
 * one that end with '*'.
 *
 * The test works as follows.
 * - The debugger
 *   - sets up StepRequest1 and restricts it using a pattern that
 *     begins with '*'
 *   - resumes the debuggee, and waits for expected StepEvent.
 * - The debuggee creates and starts a thread, thread1,
 *   which being run, invokes methods used
 *   to generate Events and to test the filters.
 * - Upon getting the events, the debugger performs checks required.
 *   Then the same is repeated for a pattern that ends with *.
 */

public class filter001 extends TestDebuggerType1 {

    public static void main (String argv[]) {
        int result = run(argv,System.out);
        if (result != 0) {
            throw new RuntimeException("TEST FAILED with result " + result);
        }
    }

    public static int run (String argv[], PrintStream out) {
        debuggeeName = "nsk.jdi.StepRequest.addClassExclusionFilter.filter001a";
        return new filter001().runThis(argv, out);
    }

    private String testedClassName1 = "TestClass11";
    private String testedClassName2 = "nsk.jdi.StepRequest.addClassExclusionFilter.Thread2filter001a";

    private static EventRequest eventRequest1;
    private static EventRequest eventRequest2;

    protected void testRun() {

        String        property1     = "StepRequest1";
        String        property2     = "StepRequest2";

        ThreadReference thread1 = null;
        ThreadReference thread2 = null;

        String threadName1 = "thread1";
        String threadName2 = "thread2";

        for (int i = 0; ; i++) {

            if (!shouldRunAfterBreakpoint()) {
                vm.resume();
                break;
            }

            display(":::::: case: # " + i);

            switch (i) {

                case 0:
                thread1 = debuggee.threadByFieldNameOrThrow(debuggeeClass, threadName1);
                eventRequest1 = setting23StepRequest(thread1, "*" + testedClassName1,
                                         EventRequest.SUSPEND_NONE, property1);

                eventRequest1.enable();
                eventHandler.addListener(
                     new EventHandler.EventListener() {
                         public boolean eventReceived(Event event) {
                            if (event instanceof StepEvent && event.request().equals(eventRequest1)) {
                                String str = ((StepEvent)event).location().declaringType().name();
                                if (str.endsWith(testedClassName1)) {
                                    setFailedStatus("eventRequest1: Received unexpected StepEvent for excluded class:" + str);
                                } else {
                                    display("eventRequest1: Received expected StepEvent for " + str);
                                }
                                return true;
                            }
                            return false;
                         }
                     }
                );

                display("......waiting1 for StepEvent in expected thread");
                vm.resume();
                break;

                case 1:
                thread2 = debuggee.threadByFieldNameOrThrow(debuggeeClass, threadName2);
                eventRequest2 = setting23StepRequest(thread2, testedClassName2 + "*",
                                         EventRequest.SUSPEND_NONE, property2);

                eventRequest2.enable();
                eventHandler.addListener(
                     new EventHandler.EventListener() {
                         public boolean eventReceived(Event event) {
                            if (event instanceof StepEvent && event.request().equals(eventRequest2)) {
                                String str = ((StepEvent)event).location().declaringType().name();
                                if (str.endsWith(testedClassName2)) {
                                    setFailedStatus("eventRequest2: Received unexpected StepEvent for excluded class:" + str);
                                } else {
                                    display("eventRequest2: Received expected StepEvent for " + str);
                                }
                                return true;
                            }
                            return false;
                         }
                     }
                );

                display("......waiting2 for StepEvent in expected thread");
                vm.resume();
                break;

                case 2:
                    display("disabling event requests");
                    eventRequest1.disable();
                    eventRequest2.disable();

                    display("resuming debuggee");
                    vm.resume();
                    break;

                default:
                    throw new TestBug("Test logic error");
            }
        }
        return;
    }

    private StepRequest setting23StepRequest ( ThreadReference thread,
                                              String          testedClass,
                                              int             suspendPolicy,
                                              String          property        ) {
        try {
            display("......setting up StepRequest:");
            display("       thread: " + thread + "; property: " + property);

            StepRequest
            str = eventRManager.createStepRequest(thread, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
            str.putProperty("number", property);
            // limit events number (otherwise too many events will be generated)
            str.addCountFilter(10);
            str.setSuspendPolicy(suspendPolicy);
            str.addClassExclusionFilter(testedClass);

            display("      StepRequest has been set up");
            return str;
        } catch ( Exception e ) {
            throw new Failure("** FAILURE to set up StepRequest **");
        }
    }

}
