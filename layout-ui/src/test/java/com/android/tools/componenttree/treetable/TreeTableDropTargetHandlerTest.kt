/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.componenttree.treetable

import com.android.SdkConstants
import com.android.flags.junit.SetFlagRule
import com.android.testutils.MockitoCleanerRule
import com.android.testutils.MockitoKt.any
import com.android.testutils.MockitoKt.mock
import com.android.testutils.MockitoKt.whenever
import com.android.tools.adtui.swing.IconLoaderRule
import com.android.tools.adtui.swing.laf.HeadlessTableUI
import com.android.tools.adtui.swing.laf.HeadlessTreeUI
import com.android.tools.componenttree.api.ComponentTreeBuildResult
import com.android.tools.componenttree.api.ComponentTreeBuilder
import com.android.tools.componenttree.api.IconColumn
import com.android.tools.componenttree.util.Item
import com.android.tools.componenttree.util.ItemNodeType
import com.android.tools.idea.flags.StudioFlags
import com.google.common.truth.Truth.assertThat
import com.intellij.ide.dnd.DnDAction
import com.intellij.ide.dnd.DnDEvent
import com.intellij.ide.dnd.DnDNativeTarget
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.EdtRule
import com.intellij.testFramework.RunsInEdt
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import icons.StudioIcons
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.mockito.Mockito.eq
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JScrollPane

@RunsInEdt
class TreeTableDropTargetHandlerTest {

  companion object {
    @JvmField
    @ClassRule
    val rule = ApplicationRule()
  }

  @get:Rule
  val chain = RuleChain
    .outerRule(SetFlagRule(StudioFlags.USE_COMPONENT_TREE_TABLE, true))
    .around(MockitoCleanerRule())
    .around(IconLoaderRule())
    .around(EdtRule())!!

  private val item1 = Item(SdkConstants.FQCN_LINEAR_LAYOUT)
  private val item2 = Item(SdkConstants.FQCN_GRID_LAYOUT)
  private val item3 = Item(SdkConstants.FQCN_BUTTON)
  private val item4 = Item(SdkConstants.FQCN_RELATIVE_LAYOUT)
  private val item5 = Item(SdkConstants.FQCN_CHECK_BOX)
  private val item6 = Item(SdkConstants.FQCN_TEXT_VIEW)

  private val badgeItem = object : IconColumn("Badge") {
    override fun getIcon(item: Any): Icon? = when (item) {
      item1 -> StudioIcons.Common.ERROR
      item2 -> StudioIcons.Common.FILTER
      else -> StudioIcons.Common.CLOSE
    }

    override fun getTooltipText(item: Any): String = ""
    override fun performAction(item: Any, component: JComponent, bounds: Rectangle) {}
    override fun showPopup(item: Any, component: JComponent, x: Int, y: Int) {}
  }

  // row 0: item1  (layout)
  // row 1:   item2  (layout)
  // row 2:     item3
  // row 3:     item4  (layout)
  // row 4:       item5
  // row 5:       item6

  @Before
  fun before() {
    item1.add(item2)
    item2.add(item3, item4)
    item4.add(item5, item6)
  }

  @Test
  fun testDragEnter() {
    val table = createTreeTable()
    val handler = TreeTableDropTargetHandler(table, false, mutableListOf())
    val event1 = createDnDEvent(Point(10, table.rowHeight - 2))
    handler.update(event1)
    checkPaint(table, handler, 0, 1)
    verify(table).repaint()
    verify(event1).isDropPossible = true

    // Move the cursor slightly and verify the same markings in the tree:
    val event2 = createDnDEvent(Point(11, table.rowHeight - 3))
    handler.update(event2)
    checkPaint(table, handler, 0, 1)
    verify(table).repaint()
    verify(event2).isDropPossible = true
  }

  @Test
  fun testDragExitResetsInsertionRow() {
    val table = createTreeTable()
    val handler = TreeTableDropTargetHandler(table, false, mutableListOf())
    val event = createDnDEvent(Point(10, table.rowHeight - 2))
    handler.update(event)
    checkPaint(table, handler, 0, 1)
    verify(table).repaint()
    handler.reset()
    checkNoPaint(handler)
    verify(table, times(2)).repaint()
  }

  @Test
  fun testDragOverBadgeResetsInsertionRow() {
    val table = createTreeTable()
    val handler = TreeTableDropTargetHandler(table, false, mutableListOf())
    val event1 = createDnDEvent(Point(10, table.rowHeight - 2))
    handler.update(event1)
    checkPaint(table, handler, 0, 1)
    verify(table).repaint()
    verify(event1).isDropPossible = true

    val rect = table.getCellRect(0, 1, true)
    val event2 = createDnDEvent(Point(rect.x + rect.width / 2, table.rowHeight - 2))
    handler.update(event2)
    checkNoPaint(handler)
    verify(table, times(2)).repaint()
    verify(event2).isDropPossible = false
  }

