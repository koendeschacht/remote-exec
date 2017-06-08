package be.bagofwords.exec;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by koen on 5/05/17.
 */
public class RemoteClassAnnotationPreprocessor extends AbstractProcessor {

    private String[] possiblePrefixes = {"src/main/java/", "", "java/"};
    private String[] resourcesDirNames = {"resources", "assets"};

    private Trees trees;

    public RemoteClassAnnotationPreprocessor() {
        System.out.println("Generated " + getClass());
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        trees = Trees.instance(env);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        Set<? extends Element> elementsAnnotatedWith = env.getElementsAnnotatedWith(RemoteClass.class);
        for (Element element : elementsAnnotatedWith) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Found Remote class " + element);
            final TreePath treePath = trees.getPath(element);
            try {
                String path = treePath.getCompilationUnit().getSourceFile().getName();
                File resourcesDir = null;
                String simplifiedPath = null;
                for (int i = 0; i < possiblePrefixes.length && resourcesDir == null; i++) {
                    for (int j = 0; j < resourcesDirNames.length && resourcesDir == null; j++) {
                        String prefix = possiblePrefixes[i];
                        String resourceDirName = resourcesDirNames[j];
                        String parentDir = path.replaceFirst(prefix + ".*$", "");
                        resourcesDir = new File(parentDir, resourceDirName);
                        // System.out.println("Resources dir " + resourcesDir.getAbsolutePath() + " " + resourcesDir.exists());
                        if (!resourcesDir.exists()) {
                            resourcesDir = null;
                        } else {
                            int prefixInd = path.indexOf(prefix);
                            simplifiedPath = path.substring(prefixInd + prefix.length());
                            break;
                        }
                    }
                }
                CharSequence source = treePath.getCompilationUnit().getSourceFile().getCharContent(false);
                if (resourcesDir != null) {
                    File remoteClassesDir = new File(resourcesDir, "remote-exec");
                    File outputFile = new File(remoteClassesDir, simplifiedPath + ".remote");
                    File parentDir = outputFile.getParentFile();
                    if (!parentDir.exists() && !parentDir.mkdirs()) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not generate directory " + parentDir.getAbsolutePath());
                    }
                    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                    writer.write(source.toString());
                    writer.close();
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Could not find resources dir for " + element);
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "IOException while handling " + element + " : " + e.getMessage());
            }
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Collections.singletonList(RemoteClass.class.getName()));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

}
