package com.change_vision.astah.calendar;

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics2D
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
  def JCheckBox holidayCheck

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
    createShowJapaneseHolidayCheckBox()
    footer.add(holidayCheck)
    footer.add(commitButton)
    return footer
  }

  private createShowJapaneseHolidayCheckBox() {
    holidayCheck = new JCheckBox(Messages.getString("CalendarImageAction.show_javanese_holiday_title"),true) //$NON-NLS-1$
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
  }

  private JButton createCommitButton() {
    def commitButton = new JButton()
    action = new AbstractAction(){
      public void actionPerformed(ActionEvent event) {
        monthView.setTraversable(false)
        def ps = monthView.getSize()
        def image = new BufferedImage((int)ps.width, (int)ps.height, BufferedImage.TYPE_INT_ARGB)
        def g2 = (Graphics2D)image.createGraphics()
        monthView.paint(g2)
        g2.dispose()

        stickImageToCurrentDiagram(image)
        frame.setVisible(false)
      }

    }
    action.putValue(Action.NAME,Messages.getString("CalendarImageAction.commit_action")) //$NON-NLS-1$
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
    mmEditor.createImage(image,new Point(300,300))
    tm.endTransaction()
  }


  private createMonthView() {
    monthView = new JXMonthView()
    monthView.setTraversable(true)
    monthView.setPreferredColumnCount(2)
    monthView.addPropertyChangeListener("firstDisplayedDay", { //$NON-NLS-1$
      PropertyChangeEvent event ->
      def newDate = event.getNewValue()
      if (holidayCheck.isSelected()) {
        setHolidays(newDate)
      }
      frame.pack()
    } as PropertyChangeListener)
    setHolidays(monthView.getFirstDisplayedDay())
    monthView.setDayForeground(Calendar.SUNDAY,Color.RED)
    monthView.setDayForeground(Calendar.SATURDAY,Color.BLUE)
  }

  private setHolidays(Date newDate) {
    def Calendar cal = Calendar.getInstance()
    cal.setTime(newDate)
    def year = cal.get(Calendar.YEAR)
    def month = cal.get(Calendar.MONTH)
    def nextYear = year
    def nextMonth = month + 1
    if (month == Calendar.DECEMBER){
      nextYear = year + 1
      nextMonth = Calendar.JANUARY
    }
    def Date[] currentHolidays = Holiday.listHoliDayDates(year,month)
    def Date[] nextHolidays = Holiday.listHoliDayDates(nextYear,nextMonth)
    // listHolidayDates returns null when there is no japanese holidays
    monthView.setFlaggedDates(currentHolidays)
    monthView.addFlaggedDates(nextHolidays)
  }

}
