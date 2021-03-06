/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.dmn.client.widgets.grid;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

import com.ait.lienzo.test.LienzoMockitoTestRunner;
import org.jboss.errai.common.client.api.IsElement;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.dmn.api.definition.HasExpression;
import org.kie.workbench.common.dmn.api.definition.HasName;
import org.kie.workbench.common.dmn.api.definition.v1_1.LiteralExpression;
import org.kie.workbench.common.dmn.client.widgets.grid.model.BaseUIModelMapper;
import org.kie.workbench.common.dmn.client.widgets.grid.model.DMNGridRow;
import org.kie.workbench.common.dmn.client.widgets.grid.model.GridCellTuple;
import org.uberfire.ext.wires.core.grids.client.model.GridColumn;
import org.uberfire.ext.wires.core.grids.client.model.GridData;
import org.uberfire.ext.wires.core.grids.client.widget.grid.columns.RowNumberColumn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(LienzoMockitoTestRunner.class)
public class BaseExpressionGridGeneralTest extends BaseExpressionGridTest {

    @Override
    @SuppressWarnings("unchecked")
    public BaseExpressionGrid getGrid() {
        final GridCellTuple parent = new GridCellTuple(0, 0, null);
        final HasExpression hasExpression = mock(HasExpression.class);
        final Optional<LiteralExpression> expression = Optional.of(mock(LiteralExpression.class));
        final Optional<HasName> hasName = Optional.of(mock(HasName.class));

        return new BaseExpressionGrid(parent,
                                      hasExpression,
                                      expression,
                                      hasName,
                                      gridPanel,
                                      gridLayer,
                                      renderer,
                                      sessionManager,
                                      sessionCommandManager,
                                      editorSelectedEvent,
                                      false) {
            @Override
            protected BaseUIModelMapper makeUiModelMapper() {
                return mapper;
            }

            @Override
            protected void initialiseUiColumns() {
                //Nothing for this test
            }

            @Override
            protected void initialiseUiModel() {
                //Nothing for this test
            }

            @Override
            public Optional<IsElement> getEditorControls() {
                return Optional.empty();
            }
        };
    }

    @Test
    public void testGetMinimumWidthNoColumns() {
        assertMinimumWidth(0.0);

        grid.getMinimumWidth();
    }

    @Test
    public void testGetMinimumWidthOneColumn() {
        final double COL_0_MIN = 100.0;

        assertMinimumWidth(COL_0_MIN,
                           new MockColumnData(200.0, COL_0_MIN));
    }

    @Test
    public void testGetMinimumWidthTwoColumns() {
        final double COL_0_ACTUAL = 200.0;
        final double COL_1_MIN = 150.0;

        assertMinimumWidth(COL_0_ACTUAL + COL_1_MIN,
                           new MockColumnData(COL_0_ACTUAL, 100.0),
                           new MockColumnData(225.0, COL_1_MIN));
    }

    @Test
    public void testGetMinimumWidthMultipleColumns() {
        final double COL_0_ACTUAL = 50.0;
        final double COL_1_ACTUAL = 65.0;
        final double COL_2_MIN = 150.0;

        assertMinimumWidth(COL_0_ACTUAL + COL_1_ACTUAL + COL_2_MIN,
                           new MockColumnData(COL_0_ACTUAL, 25.0),
                           new MockColumnData(COL_1_ACTUAL, 35.0),
                           new MockColumnData(225.0, COL_2_MIN));
    }

    @Test
    public void testGetViewportGridAttachedToLayer() {
        doReturn(gridParent).when(grid).getParent();
        doReturn(viewport).when(gridParent).getViewport();

        assertEquals(viewport,
                     grid.getViewport());
    }

    @Test
    public void testGetViewportGridNotAttachedToLayer() {
        assertEquals(viewport,
                     grid.getViewport());
    }

    @Test
    public void testGetLayerGridAttachedToLayer() {
        doReturn(gridParent).when(grid).getParent();
        doReturn(gridLayer).when(gridParent).getLayer();

        assertEquals(gridLayer,
                     grid.getLayer());
    }

    @Test
    public void testGetLayerGridNotAttachedToLayer() {
        assertEquals(gridLayer,
                     grid.getLayer());
    }

    @Test
    public void testSelectFirstCellWithNoRowsOrColumns() {
        grid.selectFirstCell();

        assertThat(grid.getModel().getSelectedCells()).isEmpty();
    }

    @Test
    public void testSelectFirstCellWithRowAndNonRowNumberColumn() {
        grid.getModel().appendRow(new DMNGridRow());
        appendColumns(GridColumn.class);

        grid.selectFirstCell();

        assertThat(grid.getModel().getSelectedCells()).isNotEmpty();
        assertThat(grid.getModel().getSelectedCells()).contains(new GridData.SelectedCell(0, 0));
    }

    @Test
    public void testSelectFirstCellWithRowAndRowNumberColumn() {
        grid.getModel().appendRow(new DMNGridRow());
        appendColumns(RowNumberColumn.class);

        grid.selectFirstCell();

        assertThat(grid.getModel().getSelectedCells()).isEmpty();
    }

    @Test
    public void testSelectFirstCellWithRowAndRowNumberColumnAndAnotherColumn() {
        grid.getModel().appendRow(new DMNGridRow());
        appendColumns(RowNumberColumn.class, GridColumn.class);

        grid.selectFirstCell();

        assertThat(grid.getModel().getSelectedCells()).isNotEmpty();
        assertThat(grid.getModel().getSelectedCells()).contains(new GridData.SelectedCell(0, 1));
    }

    private void assertMinimumWidth(final double expectedMinimumWidth,
                                    final MockColumnData... columnData) {
        Arrays.asList(columnData).forEach(cd -> {
            final GridColumn uiColumn = mock(GridColumn.class);
            doReturn(cd.width).when(uiColumn).getWidth();
            doReturn(cd.minWidth).when(uiColumn).getMinimumWidth();
            grid.getModel().appendColumn(uiColumn);
        });

        assertEquals(expectedMinimumWidth,
                     grid.getMinimumWidth(),
                     0.0);
    }

    @SafeVarargs
    private final void appendColumns(final Class<? extends GridColumn>... columnClasses) {
        IntStream.range(0, columnClasses.length).forEach(i -> {
            final GridColumn column = mock(columnClasses[i]);
            doReturn(i).when(column).getIndex();
            grid.getModel().appendColumn(column);
        });
    }

    private static class MockColumnData {

        private double width;
        private double minWidth;

        public MockColumnData(final double width,
                              final double minWidth) {
            this.width = width;
            this.minWidth = minWidth;
        }
    }
}
