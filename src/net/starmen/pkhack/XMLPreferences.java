/*
 * Created on Sep 5, 2003
 */
package net.starmen.pkhack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This is a simple XML prefences class. It has no application specific
 * methods. The XML file is validated by preferences.dtd.
 * 
 * @author AnyoneEB
 */
public class XMLPreferences
{
    private Document dom;
    private File f = null;

    /** Filesize of the emptyPrefs.xml file. */
    protected static int EMPTY_PREFS_XML_FILESIZE = 122;
    /** Filesize of the preferences.dtd file. */
    protected static int PREFERENCES_DTD_FILESIZE = 157;

    /**
     * Read preferences in from a file, using the given default values. If
     * there is an IO error reading the file, a new file will be created based
     * on the provided defaults.
     * 
     * @param f File to read from.
     * @param defaults Values to use for unspecified values indexed by Strings.
     */
    public XMLPreferences(File f, HashMap defaults) {
        this.f = f;

        File preferencesDTD = new File(f.getAbsoluteFile().getParent()
            + File.separatorChar + "preferences.dtd");
        if (!preferencesDTD.exists())
        {
            try
            {
                copy(ClassLoader.getSystemResourceAsStream("preferences.dtd"),
                    new FileOutputStream(preferencesDTD),
                    PREFERENCES_DTD_FILESIZE);
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        //second part of that if is really JHack specific
        if ((dom = parseFile(f)) == null
            && (dom = parseFile(new File(f.getName()))) == null)
        {
            try
            {
                copy(ClassLoader.getSystemResourceAsStream("emptyPrefs.xml"),
                    new FileOutputStream(f), EMPTY_PREFS_XML_FILESIZE);
                copy(ClassLoader.getSystemResourceAsStream("preferences.dtd"),
                    new FileOutputStream(new File(f.getAbsoluteFile()
                        .getParent()
                        + File.separatorChar + "preferences.dtd")),
                    PREFERENCES_DTD_FILESIZE);
            }
            catch (FileNotFoundException e)
            {
                System.out
                    .println("Some idiot didn't include emptyPrefs.xml or preferences.dtd in the .jar.");
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            dom = parseFile(f);
            Object[] defs = defaults.entrySet().toArray();
            for (int i = 0; i < defs.length; i++)
            {
                String[] s = defs[i].toString().split("=");
                setValue(s[0], s[1]);
            }
        }
        else
        {
            Object[] defs = defaults.entrySet().toArray();
            for (int i = 0; i < defs.length; i++)
            {
                String[] s = defs[i].toString().split("=");
                if (getValue(s[0]) == null) setValue(s[0], s[1]);
            }
        }
    }
    
    private String correctName(String name)
    {
        name = name.replace(' ', '_');
        name = name.replace('\'', '_');
        name = name.replace('(', '-');
        name = name.replace(')', '-');
        return name;
    }

    /**
     * Returns the specified preference or null if the preference is not set.
     * 
     * @param name Name of preference.
     * @return Value of preference or null
     */
    public String getValue(String name)
    {
        name = correctName(name);
        Element e = this.getElementById(name);
        if (e != null)
            return e.getAttribute("value");
        else
            return null;
    }

    /**
     * Returns if this contains the specified preference.
     * 
     * @param name Name of preference.
     * @return <code>true</code> if preference set or <code>false</code> if
     *         preference not set
     */
    public boolean hasValue(String name)
    {
        return this.getElementById(name) != null;
    }

    private Element getElementById(String id)
    {
        NodeList nl = dom.getElementsByTagName("preference");
        for (int i = 0; i < nl.getLength(); i++)
        {
            NamedNodeMap nnm = nl.item(i).getAttributes();
            if (nnm.getNamedItem("id").getNodeValue().equals(id)) { return (Element) nl
                .item(i); }
        }
        return null;
    }

    /**
     * Sets the specified preference to the specified value. Either changes the
     * value on an old preference of the same name or creates a new preference.
     * If either is null they are set to the <code>String</code> "null".
     * 
     * @param name Name of preference.
     * @param value Value of preference.
     */
    public void setValue(String name, String value)
    {
        if (name == null) name = "null";
        if (value == null) value = "null";
        name = correctName(name);
        Element e;
        if ((e = getElementById(name)) == null)
        {
            dom.getDocumentElement().appendChild(
                e = dom.createElement("preference"));
            e.setAttribute("id", name);
        }
        e.setAttribute("value", value);
    }

    /**
     * Returns the specified preference or null if the preference is not set.
     * 
     * @see #getValue(String)
     * @param name Name of preference.
     * @return Value of preference as an <code>int</code> or null
     */
    public int getValueAsInteger(String name)
    {
        return Integer.parseInt(getValue(name));
    }

    /**
     * Sets the specified preference to the specified value. Either changes the
     * value on an old preference of the same name or creates a new preference.
     * 
     * @param name Name of preference.
     * @param value int value of preference.
     */
    public void setValueAsInteger(String name, int value)
    {
        setValue(name, Integer.toString(value));
    }

    /**
     * Returns the specified preference or null if the preference is not set.
     * 
     * @see #getValue(String)
     * @param name Name of preference.
     * @return Value of preference as a <code>boolean</code> or null
     */
    public boolean getValueAsBoolean(String name)
    {
        return new Boolean(getValue(name)).booleanValue();
    }

    /**
     * Sets the specified preference to the specified value. Either changes the
     * value on an old preference of the same name or creates a new preference.
     * 
     * @param name Name of preference.
     * @param value boolean value of preference.
     */
    public void setValueAsBoolean(String name, boolean value)
    {
        setValue(name, Boolean.toString(value));
    }

    /**
     * Removes the specified preference.
     * 
     * @param name Name of preference.
     * @return true if preference deleted, false if not found
     */
    public boolean removeValue(String name)
    {
        name = correctName(name);
        Element e;
        if ((e = getElementById(name)) != null)
        {
            dom.getDocumentElement().removeChild(e);
            return true;
        }
        return false;
    }

    /**
     * Saves preferences to the specified file.
     * 
     * @param f File to save to.
     * @return true on success, false on failure.
     */
    public boolean save(File f)
    {
        dom.getDocumentElement().normalize();

        try
        {
            // Use a Transformer for output
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            String systemValue = (new File(dom.getDoctype().getSystemId()))
                .getName();
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                systemValue);

            DOMSource source = new DOMSource(dom);
            StreamResult result = new StreamResult(new FileOutputStream(f));
            transformer.transform(source, result);
            return true;
        }
        catch (TransformerConfigurationException tce)
        {
            // Error generated by the parser
            System.out.println("\n** Transformer Factory error");
            System.out.println("   " + tce.getMessage());

            // Use the contained exception, if any
            Throwable x = tce;
            if (tce.getException() != null) x = tce.getException();
            x.printStackTrace();

        }
        catch (TransformerException te)
        {
            // Error generated by the parser
            System.out.println("\n** Transformation error");
            System.out.println("   " + te.getMessage());

            // Use the contained exception, if any
            Throwable x = te;
            if (te.getException() != null) x = te.getException();
            x.printStackTrace();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * {@link #save(File)}'s to the file loaded from.
     * 
     * @return true on success, false on failure.
     */
    public boolean save()
    {
        return save(f);
    }

    private static Document parseFile(File f)
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        //factory.setNamespaceAware(true);
        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(f);

        }
        catch (SAXException sxe)
        {
            // Error generated during parsing)
            Exception x = sxe;
            if (sxe.getException() != null) x = sxe.getException();
            x.printStackTrace();
            return null;
        }
        catch (ParserConfigurationException pce)
        {
            // Parser with specified options can't be built
            pce.printStackTrace();
            return null;
        }
        catch (FileNotFoundException e)
        {
            System.out.println("File " + f.toString() + " not found. "
                + "(This message is normal on the first run of this program.)");
            return null;
        }
        catch (IOException ioe)
        {
            // I/O error
            ioe.printStackTrace();
            return null;
        }
    }

    private void copy(InputStream source, FileOutputStream dest, long bytes)
        throws IOException
    {
        byte[] b = new byte[(int) bytes];
        source.read(b);
        dest.write(b);
        dest.close();
    }
}
