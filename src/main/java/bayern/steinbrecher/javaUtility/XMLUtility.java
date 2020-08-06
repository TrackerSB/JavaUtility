package bayern.steinbrecher.javaUtility;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Contains convenient method for handling and checking XML files.
 *
 * @author Stefan Huber
 * @since 0.1
 */
public final class XMLUtility {

    private static final Logger LOGGER = Logger.getLogger(XMLUtility.class.getName());

    private XMLUtility() {
        throw new UnsupportedOperationException("Construction of an object is not allowed.");
    }

    /**
     * Checks whether the given {@link String} contains valid XML based on the given schemas.
     *
     * @param xml The XML content to check.
     * @param schema The XSD schema to validate against.
     * @return An {@link Optional} containing the error output if {@code XML} is erroneous. If {@link Optional#empty()}
     * is returned the XML does not contain errors but it may still contain warnings. If so these are logged.
     * @throws SAXException If any parse error occurs.
     * @throws IOException If any I/O error occurs.
     */
    public static Optional<String> isValidXML(String xml, URL schema) throws SAXException, IOException {
        DocumentBuilderFactory xmlBuilderFactory = DocumentBuilderFactory.newInstance();
        xmlBuilderFactory.setIgnoringComments(true);
        xmlBuilderFactory.setNamespaceAware(true);
        xmlBuilderFactory.setValidating(false);
        DocumentBuilder xmlBuilder;
        try {
            xmlBuilder = xmlBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new AssertionError("The DocumentBuilder used for the XML validation is invalid.", ex);
        }
        Map<String, List<String>> validationProblemsMap = new SupplyingMap<>(key -> new ArrayList<>());
        BooleanProperty isValidXML = new SimpleBooleanProperty(true);
        xmlBuilder.setErrorHandler(new ErrorHandler() {
            private String createLine(SAXParseException exception) {
                return "line: " + exception.getLineNumber() + ": " + exception.getMessage();
            }

            @Override
            public void warning(SAXParseException exception) throws SAXException {
                validationProblemsMap.get("warning").add(createLine(exception));
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                validationProblemsMap.get("error").add(createLine(exception));
                isValidXML.set(false);
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                validationProblemsMap.get("fatalError").add(createLine(exception));
                isValidXML.set(false);
            }
        });
        Document xmlDocument = xmlBuilder.parse(new InputSource(new StringReader(xml)));
        //NOTE The validator is created outside the following try-catch to distinguish sources of SAXExceptions
        Validator validator = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(schema)
                .newValidator();
        try {
            validator.validate(new DOMSource(xmlDocument.getFirstChild()));
        } catch (SAXException ex) {
            /*
             * NOTE: When a fatal error occurs some implementations may or may not continue evaluation.
             * (See {@link ErrorHandler#fatalError(SAXParseException)})
             */
            validationProblemsMap.get("fatalError (discontinue)").add(ex.getMessage());
            isValidXML.set(false);
        }

        String validationOutput = validationProblemsMap.entrySet()
                .stream()
                .sorted((entryA, entryB) -> entryA.getKey().compareTo(entryB.getKey()))
                .flatMap(entry -> entry.getValue().stream().map(cause -> entry.getKey() + ": " + cause))
                .collect(Collectors.joining("\n"));
        Optional<String> validationResult;
        if (isValidXML.get()) {
            if (!validationOutput.isEmpty()) {
                LOGGER.log(Level.WARNING, validationOutput);
            }
            validationResult = Optional.empty();
        } else {
            validationResult = Optional.of(validationOutput);
        }
        return validationResult;
    }
}