  @Test
  fun testDragOverBottomOfItem5() {
    val table = createTreeTable()
    val rowHeight = table.rowHeight
    val handler = TreeTableDropTargetHandler(table, false, mutableListOf())
    val event = createDnDEvent(Point(10, 5 * rowHeight - 3))
    handler.update(event)
    checkPaint(table, handler, 3, 5)
    verify(table).repaint()
    verify(event).isDropPossible = true
  }

  @Test
  fun testReceiverDependOnDepth() {
    val table = createTreeTable()
    val depth2 = table.computeLeftOffset(2)
    val depth3 = table.computeLeftOffset(3)
    val depth4 = table.computeLeftOffset(4)
    val rowHeight = table.rowHeight
    val handler = TreeTableDropTargetHandler(table, false, mutableListOf())
    val event1 = createDnDEvent(Point(depth4 + 3, 6 * rowHeight - 3))
    handler.update(event1)
    checkPaint(table, handler, 3, 6)
    verify(table).repaint()
    verify(event1).isDropPossible = true

    // Moving the mouse to the left will cause a receiver higher in the hierarchy to be selected as the receiver
    val event2 = createDnDEvent(Point(depth3 + 3, 6 * rowHeight - 3))
    handler.update(event2)
    checkPaint(table, handler, 1, 6)
    verify(table, times(2)).repaint()
    verify(event2).isDropPossible = true

    // Moving the mouse more to the left will cause a receiver even higher in the hierarchy to be selected as the receiver
    val event3 = createDnDEvent(Point(depth2 + 3, 6 * rowHeight - 3))
    handler.update(event3)
    checkPaint(table, handler, 0, 6)
    verify(table, times(3)).repaint()
    verify(event3).isDropPossible = true
  }

  @Test
  fun testNoPossibleReceivers() {
    item1.canInsert = false
    item2.canInsert = false
    item4.canInsert = false
    val table = createTreeTable()
    val depth4 = table.computeLeftOffset(4)
    val rowHeight = table.rowHeight
    val handler = TreeTableDropTargetHandler(table, false, mutableListOf())
    val event = createDnDEvent(Point(depth4 + 3, 6 * rowHeight - 3))
    handler.update(event)
    checkNoPaint(handler)
    verify(table, never()).repaint()
    verify(event).isDropPossible = false
  }

  @Test
  fun testCannotDragItemIntoItself() {
    val table = createTreeTable()
    val depth4 = table.computeLeftOffset(4)
    val rowHeight = table.rowHeight
    val handler = TreeTableDropTargetHandler(table, false, mutableListOf(item2)) // We are dragging item2 to the end
    val event = createDnDEvent(Point(depth4 + 3, 6 * rowHeight - 3))

    // Even though the drag location specify item3,
    // the dragged item2 should not be accepted in item3 or item2, but can be accepted in item1.
    handler.update(event)
    checkPaint(table, handler, 0, 6)
    verify(table).repaint()
    verify(event).isDropPossible = true
  }

  @Test
  fun testPaint() {
    val table = createTreeTable()
    table.setSize(400, 800)
    val depth4 = table.computeLeftOffset(4)
    val rowHeight = table.rowHeight
    val handler = TreeTableDropTargetHandler(table, false, mutableListOf())
    val event = createDnDEvent(Point(depth4 + 3, 6 * rowHeight - 3))
    handler.update(event)
    verify(event).isDropPossible = true
  }

  @Test
  fun testDropOnMove() {
    val draggedItems = tryNormalDrop(DnDAction.MOVE, deleteOriginOfInternalMove = false)

    // The draggedItems should be reset such that the transferHandler doesn't delete them
    assertThat(draggedItems).isEmpty()
  }

  @Test
  fun testDropOnMoveWithDeleteOriginOfInternalMove() {
    val draggedItems = tryNormalDrop(DnDAction.MOVE, deleteOriginOfInternalMove = true)

    // The draggedItems should not be changed, since the transfer handler should delete the moved items
    assertThat(draggedItems).hasSize(2)
  }

  @Test
  fun testDropOnCopy() {
    val draggedItems = tryNormalDrop(DnDAction.COPY)

    // The draggedItems should not be changed, since the transfer handler will not delete on a copy
    assertThat(draggedItems).hasSize(2)
  }

  private fun tryNormalDrop(action: DnDAction, deleteOriginOfInternalMove: Boolean = false): List<Any> {
    val table = createTreeTable()
    val depth4 = table.computeLeftOffset(4)
    val rowHeight = table.rowHeight
    val draggedItems = mutableListOf<Any>(item3, item6)
    val handler = TreeTableDropTargetHandler(table, deleteOriginOfInternalMove, draggedItems)
    val event = createDnDEvent(Point(depth4 + 3, 6 * rowHeight - 3), action)
    handler.update(event)
    checkPaint(table, handler, 3, 6)
    verify(event).isDropPossible = true

    // Move item3 & item6 to the end of item4
    handler.tryDrop(event)

    checkNoPaint(handler)
    verify(table, times(2)).repaint()
    assertThat(item4.insertions).hasSize(1)
    checkInsertion(item4.insertions[0], null, action, listOf(item3, item6))

    return draggedItems
  }

