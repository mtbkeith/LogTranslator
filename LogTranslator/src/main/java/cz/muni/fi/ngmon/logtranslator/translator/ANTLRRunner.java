package cz.muni.fi.ngmon.logtranslator.translator;

import cz.muni.fi.ngmon.logtranslator.antlr.JavaLexer;
import cz.muni.fi.ngmon.logtranslator.antlr.JavaParser;
import cz.muni.fi.ngmon.logtranslator.common.LogFile;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class ANTLRRunner {
    static CommonTokenStream tokens;
    private static LogFile currentFile;
//    public static CommonTokenStream getTokens() {
//        return tokens;
//    }

    public static void run(LogFile logFile) {
        currentFile = logFile;
        String file = logFile.getFilepath();
        InputStream antlrInputStream;

        try {
            // -- ANTLR part --
            antlrInputStream = new FileInputStream(file);
            ANTLRInputStream ais = new ANTLRInputStream(antlrInputStream);
            JavaLexer lexer = new JavaLexer(ais);
            tokens = new CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokens);
//            ParseTree tree = parser.compilationUnit();  -- difference??
            RuleContext tree = parser.compilationUnit();

//            System.out.println("ParseTree = " + tree.getText());
            ParseTreeWalker walker = new ParseTreeWalker();
            LogTranslator listener = new LogTranslator(tokens, logFile);
            walker.walk(listener, tree);

//            System.out.println("modified=\n" + listener.getRewriter().getText());


        } catch (IOException e){
            System.err.println("Unable to handle file=" + e.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static LogFile getCurrentFile() {
        return currentFile;
    }
}
