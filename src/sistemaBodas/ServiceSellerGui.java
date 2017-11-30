package sistemaBodas;



import jade.core.AID;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


class ServiceSellerGui extends JFrame {	
	private ServiceSellerAgent myAgent;
	
	private JTextField titleField, priceField, priceField2;
	
	ServiceSellerGui(ServiceSellerAgent a) {
		super(a.getLocalName());
		
		myAgent = a;
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(4, 4));
		p.add(new JLabel("Service title:"));
		titleField = new JTextField(15);
		p.add(titleField);
		p.add(new JLabel("Price:"));
		priceField = new JTextField(15);
		p.add(priceField);
		p.add(new JLabel("Min seller Price:"));
		priceField2 = new JTextField(15);
		p.add(priceField2);
		getContentPane().add(p, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Add");
		addButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String title = titleField.getText().trim();
					String price = priceField.getText().trim();
					String minPrice = priceField2.getText().trim();
					myAgent.updateCatalogue(title, Integer.parseInt(price), Integer.parseInt(minPrice));
					titleField.setText("");
					priceField.setText("");
					priceField2.setText("");
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(ServiceSellerGui.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		} );
		p = new JPanel();
		p.add(addButton);
		getContentPane().add(p, BorderLayout.SOUTH);
		
		// Make the agent terminate when the user closes 
		// the GUI using the button on the upper right corner	
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );
		
		setResizable(false);
	}
	
	public void showGui() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		super.setVisible(true);
	}	
}