  @Test
  fun testDropButNotAccepted() {
    item4.acceptInsert = false
    val table = createTreeTable()
    val depth4 = table.computeLeftOffset(4)
    val rowHeight = table.rowHeight
    val handler = TreeTableDropTargetHandler(table, false, mutableListOf())
    val event = createDnDEvent(Point(depth4 + 3, 6 * rowHeight - 3))
    handler.update(event)
    checkPaint(table, handler, 3, 6)
    verify(table).repaint()
    verify(event).isDropPossible = true

    handler.tryDrop(event)
    checkNoPaint(handler)
    verify(table, times(2)).repaint()
  }

  @Test
  fun testDropMoveItemToItself() {
    val table = createTreeTable()
    val depth4 = table.computeLeftOffset(4)
    val rowHeight = table.rowHeight
    val draggedItems = mutableListOf<Any>(item5)
    val handler = TreeTableDropTargetHandler(table, false, draggedItems) // We are dragging item5 to just before itself
    val event = createDnDEvent(Point(depth4 + 3, 4 * rowHeight + 2))
    handler.update(event)
    checkPaint(table, handler, 3, 4)
    verify(table).repaint()
    verify(event).isDropPossible = true

    handler.tryDrop(event)
    checkNoPaint(handler)
    verify(table, times(2)).repaint()

    // Verify the moved item (item5) was moved into item4 before item6
    assertThat(item4.insertions).hasSize(1)
    checkInsertion(item4.insertions[0], item6, DnDAction.MOVE, listOf(item5))
    assertThat(draggedItems).isEmpty()
  }

  private fun checkNoPaint(handler: TreeTableDropTargetHandler) {
    val g: Graphics = mock()
    handler.paintDropTargetPosition(g)
    verifyNoInteractions(g)
  }

  private fun checkPaint(table: TreeTableImpl, handler: TreeTableDropTargetHandler, expectedReceiverRow: Int, expectedInsertionRow: Int) {
    val g: Graphics = mock()
    val g2: Graphics2D = mock()
    whenever(g.create()).thenReturn(g2)
    handler.paintDropTargetPosition(g)
    val rb = table.tree.getRowBounds(expectedReceiverRow)
    val ib = table.tree.getRowBounds(maxOf(0, expectedInsertionRow - 1))
    inOrder(g2).apply {
      verify(g2).color = eq(ColorUtil.brighter(UIUtil.getTreeSelectionBackground(true), 10))
      verify(g2).setRenderingHint(eq(RenderingHints.KEY_ANTIALIASING), eq(RenderingHints.VALUE_ANTIALIAS_ON))
      verify(g2).drawRect(maxOf(0, rb.x - 2), rb.y, rb.width + 2, rb.height)
      verify(g2).drawLine(rb.x + 6, ib.y + ib.height, ib.x + ib.width, ib.y + ib.height)
      verify(g2).drawPolygon(any())
      verify(g2).fillPolygon(any())
      verify(g2).stroke = any()
      verify(g2).drawLine(rb.x + 7, rb.y + rb.height, rb.x + 7, ib.y + ib.height)
      verify(g2).dispose()
      verifyNoMoreInteractions()
    }
  }

  private fun checkInsertion(insertion: Item.Insertion, before: Any?, action: DnDAction, dragged: List<Any>) {
   assertThat(insertion.before).isSameAs(before)
   assertThat(insertion.isMove).isEqualTo(action == DnDAction.MOVE)
   assertThat(insertion.draggedFromTree).containsExactlyElementsIn(dragged).inOrder()
  }

  private fun createTreeTable(): TreeTableImpl {
    val result = createTreeWithScrollPane()
    val table = (result.component as JScrollPane).viewport.view as TreeTableImpl
    result.model.treeRoot = item1

    table.setUI(HeadlessTableUI())
    table.tree.setUI(HeadlessTreeUI())
    TreeUtil.expandAll(table.tree)
    table.setSize(400, 800)
    table.doLayout()
    runInEdtAndWait { UIUtil.dispatchAllInvocationEvents() }
    return spy(table)
  }

  private fun createTreeWithScrollPane(): ComponentTreeBuildResult {
    return ComponentTreeBuilder()
      .withNodeType(ItemNodeType())
      .withBadgeSupport(badgeItem)
      .withoutTreeSearch()
      .withInvokeLaterOption { it.run() }
      .withDnD()
      .build()
  }

  private fun createDnDEvent(location: Point, action: DnDAction = DnDAction.MOVE): DnDEvent {
    val attachedObject: DnDNativeTarget.EventInfo = mock()
    whenever(attachedObject.transferable).thenReturn(mock())
    val event: DnDEvent = mock()
    whenever(event.action).thenReturn(action)
    whenever(event.point).thenReturn(location)
    whenever(event.attachedObject).thenReturn(attachedObject)
    return event
  }
}
