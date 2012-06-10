package com.change_vision.astah.calendar;

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
import javax.swing.JDialog
import javax.swing.JPanel

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
		frame = new JDialog(window.getParent(),"Calendar");
		frame.setModal(true)
		def panel = new JPanel()
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS))
		frame.setLocationRelativeTo(window.getParent())
		
		createMonthView();
		
		panel.add(monthView)
		def commitButton = createCommitButton()
		panel.add(commitButton)
		frame.add(panel)
		frame.pack()
		frame.setVisible(true)

		return null
	}

	private JButton createCommitButton() {
		def commitButton = new JButton()
		action = new AbstractAction(){
			public void actionPerformed(ActionEvent event) {
				def ps = monthView.getPreferredSize()
				def image = new BufferedImage((int)ps.width, (int)ps.height, BufferedImage.TYPE_INT_ARGB)
				def g2 = (Graphics2D)image.getGraphics()
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						  RenderingHints.VALUE_INTERPOLATION_BILINEAR)
				monthView.paint(g2)
				g2.dispose()
				
				ProjectAccessor accessor = ProjectAccessorFactory.getProjectAccessor()
				IDiagramEditorFactory diagramEditorFactory = accessor.getDiagramEditorFactory()
				MindmapEditor mmEditor = diagramEditorFactory.getMindmapEditor()
				IViewManager viewManager = accessor.getViewManager()
				IDiagramViewManager dvm = viewManager.getDiagramViewManager()
				IDiagram diagram = dvm.getCurrentDiagram()
				mmEditor.diagram = diagram
				
				def tm = accessor.transactionManager
				tm.beginTransaction()
				def ml = MouseInfo.getPointerInfo().getLocation()
				mmEditor.createImage(image,new Point((int)(dvm.toWorldCoordX((int)ml.x)),(int)(dvm.toWorldCoordY((int)ml.y))))
				tm.endTransaction()
				frame.setVisible(false)
			}
		}
		action.putValue(Action.NAME,"commit")
		action.enabled = false
		commitButton.setAction(action)
		return commitButton
	}

	private createMonthView() {
		monthView = new JXMonthView()
		monthView.setTraversable(true)
		monthView.setPreferredColumnCount(2)
		monthView.addActionListener({ActionEvent event ->
			action.enabled = true
		} as ActionListener
		)
		monthView.addPropertyChangeListener("firstDisplayedDay", {
			PropertyChangeEvent event ->
			def newDate = event.getNewValue()
			setHolidays(newDate)
		} as PropertyChangeListener)
		setHolidays(monthView.getFirstDisplayedDay())
		monthView.setDayForeground(Calendar.SUNDAY,Color.RED)
		monthView.setDayForeground(Calendar.SATURDAY,Color.BLUE)
	}

	private setHolidays(newDate) {
		def Calendar cal = Calendar.getInstance()
		cal.setTime(newDate)
		def Date[] currentHolidays = Holiday.listHoliDayDates(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH))
		def Date[] nextHolidays = Holiday.listHoliDayDates(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH) + 1)
		monthView.setFlaggedDates(currentHolidays)
		monthView.addFlaggedDates(nextHolidays)
	}

}
