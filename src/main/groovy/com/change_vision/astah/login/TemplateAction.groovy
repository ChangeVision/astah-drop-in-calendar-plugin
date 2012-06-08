package com.change_vision.astah.login;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.SwingUtilities
import org.jdesktop.swingx.JXLoginPane
import org.jdesktop.swingx.auth.LoginService

import com.change_vision.jude.api.inf.ui.IPluginActionDelegate
import com.change_vision.jude.api.inf.ui.IWindow

public class TemplateAction implements IPluginActionDelegate {

  def run(IWindow window){
    println 'hoge'

    def frame = new JFrame("LoginPane Demo");
    frame.setSize(200, 100);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new FlowLayout());

    def button = new JButton("Login");
    button.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent event) {
        login((Component)event.getSource());
        }
        });
    frame.add(button);

    frame.setVisible(true);
    return null
  }


  private void login(parent) {
    def service = new LoginService() {
        public boolean authenticate(String name,
          char[] password,
          String server) {
        return true
        }
    }

    def loginPane = new JXLoginPane( service )

    JXLoginPane.showLoginDialog(parent, loginPane);
  }
}
