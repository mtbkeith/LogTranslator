package org.ngmon.logger.logtranslator.generator;

import org.ngmon.logger.logtranslator.common.Utils;
import org.stringtemplate.v4.ST;

/**
 * Class generates SimpleLogger java file, which is used partly as bridging
 * class between Log4j(2) and NGMON logging framework, and for logging
 * NGMON's method calls.
 */
public class SimpleLoggerGenerator {

    private static final String sep = Utils.sep;
    private static final String path = Utils.getLogTranslatorGeneratedProject() + "src" + sep + "main" + sep + "java" + sep +
        Utils.getNgmonSimpleLoggerImport().replace(".", sep) + ".java";


    public static void create() {
        String SIMPLE_LOGGER_TEMPLATE = "package <packageNamespace>;\n\n" +
            "import org.apache.logging.log4j.LogManager;\n" +
            "import <loggerImport>;\n" +
            "import <jsonerImport>;\n" +
            "import java.util.List;\n\n" +
            "public class SimpleLogger implements Logger {\n\n" +
            "    private org.apache.logging.log4j.Logger log = LogManager.getLogger(\"Log4jLogger\");\n\n" +
            "    @Override\n" +
            "    public void log() {\n" +
            "    }\n\n" +
            "    public void log(String fqnNS, String methodName, List\\<String> tags, String[] paramNames, Object[] paramValues, int level) {\n" +
            "        log.debug(JSONer.getEventJson(fqnNS, methodName, tags, paramNames, paramValues, level));\n" +
            "    }\n" +
            "}\n";
        ST simpleLoggerFile = new ST(SIMPLE_LOGGER_TEMPLATE);
        String namespace = Utils.getNgmonSimpleLoggerImport();
        namespace = namespace.substring(0, namespace.lastIndexOf("."));
        simpleLoggerFile.add("packageNamespace", namespace);
        simpleLoggerFile.add("loggerImport", Utils.getNgmonLogImport());
        simpleLoggerFile.add("jsonerImport", Utils.getNgmonJsonerImport());

        FileCreator.createDirectory(FileCreator.createPathFromString(path.substring(0, path.lastIndexOf(sep))));
        FileCreator.createFile(FileCreator.createPathFromString(path), simpleLoggerFile.render());
    }

    public static String getPath() {
        return path;
    }
}
