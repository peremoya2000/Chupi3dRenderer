import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

@SuppressWarnings("serial")
public class Renderer extends JFrame{

    @SuppressWarnings("unused")
	public static void main(String[] args) {
    	Renderer r = new Renderer();
    }
    
    public Renderer() {
        Container pane = this.getContentPane();
        pane.setLayout(new BorderLayout());

        // slider to control horizontal rotation
        JSlider headingSlider = new JSlider(-180, 180, 0);
        pane.add(headingSlider, BorderLayout.SOUTH);

        // slider to control vertical rotation
        JSlider pitchSlider = new JSlider(SwingConstants.VERTICAL, -180, 180, 0);
        pane.add(pitchSlider, BorderLayout.EAST);

        // slider to control roll
        JSlider rollSlider = new JSlider(SwingConstants.VERTICAL, -180, 180, 0);
        pane.add(rollSlider, BorderLayout.WEST);

        // slider to control FoV
        JSlider fovSlider = new JSlider(30, 180, 90);
        pane.add(fovSlider, BorderLayout.NORTH);
        
        // panel to display render results
        Viewer3d v= new Viewer3d(headingSlider,pitchSlider,rollSlider,fovSlider);
        pane.add(v, BorderLayout.CENTER);

        fovSlider.addChangeListener(e -> v.changeFov());
        
        this.addComponentListener(new ComponentAdapter() {  
                public void componentResized(ComponentEvent evt) {
                	v.pause();
                }
        });
                
        this.setSize(800, 600);        
        this.setVisible(true);
        this.setFocusable(true);
        this.requestFocus();
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        v.init();
    }
}


