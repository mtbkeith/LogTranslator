package cz.muni.fi.ngmon.logtranslator.translator;

import cz.muni.fi.ngmon.logtranslator.antlr.JavaBaseListener;
import cz.muni.fi.ngmon.logtranslator.antlr.JavaParser;
import cz.muni.fi.ngmon.logtranslator.common.Log;
import cz.muni.fi.ngmon.logtranslator.common.LogFile;
import cz.muni.fi.ngmon.logtranslator.common.Utils;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;


public class LogTranslator extends JavaBaseListener {
    //    BufferedTokenStream bufferedTokens; // intended to be used with multiple channels for handling WHITESPACES and COMMENTS
    static LoggerLoader loggerLoader = null;
    LogFile var;
    TokenStreamRewriter rewriter;
    private int currentLine = 0;
    private String logName = null; // reference to original LOG variable name
    private String logType = null; // reference to original LOG variable type


    public LogTranslator(BufferedTokenStream tokens, String filename) {
        rewriter = new TokenStreamRewriter(tokens);
        var = new LogFile();
        var.setFileName(filename);
//        rewriter.getTokenStream();
//        List<Token> cmtChannel = tokens.getHiddenTokensToRight(0, 1);
    }

    public static LoggerLoader getLoggerLoader() {
        return loggerLoader;
    }

    public TokenStreamRewriter getRewriter() {
        return rewriter;
    }

    public String getLogType() {
        if (logType == null) {
            return null;
        } else {
            // return only last part of QN
            return logType.substring(logType.lastIndexOf(".") + 1);
        }
    }

    private void storeVariable(ParserRuleContext ctx, String variableName, String variableTypeName, boolean isField) {
        checkAndStoreVariable(variableName, variableTypeName, ctx.start.getLine(),
                ctx.getStart().getCharPositionInLine(), ctx.getStop().getCharPositionInLine(),
                ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex(), isField);
    }

    void checkAndStoreVariable(String variableName, String variableType, int lineNumber,
                               int lineStartPosition, int lineStopPosition, int startPosition, int stopPosition, boolean isField) {
        LogFile.Variable p = var.new Variable();

        if (variableName == null || variableType == null) {
            throw new NullPointerException("Variable name or type are null!");
        } else {
            p.setName(variableName);
            p.setType(variableType);
        }

        p.setLineNumber(lineNumber);
        p.setStartPosition(lineStartPosition);
        p.setStopPosition(lineStopPosition);
        p.setFileStartPosition(startPosition);
        p.setFileStopPosition(stopPosition);
        p.setField(isField);
        var.putVariableList(variableName, p);
    }


    /**
     * enterEveryRule is executed always before enterAnyRule (first).
     * exitEveryRule is executed always after exitAnyRule (last).
     */
    @Override
    public void enterEveryRule(@NotNull ParserRuleContext ctx) {
        currentLine = ctx.getStart().getLine();
    }

    @Override
    public void exitEveryRule(@NotNull ParserRuleContext ctx) {
        // exitEvery rule is always before exitX visit ?? ou really?
    }

    @Override
    public void exitCompilationUnit(@NotNull JavaParser.CompilationUnitContext ctx) {
//        Map<String, LogFile.Variable> map = var.getVariableList();
//        for (String key : map.keySet()) {
//            System.out.println(key + " " + map.get(key));
//        }
        // do cleanUp()
        LoggerFactory.setActualLoggingFramework(null);
        loggerLoader = null;
    }

    // ------------------------------------------------------------------------
    @Override
    public void enterQualifiedName(@NotNull JavaParser.QualifiedNameContext ctx) {
        if (ctx.getParent().getClass() == JavaParser.ImportDeclarationContext.class) {
            // Determine actual logging framework
            if (LoggerFactory.getActualLoggingFramework() == null) {
                loggerLoader = LoggerFactory.determineLoggingFramework(ctx.getText());

                if (loggerLoader == null) {
                    // this is not log import, we can safely skip it
                    return;
                }
            }
            // Change logger factory
            if (ctx.getText().toLowerCase().contains(loggerLoader.getLogFactory().toLowerCase())) {
                System.out.println("logfactory=" + ctx.getText());
                rewriter.replace(ctx.getStart(), ctx.getStop(), Utils.getNgmonLogFactoryImport());
            }
            // Change logger and add namespace, logGlobal imports
            for (String logImport : loggerLoader.getLogger()) {
                if (ctx.getText().toLowerCase().equals(logImport.toLowerCase())) {
                    if (getLogType() == null) {
                        logType = ctx.getText();
                        System.out.println("log=" + logType);
                    }

                    String namespaceImport = "import " + ANTLRRunner.getCurrentFileInfo().getNamespace() +
                            "." + ANTLRRunner.getCurrentFileInfo().getNamespaceEnd() + "Namespace";
                    String logGlobalImport = "import " + Utils.getNgmonLogGlobal();
                    // Change Log import with Ngmon Log, currentNameSpace and LogGlobal imports
                    rewriter.replace(ctx.start, ctx.stop, Utils.getNgmonLogImport() + ";\n"
                            + namespaceImport + "\n" + logGlobalImport);

                    ANTLRRunner.getCurrentFileInfo().setNamespaceClass(
                            ANTLRRunner.getCurrentFileInfo().getNamespaceEnd() + "Namespace");
                }
            }
        }
    }

