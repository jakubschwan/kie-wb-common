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
