package com.change_vision.astah.calendar;

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics2D
import java.awt.MouseInfo
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JOptionPane;
import javax.swing.JPanel
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

import org.jdesktop.swingx.JXMonthView

import com.change_vision.jude.api.inf.editor.IDiagramEditorFactory
import com.change_vision.jude.api.inf.editor.MindmapEditor
import com.change_vision.jude.api.inf.model.IDiagram
import com.change_vision.jude.api.inf.project.ProjectAccessor
import com.change_vision.jude.api.inf.project.ProjectAccessorFactory
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate
import com.change_vision.jude.api.inf.ui.IWindow
import com.change_vision.jude.api.inf.view.IDiagramViewManager
import com.change_vision.jude.api.inf.view.IViewManager

class CalendarImageAction implements IPluginActionDelegate {

  def JDialog frame
  def JXMonthView monthView
  def Action action

  def run(IWindow window){
    frame = new JDialog(window.getParent(),Messages.getString("CalendarImageAction.title")); //$NON-NLS-1$
    frame.setModal(true)
    frame.setLocationRelativeTo(window.getParent())

    def panel = createMainPanel()
    frame.add(panel)
    frame.pack()
    frame.setVisible(true)

    return null
  }

  private JPanel createMainPanel() {
    def panel = new JPanel()
    panel.setLayout(new BorderLayout())
    createMonthView();
    panel.add(monthView,BorderLayout.CENTER)
    def footer = createFooterArea()
    panel.add(footer,BorderLayout.SOUTH)
    return panel
  }

  private JPanel createFooterArea() {
    def footer = new JPanel()
    footer.setLayout(new BoxLayout(footer,BoxLayout.X_AXIS))
    def commitButton = createCommitButton()
    def holidayCheck = createShowJapaneseHolidayCheckBox()
    footer.add(holidayCheck)
    footer.add(commitButton)
    return footer
  }

  private JCheckBox createShowJapaneseHolidayCheckBox() {
    def holidayCheck = new JCheckBox(Messages.getString("CalendarImageAction.show_javanese_holiday_title"),true) //$NON-NLS-1$
    holidayCheck.addChangeListener({
      ChangeEvent event ->
      Object obj = event.getSource()
      if (obj instanceof JCheckBox) {
        JCheckBox check = (JCheckBox) obj
        if(check.isSelected()){
          setHolidays(monthView.getFirstDisplayedDay())
        }else{
          monthView.setFlaggedDates(null)
        }
      }
    } as ChangeListener)
    return holidayCheck
  }

  private JButton createCommitButton() {
    def commitButton = new JButton()
    action = new AbstractAction(){
      public void actionPerformed(ActionEvent event) {
        monthView.setTraversable(false)
        def ps = monthView.getPreferredSize()
        def image = new BufferedImage((int)ps.width, (int)ps.height, BufferedImage.TYPE_INT_ARGB)
        def g2 = (Graphics2D)image.getGraphics()
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
              RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        monthView.paint(g2)
        g2.dispose()

        stickImageToCurrentDiagram(image)
        frame.setVisible(false)
      }

    }
    action.putValue(Action.NAME,Messages.getString("CalendarImageAction.commit_action")) //$NON-NLS-1$
    action.enabled = false
    commitButton.setAction(action)
    return commitButton
  }

  private stickImageToCurrentDiagram(BufferedImage image) {
    ProjectAccessor accessor = ProjectAccessorFactory.getProjectAccessor()
    IDiagramEditorFactory diagramEditorFactory = accessor.getDiagramEditorFactory()
    MindmapEditor mmEditor = diagramEditorFactory.getMindmapEditor()
    IViewManager viewManager = accessor.getViewManager()
    IDiagramViewManager dvm = viewManager.getDiagramViewManager()
    IDiagram diagram = dvm.getCurrentDiagram()
    if (diagram == null) {
      def message = Messages.getString("CalendarImageAction.cant_find_opened_diagrams") //$NON-NLS-1$
      JOptionPane.showMessageDialog(frame.getParent(), message, Messages.getString("CalendarImageAction.warning_title"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
    }
    mmEditor.diagram = diagram

    def tm = accessor.transactionManager
    tm.beginTransaction()
    def ml = MouseInfo.getPointerInfo().getLocation()
    mmEditor.createImage(image,new Point((int)(dvm.toWorldCoordX((int)ml.x)),(int)(dvm.toWorldCoordY((int)ml.y))))
    tm.endTransaction()
  }


  private createMonthView() {
    monthView = new JXMonthView()
    monthView.setTraversable(true)
    monthView.setPreferredColumnCount(2)
    monthView.addActionListener({ActionEvent event ->
      action.enabled = true
    } as ActionListener
    )
    monthView.addPropertyChangeListener("firstDisplayedDay", { //$NON-NLS-1$
      PropertyChangeEvent event ->
      def newDate = event.getNewValue()
      setHolidays(newDate)
    } as PropertyChangeListener)
    setHolidays(monthView.getFirstDisplayedDay())
    monthView.setDayForeground(Calendar.SUNDAY,Color.RED)
    monthView.setDayForeground(Calendar.SATURDAY,Color.BLUE)
  }

  private setHolidays(Date newDate) {
    def Calendar cal = Calendar.getInstance()
    cal.setTime(newDate)
    def Date[] currentHolidays = Holiday.listHoliDayDates(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH))
    def Date[] nextHolidays = Holiday.listHoliDayDates(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH) + 1)
    // listHolidayDates returns null when there is no japanese holidays
    monthView.setFlaggedDates(currentHolidays)
    monthView.addFlaggedDates(nextHolidays)
  }

}