    @Override
    public void exitFieldDeclaration(@NotNull JavaParser.FieldDeclarationContext ctx) {
        // Logger LOG = LogFactory.getLog(TestingClass.class);  ->
        // private static final XNamespace LOG = LoggerFactory.getLogger(XNamespace.class);
        String varName = ctx.variableDeclarators().variableDeclarator(0).variableDeclaratorId().getText();

        // TODO: log names should be in some dictionary form no "log" only
        // Test for equality of Log variable name and type
        if ((varName.toLowerCase().contains("log")) && ctx.type().getText().equals(getLogType())) {
            // store LOG variableName for further easier searching assistance
            if (logName == null) {
                logName = varName;
            }

            String logFieldDeclaration = ANTLRRunner.getCurrentFileInfo().getNamespaceClass() +
                    " LOG = LoggerFactory.getLogger(" + ANTLRRunner.getCurrentFileInfo().getNamespaceClass() + ".class);";
//            System.out.println("replacing " + ctx.getStart() + ctx.getText() + " with " + logFieldDeclaration);
            rewriter.replace(ctx.getStart(), ctx.getStop(), logFieldDeclaration);

        } else {
            // It is not LOG variable, so let's store information about it for further log transformations
            if (ctx.variableDeclarators().variableDeclarator().size() == 1) {
                storeVariable(ctx, varName, ctx.type().getText(), true);
//                checkAndStoreVariable(varName, ctx.type().getText(), ctx.start.getLine(),
//                        ctx.getStart().getCharPositionInLine(), ctx.getStop().getCharPositionInLine(),
//                        ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());
            } else {
                // Let's hope there are no 2 loggers defined on same line - should be impossible as well
                System.err.println("exitFieldDeclaration variableDeclarator().size() > 1!\n");
            }
        }
    }

    @Override
    public void exitLocalVariableDeclaration(@NotNull JavaParser.LocalVariableDeclarationContext ctx) {
        String varType = ctx.type().getText();//null;
        String[] variables = null;
//        varType =
        if (ctx.variableDeclarators().variableDeclarator().size() == 1) {
            variables = new String[]{ctx.variableDeclarators().variableDeclarator(0).variableDeclaratorId().getText()};
        } else {
            // Multiple variables are defined on one line. Ugly.. handle.
            variables = new String[ctx.variableDeclarators().getChildCount()];

            for (int i = 0; i < variables.length; i++) {
                variables[i] = ctx.variableDeclarators().getChild(i).getText();
            }
        }

        for (String varName : variables) {
            storeVariable(ctx, varName, varType, false);
//            checkAndStoreVariable(varName, varType, ctx.start.getLine(),
//                    ctx.getStart().getCharPositionInLine(), ctx.getStop().getCharPositionInLine(),
//                    ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex());
        }
    }

    @Override
    public void exitConstantDeclarator(@NotNull JavaParser.ConstantDeclaratorContext ctx) {
        // Should change log definition and store variable as well,
        // but it seems like this construction is not used at all.
        System.err.println("constant!" + ctx.getText());
        System.exit(100);
        // Maybe not used at all?!
    }

    @Override
    public void exitConstDeclaration(@NotNull JavaParser.ConstDeclarationContext ctx) {
        // This is not duplicate!
        // Should change log definition and store variable as well,
        // but it seems like this construction is not used at all.
        System.err.println("constDec=" + ctx.getText() + " in file=" + var.getFileName());
        System.exit(100);
    }

    @Override
    public void enterCatchClause(@NotNull JavaParser.CatchClauseContext ctx) {
        // Store exception into variable list
        String errorVarName = null;
        String errorTypeName;

        if (ctx.getChild(ctx.getChildCount() - 3) != null) {
            errorVarName = ctx.getChild(ctx.getChildCount() - 3).getText();
        }

        // Check for simple 'catch (Exception e)' or
        // multi-exception 'catch (NullPointerException | IllegalArgumentException | IOException ex)' usage
        if (ctx.catchType().getChildCount() == 1) {
//            System.out.println(ctx.getChild(2).getText() + " " + ctx.getChild(3).getText());
            errorTypeName = ctx.getChild(2).getText();
        } else {
            // Store Exception as variable type name (as we can not tell which exception has higher priority)
            errorTypeName = "Exception";
        }
        storeVariable(ctx, errorVarName, errorTypeName, false);
    }

