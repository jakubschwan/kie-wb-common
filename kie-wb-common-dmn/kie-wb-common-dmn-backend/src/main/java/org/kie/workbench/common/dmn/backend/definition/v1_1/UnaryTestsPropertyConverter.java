/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.dmn.backend.definition.v1_1;

import org.kie.workbench.common.dmn.api.definition.v1_1.UnaryTests;
import org.kie.workbench.common.dmn.api.property.dmn.Description;
import org.kie.workbench.common.dmn.api.property.dmn.Id;

public class UnaryTestsPropertyConverter {

    public static UnaryTests wbFromDMN(final org.kie.dmn.model.v1_1.UnaryTests dmn) {
        if (dmn == null) {
            return null;
        }
        Id id = new Id(dmn.getId());
        Description description = new Description(dmn.getDescription());

        UnaryTests result = new UnaryTests(id, description, dmn.getText(), dmn.getExpressionLanguage());
        return result;
    }

    public static org.kie.dmn.model.v1_1.UnaryTests dmnFromWB(final UnaryTests wb) {
        if (wb == null) {
            return null;
        }
        org.kie.dmn.model.v1_1.UnaryTests result = new org.kie.dmn.model.v1_1.UnaryTests();
        result.setId(wb.getId().getValue());
        result.setDescription(wb.getDescription().getValue());
        result.setText(wb.getText());
        result.setExpressionLanguage(wb.getExpressionLanguage());

        return result;
    }
}
