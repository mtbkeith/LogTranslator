package org.ngmon.logger.logtranslator.generator;

import org.ngmon.logger.logtranslator.common.Log;
import org.ngmon.logger.logtranslator.common.LogFile;
import org.ngmon.logger.logtranslator.common.Utils;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class HelperGenerator {


    /**
     * Generate few namespaces for this logging application. Resolve number of namespaces
     * for this app based on applicationNamespaceLength property and set them to LogFiles.
     *
     * @param logFileList input list of logFiles, which contain only filepath and package qualified name.
     * @return same list of logFiles, but each of them has filled appropriate namespace.
     */
    public static List<LogFile> generateNamespaces(List<LogFile> logFileList) {
        Set<String> namespaceSet = new TreeSet<>();
//      TODO trace()  System.out.println("appnamespaceLength=" + Utils.getApplicationNamespaceLength());
        for (LogFile lf : logFileList) {
//            System.out.println("packageName=" + lf.getPackageName());
            if (lf.getPackageName() == null) {
                System.err.println("null packageName in file " + lf.getFilepath());
            }
            String namespace = createNamespace(lf.getPackageName());
            namespaceSet.add(namespace);
            lf.setNamespace(namespace);
        }

        StringBuilder sb = new StringBuilder();
        for (String s : namespaceSet) {
            sb.append(s).append("\n");
        }
//        System.out.println("namespaceSet=" + sb.toString());
        return logFileList;
    }

    /**
     * Create NGMON log namespace which will contain all calls for this logs.
     * This method sets granularity level of NGMON log messages.
     * If original packageName length is longer then applicationNamespaceLength
     * property, make it shorter.
     *
     * @param packageName string to change
     * @return shortened packageName from NGMON length rules
     */
    private static String createNamespace(String packageName) {
        int numberOfDots = Utils.numberOfDotsInText(packageName);

        if (numberOfDots < Utils.getApplicationNamespaceLength()) {
            return packageName;
        } else {
            StringBuilder newPackageName = new StringBuilder();
            String[] pckgs = packageName.split("\\.", Utils.getApplicationNamespaceLength() + 1);
            pckgs[pckgs.length - 1] = "";
            for (String p : pckgs) {
                if (!p.equals("")) newPackageName.append(p).append(".");
            }
            // remove last extra dot
            newPackageName.deleteCharAt(newPackageName.length() - 1);
            return newPackageName.toString();
        }
    }

    /**
     * Generate method nam from 'comments' list - strings found in given log method call.
     * If comment list is empty, use autogenerated method name from property file.
     *
     * @param log to generate and set method log from
     */
    public static void generateMethodName(Log log) {
        if (log.getComments().size() == 0) {
            // TODO - maybe use some quick dirty hack (impossible imo)
            StringBuilder tempName = new StringBuilder();
            for (LogFile.Variable var : log.getVariables()) {
                tempName.append(var.getNgmonName());
            }

            log.setMethodName(tempName.substring(0, Utils.getNgmonEmptyLogStatementMethodNameLength()) + Utils.getNgmonEmptyLogStatement());
        } else {
            StringBuilder logName = new StringBuilder();
            int counter = 0;
            int logNameLength = Utils.getNgmonLogLength();

            for (String comment : log.getComments()) {
                for (String str : comment.split(" ")) {
                    if (counter != 0) {
                        logName.append("_");
                    }
                    if (!Utils.BANNED_LIST.contains(str)) logName.append(str);
                    counter++;
                    if (counter >= logNameLength) break;
                }
            }
            log.setMethodName(logName.toString());
        }
    }


    /**
     * Generate new log method call which will be replaced by 'original' log method call.
     * This new log method will use NGMON logger. Which is goal of this mini-application.
     *
     * @param log current log to get information from
     * @return log method calling in NGMON's syntax form
     */
    public static String generateLogMethod(String logName, Log log) {
        // TODO/wish - if line is longer then 80 chars, append to newline!
        if (log != null) {
            // generate variables
            StringBuilder vars = new StringBuilder();
            StringBuilder tags = new StringBuilder();
            for (LogFile.Variable var : log.getVariables()) {
                vars.append(var.getName());
                if (!var.equals(log.getVariables().get(log.getVariables().size() - 1))) {
                    vars.append(", ");
                }
            }

            // generate tags
            if (log.getTag() != null) {
                int tagSize = log.getTag().size();
                if (tagSize == 0) {
                    tags = null;
                } else {
                    for (int i = 0; i < tagSize; i++) {
                        tags.append(".tag(\"").append(log.getTag().get(0)).append("\")");
                    }
                }
            }
//            System.out.printf("generating=%s.%s(%s)%s%s;", "LOG", log.getMethodName(), vars, tags, log.getLevel());
            String replacementLog = String.format("%s.%s(%s)%s.%s();", logName, log.getMethodName(), vars, tags, log.getLevel());
            log.setGeneratedReplacementLog(replacementLog);
            return replacementLog;
        } else {
            return null;
        }
    }
}