    @Override
    public void exitBlockStatement(@NotNull JavaParser.BlockStatementContext ctx) {
        // Translate "if (LOG.isXEnabled())" statement to "if (LogGlobal.isXEnabled())"
        if ((ctx.statement() != null) && (ctx.statement().getChildCount() > 0)) {
            if (ctx.statement().getChild(0).getText().toLowerCase().equals("if")) {
                JavaParser.ExpressionContext exp = ctx.statement().parExpression().expression();
                if (exp.getText().startsWith(logName + ".")) {
                    // Check if Log call is in current checkerLogMethods() 'isXEnabled()'
                    ParseTree methodCall = exp.expression(0).getChild(exp.expression(0).getChildCount() - 1);
                    if (loggerLoader.getCheckerLogMethods().contains(methodCall.getText())) {
//                    if (exp.expression(0).getChild(exp.expression(0).getChildCount() - 1).getText().matches("is.*Enabled")) {
                        // Now we can safely replace logName by LogGlobal
                        JavaParser.ExpressionContext log = exp.expression(0).expression(0);
                        rewriter.replace(log.start, log.stop,
                                Utils.getQualifiedNameEnd(Utils.getNgmonLogGlobal()));
                    } else {
                        System.err.println("Not implemented translation of log call! " +
                                "Don't know what to do with '" + exp.getText() + "'.");
                    }
                }
            }
        }
    }

    @Override
    public void exitStatementExpression(@NotNull JavaParser.StatementExpressionContext ctx) {
        // Process LOG.XYZ(stuff);
        if (logName == null) {
            System.err.println("Unable to change log calls, when log factory has not been defined! Error. Exitting");
            System.exit(10);
        }
        if (ctx.getText().startsWith(logName + ".")) {
            System.out.println("exitStmnt     = " + ctx.getText() + " " + ctx.expression().getChildCount());
            if ((ctx.expression().expression(0) != null) && (ctx.expression().expression(0).getChildCount() == 3)) {
                // Get "XYZ" Log call into methodCall
                ParseTree methodCall = ctx.expression().expression(0).getChild(2);

                // if Log.operation is in currentLoggerMethodList - transform it,
                if (loggerLoader.getTranslateLogMethods().contains(methodCall.getText())) {
//                    System.out.println("yes, '" + methodCall +"' is in current logger method list.");

                    Log log = transformMethodStatement(ctx.expression().expressionList());
                    log.generateMethodName();
                    log.setLevel(methodCall.getText());

                    // TODO add transformed method to appropriate XYZNamespace  - create mapping file-variables/methods
                }
                // else throw new exception or add it to methodList?
            }
        }
    }

    /**
     *  Choose how to transform log statement input, based on logging framework
     *  or construction of statement itself. Whether it contains commas, pluses or '{}'.
     *
     *   @param expressionList expressionList statement to be evaluated (method_call)
     */
    private Log transformMethodStatement(JavaParser.ExpressionListContext expressionList) {
//        System.out.println("Transforming " + expressionList.getText());
        Log log = new Log();

        // Handle 'plus' separated log - transformation of 'Log.X("This is " + sparta + "!")'
        // Handle comma separated log statement 'Log.X("this is", sparta)'
        for (JavaParser.ExpressionContext ec : expressionList.expression()) {
            fillCurrentLog(log, ec);
        }

        if (LoggerFactory.getActualLoggingFramework().equals("slf4j") && expressionList.getText().contains("{}")) {
            // TODO implement slf4j framework
            System.out.println(expressionList.getText() + " is special slf4j {}");
            // if (currentFramework == slf4j) then handle 2 types of messages: '"msgs {}", var' and classic '"das" + das + "dsad"';
            // handle {} and "" ?
        }
        return log;
    }

    /**
     * Parse expression data (recursive tree of expressions in LOG.X(expression) call
     */
    private void fillCurrentLog(Log log, JavaParser.ExpressionContext expression) {
        if (expression == null) {
            System.err.println("Expression is null");
            return;
        }
        if (expression.getChildCount() > 1) {
            for (JavaParser.ExpressionContext ec : expression.expression()) {
                fillCurrentLog(log, ec);
            }
        } else {
            if (expression.getText().startsWith("\"")) {
                log.addComment(cultivate(expression.getText()));
            } else {
                LogFile.Variable varProperty = findVariable(expression.getText());
                log.addVariable(varProperty);
            }
        }
    }

    /**
     * Associate input variable with variable from known variables list.
     *
     * @param findMe - variable to find
     */
    private LogFile.Variable findVariable(String findMe) {
        LogFile.Variable foundVar = null;
        for (String key : LogFile.getVariableList().keySet()) {
            if (findMe.equals(key)) {
                List<LogFile.Variable> props = LogFile.getVariableList().get(findMe);
                if (props.size() > 1) {
                    // get closest line number (or field member)
                    int closest = currentLine;
                    for (LogFile.Variable p : props) {
                        if (currentLine - p.getLineNumber() < closest) {
                            closest = currentLine - p.getLineNumber();
                            foundVar = p;
                        }
                    }
                } else {
                    foundVar = props.get(0);
                }
            }
        }

        if (foundVar == null) {
            System.err.println("Unable to find variable " + findMe);
        }
        return foundVar;
    }

    /**
     * Drop quotes, extra spaces, commas, non-alphanum characters
     */
    private String cultivate(String str) {
        str = str.substring(1, str.length() - 1).trim();
        str = str.replaceAll("\\d+", "");   // remove all digits as well?
        str = str.replaceAll("\\W", " ").replaceAll("\\s+", " ");
        return str;
    }

}
