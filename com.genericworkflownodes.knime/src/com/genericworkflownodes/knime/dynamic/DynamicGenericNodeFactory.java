package com.genericworkflownodes.knime.dynamic;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.List;

import org.knime.base.node.util.exttool.ExtToolStderrNodeView;
import org.knime.base.node.util.exttool.ExtToolStdoutNodeView;
import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDescription28Proxy;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.node.v28.FullDescriptionDocument.FullDescription;
import org.knime.node.v28.InPortDocument.InPort;
import org.knime.node.v28.IntroDocument.Intro;
import org.knime.node.v28.KnimeNodeDocument;
import org.knime.node.v28.KnimeNodeDocument.KnimeNode;
import org.knime.node.v28.OptionDocument.Option;
import org.knime.node.v28.OutPortDocument.OutPort;
import org.knime.node.v28.PortsDocument.Ports;
import org.w3c.dom.Document;

import com.genericworkflownodes.knime.config.INodeConfiguration;
import com.genericworkflownodes.knime.config.reader.CTDConfigurationReader;
import com.genericworkflownodes.knime.config.reader.InvalidCTDFileException;
import com.genericworkflownodes.knime.custom.config.IPluginConfiguration;
import com.genericworkflownodes.knime.generic_node.GenericKnimeNodeDialog;
import com.genericworkflownodes.knime.parameter.Parameter;
import com.genericworkflownodes.knime.port.Port;

/**
 * <code>NodeFactory</code> for Generic KNIME Nodes that are loaded dynamically.
 * 
 * @author Fillbrunn, Alexander
 */
public abstract class DynamicGenericNodeFactory
    extends DynamicNodeFactory<DynamicGenericNodeModel> {
    
    /**
     * The configuration key for the ctd file.
     */
    public static final String CTD_FILE_CFG_KEY = "ctdFile";
    
    public static final String ID_CFG_KEY = "id";

    private static final NodeLogger logger = NodeLogger.getLogger(DynamicGenericNodeFactory.class);
    
    private String m_id;
    private String m_ctdFile;
    private INodeConfiguration m_config;
    
    /**
     * Implement this method and return the configuration of the plugin
     * the nodes are hosted in.
     * @return the plugin configuration.
     */
    protected abstract IPluginConfiguration getPluginConfig();
    
    protected String getIconPath() {
        return "";
    }
    
    public String getId() {
        return m_id;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public DynamicGenericNodeModel createNodeModel() {
        INodeConfiguration tmpConfig;
        try {
            tmpConfig = getNodeConfiguration();
            
            String[][] inputs = new String[tmpConfig.getInputPorts().size()][];
            String[][] outputs = new String[tmpConfig.getOutputPorts().size()][];
            int i = 0;
            for (Port p : tmpConfig.getInputPorts()) {
                inputs[i++] = p.getMimeTypes().toArray(new String[0]);
            }
            i = 0;
            for (Port p : tmpConfig.getOutputPorts()) {
                outputs[i++] = p.getMimeTypes().toArray(new String[0]);
            }
            
            return new DynamicGenericNodeModel(tmpConfig, getPluginConfig(), inputs, outputs);
        } catch (Exception e) {
            logger.error("Dynamic node model instantiation failed", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        // Stderr and Stdout
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<DynamicGenericNodeModel> createNodeView(final int viewIndex,
            final DynamicGenericNodeModel nodeModel) {
        if (viewIndex == 0) {
            return new ExtToolStdoutNodeView<DynamicGenericNodeModel>(nodeModel);
        } else if (viewIndex == 1) {
            return new ExtToolStderrNodeView<DynamicGenericNodeModel>(nodeModel);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        try {
            return new GenericKnimeNodeDialog(getNodeConfiguration());
        } catch (Exception e) {
            logger.error("Dynamic node view instantiation failed", e);
        }
        return null;
    }
    
    @Override
    public void loadAdditionalFactorySettings(ConfigRO config)
            throws InvalidSettingsException {
        m_ctdFile = config.getString(CTD_FILE_CFG_KEY);
        m_id = config.getString(ID_CFG_KEY);
        super.loadAdditionalFactorySettings(config);
    }
    
    @Override
    public void saveAdditionalFactorySettings(ConfigWO config) {
        config.addString(CTD_FILE_CFG_KEY, m_ctdFile);
        config.addString(ID_CFG_KEY, m_id);
        super.saveAdditionalFactorySettings(config);
    }
    
    @Override
    protected NodeDescription createNodeDescription() {
        try {
            INodeConfiguration cfg = getNodeConfiguration();
            KnimeNodeDocument doc = org.knime.node.v28.KnimeNodeDocument.Factory.newInstance();
            Document domDoc = (Document)doc.getDomNode();
            
            // Node
            KnimeNode node = doc.addNewKnimeNode();
            node.setName(cfg.getExecutableName());
            node.setIcon(getIconPath());
            node.setType(KnimeNode.Type.MANIPULATOR);
            
            node.setShortDescription(cfg.getDescription());
            
            FullDescription fullDescr = node.addNewFullDescription();
            
            // Intro
            Intro intro = fullDescr.addNewIntro();
            intro.addNewP().getDomNode().appendChild(domDoc.createTextNode(cfg.getManual()));
            
            // Options
            for (Parameter<?> p : cfg.getParameters()) {
                Option option = fullDescr.addNewOption();
                option.setName(p.getKey());
                option.getDomNode().appendChild(domDoc.createTextNode(p.getDescription()));
            }
            
            // Ports
            Ports ports = node.addNewPorts();
            int index = 0;
            for (Port p : cfg.getInputPorts()) {
                InPort ip = ports.addNewInPort();
                ip.setIndex(new BigInteger(Integer.toString(index++)));
                String mimetypes = mimetypes2String(p.getMimeTypes());
                ip.setName(p.getName() + mimetypes);
                ip.getDomNode().appendChild(domDoc.createTextNode(p.getDescription() + mimetypes));
            }
            
            index = 0;
            for (Port p : cfg.getOutputPorts()) {
                OutPort op = ports.addNewOutPort();
                op.setIndex(new BigInteger(Integer.toString(index++)));
                String mimetypes = mimetypes2String(p.getMimeTypes());
                op.setName(p.getName() + mimetypes);
                op.getDomNode().appendChild(domDoc.createTextNode(p.getDescription() + mimetypes));
            }
            
            return new NodeDescription28Proxy(doc);
        } catch (Exception e) {
            logger.error("Dynamic node description instantiation failed", e);
        }
        return null;
    }
    
    private INodeConfiguration getNodeConfiguration()
            throws InvalidCTDFileException, FileNotFoundException, IOException {
        if (m_config == null) {
            try(InputStream cfgStream = getConfigAsStream()) {
                m_config = new CTDConfigurationReader().read(cfgStream);
            }
        }
        return m_config;
    }
    
    private InputStream getConfigAsStream() throws FileNotFoundException { 
        return new FileInputStream(DynamicGenericNodeSetFactory.resolveSourceFile(getClass(), m_ctdFile));
    }
    
    private String mimetypes2String(List<String> mt) {
        StringBuilder mimetypes = new StringBuilder().append(" [");
        for (int i = 0; i < mt.size(); i++) {
            if (i != 0) {
                mimetypes.append(", ");
            }
            mimetypes.append(mt.get(i));
        }
        mimetypes.append("]");
        return mimetypes.toString();
    }
   
}
