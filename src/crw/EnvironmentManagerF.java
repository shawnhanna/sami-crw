package crw;

import crw.ui.component.WorldWindPanel;
import crw.ui.widget.SelectGeometryWidget;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import sami.environment.EnvironmentProperties;
import sami.path.Location;
import sami.ui.MissionMonitor;
import static sami.ui.MissionMonitor.LAST_EPF_FILE;
import static sami.ui.MissionMonitor.LAST_EPF_FOLDER;

/**
 *
 * @author nbb
 */
public class EnvironmentManagerF extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(EnvironmentManagerF.class.getName());
    EnvironmentProperties environmentProperties = new EnvironmentProperties();
    private File file = null;
    WorldWindPanel wwPanel;

    public EnvironmentManagerF() {
        super("EnvironmentManagerF");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        // Add map
        ArrayList<String> layerNames = new ArrayList<String>();
        layerNames.add("Bing Imagery");
        layerNames.add("Blue Marble (WMS) 2004");
        layerNames.add("Scale bar");
        layerNames.add("Place Names");
        wwPanel = new WorldWindPanel();
        wwPanel.createMap(layerNames);
        getContentPane().add(wwPanel.component, BorderLayout.CENTER);
        // Add widgets
        List<SelectGeometryWidget.SelectMode> modes = Arrays.asList(SelectGeometryWidget.SelectMode.AREA, SelectGeometryWidget.SelectMode.NONE, SelectGeometryWidget.SelectMode.CLEAR);
        SelectGeometryWidget geometry = new SelectGeometryWidget(wwPanel, modes, SelectGeometryWidget.SelectMode.NONE);
        wwPanel.addWidget(geometry);

        JPanel filePanel = new JPanel();
        JButton openB = new JButton("Open");
        openB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                open();
            }
        });
        filePanel.add(openB);
        JButton newB = new JButton("New");
        newB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                newFile();
            }
        });
        filePanel.add(newB);
        JButton saveB = new JButton("Save");
        saveB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                save();
            }
        });
        filePanel.add(saveB);
        JButton saveAsB = new JButton("Save As");
        saveAsB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                saveAs();
            }
        });
        filePanel.add(saveAsB);
        getContentPane().add(filePanel, BorderLayout.SOUTH);

        // Try to load the last used EPF file
        Preferences p = Preferences.userRoot();
        try {
            String lastEpfPath = p.get(LAST_EPF_FILE, null);
            if (lastEpfPath != null) {
                loadEpf(new File(lastEpfPath));
            }
        } catch (AccessControlException e) {
            LOGGER.severe("Failed to load last used EPF");
        }

        pack();
    }

    public void applyObstacleList(ArrayList<ArrayList<Location>> obstacleList) {
        RenderableLayer layer = (RenderableLayer) wwPanel.wwCanvas.getModel().getLayers().getLayerByName("Renderable");
        layer.removeAllRenderables();
        for (ArrayList<Location> locationList : obstacleList) {
            ArrayList<Position> positionList = new ArrayList<Position>();
            for (Location location : locationList) {
                positionList.add(Conversion.locationToPosition(location));
            }
            SurfacePolygon area = new SurfacePolygon(positionList);
            ShapeAttributes attributes = new BasicShapeAttributes();
            attributes.setInteriorOpacity(0.5);
            attributes.setInteriorMaterial(Material.YELLOW);
            attributes.setOutlineWidth(2);
            attributes.setOutlineMaterial(Material.BLACK);
            area.setAttributes(attributes);
            layer.addRenderable(area);
        }
        wwPanel.wwCanvas.redraw();
    }

    public ArrayList<ArrayList<Location>> getObstacleList() {
        ArrayList<ArrayList<Location>> obstacleList = new ArrayList<ArrayList<Location>>();
        RenderableLayer layer = (RenderableLayer) wwPanel.wwCanvas.getModel().getLayers().getLayerByName("Renderable");
        SurfacePolygon area = null;
        for (Renderable renderable : layer.getRenderables()) {
            if (renderable instanceof SurfacePolygon) {
                area = (SurfacePolygon) renderable;
                ArrayList<Location> locationList = new ArrayList<Location>();
                for (LatLon latLon : area.getLocations()) {
                    locationList.add(Conversion.latLonToLocation(latLon));
                }
                obstacleList.add(locationList);
            }
        }
        return obstacleList;
    }

    public void newFile() {
        environmentProperties = new EnvironmentProperties();
        file = null;

        // Clear ep values
        RenderableLayer layer = (RenderableLayer) wwPanel.wwCanvas.getModel().getLayers().getLayerByName("Renderable");
        layer.removeAllRenderables();
    }

    private void save() {
        if (file == null) {
            saveAs();
            if (file == null) {
                return;
            }
        }
        // Update ep values
        ArrayList<ArrayList<Location>> obstacleList = getObstacleList();
        environmentProperties.setObstacleList(obstacleList);
        environmentProperties.setDefaultLocation(Conversion.positionToLocation(wwPanel.getCanvas().getView().getCurrentEyePosition()));

        if (wwPanel.getCanvas().getView().getCurrentEyePosition() != null) {
            Location currentLocation = Conversion.positionToLocation(wwPanel.getCanvas().getView().getCurrentEyePosition());
            currentLocation.setAltitude(wwPanel.getCanvas().getView().getCurrentEyePosition().getElevation());
        } else {
            LOGGER.severe("Eye position is NULL");
        }

        Preferences p = Preferences.userRoot();
        p.put(LAST_EPF_FILE, file.getAbsolutePath());
        p.put(LAST_EPF_FOLDER, file.getParent());

        // Serialize ep
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(environmentProperties);
            LOGGER.info("Saved: " + file.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void saveAs() {
        Preferences p = Preferences.userRoot();
        String lastEpfFolder = p.get(LAST_EPF_FOLDER, "");
        JFileChooser chooser = new JFileChooser(lastEpfFolder);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Environment properties file", "epf");
        chooser.setFileFilter(filter);
        int ret = chooser.showSaveDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().getName().endsWith(".epf")) {
                file = chooser.getSelectedFile();
            } else {
                file = new File(chooser.getSelectedFile().getAbsolutePath() + ".epf");
            }
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Saving as: " + file.toString());
            save();
        }
    }

    public boolean open() {
        Preferences p = Preferences.userRoot();
        String lastEpfFolder = p.get(LAST_EPF_FOLDER, "");
        JFileChooser chooser = new JFileChooser(lastEpfFolder);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Environment properties files", "epf");
        chooser.setFileFilter(filter);
        int ret = chooser.showOpenDialog(null);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        file = chooser.getSelectedFile();
        return loadEpf(file);
    }

    public boolean loadEpf(File epfFile) {
        LOGGER.info("Reading: " + epfFile.toString());
        Preferences p = Preferences.userRoot();
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(epfFile));
            environmentProperties = (EnvironmentProperties) ois.readObject();

            if (environmentProperties == null) {
                JOptionPane.showMessageDialog(null, "Specification failed load");
                environmentProperties = new EnvironmentProperties();
                return false;
            } else {
                try {
                    p.put(LAST_EPF_FILE, epfFile.getAbsolutePath());
                    p.put(LAST_EPF_FOLDER, epfFile.getParent());
                } catch (AccessControlException e) {
                    LOGGER.severe("Failed to save preferences");
                }
                LOGGER.info("Read: " + epfFile.toString());
                setTitle("EnvironmentManagerF: " + epfFile.toString());

                // Apply ep values
                applyObstacleList(environmentProperties.getObstacleList());
                // Apply default view
                Position defaultPosition = Conversion.locationToPosition(environmentProperties.getDefaultLocation());
                wwPanel.getCanvas().getView().setEyePosition(defaultPosition);

                return true;
            }

        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return false;
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new EnvironmentManagerF().setVisible(true);
            }
        });
    }
}
