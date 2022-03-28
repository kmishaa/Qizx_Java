package put;
import com.qizx.api.*;
import com.qizx.api.util.XMLSerializer;
import com.qizx.util.basic.FileUtil;
import com.qizx.xdm.DocumentParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class putClass {
    private static void verbose(String message) {
        System.out.println(message);
    }

    private static void put(Library lib,File srcFile, String dstPath) throws IOException, QizxException, SAXException {
        if (srcFile.isDirectory()) {
            Collection collection = lib.getCollection(dstPath);
            if (collection == null) {
                verbose("Creating collection '" + dstPath + "'...");
                collection = lib.createCollection(dstPath);
            }
            File[] files = srcFile.listFiles();
            if (files == null) {
                throw new IOException("cannot list directory '" + srcFile + "'");
            }
            else {
                for (int i = 0; i < files.length; ++i) {
                    File file = files[i];
                    String dstPath2 = dstPath;
                    dstPath = Paths.get(dstPath2, file.getName()).toString();
                    put(lib, file, dstPath);
                }
            }
        }  else {
            verbose("Importing '" + srcFile + "' as document '" + dstPath
                    + "'...");
            XMLReader parser = new DocumentParser().newParser();
            parser.setFeature("http://apache.org/xml/features/xinclude", true);
            lib.importDocument("/server_path", new InputSource(srcFile.getCanonicalPath()), parser);
        }
    }

    private static void shutdown(Library lib, LibraryManager libManager)
            throws QizxException {
        if (lib.isModified()) {
            lib.rollback();
        }
        lib.close();
        libManager.closeAllLibraries(10000 /*ms*/);
    }

    private static File get(LibraryMember libMember, File dstFile)
            throws IOException, QizxException {
        File dstFile2;
        if (dstFile.isDirectory()) {
            String baseName = libMember.getName();
            if ("/".equals(baseName))
                baseName = "root";
            dstFile2 = new File(dstFile, baseName);
        } else {
            dstFile2 = dstFile;
        }
        if (libMember.isCollection()) {
            getCollection((Collection) libMember, dstFile2);
        } else {
            getDocument((Document) libMember, dstFile2);
        }
        return dstFile2;
    }

    private static void getDocument(Document doc, File dstFile) throws IOException, QizxException {
        verbose("Copying document '" + doc.getPath() + "' to file '" + dstFile + "'...");
        FileOutputStream out = new FileOutputStream(dstFile);
        try {
            doc.export(new XMLSerializer(out, "UTF-8"));
        } finally {
            out.close();
        }
    }

    private static void getCollection(Collection col, File dstFile) throws IOException, QizxException {
        verbose("Copying collection '" + col.getPath() + "' to directory '" + dstFile + "'...");
        if (!dstFile.isDirectory()) {
            verbose("Creating directory '" + dstFile + "'...");
            if (!dstFile.mkdirs()) {
                throw new IOException("Cannot create directory '" + dstFile + "'");
            }
        }
        LibraryMemberIterator iter = col.getChildren();
        while (iter.moveToNextMember()) {
            LibraryMember libMember = iter.getCurrentMember();
            File dstFile2 = new File(dstFile, libMember.getName());
            if (libMember.isCollection()) {
                getCollection((Collection) libMember, dstFile2);
            } else {
                getDocument((Document) libMember, dstFile2);
            }
        }
    }

    public static void main(String[] args) throws IOException, QizxException {
    	
    	String xlibraries = "C:\\DB\\qizx-fe-4.4p1\\server\\root\\xlibraries\\LR4";
    	
        LibraryManagerFactory factory = LibraryManagerFactory.getInstance();
        File f = new File(xlibraries);
        LibraryManager libManager;
        if (f.exists()) {
            libManager = factory.openLibraryGroup(f);
        }
        else{
            if (!f.mkdirs())
                throw new IOException("cannot create directory '" + f + "'");
            verbose("Creating library group in '" + f + "'...");
            libManager = factory.createLibraryGroup(f);
        }
        String libname = "lib";
        Library lib = libManager.openLibrary(libname);
        if (lib == null)
        {
            libManager.createLibrary(libname, f);
            lib = libManager.openLibrary(libname);
        }
        String dstPath = "C:\\DB\\JAVA\\etc";
        String file_path = "C:\\DB\\JAVA\\source";
        dstPath = xlibraries;
        Collection M = lib.getCollection(dstPath);
        if (M == null)
        {
            verbose("Creating collection '" + dstPath + "'...");
            M = lib.createCollection(dstPath);
        }
        LibraryMember dst = lib.getMember(dstPath);
        boolean dstIsCollection = (dst != null && dst.isCollection());
        try {
            File srcFile = new File(file_path + "\\books.xml");
            String dstPath2 = dstPath;
            if (dstIsCollection) {
                dstPath2 = Paths.get(file_path, "books.xml").toString();
            }
            put(lib, srcFile, "/server_path");
            
            verbose("Committing changes...");
            lib.commit();
            String path = dstPath;
            LibraryMember libMember = lib.getMember(path);
            if (libMember == null) {
                verbose("dont't find '" + path + "'");
                return;
            }
            File dstFile = new File(path);
            File fil = get(libMember, dstFile);
            Boolean IsQuery = false;
            if (IsQuery) {
            	Expression expr = lib.compileExpression(FileUtil.loadString(file_path + "\\etc\\books1.xq"));
            	ItemSequence results = expr.evaluate();
            	while (results.moveToNextItem()) {
                	Item result = results.getCurrentItem();
                	System.out.println(result.getString());
            	}
            }
            else {
            	Node res = lib.getDocument("server_path").getDocumentNode();
            	XMLSerializer xml = new XMLSerializer();
            	System.out.println("\n\nXML פאיכ:\n\n" + xml.serializeToString(res) + "\n\n");
            }
        } catch (SAXException e) {
            e.printStackTrace();
        } finally {
            shutdown(lib, libManager);
        }
    
    	verbose("End");
    }


}
