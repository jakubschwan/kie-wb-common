/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.workbench.common.services.backend.logback;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class OutputSharedMapTest {

    private String KEY = "key";

    @Test
    public void addMessageTest(){
        List<String> msgs = OutputSharedMap.getLog(KEY);
        Assert.assertTrue(msgs.equals(Collections.EMPTY_LIST));
        OutputSharedMap.addMsgToLog(KEY, "msg");
        OutputSharedMap.addMsgToLog(KEY, "msgOne");
        msgs =OutputSharedMap.getLog(KEY);
        Assert.assertTrue(msgs.size()  == 2);
        OutputSharedMap.purgeAll();
    }

    @Test
    public void getMessageTest(){
        List<String> msgs = OutputSharedMap.getLog(KEY);
        Assert.assertTrue(msgs.equals(Collections.EMPTY_LIST));
        OutputSharedMap.addMsgToLog(KEY, "msg");
        msgs =OutputSharedMap.getLog(KEY);
        Assert.assertTrue(msgs.size()  == 1);
        Assert.assertTrue(msgs.get(0).equals("msg"));
        OutputSharedMap.purgeAll();
    }

    @Test
    public void getRemoveMessageTest(){
        List<String> msgs = OutputSharedMap.getLog(KEY);
        Assert.assertTrue(msgs.equals(Collections.EMPTY_LIST));
        OutputSharedMap.addMsgToLog(KEY, "msg");
        msgs =OutputSharedMap.getLog(KEY);
        Assert.assertTrue(msgs.size()  == 1);
        Assert.assertTrue(msgs.get(0).equals("msg"));
        OutputSharedMap.removeLog(KEY);
        msgs =OutputSharedMap.getLog(KEY);
        Assert.assertTrue(msgs.equals(Collections.EMPTY_LIST));
        Assert.assertTrue(msgs.size()  == 0);
        OutputSharedMap.purgeAll();
    }

    @Test
    public void getPurgeAllTest(){
        List<String> msgs = OutputSharedMap.getLog(KEY);
        Assert.assertTrue(msgs.equals(Collections.EMPTY_LIST));
        OutputSharedMap.addMsgToLog(KEY, "msg");
        OutputSharedMap.addMsgToLog(KEY, "msgOne");
        msgs =OutputSharedMap.getLog(KEY);
        Assert.assertTrue(msgs.size()  == 2);
        OutputSharedMap.purgeAll();
        msgs =OutputSharedMap.getLog(KEY);
        Assert.assertTrue(msgs.equals(Collections.EMPTY_LIST));
        Assert.assertTrue(msgs.size()  == 0);
    }
}
