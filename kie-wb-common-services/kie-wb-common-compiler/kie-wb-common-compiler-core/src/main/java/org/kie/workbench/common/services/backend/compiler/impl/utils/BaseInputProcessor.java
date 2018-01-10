package org.kie.workbench.common.services.backend.compiler.impl.utils;

import org.guvnor.common.services.shared.builder.model.BuildMessage;

public abstract class BaseInputProcessor implements InputProcessor {

    protected BuildMessage getBuildMessage(final BuildMessage origin,
                                           final String text) {
        final BuildMessage msg = new BuildMessage();
        msg.setLevel(origin.getLevel());
        msg.setColumn(origin.getColumn());
        msg.setLine(origin.getLine());
        msg.setPath(origin.getPath());
        msg.setText(text);
        return msg;
    }

}
